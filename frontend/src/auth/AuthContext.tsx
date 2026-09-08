import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { getCurrentUser, type AuthenticatedUser, type AuthResponse } from '../api/auth'
import { clearSession, getToken, persistSession, readInitialSession } from '../api/session'
import { onUnauthorized } from '../api/unauthorized'
import { queryClient } from '../query/queryClient'
import { isAdminRole, isAgencyRole, isBuyerRole } from './roles'

interface AuthContextValue {
  user: AuthenticatedUser | null
  isReady: boolean
  isAdmin: boolean
  isBuyer: boolean
  isAgency: boolean
  setSession: (response: AuthResponse) => void
  updateUser: (user: AuthenticatedUser) => void
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthenticatedUser | null>(() => readInitialSession().user)
  const [isReady, setIsReady] = useState(() => {
    const initial = readInitialSession()
    return !initial.token || initial.user !== null
  })

  const logout = useCallback(() => {
    clearSession()
    queryClient.clear()
    setUser(null)
  }, [])

  const setSession = useCallback((response: AuthResponse) => {
    persistSession(response.token, response.user)
    setUser(response.user)
  }, [])

  const updateUser = useCallback((nextUser: AuthenticatedUser) => {
    const token = getToken()
    if (!token) {
      return
    }
    persistSession(token, nextUser)
    setUser(nextUser)
  }, [])

  useEffect(() => {
    return onUnauthorized(() => {
      clearSession()
      queryClient.clear()
      setUser(null)
    })
  }, [])

  useEffect(() => {
    const token = getToken()
    if (!token) {
      return
    }

    let cancelled = false

    getCurrentUser()
      .then((freshUser) => {
        if (cancelled) {
          return
        }
        persistSession(token, freshUser)
        setUser(freshUser)
      })
      .catch(() => {
        if (cancelled) {
          return
        }
        clearSession()
        queryClient.clear()
        setUser(null)
      })
      .finally(() => {
        if (!cancelled) {
          setIsReady(true)
        }
      })

    return () => {
      cancelled = true
    }
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isReady,
      isAdmin: isAdminRole(user?.role ?? ''),
      isBuyer: isBuyerRole(user?.role ?? ''),
      isAgency: isAgencyRole(user?.role ?? ''),
      setSession,
      updateUser,
      logout,
    }),
    [user, isReady, setSession, updateUser, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth debe usarse dentro de AuthProvider')
  }
  return context
}
