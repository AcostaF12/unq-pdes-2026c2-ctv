import type { Flight } from '../../api/packages'

function byDeparture(a: Flight, b: Flight): number {
  return `${a.flightDate} ${a.departureTime}`.localeCompare(`${b.flightDate} ${b.departureTime}`)
}

export function outboundOptions(flights: Flight[], destinationCode: string | null): Flight[] {
  if (!destinationCode) {
    return []
  }
  return flights.filter((flight) => flight.destination === destinationCode).sort(byDeparture)
}

export function returnOptions(flights: Flight[], outbound: Flight | undefined): Flight[] {
  if (!outbound) {
    return []
  }
  return flights
    .filter((flight) => flight.origin === outbound.destination && flight.destination === outbound.origin)
    .sort(byDeparture)
}

export function describeSeats(availability: number): string {
  if (availability <= 0) {
    return 'sin lugares'
  }
  return availability === 1 ? '1 lugar' : `${availability} lugares`
}

export function describeFlight(flight: Flight): string {
  return [
    flight.airline,
    `${flight.flightDate} ${flight.departureTime}`,
    `${flight.origin} → ${flight.destination}`,
    describeSeats(flight.availability),
  ].join(' · ')
}

export function flightChoiceLabel(flight: Flight): string {
  return [flight.airline, flight.departureTime.slice(0, 5), describeSeats(flight.availability)].join(' · ')
}
