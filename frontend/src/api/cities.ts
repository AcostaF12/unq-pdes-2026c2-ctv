import { apiClient } from './client'

export interface City {
  code: string
  name: string
}

export async function listCities(): Promise<City[]> {
  const { data } = await apiClient.get<City[]>('/cities')
  return data
}
