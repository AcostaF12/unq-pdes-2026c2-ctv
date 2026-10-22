import { apiClient } from './client'
import type { Agency } from './packages'

export async function listAgencies(): Promise<Agency[]> {
  const { data } = await apiClient.get<Agency[]>('/agencies')
  return data
}

export async function createAgency(name: string): Promise<Agency> {
  const { data } = await apiClient.post<Agency>('/agencies', { name })
  return data
}

export async function updateAgency(id: number, name: string): Promise<Agency> {
  const { data } = await apiClient.put<Agency>(`/agencies/${id}`, { name })
  return data
}
