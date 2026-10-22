import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { getApiErrorMessage } from '../../api/errors'
import { listFlights } from '../../api/flights'
import { queryKeys } from '../../query/keys'
import { Select } from '../Select/Select'
import { describeFlight, outboundOptions, returnOptions } from './flightOptions'
import './FlightPicker.css'

export interface FlightSelection {
  outboundFlightId: string
  returnFlightId: string
}

interface FlightPickerProps extends FlightSelection {
  destinationCode: string | null
  onChange: (selection: FlightSelection) => void
  outboundError?: string
  returnError?: string
}

export function FlightPicker({
  destinationCode,
  outboundFlightId,
  returnFlightId,
  onChange,
  outboundError,
  returnError,
}: FlightPickerProps) {
  const outboundQuery = useQuery({
    queryKey: queryKeys.flights.search({ destination: destinationCode ?? '' }),
    queryFn: () => listFlights({ destination: destinationCode ?? '' }),
    enabled: destinationCode !== null,
  })

  const outbound = useMemo(
    () => outboundOptions(outboundQuery.data ?? [], destinationCode),
    [outboundQuery.data, destinationCode],
  )
  const selectedOutbound = outbound.find((flight) => String(flight.id) === outboundFlightId)

  const returnParams = selectedOutbound
    ? { origin: selectedOutbound.destination, destination: selectedOutbound.origin }
    : null
  const returnQuery = useQuery({
    queryKey: queryKeys.flights.search(returnParams ?? {}),
    queryFn: () => listFlights(returnParams ?? {}),
    enabled: returnParams !== null,
  })
  const inbound = useMemo(
    () => returnOptions(returnQuery.data ?? [], selectedOutbound),
    [returnQuery.data, selectedOutbound],
  )

  const loadError = outboundQuery.error ?? returnQuery.error

  let outboundPlaceholder = 'Elegí un vuelo de ida'
  if (destinationCode === null) {
    outboundPlaceholder = 'Elegí un hotel primero'
  } else if (outboundQuery.isPending) {
    outboundPlaceholder = 'Cargando vuelos…'
  } else if (outbound.length === 0) {
    outboundPlaceholder = `No hay vuelos hacia ${destinationCode}`
  }

  let returnPlaceholder = 'Elegí un vuelo de vuelta'
  if (!selectedOutbound) {
    returnPlaceholder = 'Elegí primero el vuelo de ida'
  } else if (returnQuery.isPending) {
    returnPlaceholder = 'Cargando vuelos…'
  } else if (inbound.length === 0) {
    returnPlaceholder = 'No hay vuelos de regreso para esa ruta'
  }

  return (
    <div className="flight-picker" data-testid="flight-picker">
      <Select
        label="Vuelo de ida"
        data-testid="package-outbound-flight"
        value={outboundFlightId}
        disabled={destinationCode === null}
        error={outboundError}
        onChange={(event) => onChange({ outboundFlightId: event.target.value, returnFlightId: '' })}
      >
        <option value="">{outboundPlaceholder}</option>
        {outbound.map((flight) => (
          <option key={flight.id} value={flight.id}>
            {describeFlight(flight)}
          </option>
        ))}
      </Select>
      <Select
        label="Vuelo de vuelta"
        data-testid="package-return-flight"
        value={returnFlightId}
        disabled={!selectedOutbound}
        error={returnError}
        onChange={(event) => onChange({ outboundFlightId, returnFlightId: event.target.value })}
      >
        <option value="">{returnPlaceholder}</option>
        {inbound.map((flight) => (
          <option key={flight.id} value={flight.id}>
            {describeFlight(flight)}
          </option>
        ))}
      </Select>
      {loadError ? (
        <p className="flight-picker__error" role="alert">
          {getApiErrorMessage(loadError) ?? 'No pudimos cargar los vuelos. Intentá de nuevo.'}
        </p>
      ) : null}
    </div>
  )
}
