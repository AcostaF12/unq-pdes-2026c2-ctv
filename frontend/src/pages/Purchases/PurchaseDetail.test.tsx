import { QueryClientProvider } from '@tanstack/react-query'
import { AxiosError, type AxiosResponse } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { PurchaseDetail } from './PurchaseDetail'
import { AuthProvider } from '../../auth/AuthContext'
import { getCurrentUser } from '../../api/auth'
import { getPurchase, type PurchaseDetail as PurchaseDetailModel } from '../../api/purchases'
import { persistSession } from '../../api/session'
import { createTestQueryClient } from '../../test/queryClient'

vi.mock('../../api/auth', () => ({ getCurrentUser: vi.fn() }))
vi.mock('../../api/purchases', () => ({ getPurchase: vi.fn() }))

const mockedGetCurrentUser = vi.mocked(getCurrentUser)
const mockedGetPurchase = vi.mocked(getPurchase)

const buyer = { id: 5, username: 'bruno', firstName: 'Bruno', lastName: 'Buyer', role: 'BUYER' }
const agency = {
  id: 9,
  username: 'agency',
  firstName: 'Agus',
  lastName: 'Agency',
  role: 'AGENCY',
}

const purchase: PurchaseDetailModel = {
  id: 12,
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
  purchasePrice: 1750,
  purchasedAt: '2026-03-15T14:30:00',
  outboundFlight: {
    id: 1,
    airline: 'Aerolíneas Argentinas',
    flightDate: '2026-12-01',
    departureTime: '08:00:00',
    origin: 'BUE',
    destination: 'PAR',
    capacity: 180,
    availability: 120,
  },
  returnFlight: {
    id: 2,
    airline: 'Aerolíneas Argentinas',
    flightDate: '2026-12-08',
    departureTime: '20:30:00',
    origin: 'PAR',
    destination: 'BUE',
    capacity: 180,
    availability: 120,
  },
  review: null,
}

function renderDetail(role: typeof buyer | typeof agency = buyer) {
  persistSession('token', role)
  mockedGetCurrentUser.mockResolvedValue(role)
  return render(
    <QueryClientProvider client={createTestQueryClient()}>
      <AuthProvider>
        <MemoryRouter initialEntries={['/purchases/12']}>
          <Routes>
            <Route path="/purchases/:id" element={<PurchaseDetail />} />
          </Routes>
        </MemoryRouter>
      </AuthProvider>
    </QueryClientProvider>,
  )
}

describe('PurchaseDetail', () => {
  beforeEach(() => {
    mockedGetCurrentUser.mockReset()
    mockedGetPurchase.mockReset()
  })

  it('shows the whole trip: code, flights, hotel, passenger and price paid', async () => {
    mockedGetPurchase.mockResolvedValue(purchase)
    renderDetail()

    expect(await screen.findByRole('heading', { name: 'Escapada a París' })).toBeInTheDocument()
    expect(screen.getByText('CTV-000012')).toBeInTheDocument()
    expect(screen.getByText('USD 1750')).toBeInTheDocument()
    expect(screen.getByText('BUE → PAR')).toBeInTheDocument()
    expect(screen.getByText('PAR → BUE')).toBeInTheDocument()
    expect(screen.getByText('7 noches en París')).toBeInTheDocument()
    expect(screen.getByText('Hotel Lumière')).toBeInTheDocument()
    expect(screen.getByText('Viajes del Sur')).toBeInTheDocument()
    expect(screen.getByText('Bruno Buyer')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '← Mis compras' })).toHaveAttribute('href', '/purchases')
    expect(screen.getByRole('link', { name: 'Dejar reseña →' })).toHaveAttribute('href', '/trips/10')
  })

  it('shows the buyer review when present', async () => {
    mockedGetPurchase.mockResolvedValue({
      ...purchase,
      review: { id: 3, buyerUsername: 'bruno', packageId: 10, score: 9, comment: 'Inolvidable' },
    })
    renderDetail()

    expect(await screen.findByText('Inolvidable')).toBeInTheDocument()
    expect(screen.getByText('9/10')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Dejar reseña →' })).not.toBeInTheDocument()
  })

  it('degrades gracefully when flight data is unavailable and points agencies back to sales', async () => {
    mockedGetPurchase.mockResolvedValue({ ...purchase, outboundFlight: null, returnFlight: null })
    renderDetail(agency)

    expect(await screen.findByText(/Vuelo #1 · datos no disponibles/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '← Ventas' })).toHaveAttribute('href', '/agency/sales')
    expect(screen.queryByText('Tu reseña')).not.toBeInTheDocument()
  })

  it('shows a not-found state for purchases that do not exist or are not accessible', async () => {
    const response = { status: 404, data: {} } as AxiosResponse
    mockedGetPurchase.mockRejectedValue(new AxiosError('nope', '404', undefined, undefined, response))
    renderDetail()

    expect(await screen.findByText('No encontramos esta compra')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Volver' })).toHaveAttribute('href', '/purchases')
  })
})
