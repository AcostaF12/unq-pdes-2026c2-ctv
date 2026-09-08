import { QueryClientProvider } from '@tanstack/react-query'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { PackageDetail } from './PackageDetail'
import { AuthProvider } from '../../auth/AuthContext'
import { persistSession } from '../../api/session'
import { getCurrentUser } from '../../api/auth'
import { addFavorite, getFavorites, type Favorite } from '../../api/favorites'
import { getPackage, type TravelPackageDetail } from '../../api/packages'
import { getMyPurchases } from '../../api/purchases'
import { createTestQueryClient } from '../../test/queryClient'

vi.mock('../../api/auth', () => ({
  getCurrentUser: vi.fn(),
}))

vi.mock('../../api/packages', () => ({
  getPackage: vi.fn(),
}))

vi.mock('../../api/favorites', () => ({
  getFavorites: vi.fn(),
  addFavorite: vi.fn(),
  removeFavorite: vi.fn(),
}))

vi.mock('../../api/purchases', () => ({
  getMyPurchases: vi.fn(),
  buyPackage: vi.fn(),
}))

vi.mock('../../api/reviews', () => ({
  createReview: vi.fn(),
}))

const buyer = {
  id: 2,
  username: 'buyer',
  firstName: 'Bruno',
  lastName: 'Buyer',
  role: 'BUYER',
}

const travelPackage: TravelPackageDetail = {
  id: 10,
  name: 'París Romántico',
  price: 1500,
  agency: { id: 1, name: 'Despegar' },
  hotel: {
    id: 2,
    name: 'Hotel Palacio de París',
    photoUrl: 'https://example.com/par.jpg',
    city: { code: 'PAR', name: 'Paris' },
  },
  origin: { code: 'BUE', name: 'Buenos Aires' },
  destination: { code: 'PAR', name: 'Paris' },
  outboundFlightId: 1,
  returnFlightId: 2,
  outboundFlight: null,
  returnFlight: null,
  reviews: [],
  averageScore: null,
}

const mockedGetCurrentUser = vi.mocked(getCurrentUser)
const mockedGetPackage = vi.mocked(getPackage)
const mockedGetFavorites = vi.mocked(getFavorites)
const mockedAddFavorite = vi.mocked(addFavorite)
const mockedGetMyPurchases = vi.mocked(getMyPurchases)

describe('PackageDetail page', () => {
  beforeEach(() => {
    mockedGetCurrentUser.mockReset()
    mockedGetPackage.mockReset()
    mockedGetFavorites.mockReset()
    mockedAddFavorite.mockReset()
    mockedGetMyPurchases.mockReset()
    persistSession('token', buyer)
    mockedGetCurrentUser.mockResolvedValue(buyer)
    mockedGetPackage.mockResolvedValue(travelPackage)
    mockedGetMyPurchases.mockResolvedValue([])
  })

  it('refetches favorites after starring a package', async () => {
    const user = userEvent.setup()
    let favorites: Favorite[] = []
    mockedGetFavorites.mockImplementation(async () => favorites)
    mockedAddFavorite.mockImplementation(async (packageId) => {
      const favorite: Favorite = {
        id: 1,
        packageId,
        travelPackage,
      }
      favorites = [favorite]
      return favorite
    })

    render(
      <QueryClientProvider client={createTestQueryClient()}>
        <AuthProvider>
          <MemoryRouter initialEntries={['/trips/10']}>
            <Routes>
              <Route path="/trips/:id" element={<PackageDetail />} />
            </Routes>
          </MemoryRouter>
        </AuthProvider>
      </QueryClientProvider>,
    )

    expect(await screen.findByRole('button', { name: 'Favorito' })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Favorito' }))
    expect(await screen.findByRole('button', { name: 'Quitar favorito' })).toBeInTheDocument()
    expect(mockedAddFavorite).toHaveBeenCalledWith(10)
  })

  it('shows a catalog empty state when the package cannot be loaded', async () => {
    mockedGetPackage.mockRejectedValueOnce(new Error('boom'))
    mockedGetFavorites.mockResolvedValueOnce([])
    mockedGetMyPurchases.mockResolvedValueOnce([])

    render(
      <QueryClientProvider client={createTestQueryClient()}>
        <AuthProvider>
          <MemoryRouter initialEntries={['/trips/10']}>
            <Routes>
              <Route path="/trips/:id" element={<PackageDetail />} />
            </Routes>
          </MemoryRouter>
        </AuthProvider>
      </QueryClientProvider>,
    )

    expect(
      await screen.findByText('No pudimos cargar este paquete. Intentá de nuevo.'),
    ).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /volver al catálogo/i })).toHaveAttribute('href', '/trips')
  })
})
