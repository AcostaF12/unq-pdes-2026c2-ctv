import { apiClient } from './client'

export interface AgencySummary {
  id: number
  name: string
}

export interface AuthenticatedUser {
  id: number
  username: string
  firstName: string
  lastName: string
  role: string
  agency?: AgencySummary
}

export interface LoginCredentials {
  username: string
  password: string
}

export interface RegisterPayload {
  username: string
  password: string
  firstName: string
  lastName: string
}

export interface AuthResponse {
  token: string
  user: AuthenticatedUser
}

export async function login(credentials: LoginCredentials): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>('/auth/login', credentials)
  return data
}

export async function register(payload: RegisterPayload): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>('/auth/register', payload)
  return data
}

export async function getCurrentUser(): Promise<AuthenticatedUser> {
  const { data } = await apiClient.get<AuthenticatedUser>('/users/me')
  return data
}

export interface UpdateProfilePayload {
  firstName: string
  lastName: string
}

export interface ChangePasswordPayload {
  currentPassword: string
  newPassword: string
}

export async function updateCurrentUser(payload: UpdateProfilePayload): Promise<AuthenticatedUser> {
  const { data } = await apiClient.patch<AuthenticatedUser>('/users/me', payload)
  return data
}

export async function changePassword(payload: ChangePasswordPayload): Promise<void> {
  await apiClient.put('/users/me/password', payload)
}
