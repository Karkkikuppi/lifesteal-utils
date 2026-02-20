import config from '../prisma.config.ts'
import { PrismaPg } from '@prisma/adapter-pg'
import { Pool } from 'pg'
import { PrismaClient } from '../generated/prisma/client.ts'

const DATABASE_URL = config.datasource?.url ?? process.env.DATABASE_URL
const IS_TEST_ENV = process.env.NODE_ENV === 'test' || process.env.BUN_TEST === '1'

export const pool = new Pool({
    connectionString: DATABASE_URL,
    allowExitOnIdle: IS_TEST_ENV
})

export const db = new PrismaClient({
    adapter: new PrismaPg(pool)
})

let hasClosed = false

export const closeDb = async () => {
    if (hasClosed) {
        return
    }

    hasClosed = true
    
    try {
        await db.$disconnect()
    } catch (error) {
        console.error('Error disconnecting Prisma:', error)
    }
    
    try {
        await pool.end()
    } catch (error) {
        console.error('Error ending pool:', error)
    }
}

// only register process handlers outside of test environment
// tests handle cleanup themselves to avoid race conditions
if (!IS_TEST_ENV) {
    const scheduleClose = () => {
        void closeDb().catch(() => undefined)
    }

    process.once('beforeExit', scheduleClose)
    process.once('SIGINT', scheduleClose)
}
