import { describe, expect, it } from 'vitest'
import { buildPackageFilterChips } from './packageSearchChips'
import { buildPackageSearchFilters, createSearchFormValues, hasAdvancedValues } from './packageSearchFilters'
import { DEFAULT_SORT, parseSortOptionValue, parseSortQueryParam, sortToQueryParam } from './packageSearchSort'

describe('buildPackageSearchFilters', () => {
  it('normalizes text values and preserves zero as a valid price bound', () => {
    const result = buildPackageSearchFilters({
      ...createSearchFormValues(),
      name: '  Paris  ',
      origin: ' BUE ',
      minPrice: '0',
      maxPrice: '1500.50',
    })

    expect(result).toEqual({
      filters: { name: 'Paris', origin: 'BUE', minPrice: 0, maxPrice: 1500.5 },
    })
  })

  it('rejects invalid and inverted price ranges', () => {
    expect(buildPackageSearchFilters({ ...createSearchFormValues(), minPrice: '-1' }).error).toBe(
      'Ingresá un precio mínimo válido.',
    )
    expect(buildPackageSearchFilters({ ...createSearchFormValues(), minPrice: '200', maxPrice: '100' }).error).toBe(
      'El precio mínimo no puede superar al precio máximo.',
    )
  })
})

describe('advanced package filters', () => {
  it('builds agency, hotel and rating filters as typed values', () => {
    const result = buildPackageSearchFilters({
      ...createSearchFormValues(),
      agencyId: '2',
      hotel: '  Palacio ',
      minRating: '8',
    })

    expect(result).toEqual({ filters: { agencyId: 2, hotel: 'Palacio', minRating: 8 } })
  })

  it('detects advanced values and ignores blank ones', () => {
    expect(hasAdvancedValues(createSearchFormValues())).toBe(false)
    expect(hasAdvancedValues({ ...createSearchFormValues(), hotel: ' ' })).toBe(false)
    expect(hasAdvancedValues({ ...createSearchFormValues(), minRating: '7' })).toBe(true)
  })
})

describe('buildPackageFilterChips', () => {
  const cities = [
    { code: 'BUE', name: 'Buenos Aires' },
    { code: 'PAR', name: 'Paris' },
  ]
  const agencies = [{ id: 2, name: 'Almundo' }]

  it('describes every active filter and how to clear it', () => {
    const chips = buildPackageFilterChips(
      {
        ...createSearchFormValues(),
        name: 'Romántico',
        destination: 'PAR',
        minPrice: '900',
        agencyId: '2',
        minRating: '8',
      },
      cities,
      agencies,
    )

    expect(chips.map((chip) => chip.label)).toEqual([
      'Nombre: Romántico',
      'Hacia Paris',
      'Desde USD 900',
      'Agencia: Almundo',
      'Puntaje 8+',
    ])
    expect(chips[1].clear).toEqual({ destination: '' })
  })

  it('returns no chips when nothing is filtered', () => {
    expect(buildPackageFilterChips(createSearchFormValues(), cities, agencies)).toEqual([])
  })
})

describe('package sort parsing', () => {
  it('accepts the destination sort and falls back to the default for unknown values', () => {
    expect(parseSortQueryParam('destination.name,asc')).toEqual({ field: 'destination.name', direction: 'asc' })
    expect(parseSortQueryParam('popularity,desc')).toEqual(DEFAULT_SORT)
    expect(parseSortQueryParam('rating,desc')).toEqual({ field: 'rating', direction: 'desc' })
    expect(sortToQueryParam(parseSortOptionValue('price-desc'))).toBe('price,desc')
  })
})
