import './setup.ts'
import { beforeEach, describe, expect, test } from '@jest/globals'
import {
    getPrometheusMetrics,
    normalizeRouteLabel,
    recordGatewayConnectionClosed,
    recordGatewayConnectionOpened,
    recordHttpRequest,
    resetMetricsForTests
} from '../services/metrics.ts'

describe('Metrics service', () => {
    beforeEach(() => {
        resetMetricsForTests()
    })

    test('normalizes dynamic route segments', () => {
        const normalized = normalizeRouteLabel('/v1/alliances/123e4567-e89b-12d3-a456-426614174000/members/42')

        expect(normalized).toBe('/v1/alliances/:uuid/members/:id')
    })

    test('records http request counters and duration histograms', async () => {
        recordHttpRequest('get', '/v1/alliances/42', 200, 123)

        const metrics = await getPrometheusMetrics()

        expect(metrics).toContain('gaia_http_requests_total{method="GET",route="/v1/alliances/:id",status="200"} 1')
        expect(metrics).toContain('gaia_http_request_duration_seconds_count{method="GET",route="/v1/alliances/:id",status="200"} 1')
    })

    test('tracks gateway active connections and events', async () => {
        recordGatewayConnectionOpened(1)
        recordGatewayConnectionOpened(2)
        recordGatewayConnectionClosed(1)

        const metrics = await getPrometheusMetrics()

        expect(metrics).toContain('gaia_gateway_active_connections 1')
        expect(metrics).toContain('gaia_gateway_connection_events_total{event="open"} 2')
        expect(metrics).toContain('gaia_gateway_connection_events_total{event="close"} 1')
    })
})
