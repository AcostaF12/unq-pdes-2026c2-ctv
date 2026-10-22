import { QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { listFlights } from '../../api/flights'
import { listHotels } from '../../api/hotels'
import { searchPackages } from '../../api/packages'
import { allFlights, buenosAiresHotel, parisHotel } from '../../test/fixtures'
import { createTestQueryClient } from '../../test/queryClient'
import { AdminHome } from './AdminHome'

vi.mock('../../api/flights', () => ({ listFlights: vi.fn() }))
vi.mock('../../api/hotels', () => ({ listHotels: vi.fn() }))
vi.mock('../../api/packages', () => ({ searchPackages: vi.fn() }))

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
  })

  it('links to packages, flights and hotels management', () => {
    renderHub()

    expect(screen.getByRole('heading', { level: 1, name: 'Administración' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Gestionar paquetes' })).toHaveAttribute('href', '/admin/packages')
    expect(screen.getByRole('link', { name: 'Gestionar vuelos' })).toHaveAttribute('href', '/admin/flights')
    expect(screen.getByRole('link', { name: 'Gestionar hoteles' })).toHaveAttribute('href', '/hotels')
    expect(screen.getByRole('link', { name: 'Nuevo hotel' })).toHaveAttribute('href', '/hotels/new')
    expect(screen.getByRole('link', { name: 'Gestionar usuarios' })).toHaveAttribute('href', '/admin/users')
    expect(screen.getByRole('link', { name: 'Gestionar agencias' })).toHaveAttribute('href', '/admin/agencies')
    expect(screen.getByRole('link', { name: 'Consultar ventas' })).toHaveAttribute('href', '/admin/sales')
  })

  it('shows quick counts once they load', async () => {
    renderHub()

    await waitFor(() => {
      expect(screen.getByTestId('admin-count-packages')).toHaveTextContent('12 paquetes')
    })
    expect(screen.getByTestId('admin-count-flights')).toHaveTextContent(`${allFlights.length} vuelos`)
    expect(screen.getByTestId('admin-count-hotels')).toHaveTextContent('2 hoteles')
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
})
