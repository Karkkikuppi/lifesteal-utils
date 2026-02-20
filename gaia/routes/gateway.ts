import { Elysia } from 'elysia'
import { db } from '../services/db.ts'
import { verifyJwt, type AuthenticatedUser } from './imperium/auth.ts'

const GATEWAY_CACHE_TTL_MS = 30_000
const CLOSE_POLICY_VIOLATION = 1008
const CLOSE_UNSUPPORTED_DATA = 1003
const SOCKET_OPEN_STATE = 1
const CLIENT_OP_PING = 'ping'
const CLIENT_OP_REFRESH = 'refresh'

const extractBearerToken = (authHeader: string | null) => {
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return null
    }

    return authHeader.substring(7)
}

type GatewayClientMessage = {
    op: typeof CLIENT_OP_PING | typeof CLIENT_OP_REFRESH
}

type GatewayServerMessage =
    | {
          op: 'ready'
          data: {
              user: AuthenticatedUser
              allianceIds: string[]
              serverTimeMs: number
          }
      }
    | {
          op: 'event'
          type: string
          data: Record<string, unknown>
      }
    | {
          op: 'pong'
          data: {
              serverTimeMs: number
          }
      }
    | {
          op: 'error'
          error: {
              code: string
              message: string
          }
      }

type GatewaySocket = {
    data: { request: Request }
    raw?: object
    send: (data: unknown) => void
    close: (code?: number, reason?: string) => void
    readyState: number
}

type GatewayConnection = {
    user: AuthenticatedUser
    allianceIds: string[]
}

type UserAllianceCache = {
    allianceIds: string[]
    expiresAt: number
}

type AllianceMemberCache = {
    memberUuids: Set<string>
    expiresAt: number
}

const isGatewayClientMessage = (message: unknown): message is GatewayClientMessage => {
    if (!message || typeof message !== 'object') {
        return false
    }

    const op = (message as GatewayClientMessage).op
    return op === CLIENT_OP_PING || op === CLIENT_OP_REFRESH
}

/**
 * manages gateway websocket connections, membership caches, and event dispatch.
 */
export class GatewayHub {
    private readonly connectionsByUser = new Map<string, Set<object>>()
    private readonly connectionState = new WeakMap<object, GatewayConnection>()
    private readonly socketByKey = new Map<object, GatewaySocket>()
    private readonly userAllianceCache = new Map<string, UserAllianceCache>()
    private readonly allianceMemberCache = new Map<string, AllianceMemberCache>()

    /**
     * handle new websocket connections.
     */
    public async handleOpen(ws: GatewaySocket) {
        const request = ws.data.request
        const tokenFromHeader = extractBearerToken(request.headers.get('authorization'))
        const tokenFromQuery = new URL(request.url).searchParams.get('token')
        const token = tokenFromHeader ?? tokenFromQuery

        if (!token) {
            this.sendError(ws, 'unauthorized', 'missing token')
            ws.close(CLOSE_POLICY_VIOLATION, 'unauthorized')
            return
        }

        const payload = verifyJwt(token)
        if (!payload) {
            this.sendError(ws, 'unauthorized', 'invalid token')
            ws.close(CLOSE_POLICY_VIOLATION, 'unauthorized')
            return
        }

        const user: AuthenticatedUser = {
            uuid: payload.uuid,
            name: payload.name
        }

        const allianceIds = await this.getUserAllianceIds(user.uuid)
        this.trackConnection(user, allianceIds, ws)
        this.sendReady(ws, user, allianceIds)
    }

    /**
     * handle websocket close.
     */
    public handleClose(ws: GatewaySocket) {
        this.untrackConnection(ws)
    }

    /**
     * handle client messages.
     */
    public async handleMessage(ws: GatewaySocket, message: unknown) {
        const socketKey = this.getSocketKey(ws)
        if (!isGatewayClientMessage(message)) {
            this.sendError(ws, 'invalid_message', 'unsupported message format')
            ws.close(CLOSE_UNSUPPORTED_DATA, 'unsupported message format')
            return
        }

        const connection = this.connectionState.get(socketKey)
        if (!connection) {
            this.sendError(ws, 'unauthorized', 'connection not initialized')
            ws.close(CLOSE_POLICY_VIOLATION, 'unauthorized')
            return
        }

        if (message.op === CLIENT_OP_PING) {
            ws.send({
                op: 'pong',
                data: {
                    serverTimeMs: Date.now()
                }
            } satisfies GatewayServerMessage)
            return
        }

        if (message.op === CLIENT_OP_REFRESH) {
            await this.refreshUserAlliances(connection.user.uuid)
            const allianceIds = await this.getUserAllianceIds(connection.user.uuid)
            this.trackConnection(connection.user, allianceIds, ws)
            this.sendReady(ws, connection.user, allianceIds)
        }
    }

    /**
     * notify all online members of a specific alliance.
     */
    public async notifyAllianceMembers(allianceId: string, payload: { type: string; data: Record<string, unknown> }) {
        const memberUuids = await this.getAllianceMemberUuids(allianceId)
        const message: GatewayServerMessage = {
            op: 'event',
            type: payload.type,
            data: payload.data
        }

        for (const uuid of memberUuids) {
            this.sendToUser(uuid, message)
        }
    }

    /**
     * notify a specific user across all active connections.
     */
    public async notifyUser(uuid: string, payload: { type: string; data: Record<string, unknown> }) {
        const message: GatewayServerMessage = {
            op: 'event',
            type: payload.type,
            data: payload.data
        }

        this.sendToUser(uuid, message)
    }

    /**
     * refresh cached membership data for a user.
     */
    public async refreshUserAlliances(uuid: string) {
        this.userAllianceCache.delete(uuid)
    }

    /**
     * refresh cached membership data for an alliance.
     */
    public async refreshAllianceMembers(allianceId: string) {
        this.allianceMemberCache.delete(allianceId)
    }

    /**
     * clear cached data for tests.
     */
    public resetForTests() {
        for (const ws of this.socketByKey.values()) {
            try {
                if (ws.readyState === 1) {
                    ws.close(1000, 'test cleanup')
                }
            } catch {
                // ignore close failures in test cleanup
            }
        }
        this.connectionsByUser.clear()
        this.socketByKey.clear()
        this.userAllianceCache.clear()
        this.allianceMemberCache.clear()
    }

    private sendReady(ws: { send: (data: unknown) => void }, user: AuthenticatedUser, allianceIds: string[]) {
        ws.send({
            op: 'ready',
            data: {
                user,
                allianceIds,
                serverTimeMs: Date.now()
            }
        } satisfies GatewayServerMessage)
    }

    private sendError(ws: { send: (data: unknown) => void }, code: string, message: string) {
        ws.send({
            op: 'error',
            error: {
                code,
                message
            }
        } satisfies GatewayServerMessage)
    }

    private trackConnection(user: AuthenticatedUser, allianceIds: string[], ws: GatewaySocket) {
        const socketKey = this.getSocketKey(ws)
        const existing = this.connectionsByUser.get(user.uuid)
        const connections = existing ?? new Set<object>()
        connections.add(socketKey)
        this.connectionsByUser.set(user.uuid, connections)
        this.connectionState.set(socketKey, { user, allianceIds })
        this.socketByKey.set(socketKey, ws)
    }

    private untrackConnection(ws: GatewaySocket) {
        const socketKey = this.getSocketKey(ws)
        const connection = this.connectionState.get(socketKey)
        if (!connection) {
            return
        }

        const connections = this.connectionsByUser.get(connection.user.uuid)
        if (connections) {
            connections.delete(socketKey)
            if (connections.size === 0) {
                this.connectionsByUser.delete(connection.user.uuid)
            }
        }

        this.connectionState.delete(socketKey)
        this.socketByKey.delete(socketKey)
    }

    private sendToUser(uuid: string, message: GatewayServerMessage) {
        const connections = this.connectionsByUser.get(uuid)
        if (!connections) {
            return
        }

        for (const socketKey of connections) {
            const ws = this.socketByKey.get(socketKey)
            if (!ws) {
                continue
            }

            if (ws.readyState !== SOCKET_OPEN_STATE) {
                continue
            }

            ws.send(message)
        }
    }

    private getSocketKey(ws: GatewaySocket): object {
        return ws.raw ?? ws
    }

    private isCacheValid(expiresAt: number) {
        return expiresAt > Date.now()
    }

    private async getUserAllianceIds(uuid: string) {
        const cached = this.userAllianceCache.get(uuid)
        if (cached && this.isCacheValid(cached.expiresAt)) {
            return cached.allianceIds
        }

        const memberships = await db.allianceMember.findMany({
            where: {
                uuid,
                membershipState: 'JOINED'
            },
            select: {
                allianceId: true
            }
        })

        const allianceIds = memberships.map((membership) => membership.allianceId)
        this.userAllianceCache.set(uuid, {
            allianceIds,
            expiresAt: Date.now() + GATEWAY_CACHE_TTL_MS
        })

        return allianceIds
    }

    private async getAllianceMemberUuids(allianceId: string) {
        const cached = this.allianceMemberCache.get(allianceId)
        if (cached && this.isCacheValid(cached.expiresAt)) {
            return cached.memberUuids
        }

        const memberships = await db.allianceMember.findMany({
            where: {
                allianceId,
                membershipState: 'JOINED'
            },
            select: {
                uuid: true
            }
        })

        const memberUuids = new Set(memberships.map((membership) => membership.uuid))
        this.allianceMemberCache.set(allianceId, {
            memberUuids,
            expiresAt: Date.now() + GATEWAY_CACHE_TTL_MS
        })

        return memberUuids
    }
}

export const gatewayHub = new GatewayHub()

export const gatewayRouter = new Elysia({ prefix: '/v1/gateway' }).ws('/connect', {
    open: async (ws) => {
        await gatewayHub.handleOpen(ws)
    },
    message: async (ws, message) => {
        await gatewayHub.handleMessage(ws, message)
    },
    close: (ws) => {
        gatewayHub.handleClose(ws)
    }
})
