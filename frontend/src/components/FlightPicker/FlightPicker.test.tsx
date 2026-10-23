import { useState } from 'react'
import { QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { listCities } from '../../api/cities'
import { listFlights } from '../../api/flights'
import { allFlights, apiError, laterOutboundToParis, outboundToParis, returnFromParis } from '../../test/fixtures'
import { createTestQueryClient } from '../../test/queryClient'
import { FlightPicker, type FlightSelection } from './FlightPicker'
import { describeFlight, describeSeats, flightChoiceLabel, outboundOptions, returnOptions } from './flightOptions'

vi.mock('../../api/flights', () => ({
  listFlights: vi.fn(),
}))
vi.mock('../../api/cities', () => ({ listCities: vi.fn() }))

const mockedListFlights = vi.mocked(listFlights)

describe('flight option helpers', () => {
  it('keeps only flights arriving at the destination, ordered by departure', () => {
    const options = outboundOptions(allFlights, 'PAR')

    expect(options.map((flight) => flight.id)).toEqual([1, 6])
    expect(outboundOptions(allFlights, null)).toEqual([])
  })

  it('keeps only flights on the reverse route of the outbound flight', () => {
    expect(returnOptions(allFlights, outboundToParis).map((flight) => flight.id)).toEqual([2])
    expect(returnOptions(allFlights, undefined)).toEqual([])
  })

  it('describes flights with airline, date, time, route and seats left', () => {
    expect(describeFlight(returnFromParis)).toBe('Air Sur · 2026-12-10 08:30 · PAR → BUE · 1 lugar')
    expect(flightChoiceLabel(returnFromParis)).toBe('Air Sur · 08:30 · 1 lugar')
    expect(describeSeats(0)).toBe('sin lugares')
    expect(describeSeats(5)).toBe('5 lugares')
  })
})

function Harness({ destinationCode }: { destinationCode: string | null }) {
  const [selection, setSelection] = useState<FlightSelection>({ outboundFlightId: '', returnFlightId: '' })
  return (
    <FlightPicker destinationCode={destinationCode} {...selection} onChange={setSelection} />
  )
}

function renderPicker(destinationCode: string | null) {
  return render(
    <QueryClientProvider client={createTestQueryClient()}>
      <Harness destinationCode={destinationCode} />
    </QueryClientProvider>,
  )
}

function optionLabels(select: HTMLElement): string[] {
  return within(select).getAllByRole('option').map((option) => option.textContent ?? '')
}

describe('FlightPicker', () => {
  beforeEach(() => {
    mockedListFlights.mockReset()
    mockedListFlights.mockResolvedValue(allFlights)
    vi.mocked(listCities).mockResolvedValue([
      { code: 'BUE', name: 'Buenos Aires' },
      { code: 'MAD', name: 'Madrid' },
    ])
  })

  it('asks for a hotel first and does not load flights without a destination', () => {
    renderPicker(null)

    expect(screen.getByLabelText('Vuelo de ida')).toBeDisabled()
    expect(screen.getByLabelText('Vuelo de ida')).toHaveDisplayValue('Elegí un hotel primero')
    expect(mockedListFlights).not.toHaveBeenCalled()
  })

  it('offers only flights to the hotel city and the matching reverse flights', async () => {
    const user = userEvent.setup()
    renderPicker('PAR')

    const outbound = screen.getByLabelText('Vuelo de ida')
    await waitFor(() => {
      expect(optionLabels(outbound)).toEqual([
        'Elegí un vuelo de ida',
        flightChoiceLabel(outboundToParis),
        flightChoiceLabel(laterOutboundToParis),
      ])
    })
    expect(mockedListFlights).toHaveBeenCalledWith({ destination: 'PAR' })
    expect(screen.getByLabelText('Vuelo de vuelta')).toBeDisabled()

    await user.selectOptions(outbound, String(outboundToParis.id))

    const inbound = screen.getByLabelText('Vuelo de vuelta')
    await waitFor(() => {
      expect(optionLabels(inbound)).toEqual(['Elegí un vuelo de vuelta', flightChoiceLabel(returnFromParis)])
      expect(inbound).toHaveValue(String(returnFromParis.id))
    })
    expect(mockedListFlights).toHaveBeenCalledWith({ origin: 'PAR', destination: 'BUE' })
  })

  it('keeps the return flight when the new outbound uses the same route', async () => {
    const user = userEvent.setup()
    renderPicker('PAR')

    const outbound = screen.getByLabelText('Vuelo de ida')
    await screen.findByRole('option', { name: flightChoiceLabel(outboundToParis) })
    await user.selectOptions(outbound, String(outboundToParis.id))
    await screen.findByRole('option', { name: flightChoiceLabel(returnFromParis) })
    await waitFor(() => {
      expect(screen.getByLabelText('Vuelo de vuelta')).toHaveValue(String(returnFromParis.id))
    })

    await user.selectOptions(outbound, String(laterOutboundToParis.id))

    expect(screen.getByLabelText('Vuelo de vuelta')).toHaveValue(String(returnFromParis.id))
  })

  it('tells the admin when there are no flights to the destination', async () => {
    mockedListFlights.mockResolvedValue([])
    renderPicker('PAR')

    expect(await screen.findByRole('option', { name: 'No hay vuelos hacia PAR' })).toBeInTheDocument()
  })

  it('shows the server message when flights cannot be loaded', async () => {
    mockedListFlights.mockRejectedValue(apiError('Servicio de vuelos caído', 503))
    renderPicker('PAR')

    expect(await screen.findByRole('alert')).toHaveTextContent('Servicio de vuelos caído')
  })

  it('combines origin and outbound date with the hotel destination and allows clearing them', async () => {
    const user = userEvent.setup()
    mockedListFlights.mockImplementation(async (params) => allFlights.filter((flight) =>
      (!params?.origin || flight.origin === params.origin)
      && (!params?.destination || flight.destination === params.destination)
      && (!params?.date || flight.flightDate === params.date),
    ))
    renderPicker('PAR')
    await screen.findByRole('option', { name: 'Buenos Aires (BUE)' })
    await user.selectOptions(screen.getByLabelText('Origen de ida'), 'BUE')
    fireEvent.change(screen.getByLabelText('Fecha de ida'), { target: { value: '2026-12-01' } })

    await waitFor(() => expect(mockedListFlights).toHaveBeenLastCalledWith({
      destination: 'PAR', origin: 'BUE', date: '2026-12-01',
    }))
    await waitFor(() => expect(optionLabels(screen.getByLabelText('Vuelo de ida'))).toEqual([
      'Elegí un vuelo de ida', 'Aerolíneas Test · 2026-12-01 08:30 · BUE → PAR · 40 lugares',
    ]))

    await user.selectOptions(screen.getByLabelText('Origen de ida'), '')
    fireEvent.change(screen.getByLabelText('Fecha de ida'), { target: { value: '' } })
    await waitFor(() => expect(optionLabels(screen.getByLabelText('Vuelo de ida'))).toHaveLength(3))
    expect(screen.getByLabelText('Origen de ida')).toHaveValue('')
  })

  it.each(['Origen de ida', 'Fecha de ida'])('clears both selected flights when %s changes', async (label) => {
    const user = userEvent.setup()
    renderPicker('PAR')
    await screen.findByRole('option', { name: describeFlight(outboundToParis) })
    await user.selectOptions(screen.getByLabelText('Vuelo de ida'), '1')
    await screen.findByRole('option', { name: describeFlight(returnFromParis) })
    await user.selectOptions(screen.getByLabelText('Vuelo de vuelta'), '2')

    if (label === 'Origen de ida') {
      await user.selectOptions(screen.getByLabelText(label), 'MAD')
    } else {
      fireEvent.change(screen.getByLabelText(label), { target: { value: '2026-12-05' } })
    }

    expect(screen.getByLabelText('Vuelo de ida')).toHaveValue('')
    expect(screen.getByLabelText('Vuelo de vuelta')).toHaveValue('')
    expect(screen.getByLabelText('Vuelo de vuelta')).toBeDisabled()
  })

  it('filters the reverse route by return date and clears only the selected return flight', async () => {
    const user = userEvent.setup()
    mockedListFlights.mockImplementation(async (params) => allFlights.filter((flight) =>
      (!params?.origin || flight.origin === params.origin)
      && (!params?.destination || flight.destination === params.destination)
      && (!params?.date || flight.flightDate === params.date),
    ))
    renderPicker('PAR')
    await screen.findByRole('option', { name: describeFlight(outboundToParis) })
    await user.selectOptions(screen.getByLabelText('Vuelo de ida'), '1')
    await screen.findByRole('option', { name: describeFlight(returnFromParis) })
    await user.selectOptions(screen.getByLabelText('Vuelo de vuelta'), '2')
    fireEvent.change(screen.getByLabelText('Fecha de vuelta'), { target: { value: '2026-12-11' } })

    await waitFor(() => expect(mockedListFlights).toHaveBeenLastCalledWith({
      origin: 'PAR', destination: 'BUE', date: '2026-12-11',
    }))
    expect(screen.getByLabelText('Vuelo de ida')).toHaveValue('1')
    expect(screen.getByLabelText('Vuelo de vuelta')).toHaveValue('')
    expect(await screen.findByRole('option', { name: 'No hay vuelos de regreso para esa ruta' })).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Fecha de vuelta'), { target: { value: '' } })
    expect(await screen.findByRole('option', { name: describeFlight(returnFromParis) })).toBeInTheDocument()
  })

  it('shows city loading errors without blocking unfiltered flight selection', async () => {
    vi.mocked(listCities).mockRejectedValue(apiError('No pudimos cargar las ciudades', 503))
    renderPicker('PAR')

    expect(await screen.findByRole('alert')).toHaveTextContent('No pudimos cargar las ciudades')
    expect(screen.getByLabelText('Origen de ida')).toBeDisabled()
    expect(await screen.findByRole('option', { name: describeFlight(outboundToParis) })).toBeInTheDocument()
  })
})
