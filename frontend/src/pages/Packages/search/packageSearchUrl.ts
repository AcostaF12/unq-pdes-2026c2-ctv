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
  if (filters.name) {
    params.set('name', filters.name)
  }
  if (filters.origin) {
    params.set('origin', filters.origin)
  }
  if (filters.destination) {
    params.set('destination', filters.destination)
  }
  if (filters.minPrice != null) {
    params.set('minPrice', String(filters.minPrice))
  }
  if (filters.maxPrice != null) {
    params.set('maxPrice', String(filters.maxPrice))
  }
  return params
}
