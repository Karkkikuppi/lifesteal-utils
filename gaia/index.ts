import { Elysia } from 'elysia'
import pc from 'picocolors'
import { alliancesRouter } from './routes/imperium/alliances.ts'
import { authRouter } from './routes/imperium/auth.ts'
import { collectivumRouter } from './routes/collectivum/baltop.ts'
import { curiositasBaltopSnapshotRouter } from './routes/curiositas/baltopPastSnapshot.ts'
import { gatewayRouter } from './routes/gateway.ts'
import { getPrometheusContentType, getPrometheusMetrics, recordHttpRequest } from './services/metrics.ts'

const parsePort = (rawValue: string | undefined, fallback: number) => {
    if (!rawValue) {
        return fallback
    }

    const parsedValue = Number.parseInt(rawValue, 10)
    return Number.isNaN(parsedValue) ? fallback : parsedValue
}

const APP_PORT = parsePort(process.env.GAIA_PORT, 3030)
const METRICS_PORT = parsePort(process.env.GAIA_METRICS_PORT, 9090)
const requestStartByRequest = new WeakMap<Request, number>()
const requestRecordedByRequest = new WeakMap<Request, boolean>()

new Elysia()
    .onRequest(({ request }) => {
        requestStartByRequest.set(request, performance.now())
        requestRecordedByRequest.set(request, false)
    })
    .onBeforeHandle(({ body, request }) => {
        // log request body if it exists
        if (body && typeof body === 'object' && Object.keys(body).length > 0) {
            console.log(`${pc.magenta('Body for')} ${pc.cyan(request.method)} ${pc.yellow(request.url)}`)
            console.log(JSON.stringify(body, null, 2))
        }
    })
    .onAfterHandle(({ request, response, set }) => {
        const end = performance.now()
        const startedAt = requestStartByRequest.get(request) ?? end
        const durationMs = Math.max(end - startedAt, 0)
        const duration = durationMs.toFixed(2)
        const responseStatus =
            response && typeof response === 'object' && 'status' in response && typeof response.status === 'number'
                ? response.status
                : undefined
        const status = responseStatus ?? (typeof set.status === 'number' ? set.status : 200)
        const pathname = new URL(request.url).pathname

        if (!requestRecordedByRequest.get(request)) {
            recordHttpRequest(request.method, pathname, status, durationMs)
            requestRecordedByRequest.set(request, true)
        }

        console.log(
            `${pc.cyan(request.method)} ${pc.yellow(request.url)} - ${pc.magenta(status.toString())} - ${pc.green(
                duration + ' ms'
            )}`
        )
    })
    .onError(({ code, error, request, set }) => {
        const status = typeof set.status === 'number' ? set.status : 500
        const startedAt = requestStartByRequest.get(request) ?? performance.now()
        const durationMs = Math.max(performance.now() - startedAt, 0)
        const pathname = new URL(request.url).pathname

        if (!requestRecordedByRequest.get(request)) {
            recordHttpRequest(request.method, pathname, status, durationMs)
            requestRecordedByRequest.set(request, true)
        }

        console.error(
            `${pc.red('Gaia error')} ${pc.cyan(request.method)} ${pc.yellow(request.url)} - ${pc.magenta(
                status.toString()
            )} (${code})`
        )
        console.error(error)
    })
    .use(alliancesRouter)
    .use(authRouter)
    .use(collectivumRouter)
    .use(curiositasBaltopSnapshotRouter)
    .use(gatewayRouter)
    .listen(APP_PORT, () => {
        console.log(`${pc.green('✓')} Gaia operating on port ${pc.cyan(`${APP_PORT}`)}`)
    })

new Elysia()
    .get('/health', () => {
        return {
            ok: true
        }
    })
    .get('/metrics', async ({ set }) => {
        set.headers['content-type'] = getPrometheusContentType()
        return await getPrometheusMetrics()
    })
    .listen(METRICS_PORT, () => {
        console.log(`${pc.green('✓')} Gaia metrics exporter operating on port ${pc.cyan(`${METRICS_PORT}`)}`)
    })
