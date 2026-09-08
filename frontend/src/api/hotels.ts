import { apiClient } from './client'
import type { City } from './cities'

export interface Hotel {
  id: number
  name: string
  city: City
  photoUrl: string
}

export interface HotelRequest {
  name: string
  cityCode: string
  photoUrl: string
}

export async function listHotels(): Promise<Hotel[]> {
  const { data } = await apiClient.get<Hotel[]>('/hotels')
  return data
}

export async function getHotel(id: number): Promise<Hotel> {
  const { data } = await apiClient.get<Hotel>(`/hotels/${id}`)
  return data
}

export async function createHotel(payload: HotelRequest): Promise<Hotel> {
  const { data } = await apiClient.post<Hotel>('/hotels', payload)
  return data
}

export async function updateHotel(id: number, payload: HotelRequest): Promise<Hotel> {
  const { data } = await apiClient.put<Hotel>(`/hotels/${id}`, payload)
  return data
}

export async function deleteHotel(id: number): Promise<void> {
  await apiClient.delete(`/hotels/${id}`)
}
