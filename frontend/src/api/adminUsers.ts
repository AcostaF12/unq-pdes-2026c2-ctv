import { apiClient } from './client'
import type { Agency } from './packages'
import type { UserRole } from '../auth/roles'

export interface ManagedUser {
  id: number
  username: string
  firstName: string
  lastName: string
  role: UserRole
  enabled: boolean
  agency: Agency | null
}

export interface ManagedUsersPage {
  content: ManagedUser[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type UpdateManagedUser = Pick<ManagedUser, 'firstName' | 'lastName' | 'role' | 'enabled'> & {
  agencyId?: number
}

export type CreateManagedUser = Omit<UpdateManagedUser, 'enabled'> & { username: string; password: string }

export async function listManagedUsers(page: number, size = 20): Promise<ManagedUsersPage> {
  const { data } = await apiClient.get<ManagedUsersPage>('/admin/users', { params: { page, size } })
  return data
}

export async function createManagedUser(payload: CreateManagedUser): Promise<ManagedUser> {
  const { data } = await apiClient.post<ManagedUser>('/admin/users', payload)
  return data
}

export async function updateManagedUser(id: number, payload: UpdateManagedUser): Promise<ManagedUser> {
  const { data } = await apiClient.put<ManagedUser>(`/admin/users/${id}`, payload)
  return data
}
