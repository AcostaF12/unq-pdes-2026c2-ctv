import { QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { HotelDetail } from './HotelDetail'
import { AuthProvider } from '../../auth/AuthContext'
import { getCurrentUser } from '../../api/auth'
import { getHotel } from '../../api/hotels'
import { searchPackages, type TravelPackage } from '../../api/packages'
import { persistSession } from '../../api/session'
import { createTestQueryClient } from '../../test/queryClient'

vi.mock('../../api/auth', () => ({ getCurrentUser: vi.fn() }))
vi.mock('../../api/hotels', () => ({ getHotel: vi.fn(), deleteHotel: vi.fn() }))
vi.mock('../../api/packages', () => ({ searchPackages: vi.fn() }))

const mockedGetCurrentUser = vi.mocked(getCurrentUser)
const mockedGetHotel = vi.mocked(getHotel)
const mockedSearchPackages = vi.mocked(searchPackages)

const buyer = { id: 2, username: 'buyer', firstName: 'Bruno', lastName: 'Buyer', role: 'BUYER' }
const hotel = {
  id: 4,
  name: 'Hotel Palacio de París',
  city: { code: 'PAR', name: 'Paris' },
  photoUrl: 'https://example.com/par.jpg',
}
const travelPackage: TravelPackage = {
  id: 10,
  name: 'París Romántico',
  price: 1500,
  agency: { id: 1, name: 'Despegar' },
  hotel,
  origin: { code: 'BUE', name: 'Buenos Aires' },
  destination: { code: 'PAR', name: 'Paris' },
  outboundFlightId: 1,
  returnFlightId: 2,
}

function renderHotel() {
  persistSession('token', buyer)
  mockedGetCurrentUser.mockResolvedValue(buyer)
  return render(
    <QueryClientProvider client={createTestQueryClient()}>
      <AuthProvider>
        <MemoryRouter initialEntries={['/hotels/4']}>
          <Routes>
            <Route path="/hotels/:id" element={<HotelDetail />} />
          </Routes>
        </MemoryRouter>
      </AuthProvider>
    </QueryClientProvider>,
  )
}

describe('HotelDetail', () => {
  beforeEach(() => {
    mockedGetCurrentUser.mockReset()
    mockedGetHotel.mockReset()
    mockedSearchPackages.mockReset()
    mockedGetHotel.mockResolvedValue(hotel)
  })

  it('lists the packages that stay at this hotel and links to the filtered catalog', async () => {
    mockedSearchPackages.mockResolvedValue({
      content: [travelPackage],
      page: 0,
      size: 4,
      totalElements: 1,
      totalPages: 1,
    })
    renderHotel()

    expect(await screen.findByRole('heading', { name: 'Paquetes en este hotel' })).toBeInTheDocument()
    expect(await screen.findByRole('heading', { name: 'París Romántico' })).toBeInTheDocument()
    expect(mockedSearchPackages).toHaveBeenCalledWith({ hotel: 'Hotel Palacio de París', size: 4 })
    expect(screen.getByRole('link', { name: 'Ver todos →' })).toHaveAttribute(
      'href',
      '/trips?hotel=Hotel%20Palacio%20de%20Par%C3%ADs',
    )
  })

  it('shows no package section when the hotel has no packages', async () => {
    mockedSearchPackages.mockResolvedValue({ content: [], page: 0, size: 4, totalElements: 0, totalPages: 0 })
    renderHotel()

    expect(await screen.findByRole('heading', { name: 'Hotel Palacio de París' })).toBeInTheDocument()
    expect(screen.queryByText('Paquetes en este hotel')).not.toBeInTheDocument()
  })
})
