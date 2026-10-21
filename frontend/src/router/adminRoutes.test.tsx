import { Suspense } from 'react'
import { QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { listFlights } from '../api/flights'
import { listHotels } from '../api/hotels'
import { searchPackages } from '../api/packages'
import { getCurrentUser } from '../api/auth'
import { persistSession } from '../api/session'
import { AuthProvider } from '../auth/AuthContext'
import { createTestQueryClient } from '../test/queryClient'
import { adminRoute } from './adminRoutes'

vi.mock('../api/auth', () => ({ getCurrentUser: vi.fn() }))
vi.mock('../api/flights', () => ({ listFlights: vi.fn(), createFlight: vi.fn() }))
vi.mock('../api/hotels', () => ({ listHotels: vi.fn() }))
vi.mock('../api/agencies', () => ({ listAgencies: vi.fn() }))
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
  })

  it.each(['/admin', '/admin/packages', '/admin/flights'])('lets an admin open %s', async (path) => {
    renderAt(path, 'ADMIN')

    await screen.findByRole('heading', { level: 1 })
    expect(screen.queryByText('Sin permiso')).not.toBeInTheDocument()
  })

  it.each([
    ['/admin', 'BUYER'],
    ['/admin/packages', 'AGENCY'],
    ['/admin/flights', 'BUYER'],
  ])('blocks %s for the %s role', async (path, role) => {
    renderAt(path, role)

    expect(await screen.findByText('Sin permiso')).toBeInTheDocument()
    expect(screen.queryByTestId('admin-hub')).not.toBeInTheDocument()
    expect(searchPackages).not.toHaveBeenCalled()
    expect(listFlights).not.toHaveBeenCalled()
  })
})
