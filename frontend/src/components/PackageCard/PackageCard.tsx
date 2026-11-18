import { Link } from 'react-router-dom'
import type { TravelPackage } from '../../api/packages'
import { FavoriteButton } from '../FavoriteButton/FavoriteButton'
import { HotelPhoto } from '../HotelPhoto/HotelPhoto'
import { Rating } from '../Rating/Rating'
import './PackageCard.css'

interface PackageCardProps {
  travelPackage: TravelPackage
  /** Stagger position inside a list, drives the entrance delay. */
  index?: number
  /** Omit for roles that cannot save favorites: the heart is not rendered. */
  favorite?: {
    active: boolean
    pending: boolean
    onToggle: () => void
  }
  leaving?: boolean
}

const FEATURED_MIN_SCORE = 9
const FEATURED_MIN_REVIEWS = 2

export function PackageCard({ travelPackage, index = 0, favorite, leaving = false }: PackageCardProps) {
  const featured =
    (travelPackage.averageScore ?? 0) >= FEATURED_MIN_SCORE &&
    (travelPackage.reviewCount ?? 0) >= FEATURED_MIN_REVIEWS
  const classes = ['package-card', 'reveal', leaving ? 'package-card--leaving' : '']
    .filter(Boolean)
    .join(' ')

  return (
    <article className={classes} style={{ ['--i' as string]: index }}>
      <Link className="package-card__link" to={`/trips/${travelPackage.id}`}>
        <HotelPhoto
          src={travelPackage.hotel.photoUrl}
          alt={travelPackage.hotel.name}
          cityCode={travelPackage.destination.code}
        />
        {featured ? <span className="package-card__badge">★ Destacado</span> : null}
        <div className="package-card__body">
          <p className="catalog__route">
            {travelPackage.origin.name} → {travelPackage.destination.name}
          </p>
          <h2 className="package-card__title">{travelPackage.name}</h2>
          <p className="catalog__muted">{travelPackage.hotel.name}</p>
          <p className="catalog__muted">{travelPackage.agency.name}</p>
          <Rating score={travelPackage.averageScore} count={travelPackage.reviewCount} />
          <p className="package-card__price">USD {travelPackage.price}</p>
        </div>
      </Link>
      {favorite ? (
        <FavoriteButton
          className="package-card__favorite"
          active={favorite.active}
          disabled={favorite.pending}
          onClick={favorite.onToggle}
        />
      ) : null}
    </article>
  )
}
