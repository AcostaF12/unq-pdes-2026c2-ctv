import { QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { listCities } from '../../api/cities'
import { createFlight, listFlights } from '../../api/flights'
import { allFlights, apiError, outboundToParis } from '../../test/fixtures'
import { createTestQueryClient } from '../../test/queryClient'
import { AdminFlights } from './AdminFlights'

vi.mock('../../api/cities', () => ({ listCities: vi.fn() }))
vi.mock('../../api/flights', () => ({ listFlights: vi.fn(), createFlight: vi.fn() }))

const mockedListFlights = vi.mocked(listFlights)
const mockedCreateFlight = vi.mocked(createFlight)

function renderAdminFlights() {
  return render(
    <QueryClientProvider client={createTestQueryClient()}>
      <MemoryRouter>
        <AdminFlights />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

async function waitForCities() {
  await within(screen.getByLabelText('Origen')).findByRole('option', { name: 'Madrid (MAD)' })
}

async function fillValidFlight(user: ReturnType<typeof userEvent.setup>) {
  await waitForCities()
  await user.type(screen.getByLabelText('Aerolínea'), 'Aerolíneas Test')
  await user.selectOptions(screen.getByLabelText('Origen'), 'BUE')
  await user.selectOptions(screen.getByLabelText('Destino'), 'PAR')
  fireEvent.change(screen.getByLabelText('Fecha'), { target: { value: '2026-12-20' } })
  fireEvent.change(screen.getByLabelText('Hora de salida'), { target: { value: '14:45' } })
  await user.type(screen.getByLabelText('Capacidad'), '120')
  await user.type(screen.getByLabelText('Lugares disponibles'), '120')
}

describe('AdminFlights page', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    mockedListFlights.mockResolvedValue(allFlights)
    vi.mocked(listCities).mockResolvedValue([
      { code: 'BUE', name: 'Buenos Aires' },
      { code: 'PAR', name: 'París' },
      { code: 'MAD', name: 'Madrid' },
    ])
  })

  it('lists flights ordered by departure with route and seats', async () => {
    renderAdminFlights()

    const items = await screen.findAllByTestId('flight-item')
    expect(items).toHaveLength(allFlights.length)
    expect(within(items[0]).getByText('Buenos Aires (BUE) → París (PAR)')).toBeInTheDocument()
    expect(within(items[0]).getByText('2026-12-01 · 08:30')).toBeInTheDocument()
    expect(within(items[0]).getByText('40 / 100 lugares')).toBeInTheDocument()
    expect(mockedListFlights).toHaveBeenCalledWith({})
  })

  it('sends the origin, destination and date filters to the backend', async () => {
    const user = userEvent.setup()
    renderAdminFlights()
    await screen.findAllByTestId('flight-item')
    await waitForCities()

    await user.selectOptions(screen.getByLabelText('Filtrar por origen'), 'BUE')
    await waitFor(() => {
      expect(mockedListFlights).toHaveBeenLastCalledWith({ origin: 'BUE' })
    })

    await user.selectOptions(screen.getByLabelText('Filtrar por destino'), 'PAR')
    fireEvent.change(screen.getByLabelText('Filtrar por fecha'), { target: { value: '2026-12-01' } })
    await waitFor(() => {
      expect(mockedListFlights).toHaveBeenLastCalledWith({
        origin: 'BUE',
        destination: 'PAR',
        date: '2026-12-01',
      })
    })

    mockedListFlights.mockResolvedValue([])
    await user.click(screen.getByRole('button', { name: 'Limpiar filtros' }))
    await waitFor(() => {
      expect(mockedListFlights).toHaveBeenLastCalledWith({})
    })
  })

  it('shows an empty state that mentions the filters', async () => {
    const user = userEvent.setup()
    renderAdminFlights()
    await screen.findAllByTestId('flight-item')
    await waitForCities()
    mockedListFlights.mockResolvedValue([])

    await user.selectOptions(screen.getByLabelText('Filtrar por origen'), 'MAD')

    expect(await screen.findByText('No hay vuelos para esos filtros.')).toBeInTheDocument()
  })

  it('validates the form on the client before calling the API', async () => {
    const user = userEvent.setup()
    renderAdminFlights()

    await user.click(await screen.findByRole('button', { name: 'Crear vuelo' }))

    await waitFor(() => {
      expect(screen.getAllByRole('alert').map((alert) => alert.textContent)).toEqual([
        'Ingresá la aerolínea',
        'Elegí el origen',
        'Elegí el destino',
        'Ingresá la fecha',
        'Ingresá la hora de salida',
        'Ingresá un número entero',
        'Ingresá un número entero',
      ])
    })
    expect(screen.getByLabelText('Origen')).toHaveAttribute('aria-invalid', 'true')
    expect(mockedCreateFlight).not.toHaveBeenCalled()
  })

  it('rejects an identical origin and destination and more seats than capacity', async () => {
    const user = userEvent.setup()
    renderAdminFlights()
    await fillValidFlight(user)
    await user.selectOptions(screen.getByLabelText('Destino'), 'BUE')
    await user.clear(screen.getByLabelText('Lugares disponibles'))
    await user.type(screen.getByLabelText('Lugares disponibles'), '500')
    await user.click(screen.getByRole('button', { name: 'Crear vuelo' }))

    expect(await screen.findByText('El destino debe ser distinto del origen')).toBeInTheDocument()
    expect(mockedCreateFlight).not.toHaveBeenCalled()

    await user.selectOptions(screen.getByLabelText('Destino'), 'PAR')
    await user.click(screen.getByRole('button', { name: 'Crear vuelo' }))

    expect(
      await screen.findByText('Los lugares disponibles no pueden superar la capacidad'),
    ).toBeInTheDocument()
    expect(mockedCreateFlight).not.toHaveBeenCalled()
  })

  it('creates a flight, confirms it and refreshes the list', async () => {
    const user = userEvent.setup()
    mockedCreateFlight.mockResolvedValue(outboundToParis)
    renderAdminFlights()
    await fillValidFlight(user)
    await user.click(screen.getByRole('button', { name: 'Crear vuelo' }))

    await waitFor(() => {
      expect(mockedCreateFlight).toHaveBeenCalledTimes(1)
    })
    expect(mockedCreateFlight.mock.calls[0][0]).toEqual({
      airline: 'Aerolíneas Test',
      origin: 'BUE',
      destination: 'PAR',
      flightDate: '2026-12-20',
      departureTime: '14:45',
      capacity: 120,
      availability: 120,
    })
    expect(await screen.findByText('Vuelo creado.')).toBeInTheDocument()
    expect(screen.getByLabelText('Aerolínea')).toHaveValue('')
    await waitFor(() => {
      expect(mockedListFlights.mock.calls.length).toBeGreaterThan(1)
    })
  })

  it('shows the server message when the flight is rejected', async () => {
    const user = userEvent.setup()
    mockedCreateFlight.mockRejectedValue(apiError('La capacidad debe ser mayor a cero'))
    renderAdminFlights()
    await fillValidFlight(user)
    await user.click(screen.getByRole('button', { name: 'Crear vuelo' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('La capacidad debe ser mayor a cero')
  })
})
