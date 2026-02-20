import './setup.ts'
import { describe, expect, test, beforeAll, afterAll } from '@jest/globals'
import { Elysia } from 'elysia'
import { alliancesRouter } from '../routes/imperium/alliances.ts'
import { authRouter } from '../routes/imperium/auth.ts'
import { db } from '../services/db.ts'
import { createHmac } from 'crypto'

type MemberDto = {
    id: string
    uuid: string
    cachedName: string
    membershipState: 'INVITED' | 'JOINED' | 'DECLINED'
    permissions: string[]
}

type AllianceDto = {
    id: string
    name: string
    prefix?: string | null
    color?: string | null
    ownedBy?: string
    members?: MemberDto[]
}

type CreateAllianceSuccess = { success: true; alliance: AllianceDto }
type InviteMemberSuccess = { success: true; member: MemberDto }
type GetAllianceSuccess = { success: true; alliance: AllianceDto & { members: MemberDto[] } }
type ModifyMemberSuccess = { success: true; member: MemberDto }
type JoinAllianceSuccess = { success: true; member: MemberDto }
type UpdateAllianceSuccess = { success: true; alliance: AllianceDto }
type DeleteSuccess = { success: true }
type ApiError = { success: false; error: string }

const ADMIN_PERMISSION = '*'

const parseJson = async <T>(response: Response): Promise<T> => {
    return (await response.json()) as T
}

// helper to create JWT tokens for testing
const base64UrlEncode = (value: string) => {
    return Buffer.from(value)
        .toString('base64')
        .replace(/=/g, '')
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
}

const createTestJwt = (uuid: string, name: string) => {
    const secret = process.env.GAIA_JWT_SECRET
    if (!secret) {
        throw new Error('GAIA_JWT_SECRET not set')
    }

    const nowSeconds = Math.floor(Date.now() / 1000)
    const header = { alg: 'HS256', typ: 'JWT' }
    const payload = {
        iss: 'gaia.candycup.dev',
        iat: nowSeconds,
        exp: nowSeconds + 3600,
        uuid,
        name
    }

    const encodedHeader = base64UrlEncode(JSON.stringify(header))
    const encodedPayload = base64UrlEncode(JSON.stringify(payload))
    const data = `${encodedHeader}.${encodedPayload}`
    const signature = createHmac('sha256', secret).update(data).digest('base64')
    const encodedSignature = signature.replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_')

    return `${data}.${encodedSignature}`
}

// create test app with both routers
// note: alliancesRouter already has requireAuth applied, but we need to mount authRouter separately
const app = new Elysia()
    .use(authRouter) // auth endpoints don't require auth
    .use(alliancesRouter) // alliance endpoints all require auth

// test users
const testUser1 = {
    uuid: '123e4567-e89b-12d3-a456-426614174000',
    name: 'TestPlayer1'
}

const testUser2 = {
    uuid: '223e4567-e89b-12d3-a456-426614174000',
    name: 'TestPlayer2'
}

const testUser3 = {
    uuid: '323e4567-e89b-12d3-a456-426614174000',
    name: 'TestPlayer3'
}

let testUser1Token: string
let testUser2Token: string
let testUser3Token: string

// store test data
let testAllianceId: string
let testMemberId: string

describe('Alliance API', () => {
    beforeAll(async () => {
        // clean up any existing test data
        await db.allianceMember.deleteMany()
        await db.alliance.deleteMany()
        
        // create test JWT tokens
        testUser1Token = createTestJwt(testUser1.uuid, testUser1.name)
        testUser2Token = createTestJwt(testUser2.uuid, testUser2.name)
        testUser3Token = createTestJwt(testUser3.uuid, testUser3.name)
    })

    afterAll(async () => {
        // clean up test data
        await db.allianceMember.deleteMany()
        await db.alliance.deleteMany()
    })

    describe('Authentication', () => {
        test('should reject requests without authorization header', async () => {
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/create', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        name: 'Test Alliance',
                        description: 'Test',
                        motd: 'Test'
                    })
                })
            )

            expect(response.status).toBe(401)
            const data = await parseJson<ApiError>(response)
            expect(data.success).toBe(false)
            expect(data.error).toContain('authorization')
        })

        test('should reject requests with invalid JWT', async () => {
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/create', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': 'Bearer invalid.token.here'
                    },
                    body: JSON.stringify({
                        name: 'Test Alliance',
                        description: 'Test',
                        motd: 'Test'
                    })
                })
            )

            expect(response.status).toBe(401)
            const data = await parseJson<ApiError>(response)
            expect(data.success).toBe(false)
        })
    })

    describe('POST /v1/imperium/alliances/create', () => {
        test('should create a new alliance with valid JWT', async () => {
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/create', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({
                        name: 'Test Alliance',
                        description: 'A test alliance for unit testing',
                        motd: 'Welcome to our test alliance!'
                    })
                })
            )

            expect(response.status).toBe(200)
            const data = await parseJson<CreateAllianceSuccess>(response)
            expect(data.success).toBe(true)
            expect(data.alliance).toBeDefined()
            expect(data.alliance.name).toBe('Test Alliance')
            expect(data.alliance.prefix).toBeNull()
            expect(data.alliance.id).toBeDefined()
            expect(data.alliance.ownedBy).toBe(testUser1.uuid)
            
            // store for later tests
            testAllianceId = data.alliance.id
        })

        test('should enforce name length limit', async () => {
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/create', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({
                        name: 'A'.repeat(31), // exceeds 30 char limit
                        description: 'Test',
                        motd: 'Test'
                    })
                })
            )

            expect(response.status).toBe(400)
        })

        test('should require all fields', async () => {
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/create', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({
                        name: 'Test'
                        // missing description and motd
                    })
                })
            )

            expect(response.status).toBe(400)
        })

        test('should create alliance with optional prefix', async () => {
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/create', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({
                        name: 'Prefix Alliance',
                        prefix: 'Awesome',
                        description: 'Has a prefix',
                        motd: 'Prefix test'
                    })
                })
            )

            expect(response.status).toBe(200)
            const data = await parseJson<CreateAllianceSuccess>(response)
            expect(data.success).toBe(true)
            expect(data.alliance.prefix).toBe('Awesome')
        })

        test('should create alliance with optional color', async () => {
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/create', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({
                        name: 'Color Alliance',
                        color: '#00AAFF',
                        description: 'Has a color',
                        motd: 'Color test'
                    })
                })
            )

            expect(response.status).toBe(200)
            const data = await parseJson<CreateAllianceSuccess>(response)
            expect(data.success).toBe(true)
            expect(data.alliance.color).toBe('#00AAFF')
        })
    })

    describe('POST /v1/imperium/alliances/update', () => {
        test('should update alliance fields for admins', async () => {
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/update', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({
                        allianceId: testAllianceId,
                        name: 'Updated Alliance'
                    })
                })
            )

            expect(response.status).toBe(200)
            const data = await parseJson<UpdateAllianceSuccess>(response)
            expect(data.success).toBe(true)
            expect(data.alliance.name).toBe('Updated Alliance')
        })

        test('should reject updates from non-admins', async () => {
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/update', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser2Token}`
                    },
                    body: JSON.stringify({
                        allianceId: testAllianceId,
                        description: 'Should fail'
                    })
                })
            )

            expect(response.status).toBe(403)
        })

        test('should update alliance prefix for admins', async () => {
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/update', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({
                        allianceId: testAllianceId,
                        prefix: 'Legendary'
                    })
                })
            )

            expect(response.status).toBe(200)
            const data = await parseJson<UpdateAllianceSuccess>(response)
            expect(data.success).toBe(true)
            expect(data.alliance.prefix).toBe('Legendary')
        })

        test('should clear alliance prefix when set to blank', async () => {
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/update', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({
                        allianceId: testAllianceId,
                        prefix: '   '
                    })
                })
            )

            expect(response.status).toBe(200)
            const data = await parseJson<UpdateAllianceSuccess>(response)
            expect(data.success).toBe(true)
            expect(data.alliance.prefix).toBeNull()
        })

        test('should update alliance color for admins', async () => {
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/update', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({
                        allianceId: testAllianceId,
                        color: '#AA00FF'
                    })
                })
            )

            expect(response.status).toBe(200)
            const data = await parseJson<UpdateAllianceSuccess>(response)
            expect(data.success).toBe(true)
            expect(data.alliance.color).toBe('#AA00FF')
        })
    })

    describe('POST /v1/imperium/alliances/invite-member', () => {
        test('should add creator as joined admin member', async () => {
            const member = await db.allianceMember.findFirst({
                where: {
                    allianceId: testAllianceId,
                    uuid: testUser1.uuid
                }
            })

            expect(member).toBeDefined()
            expect(member?.membershipState).toBe('JOINED')
            expect(member?.permissions).toContain(ADMIN_PERMISSION)
            expect(member?.addedBy).toBe(testUser1.uuid)
        })

        test('should invite a member to an alliance', async () => {
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/invite-member', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({
                        allianceId: testAllianceId,
                        uuid: testUser2.uuid,
                        cachedName: testUser2.name,
                        permissions: ['manage_members']
                    })
                })
            )

            expect(response.status).toBe(200)
            const data = await parseJson<InviteMemberSuccess>(response)
            expect(data.success).toBe(true)
            expect(data.member).toBeDefined()
            expect(data.member.cachedName).toBe(testUser2.name)
            expect(data.member.membershipState).toBe('INVITED')
            expect(data.member.uuid).toBe(testUser2.uuid)
            
            // store for later tests
            testMemberId = data.member.id
        })

        test('should use authenticated user as addedBy', async () => {
            // verify in database that addedBy is testUser1.uuid
            const member = await db.allianceMember.findUnique({
                where: { id: testMemberId }
            })
            
            expect(member).toBeDefined()
            expect(member?.addedBy).toBe(testUser1.uuid)
        })

        test('should require valid permissions array', async () => {
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/invite-member', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({
                        allianceId: testAllianceId,
                        uuid: 'test-uuid',
                        cachedName: 'Test',
                        permissions: 'not-an-array'
                    })
                })
            )

            expect(response.status).toBe(400)
        })
    })

    describe('POST /v1/imperium/alliances/@self/join', () => {
        test('should allow invited member to join alliance', async () => {
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/@self/join', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser2Token}`
                    },
                    body: JSON.stringify({
                        allianceId: testAllianceId
                    })
                })
            )

            expect(response.status).toBe(200)
            const data = await parseJson<JoinAllianceSuccess>(response)
            expect(data.success).toBe(true)
            expect(data.member).toBeDefined()
            expect(data.member.membershipState).toBe('JOINED')
            expect(data.member.uuid).toBe(testUser2.uuid)
        })

        test('should reject join request if no invitation exists', async () => {
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/@self/join', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}` // user1 has no invitation
                    },
                    body: JSON.stringify({
                        allianceId: testAllianceId
                    })
                })
            )

            expect(response.status).toBe(404)
            const data = await parseJson<ApiError>(response)
            expect(data.success).toBe(false)
            expect(data.error).toContain('invitation')
        })

        test('should reject join request if already joined', async () => {
            // testUser2 has already joined, try to join again
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/@self/join', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser2Token}`
                    },
                    body: JSON.stringify({
                        allianceId: testAllianceId
                    })
                })
            )

            expect(response.status).toBe(404)
            const data = await parseJson<ApiError>(response)
            expect(data.success).toBe(false)
        })
    })

    describe('GET /v1/imperium/alliances/@self and /@self/invites', () => {
        test('should return joined alliances in /@self', async () => {
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/@self', {
                    method: 'GET',
                    headers: {
                        'Authorization': `Bearer ${testUser2Token}`
                    }
                })
            )

            expect(response.status).toBe(200)
            const data = await parseJson<{ success: true; alliances: AllianceDto[] }>(response)
            expect(data.success).toBe(true)
            expect(data.alliances.some((alliance) => alliance.id === testAllianceId)).toBe(true)
        })

        test('should return pending invites in /@self/invites', async () => {
            const inviteResponse = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/invite-member', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({
                        allianceId: testAllianceId,
                        uuid: testUser3.uuid,
                        cachedName: testUser3.name,
                        permissions: ['read_only']
                    })
                })
            )

            expect(inviteResponse.status).toBe(200)

            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/@self/invites', {
                    method: 'GET',
                    headers: {
                        'Authorization': `Bearer ${testUser3Token}`
                    }
                })
            )

            expect(response.status).toBe(200)
            const data = await parseJson<{ success: true; alliances: AllianceDto[] }>(response)
            expect(data.success).toBe(true)
            expect(data.alliances.some((alliance) => alliance.id === testAllianceId)).toBe(true)
        })

        test('should not include pending invites in /@self', async () => {
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/@self', {
                    method: 'GET',
                    headers: {
                        'Authorization': `Bearer ${testUser3Token}`
                    }
                })
            )

            expect(response.status).toBe(200)
            const data = await parseJson<{ success: true; alliances: AllianceDto[] }>(response)
            expect(data.success).toBe(true)
            expect(data.alliances.some((alliance) => alliance.id === testAllianceId)).toBe(false)
        })
    })

    describe('POST /v1/imperium/alliances/@self/reject', () => {
        test('should allow invited member to reject alliance invitation', async () => {
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/@self/reject', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser3Token}`
                    },
                    body: JSON.stringify({
                        allianceId: testAllianceId
                    })
                })
            )

            expect(response.status).toBe(200)
            const data = await parseJson<JoinAllianceSuccess>(response)
            expect(data.success).toBe(true)
            expect(data.member.membershipState).toBe('DECLINED')
        })

        test('should remove rejected invites from /@self/invites', async () => {
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/@self/invites', {
                    method: 'GET',
                    headers: {
                        'Authorization': `Bearer ${testUser3Token}`
                    }
                })
            )

            expect(response.status).toBe(200)
            const data = await parseJson<{ success: true; alliances: AllianceDto[] }>(response)
            expect(data.success).toBe(true)
            expect(data.alliances.some((alliance) => alliance.id === testAllianceId)).toBe(false)
        })

        test('should reject reject request if no invitation exists', async () => {
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/@self/reject', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser2Token}`
                    },
                    body: JSON.stringify({
                        allianceId: testAllianceId
                    })
                })
            )

            expect(response.status).toBe(404)
            const data = await parseJson<ApiError>(response)
            expect(data.success).toBe(false)
        })
    })

    describe('GET /v1/imperium/alliances/:id', () => {
        test('should retrieve an alliance with members', async () => {
            const response = await app.handle(
                new Request(`http://localhost/v1/imperium/alliances/${testAllianceId}`, {
                    method: 'GET',
                    headers: {
                        'Authorization': `Bearer ${testUser1Token}`
                    }
                })
            )

            expect(response.status).toBe(200)
            const data = await parseJson<GetAllianceSuccess>(response)
            expect(data.success).toBe(true)
            expect(data.alliance).toBeDefined()
            expect(data.alliance.members).toBeDefined()
            expect(data.alliance.members.length).toBeGreaterThan(0)
            
            // verify membership states are included
            const joinedMember = data.alliance.members.find(m => m.uuid === testUser2.uuid)
            expect(joinedMember).toBeDefined()
            expect(joinedMember?.membershipState).toBe('JOINED')
        })

        test('should return error for non-existent alliance', async () => {
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/00000000-0000-0000-0000-000000000000', {
                    method: 'GET',
                    headers: {
                        'Authorization': `Bearer ${testUser1Token}`
                    }
                })
            )

            expect(response.status).toBe(404)
            const data = await parseJson<ApiError>(response)
            expect(data.success).toBe(false)
            expect(data.error).toBe('Alliance not found')
        })
    })

    describe('POST /v1/imperium/alliances/modify-member', () => {
        test('should modify member permissions', async () => {
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/modify-member', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({
                        memberId: testMemberId,
                        permissions: ['*']
                    })
                })
            )

            expect(response.status).toBe(200)
            const data = await parseJson<ModifyMemberSuccess>(response)
            expect(data.success).toBe(true)
            expect(data.member.permissions).toContain('*')
        })

        test('should modify member cached name', async () => {
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/modify-member', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({
                        memberId: testMemberId,
                        cachedName: 'UpdatedName'
                    })
                })
            )

            expect(response.status).toBe(200)
            const data = await parseJson<ModifyMemberSuccess>(response)
            expect(data.success).toBe(true)
            expect(data.member.cachedName).toBe('UpdatedName')
        })

        test('should reject update with no fields', async () => {
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/modify-member', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({
                        memberId: testMemberId
                    })
                })
            )

            expect(response.status).toBe(400)
        })
    })

    describe('POST /v1/imperium/alliances/remove-member', () => {
        test('should remove a member from an alliance', async () => {
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/remove-member', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({
                        memberId: testMemberId
                    })
                })
            )

            expect(response.status).toBe(200)
            const data = await parseJson<DeleteSuccess>(response)
            expect(data.success).toBe(true)
            
            // verify member was deleted
            const member = await db.allianceMember.findUnique({
                where: { id: testMemberId }
            })
            expect(member).toBeNull()
        })
    })

    describe('POST /v1/imperium/alliances/delete', () => {
        test('should delete an alliance', async () => {
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/delete', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({
                        allianceId: testAllianceId
                    })
                })
            )

            expect(response.status).toBe(200)
            const data = await parseJson<DeleteSuccess>(response)
            expect(data.success).toBe(true)
        })

        test('should cascade delete members when alliance is deleted', async () => {
            // create new alliance
            const createRes = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/create', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({
                        name: 'Delete Test',
                        description: 'Test cascade delete',
                        motd: 'Test'
                    })
                })
            )
            const createData = await parseJson<CreateAllianceSuccess>(createRes)
            const allianceId = createData.alliance.id

            // add user1 as admin first
            await app.handle(
                new Request('http://localhost/v1/imperium/alliances/invite-member', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({
                        allianceId,
                        uuid: testUser1.uuid,
                        cachedName: testUser1.name,
                        permissions: ['*']
                    })
                })
            )

            // join the alliance
            await app.handle(
                new Request('http://localhost/v1/imperium/alliances/@self/join', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({ allianceId })
                })
            )

            // now invite another member
            await app.handle(
                new Request('http://localhost/v1/imperium/alliances/invite-member', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({
                        allianceId,
                        uuid: 'test-uuid-cascade',
                        cachedName: 'Test',
                        permissions: []
                    })
                })
            )

            // delete the alliance
            const deleteRes = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/delete', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({ allianceId })
                })
            )

            expect(deleteRes.status).toBe(200)
            
            // verify members were cascade deleted
            const members = await db.allianceMember.findMany({
                where: { allianceId }
            })
            expect(members.length).toBe(0)
        })
    })

    describe('Authorization Tests', () => {
        let adminAllianceId: string
        let adminMemberId: string
        let nonAdminMemberId: string

        test('setup: create alliance with admin and non-admin members', async () => {
            // create alliance
            const createRes = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/create', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({
                        name: 'Admin Test Alliance',
                        description: 'Testing authorization',
                        motd: 'Test'
                    })
                })
            )
            const createData = await parseJson<CreateAllianceSuccess>(createRes)
            adminAllianceId = createData.alliance.id

            // add user1 as admin (with * permission) and mark as JOINED
            const adminRes = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/invite-member', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({
                        allianceId: adminAllianceId,
                        uuid: testUser1.uuid,
                        cachedName: testUser1.name,
                        permissions: ['*']
                    })
                })
            )
            const adminData = await parseJson<InviteMemberSuccess>(adminRes)
            adminMemberId = adminData.member.id

            // have user1 join their own alliance
            await app.handle(
                new Request('http://localhost/v1/imperium/alliances/@self/join', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({
                        allianceId: adminAllianceId
                    })
                })
            )

            // add user2 as non-admin member (without * permission) and mark as JOINED
            const memberRes = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/invite-member', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({
                        allianceId: adminAllianceId,
                        uuid: testUser2.uuid,
                        cachedName: testUser2.name,
                        permissions: ['view']
                    })
                })
            )
            const memberData = await parseJson<InviteMemberSuccess>(memberRes)
            nonAdminMemberId = memberData.member.id

            // have user2 join
            await app.handle(
                new Request('http://localhost/v1/imperium/alliances/@self/join', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser2Token}`
                    },
                    body: JSON.stringify({
                        allianceId: adminAllianceId
                    })
                })
            )

            expect(adminAllianceId).toBeDefined()
            expect(adminMemberId).toBeDefined()
            expect(nonAdminMemberId).toBeDefined()
        })

        test('admin can invite new members', async () => {
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/invite-member', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({
                        allianceId: adminAllianceId,
                        uuid: testUser3.uuid,
                        cachedName: testUser3.name,
                        permissions: ['view']
                    })
                })
            )

            expect(response.status).toBe(200)
        })

        test('non-admin cannot invite members', async () => {
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/invite-member', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser2Token}`
                    },
                    body: JSON.stringify({
                        allianceId: adminAllianceId,
                        uuid: 'some-uuid',
                        cachedName: 'SomePlayer',
                        permissions: ['view']
                    })
                })
            )

            expect(response.status).toBe(403)
            const data = await parseJson<ApiError>(response)
            expect(data.success).toBe(false)
            expect(data.error).toContain('permission')
        })

        test('admin can modify member permissions', async () => {
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/modify-member', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({
                        memberId: nonAdminMemberId,
                        permissions: ['view', 'edit']
                    })
                })
            )

            expect(response.status).toBe(200)
        })

        test('non-admin cannot modify members', async () => {
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/modify-member', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser2Token}`
                    },
                    body: JSON.stringify({
                        memberId: adminMemberId,
                        permissions: ['view']
                    })
                })
            )

            expect(response.status).toBe(403)
            const data = await parseJson<ApiError>(response)
            expect(data.success).toBe(false)
        })

        test('admin can remove members', async () => {
            // create a temporary member to remove
            const inviteRes = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/invite-member', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({
                        allianceId: adminAllianceId,
                        uuid: 'temp-uuid',
                        cachedName: 'TempPlayer',
                        permissions: []
                    })
                })
            )
            const inviteData = await parseJson<InviteMemberSuccess>(inviteRes)

            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/remove-member', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({
                        memberId: inviteData.member.id
                    })
                })
            )

            expect(response.status).toBe(200)
        })

        test('non-admin cannot remove members', async () => {
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/remove-member', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser2Token}`
                    },
                    body: JSON.stringify({
                        memberId: adminMemberId
                    })
                })
            )

            expect(response.status).toBe(403)
            const data = await parseJson<ApiError>(response)
            expect(data.success).toBe(false)
        })

        test('admin can delete alliance', async () => {
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/delete', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({
                        allianceId: adminAllianceId
                    })
                })
            )

            expect(response.status).toBe(200)
        })

        test('non-admin cannot delete alliance', async () => {
            // create a new alliance for this test
            const createRes = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/create', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({
                        name: 'Delete Test',
                        description: 'Test',
                        motd: 'Test'
                    })
                })
            )
            const createData = await parseJson<CreateAllianceSuccess>(createRes)
            const newAllianceId = createData.alliance.id

            // add user1 as admin and join
            await app.handle(
                new Request('http://localhost/v1/imperium/alliances/invite-member', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({
                        allianceId: newAllianceId,
                        uuid: testUser1.uuid,
                        cachedName: testUser1.name,
                        permissions: ['*']
                    })
                })
            )
            await app.handle(
                new Request('http://localhost/v1/imperium/alliances/@self/join', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({ allianceId: newAllianceId })
                })
            )

            // add user2 as non-admin and join
            await app.handle(
                new Request('http://localhost/v1/imperium/alliances/invite-member', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({
                        allianceId: newAllianceId,
                        uuid: testUser2.uuid,
                        cachedName: testUser2.name,
                        permissions: ['view']
                    })
                })
            )
            await app.handle(
                new Request('http://localhost/v1/imperium/alliances/@self/join', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser2Token}`
                    },
                    body: JSON.stringify({ allianceId: newAllianceId })
                })
            )

            // try to delete as non-admin
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/delete', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser2Token}`
                    },
                    body: JSON.stringify({
                        allianceId: newAllianceId
                    })
                })
            )

            expect(response.status).toBe(403)
            const data = await parseJson<ApiError>(response)
            expect(data.success).toBe(false)
        })

        test('non-member cannot manage alliance', async () => {
            // create alliance with user1
            const createRes = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/create', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({
                        name: 'Exclusive Alliance',
                        description: 'Test',
                        motd: 'Test'
                    })
                })
            )
            const createData = await parseJson<CreateAllianceSuccess>(createRes)

            // add user1 as admin and join
            await app.handle(
                new Request('http://localhost/v1/imperium/alliances/invite-member', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({
                        allianceId: createData.alliance.id,
                        uuid: testUser1.uuid,
                        cachedName: testUser1.name,
                        permissions: ['*']
                    })
                })
            )
            await app.handle(
                new Request('http://localhost/v1/imperium/alliances/@self/join', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser1Token}`
                    },
                    body: JSON.stringify({ allianceId: createData.alliance.id })
                })
            )

            // try to invite as user3 (not a member)
            const response = await app.handle(
                new Request('http://localhost/v1/imperium/alliances/invite-member', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${testUser3Token}`
                    },
                    body: JSON.stringify({
                        allianceId: createData.alliance.id,
                        uuid: 'new-user',
                        cachedName: 'NewUser',
                        permissions: []
                    })
                })
            )

            expect(response.status).toBe(403)
            const data = await parseJson<ApiError>(response)
            expect(data.success).toBe(false)
            expect(data.error).toContain('not a member')
        })
    })
})
