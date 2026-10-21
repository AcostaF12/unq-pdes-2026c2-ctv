import { apiClient } from './client'
import type { Flight } from './packages'

export interface FlightSearchParams {
  origin?: string
  destination?: string
  date?: string
}

export interface FlightPayload {
  airline: string
  flightDate: string
  departureTime: string
  origin: string
  destination: string
  capacity: number
  availability: number
}

export async function listFlights(params?: FlightSearchParams): Promise<Flight[]> {
  const { data } = await apiClient.get<Flight[]>('/flights', { params })
  return data
}

export async function createFlight(payload: FlightPayload): Promise<Flight> {
  const { data } = await apiClient.post<Flight>('/flights', payload)
  return data
}
