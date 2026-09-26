import { QueryClientProvider } from '@tanstack/react-query'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { Purchases } from './Purchases'
import { getMyPurchases } from '../../api/purchases'
import { createTestQueryClient } from '../../test/queryClient'

vi.mock('../../api/purchases', () => ({
  getMyPurchases: vi.fn(),
}))

const mockedGetMyPurchases = vi.mocked(getMyPurchases)

const purchases = [
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
]

function renderPurchases() {
  return render(
    <QueryClientProvider client={createTestQueryClient()}>
      <MemoryRouter>
        <Purchases />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('Purchases page', () => {
  beforeEach(() => {
    mockedGetMyPurchases.mockReset()
  })

  it('shows purchase tickets after loading', async () => {
    mockedGetMyPurchases.mockResolvedValueOnce(purchases)
    renderPurchases()

    expect(screen.getByRole('status', { name: 'Cargando compras' })).toBeInTheDocument()
    expect(await screen.findByRole('link', { name: 'Escapada a París' })).toHaveAttribute(
      'href',
      '/trips/10',
    )
    expect(screen.getByText('Viajes del Sur')).toBeInTheDocument()
    expect(screen.getByText('Buenos Aires → París')).toBeInTheDocument()
    expect(screen.getByText('Hotel: Hotel Lumière')).toBeInTheDocument()
    expect(screen.getByText('Precio abonado')).toBeInTheDocument()
    expect(screen.getByText('USD 1200')).toBeInTheDocument()
    expect(document.querySelector('time')).toHaveAttribute('datetime', '2026-03-15T14:30:00')
    expect(screen.queryByText(/No pudimos cargar tus compras/)).not.toBeInTheDocument()
    expect(screen.queryByText('Todavía no hay viajes comprados')).not.toBeInTheDocument()
  })

  it('shows an empty state without an error', async () => {
    mockedGetMyPurchases.mockResolvedValueOnce([])
    renderPurchases()

    expect(await screen.findByText('Todavía no hay viajes comprados')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ver paquetes' })).toHaveAttribute('href', '/trips')
    expect(screen.queryByText(/No pudimos cargar tus compras/)).not.toBeInTheDocument()
  })

  it('shows an error without the empty state and allows retry', async () => {
    const user = userEvent.setup()
    mockedGetMyPurchases.mockRejectedValueOnce(new Error('boom'))
    mockedGetMyPurchases.mockResolvedValueOnce(purchases)
    renderPurchases()

    expect(await screen.findByText(/No pudimos cargar tus compras/)).toBeInTheDocument()
    expect(screen.queryByText('Todavía no hay viajes comprados')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /reintentar/i }))

    expect(await screen.findByRole('link', { name: 'Escapada a París' })).toBeInTheDocument()
    expect(screen.queryByText(/No pudimos cargar tus compras/)).not.toBeInTheDocument()
  })
})
