import './setup.ts'
import { afterAll, beforeEach, describe, expect, test } from '@jest/globals'
import { Elysia } from 'elysia'
import { createHmac } from 'crypto'
import { curiositasBaltopSnapshotRouter } from '../routes/curiositas/baltopPastSnapshot.ts'
import { db } from '../services/db.ts'

const JWT_ISSUER = 'gaia.candycup.dev'
const JWT_TTL_SECONDS = 60 * 60
const RANGE_24_HOURS = '24h'

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
        iss: JWT_ISSUER,
        iat: nowSeconds,
        exp: nowSeconds + JWT_TTL_SECONDS,
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

const app = new Elysia().use(curiositasBaltopSnapshotRouter)

const createSubmission = async (
    submittedAt: Date,
    entries: Array<{ uuid: string; username: string; amount: bigint }>
) => {
    const submission = await db.currencySubmission.create({
        data: {
            submitterName: 'Collector',
            submitterUuid: '00000000000000000000000000000000',
            modVersion: 'test',
            submittedAt
        }
    })

    await db.currencyRecord.createMany({
        data: entries.map((entry) => ({
            uuid: entry.uuid,
            username: entry.username,
            amount: entry.amount,
            currencyType: 'COINS',
            submissionId: submission.id
        }))
    })
}

describe('Curiositas baltop past snapshot API', () => {
    beforeEach(async () => {
        await db.currencyRecord.deleteMany()
        await db.currencySubmission.deleteMany()
    })

    afterAll(async () => {
        await db.currencyRecord.deleteMany()
        await db.currencySubmission.deleteMany()
    })

    test('requires authentication', async () => {
        const response = await app.handle(
            new Request(`http://localhost/v1/curiositas/baltop-past-snapshot?range=${RANGE_24_HOURS}`, {
                method: 'GET'
            })
        )

        expect(response.status).toBe(401)
    })

    test('rejects unsupported range values', async () => {
        const token = createTestJwt('123e4567e89b12d3a456426614174000', 'RangeTester')

        const response = await app.handle(
            new Request('http://localhost/v1/curiositas/baltop-past-snapshot?range=30d', {
                method: 'GET',
                headers: {
                    Authorization: `Bearer ${token}`
                }
            })
        )

        expect(response.status).toBe(400)
        const data = (await response.json()) as { success: boolean }
        expect(data.success).toBe(false)
    })

    test('returns current top players with nullable past amounts at snapshot cutoff', async () => {
        const now = Date.now()
        const oldSubmissionTime = new Date(now - 25 * 60 * 60 * 1000)
        const recentSubmissionTime = new Date(now - 60 * 60 * 1000)

        await createSubmission(oldSubmissionTime, [
            { uuid: 'u-a', username: 'Alpha', amount: 1000n },
            { uuid: 'u-b', username: 'Bravo', amount: 2000n }
        ])

        await createSubmission(recentSubmissionTime, [
            { uuid: 'u-a', username: 'Alpha', amount: 1500n },
            { uuid: 'u-b', username: 'Bravo', amount: 1800n },
            { uuid: 'u-c', username: 'Charlie', amount: 1700n }
        ])

        const token = createTestJwt('123e4567e89b12d3a456426614174001', 'SnapshotTester')
        const response = await app.handle(
            new Request(`http://localhost/v1/curiositas/baltop-past-snapshot?range=${RANGE_24_HOURS}`, {
                method: 'GET',
                headers: {
                    Authorization: `Bearer ${token}`
                }
            })
        )

        expect(response.status).toBe(200)

        const data = (await response.json()) as {
            success: boolean
            range: string
            entries: Array<{ username: string; currentAmount: string; pastAmount: string | null }>
        }

        expect(data.success).toBe(true)
        expect(data.range).toBe(RANGE_24_HOURS)
        expect(data.entries.map((entry) => entry.username)).toEqual(['Bravo', 'Charlie', 'Alpha'])

        const byName = new Map(data.entries.map((entry) => [entry.username, entry]))
        expect(byName.get('Bravo')?.currentAmount).toBe('1800')
        expect(byName.get('Bravo')?.pastAmount).toBe('2000')
        expect(byName.get('Charlie')?.currentAmount).toBe('1700')
        expect(byName.get('Charlie')?.pastAmount).toBeNull()
        expect(byName.get('Alpha')?.currentAmount).toBe('1500')
        expect(byName.get('Alpha')?.pastAmount).toBe('1000')
    })
})
