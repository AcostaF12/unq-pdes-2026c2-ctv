import { QueryClientProvider } from '@tanstack/react-query'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
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
})
