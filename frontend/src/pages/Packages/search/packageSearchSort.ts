export type PackageSortField = 'name' | 'price'
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
]

export function isDefaultSort(sort: PackageSort): boolean {
  return sort.field === DEFAULT_SORT.field && sort.direction === DEFAULT_SORT.direction
}

export function sortToOptionValue(sort: PackageSort): string {
  return `${sort.field}-${sort.direction}`
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
  if ((field === 'name' || field === 'price') && (direction === 'asc' || direction === 'desc')) {
    return { field, direction }
  }
  return DEFAULT_SORT
}
