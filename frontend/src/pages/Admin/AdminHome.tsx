import { useQuery } from '@tanstack/react-query'
import { type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { listFlights } from '../../api/flights'
import { listHotels } from '../../api/hotels'
import { searchPackages } from '../../api/packages'
import { queryKeys } from '../../query/keys'
import {
  ADMIN_AGENCIES_PATH,
  ADMIN_FLIGHTS_PATH,
  ADMIN_PACKAGES_PATH,
  ADMIN_SALES_PATH,
  ADMIN_USERS_PATH,
} from '../../router/paths'
import '../Packages/packages.css'
import './admin.css'

interface AdminCardProps {
  id: string
  kicker: string
  title: string
  description: string
  count: number | undefined
  countLabel: string
  showCount?: boolean
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

  return (
    <section className="page" data-testid="admin-hub">
      <div className="catalog__intro">
        <p className="catalog__eyebrow">Panel de control</p>
        <h1 className="page__title">Administración</h1>
        <p className="catalog__muted">Acceso, operaciones y catálogo en un solo lugar.</p>
      </div>

      <div className="admin-hub">
        <AdminHubSection title="Acceso y organización">
          <AdminCard
            id="users"
            showCount={false}
            kicker="Cuentas"
            title="Usuarios y vendedores"
            description="Creá cuentas, asigná roles y agencias, y habilitá o deshabilitá usuarios."
            count={undefined}
            countLabel="Gestión de acceso"
            links={[{ to: ADMIN_USERS_PATH, label: 'Gestionar usuarios' }]}
          />
          <AdminCard
            id="agencies"
            showCount={false}
            kicker="Red comercial"
            title="Agencias"
            description="Creá agencias y actualizá sus datos para organizar vendedores y paquetes."
            count={undefined}
            countLabel="Organización comercial"
            links={[{ to: ADMIN_AGENCIES_PATH, label: 'Gestionar agencias' }]}
          />
          <AdminCard
            id="sales"
            showCount={false}
            kicker="Operaciones"
            title="Ventas"
            description="Consultá las ventas de todas las agencias y el detalle de cada compra."
            count={undefined}
            countLabel="Seguimiento de operaciones"
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
            links={[
              { to: '/admin/queries/users', label: 'Usuarios' },
              { to: '/admin/favorites', label: 'Favoritos' },
              { to: '/admin/reviews', label: 'Reseñas' },
              { to: '/admin/purchases', label: 'Compras' },
            ]}
          />
        </AdminHubSection>

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
              { to: '/hotels', label: 'Gestionar hoteles' },
              { to: '/hotels/new', label: 'Nuevo hotel' },
            ]}
          />
        </AdminHubSection>
      </div>
    </section>
  )
}
