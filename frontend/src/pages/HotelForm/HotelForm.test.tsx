import { QueryClientProvider } from '@tanstack/react-query'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Link, MemoryRouter, Route, Routes } from 'react-router-dom'
import { HotelForm } from './HotelForm'
import { Hotels } from '../Hotels/Hotels'
import { AuthProvider } from '../../auth/AuthContext'
import { createTestQueryClient } from '../../test/queryClient'
import { persistSession } from '../../api/session'
import { getCurrentUser } from '../../api/auth'
import { listCities } from '../../api/cities'
import { createHotel, listHotels, type Hotel } from '../../api/hotels'
import { queryKeys } from '../../query/keys'

vi.mock('../../api/auth', () => ({
  getCurrentUser: vi.fn(),
}))

vi.mock('../../api/cities', () => ({
  listCities: vi.fn(),
}))

vi.mock('../../api/hotels', () => ({
  createHotel: vi.fn(),
  getHotel: vi.fn(),
  updateHotel: vi.fn(),
  listHotels: vi.fn(),
}))

const adminUser = {
  id: 1,
  username: 'facosta',
  firstName: 'Facundo',
  lastName: 'Acosta',
  role: 'ADMIN',
}

const existingHotel: Hotel = {
  id: 1,
  name: 'Gran Hotel Buenos Aires',
  photoUrl: 'https://example.com/bue.jpg',
  city: { code: 'BUE', name: 'Buenos Aires' },
}

const createdHotel: Hotel = {
  id: 99,
  name: 'Hotel Test',
  photoUrl: 'https://example.com/hotel.jpg',
  city: { code: 'BUE', name: 'Buenos Aires' },
}

const mockedGetCurrentUser = vi.mocked(getCurrentUser)
const mockedListCities = vi.mocked(listCities)
const mockedCreateHotel = vi.mocked(createHotel)
const mockedListHotels = vi.mocked(listHotels)

function renderNewHotelForm() {
  persistSession('token', adminUser)
  mockedGetCurrentUser.mockResolvedValue(adminUser)

  return render(
    <QueryClientProvider client={createTestQueryClient()}>
      <AuthProvider>
        <MemoryRouter initialEntries={['/hotels/new']}>
          <Routes>
            <Route path="/hotels/new" element={<HotelForm />} />
          </Routes>
        </MemoryRouter>
      </AuthProvider>
    </QueryClientProvider>,
  )
}

describe('HotelForm page', () => {
  beforeEach(() => {
    mockedGetCurrentUser.mockReset()
    mockedListCities.mockReset()
    mockedCreateHotel.mockReset()
    mockedListHotels.mockReset()
    mockedListCities.mockResolvedValue([{ code: 'BUE', name: 'Buenos Aires' }])
  })

  it('shows validation errors when submitting empty fields', async () => {
    const user = userEvent.setup()
    renderNewHotelForm()

    await screen.findByLabelText('Nombre')
    await user.click(screen.getByRole('button', { name: /crear hotel/i }))

    expect(await screen.findByText('Ingresá el nombre')).toBeInTheDocument()
    expect(screen.getByLabelText('Ciudad')).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByText('Ingresá la URL de la foto')).toBeInTheDocument()
    expect(mockedCreateHotel).not.toHaveBeenCalled()
  })

  it('creates a hotel with valid data', async () => {
    const user = userEvent.setup()
    mockedCreateHotel.mockResolvedValueOnce(createdHotel)
    renderNewHotelForm()

    await screen.findByLabelText('Nombre')
    await user.type(screen.getByLabelText('Nombre'), 'Hotel Test')
    await user.selectOptions(screen.getByLabelText('Ciudad'), 'BUE')
    await user.type(screen.getByLabelText('URL de la foto'), 'https://example.com/hotel.jpg')
    await user.click(screen.getByRole('button', { name: /crear hotel/i }))

    await waitFor(() => {
      expect(mockedCreateHotel).toHaveBeenCalledWith({
        name: 'Hotel Test',
        cityCode: 'BUE',
        photoUrl: 'https://example.com/hotel.jpg',
      })
    })
  })

  it('invalidates the hotels catalog after creating a hotel', async () => {
    const user = userEvent.setup()
    const client = createTestQueryClient({
      queries: { staleTime: 60_000, gcTime: 5 * 60_000, retry: false },
    })
    client.setQueryData(queryKeys.hotels.all, [existingHotel])
    client.setQueryData(queryKeys.cities, [{ code: 'BUE', name: 'Buenos Aires' }])
    mockedCreateHotel.mockResolvedValueOnce(createdHotel)
    mockedListHotels.mockResolvedValue([existingHotel, createdHotel])

    persistSession('token', adminUser)
    mockedGetCurrentUser.mockResolvedValue(adminUser)

    render(
      <QueryClientProvider client={client}>
        <AuthProvider>
          <MemoryRouter initialEntries={['/hotels/new']}>
            <Routes>
              <Route path="/hotels/new" element={<HotelForm />} />
              <Route path="/hotels/:id" element={<Link to="/hotels">Volver al catálogo</Link>} />
              <Route path="/hotels" element={<Hotels />} />
            </Routes>
          </MemoryRouter>
        </AuthProvider>
      </QueryClientProvider>,
    )

    await screen.findByLabelText('Nombre')
    await user.type(screen.getByLabelText('Nombre'), 'Hotel Test')
    await user.selectOptions(screen.getByLabelText('Ciudad'), 'BUE')
    await user.type(screen.getByLabelText('URL de la foto'), 'https://example.com/hotel.jpg')
    await user.click(screen.getByRole('button', { name: /crear hotel/i }))

    await user.click(await screen.findByRole('link', { name: /volver al catálogo/i }))

    expect(await screen.findByText('Hotel Test')).toBeInTheDocument()
    expect(screen.getByText('Gran Hotel Buenos Aires')).toBeInTheDocument()
    expect(mockedListHotels).toHaveBeenCalled()
  })
})
