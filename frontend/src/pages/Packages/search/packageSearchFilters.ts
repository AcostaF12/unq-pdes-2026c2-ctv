import type { PackageSearchParams } from '../../../api/packages'

export interface PackageSearchFormValues {
  name: string
  origin: string
  destination: string
  minPrice: string
  maxPrice: string
  agencyId: string
  hotel: string
  minRating: string
}

export type PackageSearchField = keyof PackageSearchFormValues

export interface PackageSearchFilterResult {
  filters: PackageSearchParams
  error?: string
}

export const ADVANCED_FIELDS: PackageSearchField[] = ['agencyId', 'hotel', 'minRating']

export function createSearchFormValues(origin = ''): PackageSearchFormValues {
  return {
    name: '',
    origin,
    destination: '',
    minPrice: '',
    maxPrice: '',
    agencyId: '',
    hotel: '',
    minRating: '',
  }
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
  const agencyId = parseOptionalNumber(values.agencyId)
  const minRating = parseOptionalNumber(values.minRating)

  return {
    filters: {
      ...optionalText('name', values.name),
      ...optionalText('origin', values.origin),
      ...optionalText('destination', values.destination),
      ...optionalText('hotel', values.hotel),
      ...(minPrice != null ? { minPrice } : {}),
      ...(maxPrice != null ? { maxPrice } : {}),
      ...(agencyId != null ? { agencyId } : {}),
      ...(minRating != null ? { minRating } : {}),
    },
  }
}

export function hasPackageSearchFilters(filters: PackageSearchParams): boolean {
  return countPackageSearchFilters(filters) > 0
}

export function countPackageSearchFilters(filters: PackageSearchParams): number {
  return Object.keys(filters).length
}

export function hasAdvancedValues(values: PackageSearchFormValues): boolean {
  return ADVANCED_FIELDS.some((field) => values[field].trim() !== '')
}

function optionalText<Key extends 'name' | 'origin' | 'destination' | 'hotel'>(
  key: Key,
  value: string,
): Pick<PackageSearchParams, Key> | Record<string, never> {
  const trimmedValue = value.trim()
  return trimmedValue ? ({ [key]: trimmedValue } as Pick<PackageSearchParams, Key>) : {}
}

function parseOptionalNumber(value: string): number | undefined {
  if (value.trim() === '') {
    return undefined
  }
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : undefined
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
