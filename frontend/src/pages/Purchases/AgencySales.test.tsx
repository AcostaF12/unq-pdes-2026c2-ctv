import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getAgencyPurchases } from '../../api/purchases'
import { createTestQueryClient } from '../../test/queryClient'
import { AgencySales } from './AgencySales'

vi.mock('../../api/purchases', () => ({
  getAgencyPurchases: vi.fn(),
}))

const mockedGetAgencyPurchases = vi.mocked(getAgencyPurchases)

function renderAgencySales() {
  return render(
    <QueryClientProvider client={createTestQueryClient()}>
      <MemoryRouter>
        <AgencySales />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('AgencySales page', () => {
  beforeEach(() => {
    mockedGetAgencyPurchases.mockReset()
  })

  it('shows the buyer identity and complete trip information for every sale', async () => {
    mockedGetAgencyPurchases.mockResolvedValueOnce([
      {
        id: 1,
        buyer: { id: 5, username: 'bruno', firstName: 'Bruno', lastName: 'Buyer' },
        travelPackage: {
          id: 10,
          name: 'Escapada a París',
          price: 1800,
          agency: { id: 2, name: 'Viajes del Sur' },
          hotel: {
            id: 4,
            name: 'Hotel Lumière',
            city: { code: 'PAR', name: 'París' },
            photoUrl: 'https://example.test/hotel.jpg',
          },
          origin: { code: 'BUE', name: 'Buenos Aires' },
          destination: { code: 'PAR', name: 'París' },
          outboundFlightId: 1,
          returnFlightId: 2,
        },
        purchasePrice: 1200,
        purchasedAt: '2026-03-15T14:30:00',
      },
    ])

    renderAgencySales()

    expect(await screen.findByText('Bruno Buyer (@bruno)')).toBeInTheDocument()
    expect(screen.getByText('Buenos Aires → París')).toBeInTheDocument()
    expect(screen.getByText('Hotel: Hotel Lumière')).toBeInTheDocument()
    expect(screen.getByText('Precio abonado')).toBeInTheDocument()
    expect(screen.getByText('USD 1200')).toBeInTheDocument()
  })

  it('shows a loading state while sales are requested', () => {
    mockedGetAgencyPurchases.mockReturnValueOnce(new Promise(() => {}))

    renderAgencySales()

    expect(screen.getByRole('status', { name: 'Cargando ventas' })).toBeInTheDocument()
  })

  it('shows the empty state when the agency has no sales', async () => {
    mockedGetAgencyPurchases.mockResolvedValueOnce([])

    renderAgencySales()

    expect(await screen.findByText('Todavía no hay ventas')).toBeInTheDocument()
    expect(screen.queryByText('Algo salió mal')).not.toBeInTheDocument()
  })

  it('shows an error and retries the request', async () => {
    const user = userEvent.setup()
    mockedGetAgencyPurchases.mockRejectedValueOnce(new Error('boom'))
    mockedGetAgencyPurchases.mockResolvedValueOnce([])
    renderAgencySales()

    expect(await screen.findByText(/No pudimos cargar las ventas/)).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /reintentar/i }))

    expect(await screen.findByText('Todavía no hay ventas')).toBeInTheDocument()
  })
})
