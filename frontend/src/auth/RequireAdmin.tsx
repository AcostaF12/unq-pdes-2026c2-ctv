import type { ReactNode } from 'react'
import { EmptyState } from '../components/EmptyState/EmptyState'
import { useAuth } from './AuthContext'

export function RequireAdmin({ children }: { children: ReactNode }) {
  const { isAdmin } = useAuth()

  if (!isAdmin) {
    return (
      <EmptyState
        title="Sin permiso"
        message="Esta acción es solo para administradores."
      />
    )
  }

  return children
}
