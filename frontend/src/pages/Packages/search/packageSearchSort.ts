export type PackageSortField = 'name' | 'price' | 'destination.name' | 'rating'
export type PackageSortDirection = 'asc' | 'desc'

export interface PackageSort {
  field: PackageSortField
  direction: PackageSortDirection
}

export const DEFAULT_SORT: PackageSort = { field: 'name', direction: 'asc' }

interface SortOption {
  value: string
  label: string
  sort: PackageSort
}

export const PACKAGE_SORT_OPTIONS: SortOption[] = [
  { value: 'name-asc', label: 'Nombre (A-Z)', sort: { field: 'name', direction: 'asc' } },
  { value: 'price-asc', label: 'Precio: menor a mayor', sort: { field: 'price', direction: 'asc' } },
  { value: 'price-desc', label: 'Precio: mayor a menor', sort: { field: 'price', direction: 'desc' } },
  { value: 'rating-desc', label: 'Mejor puntuados', sort: { field: 'rating', direction: 'desc' } },
  {
    value: 'destination-asc',
    label: 'Destino (A-Z)',
    sort: { field: 'destination.name', direction: 'asc' },
  },
]

function sameSort(a: PackageSort, b: PackageSort): boolean {
  return a.field === b.field && a.direction === b.direction
}

export function isDefaultSort(sort: PackageSort): boolean {
  return sameSort(sort, DEFAULT_SORT)
}

export function sortToOptionValue(sort: PackageSort): string {
  const option = PACKAGE_SORT_OPTIONS.find((candidate) => sameSort(candidate.sort, sort))
  return option?.value ?? PACKAGE_SORT_OPTIONS[0].value
}

export function sortToQueryParam(sort: PackageSort): string {
  return `${sort.field},${sort.direction}`
}

export function parseSortOptionValue(value: string): PackageSort {
  return PACKAGE_SORT_OPTIONS.find((option) => option.value === value)?.sort ?? DEFAULT_SORT
}

export function parseSortQueryParam(value: string | null): PackageSort {
  if (!value) {
    return DEFAULT_SORT
  }
  const [field, direction] = value.split(',')
  const match = PACKAGE_SORT_OPTIONS.find(
    (option) => option.sort.field === field && option.sort.direction === direction,
  )
  return match?.sort ?? DEFAULT_SORT
}
