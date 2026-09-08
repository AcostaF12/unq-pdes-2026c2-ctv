export interface PackageSearchFilters {
  name?: string
  origin?: string
  destination?: string
}

export const queryKeys = {
  cities: ['cities'] as const,
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
    mine: ['purchases', 'me'] as const,
    agency: ['purchases', 'agency'] as const,
  },
}
