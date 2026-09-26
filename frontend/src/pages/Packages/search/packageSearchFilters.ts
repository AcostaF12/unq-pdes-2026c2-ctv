import type { PackageSearchParams } from '../../../api/packages'

export interface PackageSearchFormValues {
  name: string
  origin: string
  destination: string
  minPrice: string
  maxPrice: string
}

export type PackageSearchField = keyof PackageSearchFormValues

export interface PackageSearchFilterResult {
  filters: PackageSearchParams
  error?: string
}

export function createSearchFormValues(origin = ''): PackageSearchFormValues {
  return { name: '', origin, destination: '', minPrice: '', maxPrice: '' }
}

export function buildPackageSearchFilters(values: PackageSearchFormValues): PackageSearchFilterResult {
  const minPrice = parseOptionalPrice(values.minPrice, 'mínimo')
  const maxPrice = parseOptionalPrice(values.maxPrice, 'máximo')
  if (typeof minPrice === 'string') {
    return { filters: {}, error: minPrice }
  }
  if (typeof maxPrice === 'string') {
    return { filters: {}, error: maxPrice }
  }
  if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
    return { filters: {}, error: 'El precio mínimo no puede superar al precio máximo.' }
  }

  return {
    filters: {
      ...optionalText('name', values.name),
      ...optionalText('origin', values.origin),
      ...optionalText('destination', values.destination),
      ...(minPrice != null ? { minPrice } : {}),
      ...(maxPrice != null ? { maxPrice } : {}),
    },
  }
}

export function hasPackageSearchFilters(filters: PackageSearchParams): boolean {
  return Object.keys(filters).length > 0
}

function optionalText<Key extends 'name' | 'origin' | 'destination'>(key: Key, value: string): Pick<PackageSearchParams, Key> | Record<string, never> {
  const trimmedValue = value.trim()
  return trimmedValue ? ({ [key]: trimmedValue } as Pick<PackageSearchParams, Key>) : {}
}

function parseOptionalPrice(value: string, label: 'mínimo' | 'máximo'): number | undefined | string {
  if (value.trim() === '') {
    return undefined
  }

  const price = Number(value)
  if (!Number.isFinite(price) || price < 0) {
    return `Ingresá un precio ${label} válido.`
  }
  return price
}
