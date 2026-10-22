import { apiClient } from './client'
import type { Agency } from './packages'

export async function listAgencies(): Promise<Agency[]> {
  const { data } = await apiClient.get<Agency[]>('/agencies')
  return data
}

export interface CreateAgencyPayload {
  name: string
  username: string
  password: string
  firstName: string
  lastName: string
}

export async function createAgency(payload: CreateAgencyPayload): Promise<Agency> {
  const { data } = await apiClient.post<Agency>('/agencies', payload)
  return data
}

export async function updateAgency(id: number, payload: { name: string; active: boolean }): Promise<Agency> {
  const { data } = await apiClient.put<Agency>(`/agencies/${id}`, payload)
  return data
}
