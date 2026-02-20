const MOJANG_PROFILES_URL = 'https://api.mojang.com/profiles/minecraft'
const GEYSER_UUID_URL = 'https://api.geysermc.org/v2/utils/uuid/bedrock_or_java'
const GEYSER_PREFIX_ENV = 'GAIA_GEYSER_PREFIX'
const DEFAULT_GEYSER_PREFIX = '.'
const GEYSER_RATE_LIMIT_MESSAGE = 'geyser api rate limited or unavailable'
const GEYSER_API_ERROR_PREFIX = 'geyser api error: '
const RESOLVE_LOG_TAG = 'gaia username resolver'
const RESOLVE_LOG_FAILED_BATCH = 'failed to resolve username batch'
const RESOLVE_LOG_FAILED_PROFILE = 'failed to resolve username'
const USERNAME_BATCH_SIZE = 10
const CACHE_TTL_MS = 60 * 60 * 1000
const MAX_RETRY_ATTEMPTS = 4
const RETRY_DELAYS_MS = [250, 750, 1500, 3000]

export type ResolvedProfile = {
    uuid: string
    name: string
}

type CacheEntry = {
    profile: ResolvedProfile
    expiresAt: number
}

type ResolveResult = {
    resolved: Map<string, ResolvedProfile>
    unresolved: string[]
}

type GeyserProfilePayload = {
    id?: string
    name?: string
}

class RetryableError extends Error {
    public readonly status: number

    public constructor(message: string, status: number) {
        super(message)
        this.status = status
    }
}

const cache = new Map<string, CacheEntry>()

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))

const normalizeUsername = (username: string) => username.trim().toLowerCase()
const geyserPrefix = process.env[GEYSER_PREFIX_ENV] ?? DEFAULT_GEYSER_PREFIX
const isBedrockUsername = (username: string) => !!geyserPrefix && username.startsWith(geyserPrefix)

const chunkUsernames = (usernames: string[], chunkSize: number) => {
    const chunks: string[][] = []
    for (let i = 0; i < usernames.length; i += chunkSize) {
        chunks.push(usernames.slice(i, i + chunkSize))
    }
    return chunks
}

const fetchProfilesBatch = async (usernames: string[]) => {
    const response = await fetch(MOJANG_PROFILES_URL, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(usernames)
    })

    if (response.status === 429 || response.status >= 500) {
        throw new RetryableError('mojang api rate limited or unavailable', response.status)
    }

    if (!response.ok) {
        throw new Error(`mojang api error: ${response.status}`)
    }

    const payload = (await response.json()) as Array<{ id: string; name: string }>
    return payload.map((entry) => ({
        uuid: entry.id,
        name: entry.name
    }))
}

const fetchGeyserProfile = async (username: string) => {
    const url = new URL(`${GEYSER_UUID_URL}/${encodeURIComponent(username)}`)
    url.searchParams.set('prefix', geyserPrefix)

    const response = await fetch(url)
    if (response.status === 204 || response.status === 404) {
        return null
    }
    if (response.status === 429 || response.status >= 500) {
        throw new RetryableError(GEYSER_RATE_LIMIT_MESSAGE, response.status)
    }
    if (!response.ok) {
        throw new Error(`${GEYSER_API_ERROR_PREFIX}${response.status}`)
    }

    const payload = (await response.json()) as GeyserProfilePayload
    if (!payload.id || !payload.name) {
        return null
    }

    return {
        uuid: payload.id,
        name: payload.name
    }
}

const fetchProfilesWithRetry = async (usernames: string[]) => {
    let lastError: Error | null = null

    for (let attempt = 0; attempt < MAX_RETRY_ATTEMPTS; attempt += 1) {
        try {
            return await fetchProfilesBatch(usernames)
        } catch (error) {
            if (error instanceof RetryableError && attempt < RETRY_DELAYS_MS.length) {
                const retryDelay = RETRY_DELAYS_MS[attempt]
                if (retryDelay !== undefined) {
                    await sleep(retryDelay)
                }
                lastError = error
                continue
            }
            throw error
        }
    }

    if (lastError) {
        throw lastError
    }

    return []
}

const fetchGeyserProfileWithRetry = async (username: string) => {
    let lastError: Error | null = null

    for (let attempt = 0; attempt < MAX_RETRY_ATTEMPTS; attempt += 1) {
        try {
            return await fetchGeyserProfile(username)
        } catch (error) {
            if (error instanceof RetryableError && attempt < RETRY_DELAYS_MS.length) {
                const retryDelay = RETRY_DELAYS_MS[attempt]
                if (retryDelay !== undefined) {
                    await sleep(retryDelay)
                }
                lastError = error
                continue
            }
            throw error
        }
    }

    if (lastError) {
        throw lastError
    }

    return null
}

const collectUniqueUsernames = (usernames: string[]) => {
    const uniqueNames = new Map<string, string>()
    for (const username of usernames) {
        if (!username) {
            continue
        }
        const normalized = normalizeUsername(username)
        if (!normalized || uniqueNames.has(normalized)) {
            continue
        }
        uniqueNames.set(normalized, username.trim())
    }
    return uniqueNames
}

const collectMissingUsernames = (
    uniqueNames: Map<string, string>,
    now: number,
    resolved: Map<string, ResolvedProfile>
) => {
    const missingJava: string[] = []
    const missingBedrock: string[] = []

    for (const [normalized, original] of uniqueNames) {
        const cached = cache.get(normalized)
        if (cached && cached.expiresAt > now) {
            resolved.set(normalized, cached.profile)
            continue
        }

        if (isBedrockUsername(original)) {
            missingBedrock.push(original)
        } else {
            missingJava.push(original)
        }
    }

    return { missingJava, missingBedrock }
}

const resolveJavaUsernames = async (
    usernames: string[],
    now: number,
    resolved: Map<string, ResolvedProfile>,
    unresolved: string[]
) => {
    const batches = chunkUsernames(usernames, USERNAME_BATCH_SIZE)
    for (const batch of batches) {
        let profiles: ResolvedProfile[]
        try {
            profiles = await fetchProfilesWithRetry(batch)
        } catch (error) {
            console.warn(RESOLVE_LOG_TAG, {
                message: RESOLVE_LOG_FAILED_BATCH,
                usernames: batch,
                error: error instanceof Error ? error.message : String(error)
            })
            unresolved.push(...batch)
            continue
        }

        const foundNormalized = new Set<string>()
        for (const profile of profiles) {
            const normalized = normalizeUsername(profile.name)
            cache.set(normalized, {
                profile,
                expiresAt: now + CACHE_TTL_MS
            })
            foundNormalized.add(normalized)
            resolved.set(normalized, profile)
        }

        for (const name of batch) {
            const normalized = normalizeUsername(name)
            if (!foundNormalized.has(normalized)) {
                unresolved.push(name)
            }
        }
    }
}

const resolveBedrockUsernames = async (
    usernames: string[],
    now: number,
    resolved: Map<string, ResolvedProfile>,
    unresolved: string[]
) => {
    for (const name of usernames) {
        let profile: ResolvedProfile | null
        try {
            profile = await fetchGeyserProfileWithRetry(name)
        } catch (error) {
            console.warn(RESOLVE_LOG_TAG, {
                message: RESOLVE_LOG_FAILED_PROFILE,
                username: name,
                error: error instanceof Error ? error.message : String(error)
            })
            unresolved.push(name)
            continue
        }

        if (!profile) {
            unresolved.push(name)
            continue
        }

        const normalized = normalizeUsername(name)
        cache.set(normalized, {
            profile,
            expiresAt: now + CACHE_TTL_MS
        })
        resolved.set(normalized, profile)
    }
}

/**
 * resolves java and bedrock usernames to profile data, using mojang and geyser as needed.
 */
export const resolveUsernames = async (usernames: string[]): Promise<ResolveResult> => {
    const resolved = new Map<string, ResolvedProfile>()
    const unresolved: string[] = []
    const now = Date.now()

    const uniqueNames = collectUniqueUsernames(usernames)
    const { missingJava, missingBedrock } = collectMissingUsernames(uniqueNames, now, resolved)

    if (missingJava.length > 0) {
        await resolveJavaUsernames(missingJava, now, resolved, unresolved)
    }

    if (missingBedrock.length > 0) {
        await resolveBedrockUsernames(missingBedrock, now, resolved, unresolved)
    }

    return {
        resolved,
        unresolved
    }
}
