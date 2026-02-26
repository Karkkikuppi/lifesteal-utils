import { Elysia } from 'elysia'
import { db } from '../../services/db.ts'
import { requireAuth } from '../imperium/auth.ts'

const CURIOUSITAS_BASE_PATH = '/v1/curiositas'
const BALTOP_PAST_SNAPSHOT_PATH = '/baltop-past-snapshot'
const RANGE_24_HOURS = '24h'
const RANGE_7_DAYS = '7d'
const SUPPORTED_RANGES = [RANGE_24_HOURS, RANGE_7_DAYS] as const
const TOP_PLAYER_LIMIT = 90
const HOUR_IN_MS = 60 * 60 * 1000
const DAY_IN_MS = 24 * HOUR_IN_MS

type StatusSetter = { status?: number | string }

type SupportedRange = (typeof SUPPORTED_RANGES)[number]

type SnapshotRow = {
    uuid: string
    username: string
    currentAmount: bigint
    pastAmount: bigint | null
}

const badRequest = (set: StatusSetter, message: string) => {
    set.status = 400
    return { success: false, error: message }
}

const isSupportedRange = (value: string): value is SupportedRange => {
    return SUPPORTED_RANGES.includes(value as SupportedRange)
}

const getRangeMs = (range: SupportedRange) => {
    if (range === RANGE_24_HOURS) {
        return DAY_IN_MS
    }
    return 7 * DAY_IN_MS
}

const parseRange = (requestUrl: string) => {
    const url = new URL(requestUrl)
    return url.searchParams.get('range')
}

const fetchSnapshotRows = async (snapshotAt: Date) => {
    return db.$queryRaw<SnapshotRow[]>`
        WITH current_latest AS (
            SELECT DISTINCT ON (cr.uuid)
                cr.uuid,
                cr.username,
                cr.amount AS current_amount
            FROM currency_records cr
            INNER JOIN currency_submissions cs ON cs.id = cr."submissionId"
            WHERE cr."currencyType" = 'COINS'
            ORDER BY cr.uuid, cs."submittedAt" DESC, cr.id DESC
        ),
        top_current AS (
            SELECT
                uuid,
                username,
                current_amount
            FROM current_latest
            ORDER BY current_amount DESC, username ASC
            LIMIT ${TOP_PLAYER_LIMIT}
        ),
        past_latest AS (
            SELECT DISTINCT ON (cr.uuid)
                cr.uuid,
                cr.amount AS past_amount
            FROM currency_records cr
            INNER JOIN currency_submissions cs ON cs.id = cr."submissionId"
            WHERE cr."currencyType" = 'COINS'
                AND cr.uuid IN (SELECT uuid FROM top_current)
                AND cs."submittedAt" <= ${snapshotAt}
            ORDER BY cr.uuid, cs."submittedAt" DESC, cr.id DESC
        )
        SELECT
            tc.uuid,
            tc.username,
            tc.current_amount AS "currentAmount",
            pl.past_amount AS "pastAmount"
        FROM top_current tc
        LEFT JOIN past_latest pl ON pl.uuid = tc.uuid
        ORDER BY tc.current_amount DESC, tc.username ASC
    `
}

export const curiositasBaltopSnapshotRouter = new Elysia({ prefix: CURIOUSITAS_BASE_PATH })
    .use(requireAuth)
    .get(BALTOP_PAST_SNAPSHOT_PATH, async ({ request, set, user }) => {
        if (!user) {
            set.status = 401
            return { success: false, error: 'Missing or invalid authorization header' }
        }

        const range = parseRange(request.url)
        if (!range || !isSupportedRange(range)) {
            return badRequest(set, `Invalid range. Supported values: ${SUPPORTED_RANGES.join(', ')}`)
        }

        const rangeMs = getRangeMs(range)
        const snapshotAt = new Date(Date.now() - rangeMs)
        const rows = await fetchSnapshotRows(snapshotAt)

        return {
            success: true,
            range,
            snapshotAt: snapshotAt.toISOString(),
            entries: rows.map((row) => ({
                uuid: row.uuid,
                username: row.username,
                currentAmount: row.currentAmount.toString(),
                pastAmount: row.pastAmount === null ? null : row.pastAmount.toString()
            }))
        }
    })