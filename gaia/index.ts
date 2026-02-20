import { Elysia } from 'elysia'
import pc from 'picocolors'
import { alliancesRouter } from './routes/imperium/alliances.ts'
import { authRouter } from './routes/imperium/auth.ts'
import { collectivumRouter } from './routes/collectivum/baltop.ts'
import { curiositasBaltopSnapshotRouter } from './routes/curiositas/baltopPastSnapshot.ts'
import { gatewayRouter } from './routes/gateway.ts'

const PORT = 3030

new Elysia()
    .state('start', 0)
    .onRequest(({ store }) => {
        store.start = performance.now()
    })
    .onBeforeHandle(({ body, request }) => {
        // log request body if it exists
        if (body && typeof body === 'object' && Object.keys(body).length > 0) {
            console.log(`${pc.magenta('Body for')} ${pc.cyan(request.method)} ${pc.yellow(request.url)}`)
            console.log(JSON.stringify(body, null, 2))
        }
    })
    .onAfterHandle(({ request, store, response, set }) => {
        const end = performance.now()
        const duration = (end - store.start).toFixed(2)
        const responseStatus =
            response && typeof response === 'object' && 'status' in response && typeof response.status === 'number'
                ? response.status
                : undefined
        const status = responseStatus ?? (typeof set.status === 'number' ? set.status : 200)
        console.log(
            `${pc.cyan(request.method)} ${pc.yellow(request.url)} - ${pc.magenta(status.toString())} - ${pc.green(
                duration + ' ms'
            )}`
        )
    })
    .onError(({ code, error, request, set }) => {
        const status = typeof set.status === 'number' ? set.status : 500
        console.error(
            `${pc.red('Gaia error')} ${pc.cyan(request.method)} ${pc.yellow(request.url)} - ${pc.magenta(
                status.toString()
            )} (${code})`
        )
        console.error(error)
    })
    .get('/', 'Hello Elysia')
    .use(alliancesRouter)
    .use(authRouter)
    .use(collectivumRouter)
    .use(curiositasBaltopSnapshotRouter)
    .use(gatewayRouter)
    .listen(PORT, () => {
        console.log(`${pc.green('✓')} Gaia operating on ${pc.cyan(`http://localhost:${PORT}`)}`)
    })
