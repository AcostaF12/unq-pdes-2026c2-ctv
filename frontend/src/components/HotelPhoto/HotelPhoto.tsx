import { useState } from 'react'
import { isUnusableHotelPhotoUrl, photosForCity } from '../../data/cityPhotos'
import './HotelPhoto.css'

interface HotelPhotoProps {
  src: string
  alt: string
  cityCode: string
  size?: 'default' | 'large'
}

export function HotelPhoto({ src, alt, cityCode, size = 'default' }: HotelPhotoProps) {
  const [failedSrcs, setFailedSrcs] = useState<ReadonlySet<string>>(() => new Set())
  const citySrc = photosForCity(cityCode)[0]
  const hotelUsable = Boolean(src) && !isUnusableHotelPhotoUrl(src) && !failedSrcs.has(src)
  const cityUsable = Boolean(citySrc) && !failedSrcs.has(citySrc)
  const displaySrc = hotelUsable ? src : cityUsable ? citySrc : null
  const classes = ['hotel-photo', size === 'large' ? 'hotel-photo--large' : '']
    .filter(Boolean)
    .join(' ')

  if (!displaySrc) {
    return (
      <div className={`${classes} hotel-photo--fallback`} aria-hidden="true">
        <span>{cityCode}</span>
      </div>
    )
  }

  return (
    <div className={classes}>
      <img
        className="hotel-photo__img"
        src={displaySrc}
        alt={alt}
        loading={size === 'large' ? 'eager' : 'lazy'}
        decoding="async"
        onError={() => {
          setFailedSrcs((current) => {
            const next = new Set(current)
            next.add(displaySrc)
            return next
          })
        }}
      />
      <span className="hotel-photo__badge" aria-hidden="true">
        {cityCode}
      </span>
    </div>
  )
}
