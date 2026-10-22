import { Suspense } from 'react'
import { QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { listFlights } from '../api/flights'
import { listAgencies } from '../api/agencies'
import { getAdminUsers, getAdminFavorites, getAdminReviews, getAdminPurchases, getAdminReports } from '../api/admin'
import { listHotels } from '../api/hotels'
import { searchPackages } from '../api/packages'
import { getCurrentUser } from '../api/auth'
import { persistSession } from '../api/session'
import { AuthProvider } from '../auth/AuthContext'
import { createTestQueryClient } from '../test/queryClient'
import { adminRoute } from './adminRoutes'

vi.mock('../api/auth', () => ({ getCurrentUser: vi.fn() }))
vi.mock('../api/admin', () => ({ getAdminUsers: vi.fn(), getAdminFavorites: vi.fn(), getAdminReviews: vi.fn(), getAdminPurchases: vi.fn(), getAdminReports: vi.fn() }))
vi.mock('../api/flights', () => ({ listFlights: vi.fn(), createFlight: vi.fn() }))
vi.mock('../api/hotels', () => ({ listHotels: vi.fn() }))
vi.mock('../api/agencies', () => ({ listAgencies: vi.fn(), createAgency: vi.fn(), updateAgency: vi.fn() }))
vi.mock('../api/cities', () => ({ listCities: vi.fn() }))
vi.mock('../api/packages', () => ({
  searchPackages: vi.fn(),
  createPackage: vi.fn(),
  updatePackage: vi.fn(),
  deletePackage: vi.fn(),
}))

function renderAt(path: string, role: string) {
  const user = { id: 1, username: 'u', firstName: 'Un', lastName: 'Usuario', role }
  persistSession('token', user)
  vi.mocked(getCurrentUser).mockResolvedValue(user)
  const router = createMemoryRouter([adminRoute], { initialEntries: [path] })

  return render(
    <QueryClientProvider client={createTestQueryClient()}>
      <AuthProvider>
        <Suspense fallback={<p>Cargando</p>}>
          <RouterProvider router={router} />
        </Suspense>
      </AuthProvider>
    </QueryClientProvider>,
  )
}

describe('admin routes', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(searchPackages).mockResolvedValue({
      content: [],
      page: 0,
      size: 1,
      totalElements: 0,
      totalPages: 0,
    })
    vi.mocked(listFlights).mockResolvedValue([])
    vi.mocked(listHotels).mockResolvedValue([])
    vi.mocked(listAgencies).mockResolvedValue([])
    const empty = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }
    vi.mocked(getAdminUsers).mockResolvedValue(empty)
    vi.mocked(getAdminFavorites).mockResolvedValue(empty)
    vi.mocked(getAdminReviews).mockResolvedValue(empty)
    vi.mocked(getAdminPurchases).mockResolvedValue(empty)
    vi.mocked(getAdminReports).mockResolvedValue({ topBuyers: [], topAgencies: [], topDestinations: [], topRatedDestinations: [] })
  })

  it.each(['/admin', '/admin/packages', '/admin/flights', '/admin/agencies', '/admin/users', '/admin/favorites', '/admin/reviews', '/admin/purchases', '/admin/reports'])('lets an admin open %s', async (path) => {
    renderAt(path, 'ADMIN')

    await screen.findByRole('heading', { level: 1 })
    expect(screen.queryByText('Sin permiso')).not.toBeInTheDocument()
  })

  it.each([
    ['/admin', 'BUYER'],
    ['/admin/packages', 'AGENCY'],
    ['/admin/flights', 'BUYER'],
    ['/admin/agencies', 'BUYER'],
    ['/admin/agencies', 'AGENCY'],
    ['/admin/users', 'BUYER'],
    ['/admin/users', 'AGENCY'],
    ['/admin/favorites', 'BUYER'],
    ['/admin/favorites', 'AGENCY'],
    ['/admin/reviews', 'BUYER'],
    ['/admin/reviews', 'AGENCY'],
    ['/admin/purchases', 'BUYER'],
    ['/admin/purchases', 'AGENCY'],
    ['/admin/reports', 'BUYER'],
    ['/admin/reports', 'AGENCY'],
  ])('blocks %s for the %s role', async (path, role) => {
    renderAt(path, role)

    expect(await screen.findByText('Sin permiso')).toBeInTheDocument()
    expect(screen.queryByTestId('admin-hub')).not.toBeInTheDocument()
    expect(searchPackages).not.toHaveBeenCalled()
    expect(listFlights).not.toHaveBeenCalled()
    expect(getAdminUsers).not.toHaveBeenCalled()
    expect(getAdminFavorites).not.toHaveBeenCalled()
    expect(getAdminReviews).not.toHaveBeenCalled()
    expect(getAdminPurchases).not.toHaveBeenCalled()
    expect(getAdminReports).not.toHaveBeenCalled()
  })
})
