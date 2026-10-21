import { useState } from 'react'
import { QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { listFlights } from '../../api/flights'
import { allFlights, apiError, laterOutboundToParis, outboundToParis, returnFromParis } from '../../test/fixtures'
import { createTestQueryClient } from '../../test/queryClient'
import { FlightPicker, type FlightSelection } from './FlightPicker'
import { describeFlight, describeSeats, outboundOptions, returnOptions } from './flightOptions'

vi.mock('../../api/flights', () => ({
  listFlights: vi.fn(),
}))

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
        describeFlight(outboundToParis),
        describeFlight(laterOutboundToParis),
      ])
    })
    expect(mockedListFlights).toHaveBeenCalledWith({ destination: 'PAR' })
    expect(screen.getByLabelText('Vuelo de vuelta')).toBeDisabled()

    await user.selectOptions(outbound, String(outboundToParis.id))

    const inbound = screen.getByLabelText('Vuelo de vuelta')
    await waitFor(() => {
      expect(optionLabels(inbound)).toEqual(['Elegí un vuelo de vuelta', describeFlight(returnFromParis)])
    })
    expect(mockedListFlights).toHaveBeenCalledWith({ origin: 'PAR', destination: 'BUE' })

    await user.selectOptions(inbound, String(returnFromParis.id))
    expect(inbound).toHaveValue(String(returnFromParis.id))
  })

  it('clears the return flight when the outbound flight changes', async () => {
    const user = userEvent.setup()
    renderPicker('PAR')

    const outbound = screen.getByLabelText('Vuelo de ida')
    await screen.findByRole('option', { name: describeFlight(outboundToParis) })
    await user.selectOptions(outbound, String(outboundToParis.id))
    await screen.findByRole('option', { name: describeFlight(returnFromParis) })
    await user.selectOptions(screen.getByLabelText('Vuelo de vuelta'), String(returnFromParis.id))

    await user.selectOptions(outbound, String(laterOutboundToParis.id))

    expect(screen.getByLabelText('Vuelo de vuelta')).toHaveValue('')
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
})
