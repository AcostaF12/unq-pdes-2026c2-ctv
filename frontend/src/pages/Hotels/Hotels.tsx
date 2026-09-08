import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { listCities } from '../../api/cities'
import { listHotels } from '../../api/hotels'
import { getApiErrorMessage } from '../../api/errors'
import { useAuth } from '../../auth/AuthContext'
import { Button } from '../../components/Button/Button'
import { EmptyState } from '../../components/EmptyState/EmptyState'
import { HotelCard } from '../../components/HotelCard/HotelCard'
import { ListSkeleton } from '../../components/ListSkeleton/ListSkeleton'
import { Select } from '../../components/Select/Select'
import { queryKeys } from '../../query/keys'
import './Hotels.css'

export function Hotels() {
  const { isAdmin } = useAuth()
  const [cityCode, setCityCode] = useState('')

  const hotelsQuery = useQuery({
    queryKey: queryKeys.hotels.all,
    queryFn: listHotels,
  })
  const citiesQuery = useQuery({
    queryKey: queryKeys.cities,
    queryFn: listCities,
  })

  const hotels = hotelsQuery.data
  const cities = citiesQuery.data ?? []
  const error = hotelsQuery.error ?? citiesQuery.error
  const loading = hotelsQuery.isPending || citiesQuery.isPending

  const visibleHotels = useMemo(() => {
    if (!hotels) {
      return []
    }
    if (!cityCode) {
      return hotels
    }
    return hotels.filter((hotel) => hotel.city.code === cityCode)
  }, [hotels, cityCode])

  const retry = () => {
    void hotelsQuery.refetch()
    void citiesQuery.refetch()
  }

  if (loading) {
    return (
      <section className="page">
        <h1 className="page__title">Hoteles</h1>
        <ListSkeleton variant="cards" label="Cargando hoteles" />
      </section>
    )
  }

  if (error) {
    return (
      <section className="page">
        <h1 className="page__title">Hoteles</h1>
        <EmptyState
          tone="error"
          title="Algo salió mal"
          message={getApiErrorMessage(error) ?? 'No pudimos cargar los hoteles. Intentá de nuevo.'}
          action={
            <Button type="button" onClick={retry}>
              Reintentar
            </Button>
          }
        />
      </section>
    )
  }

  return (
    <section className="page">
      <div className="page__header">
        <h1 className="page__title">Hoteles</h1>
        {isAdmin ? (
          <Link className="hotels__new" to="/hotels/new">
            Nuevo hotel
          </Link>
        ) : null}
      </div>

      <Select
        label="Filtrar por ciudad"
        value={cityCode}
        onChange={(event) => {
          setCityCode(event.target.value)
        }}
      >
        <option value="">Todos</option>
        {cities.map((city) => (
          <option key={city.code} value={city.code}>
            {city.name} ({city.code})
          </option>
        ))}
      </Select>

      {visibleHotels.length === 0 ? (
        <EmptyState
          title="Nada por acá"
          message={
            cityCode
              ? 'No hay hoteles para esa ciudad.'
              : 'Todavía no hay hoteles cargados.'
          }
        />
      ) : (
        <ul className="hotels__grid">
          {visibleHotels.map((hotel) => (
            <li key={hotel.id}>
              <HotelCard hotel={hotel} />
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
