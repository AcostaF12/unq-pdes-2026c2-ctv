import { useState, type KeyboardEvent } from 'react'
import { photosForCity } from '../../data/cityPhotos'
import './PhotoCarousel.css'

interface PhotoCarouselProps {
  cityCode: string
  alt: string
}

interface CarouselState {
  cityCode: string
  requestedIndex: number
  failed: ReadonlySet<number>
}

function usableIndex(
  photos: string[],
  failed: ReadonlySet<number>,
  requestedIndex: number,
): number {
  if (photos.length === 0) {
    return 0
  }
  const start = ((requestedIndex % photos.length) + photos.length) % photos.length
  for (let step = 0; step < photos.length; step += 1) {
    const candidate = (start + step) % photos.length
    if (!failed.has(candidate)) {
      return candidate
    }
  }
  return start
}

function initialState(cityCode: string): CarouselState {
  return {
    cityCode,
    requestedIndex: 0,
    failed: new Set(),
  }
}

export function PhotoCarousel({ cityCode, alt }: PhotoCarouselProps) {
  const [state, setState] = useState<CarouselState>(() => initialState(cityCode))
  if (state.cityCode !== cityCode) {
    setState(initialState(cityCode))
  }

  const photos = photosForCity(cityCode)
  const index = usableIndex(photos, state.failed, state.requestedIndex)
  const currentSrc = photos[index]
  const showFallback = photos.length === 0 || state.failed.size >= photos.length || !currentSrc

  const goTo = (nextIndex: number) => {
    if (photos.length === 0) {
      return
    }
    setState((current) => ({
      ...current,
      requestedIndex: (nextIndex + photos.length) % photos.length,
    }))
  }

  const goPrev = () => {
    goTo(index - 1)
  }

  const goNext = () => {
    goTo(index + 1)
  }

  const onKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    if (event.key === 'ArrowLeft') {
      event.preventDefault()
      goPrev()
    }
    if (event.key === 'ArrowRight') {
      event.preventDefault()
      goNext()
    }
  }

  if (showFallback) {
    return (
      <div className="photo-carousel photo-carousel--fallback" aria-hidden="true">
        <span>{cityCode}</span>
      </div>
    )
  }

  const showControls = photos.length > 1

  return (
    <div
      className="photo-carousel"
      role="region"
      aria-roledescription="carrusel"
      aria-label={alt}
      tabIndex={0}
      onKeyDown={onKeyDown}
    >
      <img
        className="photo-carousel__img"
        src={currentSrc}
        alt={`${alt} — foto ${index + 1} de ${photos.length}`}
        decoding="async"
        onError={() => {
          setState((current) => {
            const nextFailed = new Set(current.failed)
            nextFailed.add(index)
            return {
              ...current,
              failed: nextFailed,
            }
          })
        }}
      />
      {showControls ? (
        <>
          <button
            type="button"
            className="photo-carousel__nav photo-carousel__nav--prev"
            aria-label="Foto anterior"
            onClick={goPrev}
          >
            ‹
          </button>
          <button
            type="button"
            className="photo-carousel__nav photo-carousel__nav--next"
            aria-label="Foto siguiente"
            onClick={goNext}
          >
            ›
          </button>
          <div className="photo-carousel__dots">
            {photos.map((photo, photoIndex) => (
              <button
                key={photo}
                type="button"
                aria-current={photoIndex === index ? 'true' : undefined}
                aria-label={`Ir a la foto ${photoIndex + 1}`}
                className={
                  photoIndex === index
                    ? 'photo-carousel__dot photo-carousel__dot--active'
                    : 'photo-carousel__dot'
                }
                onClick={() => {
                  goTo(photoIndex)
                }}
              />
            ))}
          </div>
        </>
      ) : null}
    </div>
  )
}
