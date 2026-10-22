import { QueryClientProvider } from '@tanstack/react-query'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { PackageDetail } from './PackageDetail'
import { AuthProvider } from '../../auth/AuthContext'
import { persistSession } from '../../api/session'
import { getCurrentUser } from '../../api/auth'
import { addFavorite, getFavorites, type Favorite } from '../../api/favorites'
import { getPackage, searchPackages, type TravelPackageDetail } from '../../api/packages'
import { buyPackage, hasPurchasedPackage } from '../../api/purchases'
import { createReview } from '../../api/reviews'
import { createTestQueryClient } from '../../test/queryClient'

vi.mock('../../api/auth', () => ({
  getCurrentUser: vi.fn(),
}))

vi.mock('../../api/packages', () => ({
  getPackage: vi.fn(),
  searchPackages: vi.fn(),
}))

vi.mock('../../api/favorites', () => ({
  getFavorites: vi.fn(),
  addFavorite: vi.fn(),
  removeFavorite: vi.fn(),
}))

vi.mock('../../api/purchases', () => ({
  hasPurchasedPackage: vi.fn(),
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
const mockedSearchPackages = vi.mocked(searchPackages)
const mockedGetFavorites = vi.mocked(getFavorites)
const mockedAddFavorite = vi.mocked(addFavorite)
const mockedHasPurchasedPackage = vi.mocked(hasPurchasedPackage)
const mockedBuyPackage = vi.mocked(buyPackage)
const mockedCreateReview = vi.mocked(createReview)

describe('PackageDetail page', () => {
  beforeEach(() => {
    mockedGetCurrentUser.mockReset()
    mockedGetPackage.mockReset()
    mockedGetFavorites.mockReset()
    mockedAddFavorite.mockReset()
    mockedHasPurchasedPackage.mockReset()
    mockedBuyPackage.mockReset()
    persistSession('token', buyer)
    mockedGetCurrentUser.mockResolvedValue(buyer)
    mockedGetPackage.mockResolvedValue(travelPackage)
    mockedSearchPackages.mockResolvedValue({ content: [], page: 0, size: 4, totalElements: 0, totalPages: 0 })
    mockedHasPurchasedPackage.mockResolvedValue(false)
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
    await waitFor(() => expect(screen.getByRole('button', { name: 'Favorito' })).toBeEnabled())
    await user.click(screen.getByRole('button', { name: 'Favorito' }))
    expect(await screen.findByRole('button', { name: 'Quitar favorito' })).toBeInTheDocument()
    expect(mockedAddFavorite).toHaveBeenCalledWith(10)
  })

  it('shows a catalog empty state when the package cannot be loaded', async () => {
    mockedGetPackage.mockRejectedValueOnce(new Error('boom'))
    mockedGetFavorites.mockResolvedValueOnce([])
    mockedHasPurchasedPackage.mockResolvedValueOnce(false)

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

  function renderDetail() {
    return render(
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
  }

  it('confirms the purchase in a dialog and shows the booking code', async () => {
    const user = userEvent.setup()
    mockedGetFavorites.mockResolvedValue([])
    mockedBuyPackage.mockResolvedValue({
      id: 7,
      buyer,
      travelPackage,
      purchasePrice: 1500,
      purchasedAt: '2026-03-15T14:30:00',
    })
    renderDetail()

    await user.click(await screen.findByRole('button', { name: 'Comprar' }))
    const dialog = await screen.findByRole('dialog', { name: 'Confirmá tu compra' })
    expect(dialog).toHaveTextContent('Bruno Buyer')
    expect(dialog).toHaveTextContent('USD 1500')
    expect(mockedBuyPackage).not.toHaveBeenCalled()

    mockedHasPurchasedPackage.mockResolvedValue(true)
    await user.click(screen.getByRole('button', { name: 'Confirmar compra' }))

    expect(
      await screen.findByText('Compra realizada. Los asientos de ida y vuelta quedaron reservados.'),
    ).toBeInTheDocument()
    expect(screen.getByText('CTV-000007')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ver detalle de mi compra' })).toHaveAttribute(
      'href',
      '/purchases/7',
    )
    expect(mockedBuyPackage).toHaveBeenCalledWith(10)

    await user.click(screen.getByRole('button', { name: 'Seguir explorando' }))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(await screen.findByRole('button', { name: 'Ya comprado' })).toBeDisabled()
  })

  it('keeps the dialog open with the API error when the purchase fails and can be cancelled', async () => {
    const user = userEvent.setup()
    mockedGetFavorites.mockResolvedValue([])
    mockedBuyPackage.mockRejectedValue(new Error('boom'))
    renderDetail()

    await user.click(await screen.findByRole('button', { name: 'Comprar' }))
    await user.click(await screen.findByRole('button', { name: 'Confirmar compra' }))

    expect(await screen.findByText('No se pudo completar la compra.')).toBeInTheDocument()
    expect(screen.getByRole('dialog')).toBeInTheDocument()

    await user.keyboard('{Escape}')
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('warns and blocks the purchase when a flight has no seats left', async () => {
    const user = userEvent.setup()
    mockedGetFavorites.mockResolvedValue([])
    mockedGetPackage.mockResolvedValue({
      ...travelPackage,
      outboundFlight: {
        id: 1,
        airline: 'Aerolíneas Argentinas',
        flightDate: '2026-12-01',
        departureTime: '08:00:00',
        origin: 'BUE',
        destination: 'PAR',
        capacity: 180,
        availability: 0,
      },
    })
    renderDetail()

    await user.click(await screen.findByRole('button', { name: 'Comprar' }))

    expect(await screen.findByText('Uno de los vuelos ya no tiene asientos disponibles.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Confirmar compra' })).toBeDisabled()
  })

  it('shows the rating summary and the score picker for a buyer who purchased', async () => {
    const user = userEvent.setup()
    mockedGetFavorites.mockResolvedValue([])
    mockedHasPurchasedPackage.mockResolvedValue(true)
    mockedGetPackage.mockResolvedValue({
      ...travelPackage,
      averageScore: 9,
      reviews: [{ id: 1, buyerUsername: 'otra', packageId: 10, score: 9, comment: 'Soñado' }],
    })
    renderDetail()

    expect(await screen.findByText('Soñado')).toBeInTheDocument()
    expect(screen.getByText('Excelente (9-10)')).toBeInTheDocument()
    expect(screen.getByRole('radio', { name: '8' })).toBeChecked()
    await user.click(screen.getByRole('radio', { name: '10' }))
    expect(screen.getByRole('radio', { name: '10' })).toBeChecked()
  })

  it('suggests other trips to the same destination without repeating the current one', async () => {
    mockedGetFavorites.mockResolvedValue([])
    mockedSearchPackages.mockResolvedValue({
      content: [
        { ...travelPackage, id: 10 },
        { ...travelPackage, id: 11, name: 'París Gourmet' },
      ],
      page: 0,
      size: 4,
      totalElements: 2,
      totalPages: 1,
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

    expect(await screen.findByRole('heading', { name: 'París Gourmet' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ver todos →' })).toHaveAttribute('href', '/trips?destination=PAR')
    expect(mockedSearchPackages).toHaveBeenCalledWith({ destination: 'PAR', size: 4 })
    // the current package only appears as the page heading, not as a suggestion
    expect(screen.getAllByRole('heading', { name: 'París Romántico' })).toHaveLength(1)
  })

  it('shows a failed review inside the form instead of at the top of the page', async () => {
    const user = userEvent.setup()
    mockedGetFavorites.mockResolvedValue([])
    mockedHasPurchasedPackage.mockResolvedValue(true)
    mockedCreateReview.mockRejectedValue(new Error('boom'))
    renderDetail()

    await user.click(await screen.findByRole('button', { name: 'Publicar reseña' }))

    const alert = await screen.findByText('No se pudo publicar la reseña.')
    expect(alert.closest('form')).not.toBeNull()
  })

  it('refreshes the flights after a purchase fails so seat numbers are not stale', async () => {
    const user = userEvent.setup()
    mockedGetFavorites.mockResolvedValue([])
    mockedBuyPackage.mockRejectedValue(new Error('boom'))
    renderDetail()

    await user.click(await screen.findByRole('button', { name: 'Comprar' }))
    await user.click(await screen.findByRole('button', { name: 'Confirmar compra' }))
    await screen.findByText('No se pudo completar la compra.')

    await waitFor(() => expect(mockedGetPackage.mock.calls.length).toBeGreaterThan(1))
  })

  it('closes the checkout with Escape even when the focus is outside the dialog', async () => {
    const user = userEvent.setup()
    mockedGetFavorites.mockResolvedValue([])
    renderDetail()

    await user.click(await screen.findByRole('button', { name: 'Comprar' }))
    await screen.findByRole('dialog')
    ;(document.activeElement as HTMLElement | null)?.blur()
    await user.keyboard('{Escape}')

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })
})
