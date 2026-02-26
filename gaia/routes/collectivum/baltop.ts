import { Elysia } from 'elysia'
import { Prisma } from '../../generated/prisma/client.ts'
import { db } from '../../services/db.ts'
import { resolveUsernames } from '../../services/uuidResolver.ts'
import { requireAuth } from '../imperium/auth.ts'

const MAX_ENTRIES = 500
const MAX_USERNAME_LENGTH = 30
const MOD_VERSION_PREFIX = 'LifestealUtils/'
const UNKNOWN_MOD_VERSION = 'unknown'
const CURRENCY_TYPE_COINS = 'COINS' as const
const NO_CONTENT_STATUS = 204
const UNAUTHORIZED_STATUS = 401
const BAD_GATEWAY_STATUS = 502
const OK_STATUS = 200
const REQUEST_LOG_TAG = 'gaia collectivum baltop'

type StatusSetter = { status?: number | string }

type BaltopEntryPayload = {
    username: string
    amount: number
}

const badRequest = (set: StatusSetter, message: string) => {
    set.status = 400
    return { success: false, error: message }
}

const handlePrismaError = (error: unknown, set: StatusSetter) => {
    if (error instanceof Prisma.PrismaClientKnownRequestError) {
        if (error.code === 'P2025') {
            set.status = 404
            return { success: false, error: 'Resource not found' }
        }
        if (error.code === 'P2003') {
            set.status = 404
            return { success: false, error: 'Related resource not found' }
        }
        if (error.code === 'P2000') {
            set.status = 400
            return { success: false, error: 'Input value too long' }
        }
    }

    set.status = 500
    return { success: false, error: 'Internal server error' }
}

const extractModVersion = (userAgent: string | null) => {
    if (!userAgent) {
        return UNKNOWN_MOD_VERSION
    }

    const index = userAgent.indexOf(MOD_VERSION_PREFIX)
    if (index < 0) {
        return UNKNOWN_MOD_VERSION
    }

    const start = index + MOD_VERSION_PREFIX.length
    const tail = userAgent.slice(start)
    const version = tail.split(' ')[0]
    return version || UNKNOWN_MOD_VERSION
}

const parseEntries = (payload: unknown, set: StatusSetter) => {
    if (!Array.isArray(payload)) {
        return { error: badRequest(set, 'Payload must be an array'), entries: [] as BaltopEntryPayload[] }
    }

    if (payload.length === 0) {
        return { error: badRequest(set, 'Payload is empty'), entries: [] as BaltopEntryPayload[] }
    }

    if (payload.length > MAX_ENTRIES) {
        return { error: badRequest(set, `Payload exceeds ${MAX_ENTRIES} entries`), entries: [] as BaltopEntryPayload[] }
    }

    const entries: BaltopEntryPayload[] = []
    for (const entry of payload) {
        if (!entry || typeof entry !== 'object') {
            return { error: badRequest(set, 'Invalid entry format'), entries: [] as BaltopEntryPayload[] }
        }

        const username = (entry as BaltopEntryPayload).username
        const amount = (entry as BaltopEntryPayload).amount

        if (!username || typeof username !== 'string') {
            return { error: badRequest(set, 'Invalid username'), entries: [] as BaltopEntryPayload[] }
        }

        const trimmed = username.trim()
        if (!trimmed) {
            return { error: badRequest(set, 'Username cannot be empty'), entries: [] as BaltopEntryPayload[] }
        }

        if (trimmed.length > MAX_USERNAME_LENGTH) {
            return { error: badRequest(set, `Username must be ${MAX_USERNAME_LENGTH} characters or less`), entries: [] as BaltopEntryPayload[] }
        }

        if (typeof amount !== 'number' || !Number.isFinite(amount)) {
            return { error: badRequest(set, 'Invalid amount'), entries: [] as BaltopEntryPayload[] }
        }

        if (!Number.isSafeInteger(amount) || amount < 0) {
            return { error: badRequest(set, 'Amount must be a non-negative safe integer'), entries: [] as BaltopEntryPayload[] }
        }

        entries.push({ username: trimmed, amount })
    }

    return { entries }
}

const normalizeUsername = (username: string) => username.trim().toLowerCase()
const logRequestResult = (status: number, details: Record<string, unknown>) => {
    console.info(REQUEST_LOG_TAG, {
        status,
        ...details
    })
}

const countResolvedEntries = (entries: BaltopEntryPayload[], resolved: Map<string, { uuid: string; name: string }>) => {
    let count = 0
    for (const entry of entries) {
        if (resolved.has(normalizeUsername(entry.username))) {
            count += 1
        }
    }
    return count
}

const buildRecordData = (
    entries: BaltopEntryPayload[],
    resolved: Map<string, { uuid: string; name: string }>,
    submissionId: string
) => {
    return entries.map((entry) => {
        const profile = resolved.get(normalizeUsername(entry.username))
        if (!profile) {
            return null
        }

        return {
            username: profile.name,
            uuid: profile.uuid,
            currencyType: CURRENCY_TYPE_COINS,
            amount: BigInt(entry.amount),
            submissionId
        }
    })
}

export const collectivumRouter = new Elysia({ prefix: '/v1/collectivum' })
    .use(requireAuth)
    .post('/baltop', async ({ body, set, user, request }) => {
        if (!user) {
            set.status = UNAUTHORIZED_STATUS
            logRequestResult(UNAUTHORIZED_STATUS, { reason: 'unauthorized' })
            return { success: false, error: 'Unauthorized' }
        }

        const { entries, error } = parseEntries(body, set)
        if (error) {
            logRequestResult(Number(set.status ?? 400), { reason: 'payload_invalid' })
            return error
        }

        let resolved
        try {
            resolved = await resolveUsernames(entries.map((entry) => entry.username))
        } catch (error) {
            set.status = BAD_GATEWAY_STATUS
            logRequestResult(BAD_GATEWAY_STATUS, { reason: 'username_resolution_failed' })
            return { success: false, error: 'Failed to resolve usernames' }
        }

        const modVersion = extractModVersion(request.headers.get('user-agent'))
        const resolvedCount = countResolvedEntries(entries, resolved.resolved)
        const unresolvedCount = resolved.unresolved.length

        if (resolvedCount === 0) {
            logRequestResult(NO_CONTENT_STATUS, {
                submissionId: null,
                recordCount: 0,
                unresolvedCount
            })
            return new Response(null, { status: NO_CONTENT_STATUS })
        }

        try {
            const result = await db.$transaction(async (tx) => {
                const submission = await tx.currencySubmission.create({
                    data: {
                        submitterName: user.name,
                        submitterUuid: user.uuid,
                        modVersion
                    }
                })

                const recordData = buildRecordData(entries, resolved.resolved, submission.id).filter(
                    (record): record is NonNullable<typeof record> => record !== null
                )

                await tx.currencyRecord.createMany({
                    data: recordData
                })

                return {
                    submissionId: submission.id,
                    recordCount: recordData.length
                }
            })

            if (unresolvedCount > 0) {
                logRequestResult(NO_CONTENT_STATUS, {
                    submissionId: result.submissionId,
                    recordCount: result.recordCount,
                    unresolvedCount
                })
                return new Response(null, { status: NO_CONTENT_STATUS })
            }

            logRequestResult(OK_STATUS, {
                submissionId: result.submissionId,
                recordCount: result.recordCount,
                unresolvedCount
            })

            return {
                success: true,
                submissionId: result.submissionId,
                recordCount: result.recordCount
            }
        } catch (error) {
            const response = handlePrismaError(error, set)
            logRequestResult(Number(set.status ?? 500), { reason: 'database_error' })
            return response
        }
    })
