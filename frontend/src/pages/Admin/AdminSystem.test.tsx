import { QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { getAdminSystem, type AdminSystemSnapshot } from '../../api/admin'
import { createTestQueryClient } from '../../test/queryClient'
import { AdminSystem } from './AdminSystem'

vi.mock('../../api/admin', () => ({ getAdminSystem: vi.fn() }))

const snapshot: AdminSystemSnapshot = {
  capturedAt: '2026-11-26T15:04:00Z',
  systems: [
    { name: 'backend', status: 'UP' },
    { name: 'database', status: 'UP' },
    { name: 'flights', status: 'DOWN' },
  ],
  http: {
    requestCount: 40,
    clientErrors: 3,
    serverErrors: 1,
    meanMillis: 18.4,
    endpoints: [
      { method: 'POST', uri: '/purchases', requestCount: 12, serverErrors: 1, p95Millis: 240.2 },
    ],
  },
  concurrency: {
    poolActive: 4,
    poolIdle: 6,
    poolPending: 1,
    poolMax: 10,
    threadsBusy: 3,
    threadsCurrent: 8,
    threadsMax: 200,
  },
  circuitBreaker: { name: 'flightsService', state: 'HALF_OPEN', failureRate: 42.5 },
}

function renderPage() {
  return render(
    <QueryClientProvider client={createTestQueryClient()}>
      <MemoryRouter>
        <AdminSystem />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('AdminSystem page', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(getAdminSystem).mockResolvedValue(snapshot)
  })

  it('shows health, the breaker, the pool and the busiest endpoint', async () => {
    renderPage()

    expect(await screen.findByRole('heading', { name: 'Salud del proceso' })).toBeInTheDocument()
    expect(screen.getByText('Vuelos')).toBeInTheDocument()
    expect(screen.getByText('Caído')).toBeInTheDocument()
    expect(screen.getByText('Probando')).toBeInTheDocument()
    expect(screen.getByText('42,5 % de fallos')).toBeInTheDocument()
    expect(screen.getByText('Pool, hilos y tráfico').closest('details')).toHaveTextContent('4 / 10')
    expect(screen.getByText('Pool, hilos y tráfico').closest('details')).toHaveTextContent('/purchases')
    expect(screen.getByRole('heading', { name: 'Flujo de una compra' })).toBeInTheDocument()
    expect(screen.getByRole('list', { name: 'Pasos de la compra' })).toBeInTheDocument()
    expect(screen.getByText('Vender la ida')).toBeInTheDocument()
    const folded = screen.getByText('Cancelación, estrés y demo').closest('details')
    expect(folded).toHaveTextContent('purchase-throughput')
    expect(folded).toHaveTextContent('catalog-spike')
    expect(folded).toHaveTextContent('Nueva York Navidad')
    expect(screen.getByText('p95 de POST /purchases').closest('div')).toHaveTextContent('240,2 ms')
    expect(screen.getByRole('link', { name: /Grafana/ })).toHaveAttribute('href', 'http://localhost:3001')
    expect(screen.getByRole('link', { name: /Kibana/ })).toHaveAttribute('href', 'http://localhost:5601')
    expect(screen.getByRole('link', { name: /Zipkin/ })).toHaveAttribute('href', 'http://localhost:9411')
  })

  it('shows a dash when a gauge is missing', async () => {
    vi.mocked(getAdminSystem).mockResolvedValue({
      ...snapshot,
      concurrency: {
        ...snapshot.concurrency,
        threadsBusy: null,
        threadsCurrent: null,
        threadsMax: null,
      },
    })
    renderPage()

    const pool = await screen.findByText('Pool, hilos y tráfico')
    const details = pool.closest('details')
    expect(details).toHaveTextContent('Hilos ocupados')
    expect(details).toHaveTextContent('—')
  })

  it('keeps the consoles available when the snapshot fails', async () => {
    vi.mocked(getAdminSystem).mockRejectedValue(new Error('boom'))
    renderPage()

    expect(await screen.findByText('No se pudo leer el estado')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Flujo de una compra' })).toBeInTheDocument()
    expect(screen.queryByText('p95 de POST /purchases')).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Grafana/ })).toBeInTheDocument()
  })

  it('asks for a fresh snapshot', async () => {
    const user = userEvent.setup()
    renderPage()
    await screen.findByRole('heading', { name: 'Salud del proceso' })

    await user.click(screen.getByRole('button', { name: 'Actualizar' }))

    expect(getAdminSystem).toHaveBeenCalledTimes(2)
  })
})
