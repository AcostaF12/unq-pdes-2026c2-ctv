import { Link } from 'react-router-dom'
import type { Hotel } from '../../api/hotels'
import { HotelPhoto } from '../HotelPhoto/HotelPhoto'
import './HotelCard.css'

interface HotelCardProps {
  hotel: Hotel
}

export function HotelCard({ hotel }: HotelCardProps) {
  return (
    <Link className="hotel-card" to={`/hotels/${hotel.id}`}>
      <HotelPhoto src={hotel.photoUrl} alt={hotel.name} cityCode={hotel.city.code} />
      <div className="hotel-card__body">
        <h2 className="hotel-card__title">{hotel.name}</h2>
        <p className="hotel-card__meta">
          {hotel.city.name} · {hotel.city.code}
        </p>
      </div>
    </Link>
  )
}
