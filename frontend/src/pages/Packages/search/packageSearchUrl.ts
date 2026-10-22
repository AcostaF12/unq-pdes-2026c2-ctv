import type { PackageSearchParams } from '../../../api/packages'
import { createSearchFormValues, type PackageSearchFormValues } from './packageSearchFilters'
import { parseSortQueryParam, type PackageSort } from './packageSearchSort'

const DEFAULT_PAGE = 0

export function formValuesFromSearchParams(params: URLSearchParams, fallbackOrigin = ''): PackageSearchFormValues {
  const defaults = createSearchFormValues(fallbackOrigin)
  return {
    name: params.get('name') ?? defaults.name,
    origin: params.get('origin') ?? defaults.origin,
    destination: params.get('destination') ?? defaults.destination,
    minPrice: params.get('minPrice') ?? defaults.minPrice,
    maxPrice: params.get('maxPrice') ?? defaults.maxPrice,
    agencyId: params.get('agencyId') ?? defaults.agencyId,
    hotel: params.get('hotel') ?? defaults.hotel,
    minRating: params.get('minRating') ?? defaults.minRating,
  }
}

export function pageFromSearchParams(params: URLSearchParams): number {
  const value = Number(params.get('page'))
  return Number.isInteger(value) && value > 0 ? value : DEFAULT_PAGE
}

export function sortFromSearchParams(params: URLSearchParams): PackageSort {
  return parseSortQueryParam(params.get('sort'))
}

export function filtersToSearchParams(filters: PackageSearchParams): URLSearchParams {
  const params = new URLSearchParams()
  const entries: [string, string | number | undefined][] = [
    ['name', filters.name],
    ['origin', filters.origin],
    ['destination', filters.destination],
    ['minPrice', filters.minPrice],
    ['maxPrice', filters.maxPrice],
    ['agencyId', filters.agencyId],
    ['hotel', filters.hotel],
    ['minRating', filters.minRating],
  ]
  for (const [key, value] of entries) {
    if (value !== undefined && value !== '') {
      params.set(key, String(value))
    }
  }
  return params
}
