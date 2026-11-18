import { useEffect, useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { listCities } from '../../api/cities'
import { getApiErrorMessage } from '../../api/errors'
import { listFlights } from '../../api/flights'
import type { Flight } from '../../api/packages'
import { queryKeys } from '../../query/keys'
import { Input } from '../Input/Input'
import { Select } from '../Select/Select'
import { describeFlight, flightChoiceLabel, outboundOptions, returnOptions } from './flightOptions'
import './FlightPicker.css'

const flightDateFormat = new Intl.DateTimeFormat('es-AR', {
  day: 'numeric',
  month: 'short',
  year: 'numeric',
  timeZone: 'UTC',
})

function formatFlightDate(isoDate: string): string {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(isoDate)
  if (!match) {
    return isoDate
  }
  const year = Number(match[1])
  const month = Number(match[2])
  const day = Number(match[3])
  return flightDateFormat.format(new Date(Date.UTC(year, month - 1, day)))
}

function flightsByDate(flights: Flight[]): { date: string; flights: Flight[] }[] {
  const groups: { date: string; flights: Flight[] }[] = []
  for (const flight of flights) {
    const current = groups[groups.length - 1]
    if (current && current.date === flight.flightDate) {
      current.flights.push(flight)
    } else {
      groups.push({ date: flight.flightDate, flights: [flight] })
    }
  }
  return groups
}

function listRows(flights: Flight[]): number | undefined {
  if (flights.length === 0) {
    return undefined
  }
  return Math.min(flights.length + 1 + flightsByDate(flights).length, 8)
}

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

  useEffect(() => {
    if (!selectedOutbound || !returnQuery.isSuccess || returnQuery.isFetching) {
      return
    }
    const available = returnOptions(returnQuery.data ?? [], selectedOutbound)
    const stillValid = available.some((flight) => String(flight.id) === returnFlightId)
    if (returnFlightId && stillValid) {
      return
    }
    const nextReturn = available.length === 1 ? String(available[0].id) : ''
    if (nextReturn === returnFlightId) {
      return
    }
    onChange({ outboundFlightId, returnFlightId: nextReturn })
  }, [
    selectedOutbound,
    returnQuery.isSuccess,
    returnQuery.isFetching,
    returnQuery.data,
    returnFlightId,
    outboundFlightId,
    onChange,
  ])

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

  const outboundRows = listRows(outbound)
  const returnRows = listRows(inbound)

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
        className={outboundRows ? 'field__select--list' : undefined}
        size={outboundRows}
        value={outboundFlightId}
        disabled={destinationCode === null}
        error={outboundError}
        onChange={(event) => {
          const nextId = event.target.value
          const nextFlight = outbound.find((flight) => String(flight.id) === nextId)
          const sameRoute = Boolean(
            selectedOutbound &&
              nextFlight &&
              nextFlight.origin === selectedOutbound.origin &&
              nextFlight.destination === selectedOutbound.destination,
          )
          onChange({
            outboundFlightId: nextId,
            returnFlightId: sameRoute ? returnFlightId : '',
          })
        }}
      >
        <option value="">{outboundPlaceholder}</option>
        {flightsByDate(outbound).map((group) => (
          <optgroup key={group.date} label={formatFlightDate(group.date)}>
            {group.flights.map((flight) => (
              <option key={flight.id} value={flight.id} title={describeFlight(flight)}>
                {flightChoiceLabel(flight)}
              </option>
            ))}
          </optgroup>
        ))}
      </Select>
      <Select
        label="Vuelo de vuelta"
        data-testid="package-return-flight"
        className={returnRows ? 'field__select--list' : undefined}
        size={returnRows}
        value={returnFlightId}
        disabled={!selectedOutbound}
        error={returnError}
        onChange={(event) => onChange({ outboundFlightId, returnFlightId: event.target.value })}
      >
        <option value="">{returnPlaceholder}</option>
        {flightsByDate(inbound).map((group) => (
          <optgroup key={group.date} label={formatFlightDate(group.date)}>
            {group.flights.map((flight) => (
              <option key={flight.id} value={flight.id} title={describeFlight(flight)}>
                {flightChoiceLabel(flight)}
              </option>
            ))}
          </optgroup>
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
