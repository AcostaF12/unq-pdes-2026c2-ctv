import type { ReactNode } from 'react'
import { EmptyState } from '../components/EmptyState/EmptyState'
import { useAuth } from './AuthContext'

export function RequireBuyer({ children }: { children: ReactNode }) {
  const { isBuyer } = useAuth()

  if (!isBuyer) {
    return (
      <EmptyState title="Sin permiso" message="Esta sección es solo para compradores." />
    )
  }

  return children
}

export function RequireAgency({ children }: { children: ReactNode }) {
  const { isAgency } = useAuth()

  if (!isAgency) {
    return <EmptyState title="Sin permiso" message="Esta sección es solo para agencias." />
  }

  return children
}

export function RequireBuyerOrAgency({ children }: { children: ReactNode }) {
  const { isBuyer, isAgency } = useAuth()

  if (!isBuyer && !isAgency) {
    return (
      <EmptyState title="Sin permiso" message="Esta sección es solo para compradores y agencias." />
    )
  }

  return children
}
