import type { FlightSearchParams } from '../api/flights'
import type { PackageSearchParams } from '../api/packages'

export type PackageSearchFilters = PackageSearchParams

export interface PurchaseHistoryQueryFilters {
  page: number
  size: number
  buyerUsername?: string
  packageName?: string
  from?: string
  to?: string
  destination?: string
  minPrice?: number
  maxPrice?: number
  sort?: string
}

export const queryKeys = {
  adminUsers: ['admin', 'users'] as const,
  cities: ['cities'] as const,
  agencies: ['agencies'] as const,
  flights: {
    all: ['flights'] as const,
    search: (params: FlightSearchParams) => ['flights', 'search', params] as const,
  },
  hotels: {
    all: ['hotels'] as const,
    detail: (id: number) => ['hotels', id] as const,
  },
  packages: {
    all: ['packages'] as const,
    search: (filters: PackageSearchFilters) => ['packages', 'search', filters] as const,
    agency: ['packages', 'agency'] as const,
    detail: (id: number) => ['packages', 'detail', id] as const,
  },
  favorites: ['favorites'] as const,
  purchases: {
    all: ['purchases'] as const,
    mine: ['purchases', 'me'] as const,
    agency: ['purchases', 'agency'] as const,
    minePage: (filters: PurchaseHistoryQueryFilters) => ['purchases', 'me', 'page', filters] as const,
    detail: (id: number) => ['purchases', 'detail', id] as const,
    agencyPage: (filters: PurchaseHistoryQueryFilters) => ['purchases', 'agency', filters] as const,
    ownsPackage: (packageId: number) => ['purchases', 'me', 'package', packageId] as const,
  },
}
