import { useQuery } from '@tanstack/react-query'
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
  title: string
  description: string
  count: number | undefined
  countLabel: string
  showCount?: boolean
  links: { to: string; label: string }[]
}

function AdminCard({ id, title, description, count, countLabel, links, showCount = true }: AdminCardProps) {
  return (
    <article
      className="admin-card"
      data-testid={`admin-card-${id}`}
      aria-labelledby={`admin-card-${id}-title`}
    >
      <h2 id={`admin-card-${id}-title`}>{title}</h2>
      <p className="admin-card__count" data-testid={`admin-count-${id}`}>
        {showCount ? (count ?? '—') : null}
        <span className="admin-card__count-label"> {countLabel}</span>
      </p>
      <p className="catalog__muted">{description}</p>
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
      </div>
      <div className="admin-hub__grid">
        <AdminCard
          id="users"
          showCount={false}
          title="Usuarios y vendedores"
          description="Creá cuentas, asigná roles y agencias, y habilitá o deshabilitá usuarios."
          count={undefined}
          countLabel="Gestión de acceso"
          links={[{ to: ADMIN_USERS_PATH, label: 'Gestionar usuarios' }]}
        />
        <AdminCard
          id="agencies"
          showCount={false}
          title="Agencias"
          description="Creá agencias y actualizá sus datos para organizar vendedores y paquetes."
          count={undefined}
          countLabel="Organización comercial"
          links={[{ to: ADMIN_AGENCIES_PATH, label: 'Gestionar agencias' }]}
        />
        <AdminCard
          id="sales"
          showCount={false}
          title="Ventas"
          description="Consultá las ventas de todas las agencias y el detalle de cada compra."
          count={undefined}
          countLabel="Seguimiento de operaciones"
          links={[{ to: ADMIN_SALES_PATH, label: 'Consultar ventas' }]}
        />
        <article className="admin-card">
          <h2>Reportes Top 5</h2>
          <p>Compradores, agencias y destinos con más actividad y destinos mejor puntuados.</p>
          <Link className="admin-card__link" to="/admin/reports">
            Ver reportes
          </Link>
        </article>
        <article className="admin-card">
          <h2>Consultas del sistema</h2>
          <p>Consultá usuarios, favoritos, reseñas y compras sin modificar sus datos.</p>
          <div className="page__actions">
            <Link className="admin-card__link" to="/admin/queries/users">
              Usuarios
            </Link>
            <Link className="admin-card__link" to="/admin/favorites">
              Favoritos
            </Link>
            <Link className="admin-card__link" to="/admin/reviews">
              Reseñas
            </Link>
            <Link className="admin-card__link" to="/admin/purchases">
              Compras
            </Link>
          </div>
        </article>
        <AdminCard
          id="packages"
          title="Paquetes"
          description="Cargá, editá y eliminá paquetes de cualquier agencia."
          count={packagesQuery.data?.totalElements}
          countLabel="paquetes"
          links={[{ to: ADMIN_PACKAGES_PATH, label: 'Gestionar paquetes' }]}
        />
        <AdminCard
          id="flights"
          title="Vuelos"
          description="Cargá los viajes que después se asocian a los paquetes."
          count={flightsQuery.data?.length}
          countLabel="vuelos"
          links={[{ to: ADMIN_FLIGHTS_PATH, label: 'Gestionar vuelos' }]}
        />
        <AdminCard
          id="hotels"
          title="Hoteles"
          description="Administrá el catálogo de hoteles por ciudad."
          count={hotelsQuery.data?.length}
          countLabel="hoteles"
          links={[
            { to: '/hotels', label: 'Gestionar hoteles' },
            { to: '/hotels/new', label: 'Nuevo hotel' },
          ]}
        />
      </div>
    </section>
  )
}
