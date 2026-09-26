import { describe, expect, it } from 'vitest'
import { buildPackageSearchFilters, createSearchFormValues } from './packageSearchFilters'

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
