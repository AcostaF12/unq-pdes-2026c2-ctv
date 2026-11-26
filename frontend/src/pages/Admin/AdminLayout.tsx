import { NavLink, Outlet } from 'react-router-dom'
import {
  ADMIN_AGENCIES_PATH,
  ADMIN_FLIGHTS_PATH,
  ADMIN_HOTELS_PATH,
  ADMIN_PACKAGES_PATH,
  ADMIN_PATH,
  ADMIN_SALES_PATH,
  ADMIN_SYSTEM_PATH,
  ADMIN_USERS_PATH,
  HOME_PATH,
} from '../../router/paths'
import './admin.css'

const FLOW_LINKS = [
  { to: ADMIN_PATH, label: 'Inicio', end: true },
  { to: ADMIN_HOTELS_PATH, label: 'Hoteles', end: false },
  { to: ADMIN_FLIGHTS_PATH, label: 'Vuelos', end: true },
  { to: ADMIN_PACKAGES_PATH, label: 'Paquetes', end: true },
]

const CONTROL_LINKS = [
  { to: ADMIN_AGENCIES_PATH, label: 'Agencias', end: true },
  { to: ADMIN_USERS_PATH, label: 'Usuarios', end: true },
  { to: ADMIN_SALES_PATH, label: 'Ventas', end: false },
  { to: '/admin/reports', label: 'Reportes', end: true },
  { to: ADMIN_SYSTEM_PATH, label: 'Operación', end: true },
]

export function AdminLayout() {
  return (
    <div className="admin-shell">
      <nav className="admin-nav" aria-label="Administración">
        <div className="admin-nav__track">
          <div className="admin-nav__group">
            {FLOW_LINKS.map((link) => (
              <NavLink key={link.to} to={link.to} end={link.end}>
                {link.label}
              </NavLink>
            ))}
          </div>
          <span className="admin-nav__rule" aria-hidden="true" />
          <div className="admin-nav__group">
            {CONTROL_LINKS.map((link) => (
              <NavLink key={link.to} to={link.to} end={link.end}>
                {link.label}
              </NavLink>
            ))}
          </div>
        </div>
        <NavLink className="admin-nav__catalog" to={HOME_PATH}>
          Ver catálogo
        </NavLink>
      </nav>
      <Outlet />
    </div>
  )
}
