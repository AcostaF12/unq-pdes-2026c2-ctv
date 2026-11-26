import { useQuery } from '@tanstack/react-query'
import { type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { listAgencies } from '../../api/agencies'
import {
  getAdminFavorites,
  getAdminPurchases,
  getAdminReports,
  getAdminReviews,
  getAdminSystem,
  getAdminUsers,
  type AdminReportsData,
  type AdminSystemSnapshot,
} from '../../api/admin'
import { listFlights } from '../../api/flights'
import { listHotels } from '../../api/hotels'
import { searchPackages } from '../../api/packages'
import { queryKeys } from '../../query/keys'
import {
  ADMIN_AGENCIES_PATH,
  ADMIN_FLIGHTS_PATH,
  ADMIN_HOTEL_NEW_PATH,
  ADMIN_HOTELS_PATH,
  ADMIN_PACKAGES_PATH,
  ADMIN_SALES_PATH,
  ADMIN_SYSTEM_PATH,
  ADMIN_USERS_PATH,
} from '../../router/paths'
import { statusLabel, systemName } from './systemLabels'
import '../Packages/packages.css'
import './admin.css'
import './AdminSystem.css'

interface AdminCardProps {
  id: string
  kicker: string
  title: string
  description: string
  count: number | undefined
  countLabel: string
  showCount?: boolean
  note?: string
  preview?: string[]
  links: { to: string; label: string }[]
}

function AdminCard({
  id,
  kicker,
  title,
  description,
  count,
  countLabel,
  links,
  showCount = true,
  note,
  preview,
}: AdminCardProps) {
  return (
    <article
      className="admin-card"
      data-testid={`admin-card-${id}`}
      aria-labelledby={`admin-card-${id}-title`}
    >
      <p className="admin-card__kicker">{kicker}</p>
      <h2 id={`admin-card-${id}-title`}>{title}</h2>
      {showCount ? (
        <p className="admin-card__count" data-testid={`admin-count-${id}`}>
          {count ?? '—'}
          <span className="admin-card__count-label"> {countLabel}</span>
        </p>
      ) : (
        <p className="admin-card__count admin-card__count--label-only" data-testid={`admin-count-${id}`}>
          <span className="admin-card__count-label">{countLabel}</span>
        </p>
      )}
      <p className="admin-card__desc">{description}</p>
      {note ? <p className="admin-card__note">{note}</p> : null}
      {preview && preview.length > 0 ? (
        <ul className="admin-card__preview">
          {preview.map((line) => (
            <li key={line}>{line}</li>
          ))}
        </ul>
      ) : null}
      <div className="page__actions">
        {links.map((link) => (
          <Link key={link.to} className="admin-card__link" to={link.to}>
            {link.label}
          </Link>
        ))}
      </div>
    </article>
  )
}

interface AdminHubSectionProps {
  title: string
  columns?: 2 | 3
  children: ReactNode
}

const ratingFormat = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 2 })

function countQuery(resource: string) {
  return ['admin', 'count', resource] as const
}

function reportPreview(data: AdminReportsData | undefined, failed: boolean): { note?: string; preview?: string[] } {
  if (failed) {
    return { note: 'No se pudieron cargar los rankings' }
  }
  if (!data) {
    return { note: '—' }
  }
  const preview = [
    data.topBuyers[0] ? `Compradores: ${data.topBuyers[0].name} (${data.topBuyers[0].total})` : null,
    data.topAgencies[0] ? `Agencias: ${data.topAgencies[0].name} (${data.topAgencies[0].total})` : null,
    data.topDestinations[0] ? `Vendidos: ${data.topDestinations[0].name} (${data.topDestinations[0].total})` : null,
    data.topRatedDestinations[0]
      ? `Puntaje: ${data.topRatedDestinations[0].name} (${ratingFormat.format(data.topRatedDestinations[0].average)})`
      : null,
  ].filter((line): line is string => line !== null)
  if (preview.length === 0) {
    return { note: 'Sin datos en los rankings' }
  }
  return { preview }
}

function listPreview(label: string, total: number | undefined, failed: boolean): string {
  if (failed || total === undefined) {
    return `${label}: —`
  }
  return `${label}: ${total}`
}

function OpsRibbon({
  snapshot,
  failed,
  pending,
}: {
  snapshot: AdminSystemSnapshot | undefined
  failed: boolean
  pending: boolean
}) {
  const chips = snapshot?.systems.map((system) => `${systemName(system.name)}: ${statusLabel(system.status)}`) ?? []
  const breaker = snapshot
    ? `${systemName(snapshot.circuitBreaker.name)}: ${statusLabel(snapshot.circuitBreaker.state)}`
    : null
  let note = 'Leyendo el proceso…'
  if (failed) {
    note = 'No se pudo leer el estado del proceso'
  } else if (snapshot) {
    note = 'Salud, pool de conexiones y corte hacia vuelos.'
  } else if (!pending) {
    note = 'Sin lectura del proceso'
  }

  return (
    <section className="admin-hub__section" aria-label="Operación">
      <h2 className="admin-hub__section-title">Operación</h2>
      <article className="ops-ribbon" data-testid="admin-card-system">
        <div className="ops-ribbon__top">
          <div>
            <h3>Concurrencia y monitoreo</h3>
            <p>{note}</p>
          </div>
          <Link className="ops-ribbon__link" to={ADMIN_SYSTEM_PATH}>
            Ver operación
          </Link>
        </div>
        {chips.length > 0 || breaker ? (
          <ul className="ops-ribbon__chips">
            {chips.map((chip) => (
              <li key={chip}>{chip}</li>
            ))}
            {breaker ? <li>{breaker}</li> : null}
          </ul>
        ) : null}
      </article>
    </section>
  )
}

function AdminHubSection({ title, columns = 3, children }: AdminHubSectionProps) {
  return (
    <section className="admin-hub__section" aria-label={title}>
      <h2 className="admin-hub__section-title">{title}</h2>
      <div className={`admin-hub__grid admin-hub__grid--cols-${columns}`}>{children}</div>
    </section>
  )
}

export function AdminHome() {
  const packagesQuery = useQuery({
    queryKey: queryKeys.packages.search({ page: 0, size: 1 }),
    queryFn: () => searchPackages({ page: 0, size: 1 }),
  })
  const flightsQuery = useQuery({
    queryKey: queryKeys.flights.search({}),
    queryFn: () => listFlights(),
  })
  const hotelsQuery = useQuery({
    queryKey: queryKeys.hotels.all,
    queryFn: listHotels,
  })
  const agenciesQuery = useQuery({
    queryKey: queryKeys.agencies,
    queryFn: listAgencies,
  })
  const usersQuery = useQuery({
    queryKey: countQuery('users'),
    queryFn: () => getAdminUsers(0, 1),
  })
  const favoritesQuery = useQuery({
    queryKey: countQuery('favorites'),
    queryFn: () => getAdminFavorites(0, 1),
  })
  const reviewsQuery = useQuery({
    queryKey: countQuery('reviews'),
    queryFn: () => getAdminReviews(0, 1),
  })
  const purchasesQuery = useQuery({
    queryKey: countQuery('purchases'),
    queryFn: () => getAdminPurchases(0, 1),
  })
  const reportsQuery = useQuery({
    queryKey: ['admin', 'reports'],
    queryFn: getAdminReports,
  })
  const systemQuery = useQuery({
    queryKey: ['admin', 'system'],
    queryFn: getAdminSystem,
  })

  const activeAgencies = agenciesQuery.data?.filter((agency) => agency.active !== false).length
  const agencyNote =
    activeAgencies === undefined
      ? undefined
      : `${activeAgencies} ${activeAgencies === 1 ? 'activa' : 'activas'}`
  const reports = reportPreview(reportsQuery.data, reportsQuery.isError)

  return (
    <section className="page" data-testid="admin-hub">
      <div className="catalog__intro">
        <p className="catalog__eyebrow">Panel de control</p>
        <h1 className="page__title">Administración</h1>
        <p className="catalog__muted">Cargá el catálogo, mirá la operación y consultá la actividad desde acá.</p>
      </div>

      <div className="admin-hub">
        <OpsRibbon
          snapshot={systemQuery.data}
          failed={systemQuery.isError}
          pending={systemQuery.isPending}
        />
        <AdminHubSection title="Catálogo">
          <AdminCard
            id="packages"
            kicker="Producto"
            title="Paquetes"
            description="Cargá, editá y eliminá paquetes de cualquier agencia."
            count={packagesQuery.data?.totalElements}
            countLabel="paquetes"
            links={[{ to: ADMIN_PACKAGES_PATH, label: 'Gestionar paquetes' }]}
          />
          <AdminCard
            id="flights"
            kicker="Itinerarios"
            title="Vuelos"
            description="Cargá los viajes que después se asocian a los paquetes."
            count={flightsQuery.data?.length}
            countLabel="vuelos"
            links={[{ to: ADMIN_FLIGHTS_PATH, label: 'Gestionar vuelos' }]}
          />
          <AdminCard
            id="hotels"
            kicker="Hospedaje"
            title="Hoteles"
            description="Administrá el catálogo de hoteles por ciudad."
            count={hotelsQuery.data?.length}
            countLabel="hoteles"
            links={[
              { to: ADMIN_HOTELS_PATH, label: 'Gestionar hoteles' },
              { to: ADMIN_HOTEL_NEW_PATH, label: 'Nuevo hotel' },
            ]}
          />
        </AdminHubSection>

        <AdminHubSection title="Acceso y organización">
          <AdminCard
            id="users"
            kicker="Cuentas"
            title="Usuarios y vendedores"
            description="Creá cuentas, asigná roles y agencias, y habilitá o deshabilitá usuarios."
            count={usersQuery.data?.totalElements}
            countLabel="usuarios"
            links={[{ to: ADMIN_USERS_PATH, label: 'Gestionar usuarios' }]}
          />
          <AdminCard
            id="agencies"
            kicker="Red comercial"
            title="Agencias"
            description="Creá agencias y actualizá sus datos para organizar vendedores y paquetes."
            count={agenciesQuery.isError ? undefined : agenciesQuery.data?.length}
            countLabel="agencias"
            note={agencyNote}
            links={[{ to: ADMIN_AGENCIES_PATH, label: 'Gestionar agencias' }]}
          />
          <AdminCard
            id="sales"
            kicker="Operaciones"
            title="Ventas"
            description="Consultá las ventas de todas las agencias y el detalle de cada compra."
            count={purchasesQuery.data?.totalElements}
            countLabel="compras"
            links={[{ to: ADMIN_SALES_PATH, label: 'Consultar ventas' }]}
          />
        </AdminHubSection>

        <AdminHubSection title="Inteligencia" columns={2}>
          <AdminCard
            id="reports"
            showCount={false}
            kicker="Rankings"
            title="Reportes Top 5"
            description="Compradores, agencias y destinos con más actividad y destinos mejor puntuados."
            count={undefined}
            countLabel="Actividad del sistema"
            note={reports.note}
            preview={reports.preview}
            links={[{ to: '/admin/reports', label: 'Ver reportes' }]}
          />
          <AdminCard
            id="queries"
            showCount={false}
            kicker="Sólo lectura"
            title="Consultas del sistema"
            description="Consultá usuarios, favoritos, reseñas y compras sin modificar sus datos."
            count={undefined}
            countLabel="Exploración de datos"
            preview={[
              listPreview('Favoritos', favoritesQuery.data?.totalElements, favoritesQuery.isError),
              listPreview('Reseñas', reviewsQuery.data?.totalElements, reviewsQuery.isError),
            ]}
            links={[
              { to: '/admin/queries/users', label: 'Usuarios' },
              { to: '/admin/favorites', label: 'Favoritos' },
              { to: '/admin/reviews', label: 'Reseñas' },
              { to: '/admin/purchases', label: 'Compras' },
            ]}
          />
        </AdminHubSection>
      </div>
    </section>
  )
}
