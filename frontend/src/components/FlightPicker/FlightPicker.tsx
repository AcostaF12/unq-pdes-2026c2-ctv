import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { listCities } from '../../api/cities'
import { getApiErrorMessage } from '../../api/errors'
import { listFlights } from '../../api/flights'
import { queryKeys } from '../../query/keys'
import { Input } from '../Input/Input'
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
  const [origin, setOrigin] = useState('')
  const [outboundDate, setOutboundDate] = useState('')
  const [returnDate, setReturnDate] = useState('')
  const citiesQuery = useQuery({ queryKey: queryKeys.cities, queryFn: listCities })
  const outboundParams = {
    destination: destinationCode ?? '',
    ...(origin ? { origin } : {}),
    ...(outboundDate ? { date: outboundDate } : {}),
  }
  const outboundQuery = useQuery({
    queryKey: queryKeys.flights.search(outboundParams),
    queryFn: () => listFlights(outboundParams),
    enabled: destinationCode !== null,
  })

  const outbound = useMemo(
    () => outboundOptions(outboundQuery.data ?? [], destinationCode),
    [outboundQuery.data, destinationCode],
  )
  const selectedOutbound = outbound.find((flight) => String(flight.id) === outboundFlightId)

  const returnParams = selectedOutbound
    ? {
        origin: selectedOutbound.destination,
        destination: selectedOutbound.origin,
        ...(returnDate ? { date: returnDate } : {}),
      }
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

  const loadError = outboundQuery.error ?? returnQuery.error ?? citiesQuery.error

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
      <div className="flight-picker__filters" role="group" aria-label="Filtrar vuelos">
        <p className="flight-picker__hint">Filtros opcionales. El destino lo determina el hotel elegido.</p>
        <Select
          label="Origen de ida"
          data-testid="package-flight-origin"
          value={origin}
          disabled={citiesQuery.isPending || citiesQuery.isError}
          onChange={(event) => {
            setOrigin(event.target.value)
            onChange({ outboundFlightId: '', returnFlightId: '' })
          }}
        >
          <option value="">{citiesQuery.isPending ? 'Cargando ciudades…' : 'Todos los orígenes'}</option>
          {(citiesQuery.data ?? []).map((city) => (
            <option key={city.code} value={city.code}>{city.name} ({city.code})</option>
          ))}
        </Select>
        <Input
          label="Fecha de ida"
          data-testid="package-outbound-date"
          type="date"
          value={outboundDate}
          onChange={(event) => {
            setOutboundDate(event.target.value)
            onChange({ outboundFlightId: '', returnFlightId: '' })
          }}
        />
        <Input
          label="Fecha de vuelta"
          data-testid="package-return-date"
          type="date"
          value={returnDate}
          onChange={(event) => {
            setReturnDate(event.target.value)
            onChange({ outboundFlightId, returnFlightId: '' })
          }}
        />
      </div>
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
