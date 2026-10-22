import { apiClient } from './client'
import type { Agency } from './packages'

export async function listAgencies(): Promise<Agency[]> {
  const { data } = await apiClient.get<Agency[]>('/agencies')
  return data
}
