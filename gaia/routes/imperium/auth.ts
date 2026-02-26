import { Elysia } from 'elysia'
import { createHmac } from 'crypto'

type StatusSetter = { status?: number | string }

const JWT_TTL_SECONDS = 60 * 60 * 24 * 14
const JWT_TTL_MS = JWT_TTL_SECONDS * 1000
const JWT_ALGORITHM = 'HS256'
const JWT_SECRET_ENV = 'GAIA_JWT_SECRET'
const JWT_ISSUER = 'gaia.candycup.dev'

const badRequest = (set: StatusSetter, message: string) => {
    set.status = 400
    return { success: false, error: message }
}

const forbidden = (set: StatusSetter, message: string) => {
    set.status = 403
    return { success: false, error: message }
}

const unauthorized = (set: StatusSetter, message: string) => {
    set.status = 401
    return { success: false, error: message }
}

const serverError = (set: StatusSetter, message: string) => {
    set.status = 500
    return { success: false, error: message }
}

const SESSION_SERVER_URL = 'https://sessionserver.mojang.com/session/minecraft/hasJoined'

const base64UrlEncode = (value: string) => {
    return Buffer.from(value)
        .toString('base64')
        .replace(/=/g, '')
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
}

const signJwt = (payload: Record<string, unknown>) => {
    const secret = process.env[JWT_SECRET_ENV]
    if (!secret) {
        return null
    }

    const header = {
        alg: JWT_ALGORITHM,
        typ: 'JWT'
    }

    const encodedHeader = base64UrlEncode(JSON.stringify(header))
    const encodedPayload = base64UrlEncode(JSON.stringify(payload))
    const data = `${encodedHeader}.${encodedPayload}`
    const signature = createHmac('sha256', secret).update(data).digest('base64')
    const encodedSignature = signature.replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_')

    return `${data}.${encodedSignature}`
}

export const verifyJwt = (token: string) => {
    const secret = process.env[JWT_SECRET_ENV]
    if (!secret) {
        return null
    }

    const parts = token.split('.')
    if (parts.length !== 3) {
        return null
    }

    const [encodedHeader, encodedPayload, encodedSignature] = parts
    if (!encodedHeader || !encodedPayload || !encodedSignature) {
        return null
    }
    const data = `${encodedHeader}.${encodedPayload}`
    const expectedSignature = createHmac('sha256', secret).update(data).digest('base64')
    const expectedEncodedSignature = expectedSignature.replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_')

    if (encodedSignature !== expectedEncodedSignature) {
        return null
    }

    try {
        const payload = JSON.parse(Buffer.from(encodedPayload, 'base64').toString())

        // verify expiration
        if (payload.exp && payload.exp < Math.floor(Date.now() / 1000)) {
            return null
        }

        // verify issuer
        if (payload.iss !== JWT_ISSUER) {
            return null
        }

        return payload as { uuid: string; name: string; iss: string; iat: number; exp: number }
    } catch {
        return null
    }
}

export type AuthenticatedUser = {
    uuid: string
    name: string
}

export const requireAuth = new Elysia({ name: 'auth-guard' })
    .derive(({ request, set }) => {
        const authHeader = request.headers.get('authorization')
        if (!authHeader || !authHeader.startsWith('Bearer ')) {
            set.status = 401
            return {
                user: null as AuthenticatedUser | null
            }
        }

        const token = authHeader.substring(7)
        const payload = verifyJwt(token)
        if (!payload) {
            set.status = 401
            return {
                user: null as AuthenticatedUser | null
            }
        }

        return {
            user: {
                uuid: payload.uuid,
                name: payload.name
            } as AuthenticatedUser
        }
    })
    .onBeforeHandle(({ user, set }) => {
        if (!user) {
            set.status = 401
            return { success: false, error: 'Missing or invalid authorization header' }
        }
    })
    .as('scoped')

type MojangProfile = {
    id: string
    name: string
    properties?: Array<{ name: string; value: string; signature?: string }>
}

const fetchMojangProfile = async (username: string, serverId: string) => {
    const url = new URL(SESSION_SERVER_URL)
    url.searchParams.set('username', username)
    url.searchParams.set('serverId', serverId)

    const response = await fetch(url)
    if (response.status === 204) {
        return null
    }
    if (!response.ok) {
        throw new Error(`mojang sessionserver error: ${response.status}`)
    }

    return (await response.json()) as MojangProfile
}

export const authRouter = new Elysia({ prefix: '/v1/imperium/auth' })
    .post('/confirm-handshake', async ({ body, set }) => {
        const { serverId, username } = body as {
            serverId: string
            username: string
        }

        if (!serverId || !username) {
            return badRequest(set, 'Missing required fields')
        }

        // validate that the server id has the expected prefix
        if (!serverId.startsWith('lsu-gaia-')) {
            return badRequest(set, 'Invalid server id format')
        }

        let mojangProfile: MojangProfile | null
        try {
            mojangProfile = await fetchMojangProfile(username, serverId)
        } catch (error) {
            set.status = 503
            return {
                success: false,
                error: 'Mojang session server unavailable'
            }
        }

        if (!mojangProfile) {
            return forbidden(set, 'Player has not joined')
        }

        if (mojangProfile.name.toLowerCase() !== username.toLowerCase()) {
            return forbidden(set, 'Username mismatch')
        }

        const nowSeconds = Math.floor(Date.now() / 1000)
        const tokenPayload = {
            iss: JWT_ISSUER,
            iat: nowSeconds,
            exp: nowSeconds + JWT_TTL_SECONDS,
            name: mojangProfile.name,
            uuid: mojangProfile.id
        }

        const token = signJwt(tokenPayload)
        if (!token) {
            return serverError(set, 'JWT secret not configured')
        }

        console.info('gaia auth confirmed handshake', {
            uuid: mojangProfile.id,
            name: mojangProfile.name,
            serverId
        })

        return {
            success: true,
            token,
            profile: {
                id: mojangProfile.id,
                name: mojangProfile.name
            },
            expiresInMs: JWT_TTL_MS
        }
    })