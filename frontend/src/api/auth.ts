import { apiClient } from './client'

export interface LoginCredentials {
  username: string
  password: string
}

export interface AuthenticatedUser {
  id: number
  username: string
  firstName: string
  lastName: string
  role: string
}

export interface AuthResponse {
  token: string
  user: AuthenticatedUser
}

export async function login(credentials: LoginCredentials): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>('/auth/login', credentials)
  return data
}
