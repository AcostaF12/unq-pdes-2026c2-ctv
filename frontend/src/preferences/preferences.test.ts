import { describe, expect, it } from 'vitest'
import {
  profileCompleteness,
  readPreferences,
  writePreferences,
} from './preferences'

describe('preferences', () => {
  it('reads defaults when nothing is stored', () => {
    expect(readPreferences(7)).toEqual({
      originCityCode: '',
      maxBudget: '',
      applyOriginOnSearch: true,
    })
  })

  it('persists preferences by user id', () => {
    writePreferences(7, {
      originCityCode: 'BUE',
      maxBudget: '1500',
      applyOriginOnSearch: false,
    })

    expect(readPreferences(7)).toEqual({
      originCityCode: 'BUE',
      maxBudget: '1500',
      applyOriginOnSearch: false,
    })
    expect(readPreferences(8).originCityCode).toBe('')
  })

  it('computes profile completeness from name, origin and budget', () => {
    expect(
      profileCompleteness(
        { firstName: 'Agus', lastName: 'Demo' },
        { originCityCode: '', maxBudget: '', applyOriginOnSearch: true },
      ).percent,
    ).toBe(33)

    expect(
      profileCompleteness(
        { firstName: 'Agus', lastName: 'Demo' },
        { originCityCode: 'BUE', maxBudget: '900', applyOriginOnSearch: true },
      ).percent,
    ).toBe(100)
  })
})
