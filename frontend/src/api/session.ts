import type { AuthenticatedUser } from './auth'

const TOKEN_KEY = 'ctv.token'
const USER_KEY = 'ctv.user'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function getStoredUser(): AuthenticatedUser | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw) as AuthenticatedUser
  } catch {
    return null
  }
}

export function persistSession(token: string, user: AuthenticatedUser): void {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearSession(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export function readInitialSession(): {
  token: string | null
  user: AuthenticatedUser | null
} {
  const token = getToken()
  if (!token) {
    return { token: null, user: null }
  }
  return { token, user: getStoredUser() }
}
