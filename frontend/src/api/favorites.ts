import { apiClient } from './client'
import type { TravelPackage } from './packages'

export interface Favorite {
  id: number
  packageId: number
  travelPackage: TravelPackage
}

export async function getFavorites(): Promise<Favorite[]> {
  const { data } = await apiClient.get<Favorite[]>('/favorites')
  return data
}

export async function addFavorite(packageId: number): Promise<Favorite> {
  const { data } = await apiClient.post<Favorite>('/favorites', { packageId })
  return data
}

export async function removeFavorite(packageId: number): Promise<void> {
  await apiClient.delete(`/favorites/${packageId}`)
}
