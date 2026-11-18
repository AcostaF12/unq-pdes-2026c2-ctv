import { QueryClientProvider } from '@tanstack/react-query'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { Favorites } from './Favorites'
import { getFavorites, removeFavorite, type Favorite } from '../../api/favorites'
import type { TravelPackage } from '../../api/packages'
import { createTestQueryClient } from '../../test/queryClient'

vi.mock('../../api/favorites', () => ({
  getFavorites: vi.fn(),
  removeFavorite: vi.fn(),
}))

const mockedGetFavorites = vi.mocked(getFavorites)
const mockedRemoveFavorite = vi.mocked(removeFavorite)

const travelPackage: TravelPackage = {
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
}

const favorite: Favorite = {
  id: 1,
  packageId: 10,
  travelPackage,
}

function renderFavorites() {
  return render(
    <QueryClientProvider client={createTestQueryClient()}>
      <MemoryRouter>
        <Favorites />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('Favorites page', () => {
  beforeEach(() => {
    mockedGetFavorites.mockReset()
    mockedRemoveFavorite.mockReset()
  })

  it('shows an empty state with a catalog link', async () => {
    mockedGetFavorites.mockResolvedValueOnce([])
    renderFavorites()

    expect(await screen.findByText('Todavía no guardaste ningún viaje. Explorá el catálogo y marcá los que te gusten.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /explorar paquetes/i })).toHaveAttribute('href', '/trips')
  })

  it('shows an error and allows retry', async () => {
    const user = userEvent.setup()
    mockedGetFavorites.mockRejectedValueOnce(new Error('boom'))
    mockedGetFavorites.mockResolvedValueOnce([])
    renderFavorites()

    expect(
      await screen.findByText('No pudimos cargar tus favoritos. Intentá de nuevo.'),
    ).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /reintentar/i }))

    expect(await screen.findByRole('link', { name: /explorar paquetes/i })).toBeInTheDocument()
  })

  it('keeps the list and shows a compact alert when removing a favorite fails', async () => {
    const user = userEvent.setup()
    mockedGetFavorites.mockResolvedValue([favorite])
    mockedRemoveFavorite.mockRejectedValueOnce(new Error('boom'))
    renderFavorites()

    expect(await screen.findByText('París Romántico')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /quitar/i }))

    expect(await screen.findByText('No se pudo quitar el favorito.')).toBeInTheDocument()
    expect(screen.getByText('París Romántico')).toBeInTheDocument()
  })

  it('filters saved trips locally and reports the no-match state', async () => {
    const user = userEvent.setup()
    mockedGetFavorites.mockResolvedValue([
      favorite,
      {
        id: 2,
        packageId: 11,
        travelPackage: { ...travelPackage, id: 11, name: 'Roma Imperial', price: 900 },
      },
    ])
    renderFavorites()

    expect(await screen.findByText('Roma Imperial')).toBeInTheDocument()
    expect(screen.getByText('viajes guardados', { exact: false })).toBeInTheDocument()

    await user.type(screen.getByLabelText('Buscar en favoritos'), 'roma')
    expect(screen.queryByText('París Romántico')).not.toBeInTheDocument()
    expect(screen.getByText('Roma Imperial')).toBeInTheDocument()

    await user.clear(screen.getByLabelText('Buscar en favoritos'))
    await user.type(screen.getByLabelText('Buscar en favoritos'), 'xyz')
    expect(await screen.findByText('Ningún favorito coincide con tu búsqueda.')).toBeInTheDocument()
  })

  it('removes a favorite after the exit animation', async () => {
    const user = userEvent.setup()
    mockedGetFavorites.mockResolvedValueOnce([favorite])
    mockedGetFavorites.mockResolvedValue([])
    mockedRemoveFavorite.mockResolvedValue(undefined)
    renderFavorites()

    await user.click(await screen.findByRole('button', { name: 'Quitar de favoritos' }))

    expect(await screen.findByRole('link', { name: /explorar paquetes/i })).toBeInTheDocument()
    expect(mockedRemoveFavorite).toHaveBeenCalledWith(10)
  })

  it('removes two favorites clicked in quick succession exactly once each', async () => {
    const user = userEvent.setup()
    const second: Favorite = {
      id: 2,
      packageId: 11,
      travelPackage: { ...travelPackage, id: 11, name: 'Roma Imperial' },
    }
    mockedGetFavorites.mockResolvedValueOnce([favorite, second])
    mockedGetFavorites.mockResolvedValue([])
    mockedRemoveFavorite.mockResolvedValue(undefined)
    renderFavorites()

    const hearts = await screen.findAllByRole('button', { name: 'Quitar de favoritos' })
    await user.click(hearts[0])
    await user.click(hearts[1])

    expect(await screen.findByRole('link', { name: /explorar paquetes/i })).toBeInTheDocument()
    await waitFor(() => expect(mockedRemoveFavorite).toHaveBeenCalledTimes(2))
    expect(mockedRemoveFavorite).toHaveBeenCalledWith(10)
    expect(mockedRemoveFavorite).toHaveBeenCalledWith(11)
  })

  it('lists the most recently saved favorite first', async () => {
    mockedGetFavorites.mockResolvedValue([
      favorite,
      { id: 9, packageId: 11, travelPackage: { ...travelPackage, id: 11, name: 'Roma Imperial' } },
    ])
    renderFavorites()

    const titles = (await screen.findAllByRole('heading', { level: 2 })).map((node) => node.textContent)
    expect(titles).toEqual(['Roma Imperial', 'París Romántico'])
  })
})
