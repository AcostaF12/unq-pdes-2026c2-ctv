import { AxiosError, type AxiosResponse } from 'axios'
import type { Hotel } from '../api/hotels'
import type { Flight, TravelPackage } from '../api/packages'

export const parisHotel: Hotel = {
  id: 1,
  name: 'Hotel Lumière',
  photoUrl: 'https://example.com/par.jpg',
  city: { code: 'PAR', name: 'París' },
}

export const buenosAiresHotel: Hotel = {
  id: 2,
  name: 'Gran Hotel Buenos Aires',
  photoUrl: 'https://example.com/bue.jpg',
  city: { code: 'BUE', name: 'Buenos Aires' },
}

function flight(id: number, origin: string, destination: string, flightDate: string, availability: number): Flight {
  return {
    id,
    airline: id % 2 === 0 ? 'Air Sur' : 'Aerolíneas Test',
    flightDate,
    departureTime: '08:30',
    origin,
    destination,
    capacity: 100,
    availability,
  }
}

export const outboundToParis = flight(1, 'BUE', 'PAR', '2026-12-01', 40)
export const returnFromParis = flight(2, 'PAR', 'BUE', '2026-12-10', 1)
export const laterOutboundToParis = flight(6, 'BUE', 'PAR', '2026-12-05', 0)

export const allFlights: Flight[] = [
  returnFromParis,
  laterOutboundToParis,
  outboundToParis,
  flight(3, 'BUE', 'MAD', '2026-12-02', 10),
  flight(4, 'MAD', 'BUE', '2026-12-12', 10),
  flight(5, 'PAR', 'MAD', '2026-12-11', 10),
]

export const parisPackage: TravelPackage = {
  id: 10,
  name: 'Escapada a París',
  price: 1800,
  agency: { id: 7, name: 'Viajes del Sur' },
  hotel: parisHotel,
  origin: { code: 'BUE', name: 'Buenos Aires' },
  destination: { code: 'PAR', name: 'París' },
  outboundFlightId: outboundToParis.id,
  returnFlightId: returnFromParis.id,
}

export function apiError(description: string, status = 400): AxiosError {
  return new AxiosError('Request failed', 'ERR_BAD_REQUEST', undefined, undefined, {
    status,
    data: { httpCode: status, httpStatus: 'BAD_REQUEST', errorData: { description }, timestamp: '' },
  } as AxiosResponse)
}
