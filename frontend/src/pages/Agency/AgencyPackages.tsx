import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { getApiErrorMessage } from '../../api/errors'
import { listHotels } from '../../api/hotels'
import { createPackage, deletePackage, getAgencyPackages } from '../../api/packages'
import { Button } from '../../components/Button/Button'
import { EmptyState } from '../../components/EmptyState/EmptyState'
import { Input } from '../../components/Input/Input'
import { Select } from '../../components/Select/Select'
import { ListSkeleton } from '../../components/ListSkeleton/ListSkeleton'
import { queryKeys } from '../../query/keys'
import '../Packages/packages.css'

export function AgencyPackages() {
  const queryClient = useQueryClient()
  const [name, setName] = useState('')
  const [hotelId, setHotelId] = useState('')
  const [outboundFlightId, setOutboundFlightId] = useState('')
  const [returnFlightId, setReturnFlightId] = useState('')
  const [price, setPrice] = useState('')
  const [message, setMessage] = useState<string | null>(null)

  const packagesQuery = useQuery({
    queryKey: queryKeys.packages.agency,
    queryFn: getAgencyPackages,
  })

  const hotelsQuery = useQuery({
    queryKey: queryKeys.hotels.all,
    queryFn: listHotels,
  })

  const packages = packagesQuery.data ?? []
  const hotels = hotelsQuery.data ?? []
  const selectedHotelId = hotelId || (hotels[0] ? String(hotels[0].id) : '')

  const invalidatePackages = async () => {
    await queryClient.invalidateQueries({ queryKey: queryKeys.packages.all })
  }

  const createMutation = useMutation({
    mutationFn: createPackage,
    onSuccess: async () => {
      setName('')
      setOutboundFlightId('')
      setReturnFlightId('')
      setPrice('')
      setMessage('Paquete creado.')
      await invalidatePackages()
    },
  })

  const deleteMutation = useMutation({
    mutationFn: deletePackage,
    onSuccess: async () => {
      await invalidatePackages()
    },
  })

  const onCreate = (event: FormEvent) => {
    event.preventDefault()
    setMessage(null)
    createMutation.mutate({
      name,
      hotelId: Number(selectedHotelId),
      outboundFlightId: Number(outboundFlightId),
      returnFlightId: Number(returnFlightId),
      price: Number(price),
    })
  }

  const loadError = packagesQuery.error ?? hotelsQuery.error
  const actionError = createMutation.error ?? deleteMutation.error
  const loading = packagesQuery.isPending || hotelsQuery.isPending

  return (
    <section className="page">
      <h1 className="page__title">Paquetes de la agencia</h1>
      {actionError && !loadError ? (
        <p className="catalog__error" role="alert">
          {getApiErrorMessage(actionError) ??
            (createMutation.isError ? 'No se pudo crear el paquete.' : 'No se pudo eliminar el paquete.')}
        </p>
      ) : null}
      {message ? <p className="catalog__muted">{message}</p> : null}

      <form className="catalog__form" onSubmit={onCreate}>
        <h2>Nuevo paquete</h2>
        <Input label="Nombre" value={name} onChange={(event) => setName(event.target.value)} required />
        <Select
          label="Hotel"
          value={selectedHotelId}
          onChange={(event) => setHotelId(event.target.value)}
          required
        >
          {hotels.map((hotel) => (
            <option key={hotel.id} value={hotel.id}>
              {hotel.name} ({hotel.city.code})
            </option>
          ))}
        </Select>
        <Input
          label="ID vuelo ida"
          type="number"
          value={outboundFlightId}
          onChange={(event) => setOutboundFlightId(event.target.value)}
          required
        />
        <Input
          label="ID vuelo vuelta"
          type="number"
          value={returnFlightId}
          onChange={(event) => setReturnFlightId(event.target.value)}
          required
        />
        <Input
          label="Precio"
          type="number"
          min={1}
          step="0.01"
          value={price}
          onChange={(event) => setPrice(event.target.value)}
          required
        />
        <Button type="submit" loading={createMutation.isPending}>
          Crear paquete
        </Button>
      </form>

      {loadError ? (
        <EmptyState
          tone="error"
          title="Algo salió mal"
          message={getApiErrorMessage(loadError) ?? 'No pudimos cargar la agencia. Intentá de nuevo.'}
          action={
            <Button
              type="button"
              onClick={() => {
                void packagesQuery.refetch()
                void hotelsQuery.refetch()
              }}
            >
              Reintentar
            </Button>
          }
        />
      ) : loading ? (
        <ListSkeleton variant="cards" withMedia={false} label="Cargando paquetes de la agencia" />
      ) : (
        <ul className="catalog__grid">
          {packages.map((item) => (
            <li key={item.id} className="catalog__card">
              <Link to={`/trips/${item.id}`}>
                <h2>{item.name}</h2>
              </Link>
              <p className="catalog__muted">
                {item.origin.name} → {item.destination.name}
              </p>
              <p className="catalog__price">USD {item.price}</p>
              <Button
                type="button"
                variant="ghost"
                onClick={() => deleteMutation.mutate(item.id)}
                loading={deleteMutation.isPending && deleteMutation.variables === item.id}
              >
                Eliminar
              </Button>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
