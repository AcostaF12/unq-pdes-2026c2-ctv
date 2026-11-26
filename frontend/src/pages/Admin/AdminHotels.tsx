import { useState, type ReactNode } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { getApiErrorMessage } from '../../api/errors'
import { deleteHotel, listHotels, type Hotel } from '../../api/hotels'
import { Button } from '../../components/Button/Button'
import { EmptyState } from '../../components/EmptyState/EmptyState'
import { Input } from '../../components/Input/Input'
import { ListSkeleton } from '../../components/ListSkeleton/ListSkeleton'
import { queryKeys } from '../../query/keys'
import { ADMIN_HOTEL_NEW_PATH } from '../../router/paths'
import { AdminFlow } from './AdminFlow'
import './admin.css'
import './AdminHotels.css'

function Icon({ children }: { children: ReactNode }) {
  return (
    <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      {children}
    </svg>
  )
}

function EyeIcon() {
  return (
    <Icon>
      <path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12z" />
      <circle cx="12" cy="12" r="2.5" />
    </Icon>
  )
}

function PencilIcon() {
  return (
    <Icon>
      <path d="M4 20h4l10-10-4-4L4 16v4z" />
      <path d="M13 7l4 4" />
    </Icon>
  )
}

function TrashIcon() {
  return (
    <Icon>
      <path d="M4 7h16" />
      <path d="M9 7V5h6v2" />
      <path d="M8 7l1 12h6l1-12" />
    </Icon>
  )
}

function HotelMark({ hotel }: { hotel: Hotel }) {
  const [failed, setFailed] = useState(false)
  const initial = hotel.name.trim().charAt(0).toUpperCase() || '·'
  if (failed || hotel.photoUrl.trim() === '') {
    return (
      <span className="hotel-index__mark" aria-hidden="true">
        {initial}
      </span>
    )
  }
  return (
    <img className="hotel-index__mark" src={hotel.photoUrl} alt="" onError={() => setFailed(true)} />
  )
}

function hotelCount(needle: string, total: number, shown: number): string | null {
  if (total === 0) {
    return null
  }
  if (needle) {
    return `${shown} de ${total}`
  }
  return `${total} en el catálogo`
}

function editPath(id: number): string {
  return `/admin/hotels/${id}/edit`
}

export function AdminHotels() {
  const queryClient = useQueryClient()
  const [nameQuery, setNameQuery] = useState('')
  const [pendingDeleteId, setPendingDeleteId] = useState<number | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const hotelsQuery = useQuery({ queryKey: queryKeys.hotels.all, queryFn: listHotels })

  const deleteMutation = useMutation({
    mutationFn: deleteHotel,
    onSuccess: async () => {
      setPendingDeleteId(null)
      setDeleteError(null)
      await queryClient.invalidateQueries({ queryKey: queryKeys.hotels.all })
    },
    onError: (reason: unknown) => {
      setDeleteError(getApiErrorMessage(reason) ?? 'No pudimos eliminar el hotel.')
    },
  })

  const hotels = hotelsQuery.data ?? []
  const needle = nameQuery.trim().toLocaleLowerCase('es')
  const visible = hotels.filter((hotel) =>
    needle ? `${hotel.name} ${hotel.city.name} ${hotel.city.code}`.toLocaleLowerCase('es').includes(needle) : true,
  )

  const countLabel = hotelCount(needle, hotels.length, visible.length)

  return (
    <section className="page hotel-index">
      <AdminFlow current="hotels" />
      <header className="hotel-index__intro">
        <div>
          <p className="hotel-index__eyebrow">Catálogo</p>
          <h1 className="page__title">Hoteles</h1>
          <p className="hotel-index__lead">Elegí uno para editarlo, o cargá uno nuevo antes del vuelo y el paquete.</p>
        </div>
        <Link className="admin-card__link hotel-index__new" to={ADMIN_HOTEL_NEW_PATH}>
          Nuevo hotel
        </Link>
      </header>
      <div className="hotel-index__toolbar">
        <Input label="Buscar hotel" value={nameQuery} onChange={(event) => setNameQuery(event.target.value)} />
        {countLabel ? <p className="hotel-index__count">{countLabel}</p> : null}
      </div>
      {hotelsQuery.isPending ? <ListSkeleton variant="rows" count={4} label="Cargando hoteles" /> : null}
      {hotelsQuery.isError ? (
        <EmptyState
          tone="error"
          title="No se pudieron cargar los hoteles"
          message={getApiErrorMessage(hotelsQuery.error) ?? 'Intentá de nuevo.'}
        />
      ) : null}
      {hotelsQuery.data && hotels.length === 0 ? (
        <EmptyState title="Sin hoteles" message="Todavía no hay hoteles para armar un paquete." />
      ) : null}
      {hotels.length > 0 && visible.length === 0 ? (
        <EmptyState title="Sin coincidencias" message="Ningún hotel coincide con esa búsqueda." />
      ) : null}
      {visible.length > 0 ? (
        <ul className="hotel-index__list" aria-label="Hoteles cargados">
          {visible.map((hotel) => (
            <HotelRow
              key={hotel.id}
              hotel={hotel}
              confirming={pendingDeleteId === hotel.id}
              pending={deleteMutation.isPending && pendingDeleteId === hotel.id}
              error={pendingDeleteId === hotel.id ? deleteError : null}
              onAskDelete={() => {
                setDeleteError(null)
                setPendingDeleteId(hotel.id)
              }}
              onCancel={() => setPendingDeleteId(null)}
              onConfirm={() => deleteMutation.mutate(hotel.id)}
            />
          ))}
        </ul>
      ) : null}
    </section>
  )
}

function HotelRow({
  hotel,
  confirming,
  pending,
  error,
  onAskDelete,
  onCancel,
  onConfirm,
}: {
  hotel: Hotel
  confirming: boolean
  pending: boolean
  error: string | null
  onAskDelete: () => void
  onCancel: () => void
  onConfirm: () => void
}) {
  return (
    <li className="hotel-index__row" data-testid="hotel-item">
      <HotelMark hotel={hotel} />
      <div className="hotel-index__main">
        <h3>{hotel.name}</h3>
        <p>
          {hotel.city.name} · {hotel.city.code}
        </p>
      </div>
      <div className="hotel-index__actions">
        <Link to={`/hotels/${hotel.id}`} aria-label="Ver ficha" title="Ver ficha">
          <EyeIcon />
        </Link>
        <Link to={editPath(hotel.id)} aria-label="Editar" title="Editar">
          <PencilIcon />
        </Link>
        <Button type="button" variant="ghost" aria-label={`Eliminar ${hotel.name}`} title="Eliminar" onClick={onAskDelete}>
          <TrashIcon />
        </Button>
      </div>
      {confirming ? (
        <div className="admin-confirm hotel-index__confirm" role="group" aria-label={`Confirmar eliminación de ${hotel.name}`}>
          <p>
            ¿Eliminar el hotel <strong>{hotel.name}</strong>? Esta acción no se puede deshacer.
          </p>
          {error ? (
            <p className="hotel-index__error" role="alert">
              {error}
            </p>
          ) : null}
          <div className="page__actions">
            <Button type="button" loading={pending} onClick={onConfirm}>
              Confirmar eliminación
            </Button>
            <Button type="button" variant="ghost" onClick={onCancel}>
              Cancelar
            </Button>
          </div>
        </div>
      ) : null}
    </li>
  )
}
