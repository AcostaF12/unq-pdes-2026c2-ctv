import { describe, expect, it } from 'vitest'
import { isUnusableHotelPhotoUrl, photosForCity } from './cityPhotos'

describe('photosForCity', () => {
  it('returns destination photos for a known IATA code', () => {
    const photos = photosForCity('par')

    expect(photos.length).toBeGreaterThanOrEqual(3)
    expect(photos.every((url) => url.startsWith('https://images.unsplash.com/'))).toBe(true)
  })

  it('returns generic travel photos for an unknown city', () => {
    expect(photosForCity('XYZ')).toEqual(photosForCity('zzz'))
    expect(photosForCity('XYZ')[0]).toContain('images.unsplash.com')
  })
})

describe('isUnusableHotelPhotoUrl', () => {
  it('treats empty, invalid and demo host urls as unusable', () => {
    expect(isUnusableHotelPhotoUrl('')).toBe(true)
    expect(isUnusableHotelPhotoUrl('   ')).toBe(true)
    expect(isUnusableHotelPhotoUrl('not-a-url')).toBe(true)
    expect(isUnusableHotelPhotoUrl('https://images.ctv.demo/hotels/par.jpg')).toBe(true)
  })

  it('accepts a real http url', () => {
    expect(isUnusableHotelPhotoUrl('https://example.com/hotel.jpg')).toBe(false)
  })
})
