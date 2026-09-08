export const USER_ROLES = ['BUYER', 'AGENCY', 'ADMIN'] as const

export type UserRole = (typeof USER_ROLES)[number]

export function isUserRole(role: string): role is UserRole {
  return USER_ROLES.some((candidate) => candidate === role)
}

export function isAdminRole(role: string): boolean {
  return role === 'ADMIN'
}

export function isBuyerRole(role: string): boolean {
  return role === 'BUYER'
}

export function isAgencyRole(role: string): boolean {
  return role === 'AGENCY'
}

export function roleLabel(role: string): string {
  if (!isUserRole(role)) {
    return role
  }

  switch (role) {
    case 'BUYER':
      return 'Comprador'
    case 'AGENCY':
      return 'Agencia'
    case 'ADMIN':
      return 'Administrador'
    default: {
      const exhaustive: never = role
      return exhaustive
    }
  }
}
