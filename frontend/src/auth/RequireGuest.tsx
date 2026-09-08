import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { PageFallback } from '../components/PageFallback/PageFallback'
import { HOME_PATH } from '../router/paths'
import { useAuth } from './AuthContext'

export function RequireGuest({ children }: { children: ReactNode }) {
  const { user, isReady } = useAuth()

  if (user) {
    return <Navigate to={HOME_PATH} replace />
  }

  if (!isReady) {
    return <PageFallback />
  }

  return children
}
