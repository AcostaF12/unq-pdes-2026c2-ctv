import { QueryClientProvider } from '@tanstack/react-query'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { Hotels } from './Hotels'
import { listHotels } from '../../api/hotels'
import { listCities } from '../../api/cities'
import { AuthProvider } from '../../auth/AuthContext'
import { createTestQueryClient } from '../../test/queryClient'

vi.mock('../../api/hotels', () => ({
  listHotels: vi.fn(),
}))

vi.mock('../../api/cities', () => ({
  listCities: vi.fn(),
}))

const mockedListHotels = vi.mocked(listHotels)
const mockedListCities = vi.mocked(listCities)

const hotels = [
  {
    id: 1,
    name: 'Gran Hotel Buenos Aires',
    photoUrl: 'https://example.com/bue.jpg',
    city: { code: 'BUE', name: 'Buenos Aires' },
  },
  {
    id: 2,
    name: 'Hotel Palacio de París',
    photoUrl: 'https://example.com/par.jpg',
    city: { code: 'PAR', name: 'Paris' },
  },
]

function renderHotels() {
  return render(
    <QueryClientProvider client={createTestQueryClient()}>
      <AuthProvider>
        <MemoryRouter>
          <Hotels />
        </MemoryRouter>
      </AuthProvider>
    </QueryClientProvider>,
  )
}

describe('Hotels page', () => {
  beforeEach(() => {
    mockedListHotels.mockReset()
    mockedListCities.mockReset()
    mockedListCities.mockResolvedValue([
      { code: 'BUE', name: 'Buenos Aires' },
      { code: 'PAR', name: 'Paris' },
      { code: 'NYC', name: 'New York' },
    ])
  })

  it('shows hotel cards after loading', async () => {
    mockedListHotels.mockResolvedValueOnce(hotels)
    renderHotels()

    expect(screen.getByRole('status', { name: 'Cargando hoteles' })).toBeInTheDocument()
    expect(await screen.findByText('Gran Hotel Buenos Aires')).toBeInTheDocument()
    expect(screen.getByText('Hotel Palacio de París')).toBeInTheDocument()
  })

  it('filters hotels by city', async () => {
    const user = userEvent.setup()
    mockedListHotels.mockResolvedValueOnce(hotels)
    renderHotels()

    await screen.findByText('Gran Hotel Buenos Aires')
    await user.selectOptions(screen.getByLabelText('Filtrar por ciudad'), 'PAR')

    expect(screen.queryByText('Gran Hotel Buenos Aires')).not.toBeInTheDocument()
    expect(screen.getByText('Hotel Palacio de París')).toBeInTheDocument()
  })

  it('shows an empty state when the filter has no matches', async () => {
    const user = userEvent.setup()
    mockedListHotels.mockResolvedValueOnce(hotels)
    renderHotels()

    await screen.findByText('Gran Hotel Buenos Aires')
    await user.selectOptions(screen.getByLabelText('Filtrar por ciudad'), 'NYC')

    expect(screen.queryByText('Gran Hotel Buenos Aires')).not.toBeInTheDocument()
    expect(screen.getByText('No hay hoteles para esa ciudad.')).toBeInTheDocument()
  })

  it('shows an error and allows retry', async () => {
    const user = userEvent.setup()
    mockedListHotels.mockRejectedValueOnce(new Error('boom'))
    mockedListHotels.mockResolvedValueOnce(hotels)
    renderHotels()

    expect(await screen.findByText('No pudimos cargar los hoteles. Intentá de nuevo.')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /reintentar/i }))
    expect(await screen.findByText('Gran Hotel Buenos Aires')).toBeInTheDocument()
  })

  it('does not show the create action for a guest catalog user', async () => {
    mockedListHotels.mockResolvedValueOnce(hotels)
    renderHotels()

    await screen.findByText('Gran Hotel Buenos Aires')
    expect(screen.queryByRole('link', { name: /nuevo hotel/i })).not.toBeInTheDocument()
  })
})
