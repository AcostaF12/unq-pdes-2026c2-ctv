import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { PageFallback } from '../components/PageFallback/PageFallback'
import { LOGIN_PATH } from '../router/paths'
import { useAuth } from './AuthContext'

export function RequireAuth({ children }: { children: ReactNode }) {
  const { user, isReady } = useAuth()

  if (user) {
    return children
  }

  if (!isReady) {
    return <PageFallback />
  }

  return <Navigate to={LOGIN_PATH} replace />
}
