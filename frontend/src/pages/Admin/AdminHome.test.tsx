import { QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { listAgencies } from '../../api/agencies'
import {
  getAdminFavorites,
  getAdminPurchases,
  getAdminReports,
  getAdminReviews,
  getAdminSystem,
  getAdminUsers,
} from '../../api/admin'
import { listFlights } from '../../api/flights'
import { listHotels } from '../../api/hotels'
import { searchPackages } from '../../api/packages'
import { allFlights, buenosAiresHotel, parisHotel } from '../../test/fixtures'
import { createTestQueryClient } from '../../test/queryClient'
import { AdminHome } from './AdminHome'

vi.mock('../../api/flights', () => ({ listFlights: vi.fn() }))
vi.mock('../../api/hotels', () => ({ listHotels: vi.fn() }))
vi.mock('../../api/packages', () => ({ searchPackages: vi.fn() }))
vi.mock('../../api/agencies', () => ({ listAgencies: vi.fn() }))
vi.mock('../../api/admin', () => ({
  getAdminUsers: vi.fn(),
  getAdminFavorites: vi.fn(),
  getAdminReviews: vi.fn(),
  getAdminPurchases: vi.fn(),
  getAdminReports: vi.fn(),
  getAdminSystem: vi.fn(),
}))

function countPage(totalElements: number) {
  return { content: [], page: 0, size: 1, totalElements, totalPages: totalElements }
}

function renderHub() {
  return render(
    <QueryClientProvider client={createTestQueryClient()}>
      <MemoryRouter>
        <AdminHome />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('AdminHome page', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(searchPackages).mockResolvedValue({
      content: [],
      page: 0,
      size: 1,
      totalElements: 12,
      totalPages: 12,
    })
    vi.mocked(listFlights).mockResolvedValue(allFlights)
    vi.mocked(listHotels).mockResolvedValue([parisHotel, buenosAiresHotel])
    vi.mocked(listAgencies).mockResolvedValue([
      { id: 1, name: 'Despegar', active: true },
      { id: 2, name: 'Pausada', active: false },
    ])
    vi.mocked(getAdminUsers).mockResolvedValue(countPage(5))
    vi.mocked(getAdminFavorites).mockResolvedValue(countPage(3))
    vi.mocked(getAdminReviews).mockResolvedValue(countPage(4))
    vi.mocked(getAdminPurchases).mockResolvedValue(countPage(8))
    vi.mocked(getAdminReports).mockResolvedValue({
      topBuyers: [{ id: 1, name: 'Ana', total: 4 }],
      topAgencies: [{ id: 2, name: 'Despegar', total: 9 }],
      topDestinations: [{ code: 'PAR', name: 'Paris', total: 6 }],
      topRatedDestinations: [{ code: 'ROM', name: 'Roma', average: 8.5, reviewCount: 3 }],
    })
    vi.mocked(getAdminSystem).mockResolvedValue({
      capturedAt: '2026-11-26T12:00:00Z',
      systems: [
        { name: 'backend', status: 'UP' },
        { name: 'database', status: 'UP' },
        { name: 'flights', status: 'DOWN' },
      ],
      http: { requestCount: 10, clientErrors: 1, serverErrors: 0, meanMillis: 20, endpoints: [] },
      concurrency: {
        poolActive: 1,
        poolIdle: 4,
        poolPending: 0,
        poolMax: 10,
        threadsBusy: 2,
        threadsCurrent: 8,
        threadsMax: 200,
      },
      circuitBreaker: { name: 'flightsService', state: 'CLOSED', failureRate: null },
    })
  })

  it('links to packages, flights and hotels management', () => {
    renderHub()

    expect(screen.getByRole('heading', { level: 1, name: 'Administración' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Gestionar paquetes' })).toHaveAttribute('href', '/admin/packages')
    expect(screen.getByRole('link', { name: 'Gestionar vuelos' })).toHaveAttribute('href', '/admin/flights')
    expect(screen.getByRole('link', { name: 'Gestionar hoteles' })).toHaveAttribute('href', '/admin/hotels')
    expect(screen.getByRole('link', { name: 'Nuevo hotel' })).toHaveAttribute('href', '/admin/hotels/new')
    expect(screen.getByRole('link', { name: 'Gestionar usuarios' })).toHaveAttribute('href', '/admin/users')
    expect(screen.getByRole('link', { name: 'Gestionar agencias' })).toHaveAttribute('href', '/admin/agencies')
    expect(screen.getByRole('link', { name: 'Consultar ventas' })).toHaveAttribute('href', '/admin/sales')
    expect(screen.getByRole('link', { name: 'Ver operación' })).toHaveAttribute('href', '/admin/system')
  })

  it('shows quick counts once they load', async () => {
    renderHub()

    await waitFor(() => {
      expect(screen.getByTestId('admin-count-packages')).toHaveTextContent('12 paquetes')
    })
    expect(screen.getByTestId('admin-count-flights')).toHaveTextContent(`${allFlights.length} vuelos`)
    expect(screen.getByTestId('admin-count-hotels')).toHaveTextContent('2 hoteles')
    expect(screen.getByTestId('admin-count-users')).toHaveTextContent('5 usuarios')
    expect(screen.getByTestId('admin-count-agencies')).toHaveTextContent('2 agencias')
    expect(screen.getByTestId('admin-card-agencies')).toHaveTextContent('1 activa')
    expect(screen.getByTestId('admin-count-sales')).toHaveTextContent('8 compras')
    expect(screen.getByTestId('admin-card-queries')).toHaveTextContent('Favoritos: 3')
    expect(screen.getByTestId('admin-card-queries')).toHaveTextContent('Reseñas: 4')
    expect(screen.getByTestId('admin-card-reports')).toHaveTextContent('Compradores: Ana (4)')
    expect(screen.getByTestId('admin-card-reports')).toHaveTextContent('Puntaje: Roma (8,5)')
    expect(screen.getByTestId('admin-card-system')).toHaveTextContent('Vuelos: Caído')
    expect(screen.getByTestId('admin-card-system')).toHaveTextContent('Servicio de vuelos: Cerrado')
    expect(getAdminUsers).toHaveBeenCalledWith(0, 1)
  })

  it('keeps the cards usable when a count cannot be loaded', async () => {
    vi.mocked(listFlights).mockRejectedValue(new Error('boom'))
    renderHub()

    await waitFor(() => {
      expect(screen.getByTestId('admin-count-hotels')).toHaveTextContent('2 hoteles')
    })
    const flights = screen.getByTestId('admin-card-flights')
    expect(within(flights).getByTestId('admin-count-flights')).toHaveTextContent('— vuelos')
    expect(within(flights).getByRole('link', { name: 'Gestionar vuelos' })).toBeInTheDocument()
  })

  it('keeps the operation link when the snapshot cannot be loaded', async () => {
    vi.mocked(getAdminSystem).mockRejectedValue(new Error('boom'))
    renderHub()

    expect(await screen.findByText('No se pudo leer el estado del proceso')).toBeInTheDocument()
    const system = screen.getByTestId('admin-card-system')
    expect(within(system).getByRole('link', { name: 'Ver operación' })).toHaveAttribute('href', '/admin/system')
  })
})
