import { apiClient } from './client'
import type { City } from './cities'
import type { Hotel } from './hotels'

export interface Agency {
  id: number
  name: string
}

export interface Flight {
  id: number
  airline: string
  flightDate: string
  departureTime: string
  origin: string
  destination: string
  capacity: number
  availability: number
}

export interface Review {
  id: number
  buyerUsername: string
  packageId: number
  score: number
  comment: string | null
}

export interface TravelPackage {
  id: number
  name: string
  price: number
  agency: Agency
  hotel: Hotel
  origin: City
  destination: City
  outboundFlightId: number
  returnFlightId: number
}

export interface TravelPackageDetail extends TravelPackage {
  outboundFlight: Flight | null
  returnFlight: Flight | null
  reviews: Review[]
  averageScore: number | null
}

export interface TravelPackagePayload {
  name: string
  hotelId: number
  outboundFlightId: number
  returnFlightId: number
  price: number
}

export async function searchPackages(params?: {
  name?: string
  origin?: string
  destination?: string
}): Promise<TravelPackage[]> {
  const { data } = await apiClient.get<TravelPackage[]>('/packages', { params })
  return data
}

export async function getAgencyPackages(): Promise<TravelPackage[]> {
  const { data } = await apiClient.get<TravelPackage[]>('/packages/agency')
  return data
}

export async function getPackage(id: number): Promise<TravelPackageDetail> {
  const { data } = await apiClient.get<TravelPackageDetail>(`/packages/${id}`)
  return data
}

export async function createPackage(payload: TravelPackagePayload): Promise<TravelPackage> {
  const { data } = await apiClient.post<TravelPackage>('/packages', payload)
  return data
}

export async function deletePackage(id: number): Promise<void> {
  await apiClient.delete(`/packages/${id}`)
}
