import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import { AccountMenu } from '../AccountMenu/AccountMenu'
import { BrandMark } from '../BrandMark/BrandMark'
import './AppShell.css'

export function AppShell() {
  const { user, isAdmin, isBuyer, isAgency, logout } = useAuth()

  return (
    <div className="app-shell">
      <header className="app-shell__header">
        <NavLink className="app-shell__brand" to="/trips" aria-label="Compra Tu Viaje">
          <BrandMark />
        </NavLink>
        <nav className="app-shell__nav" aria-label="Principal">
          <NavLink to="/trips">Paquetes</NavLink>
          <NavLink to="/hotels" end>
            Hoteles
          </NavLink>
          {isBuyer ? <NavLink to="/favorites">Favoritos</NavLink> : null}
          {isBuyer ? <NavLink to="/purchases">Mis compras</NavLink> : null}
          {isAgency ? <NavLink to="/agency">Agencia</NavLink> : null}
          {isAgency ? <NavLink to="/agency/sales">Ventas</NavLink> : null}
          {isAdmin ? <NavLink to="/hotels/new">Nuevo hotel</NavLink> : null}
        </nav>
        <div className="app-shell__session">
          {user ? (
            <AccountMenu
              firstName={user.firstName}
              lastName={user.lastName}
              username={user.username}
              onLogout={logout}
            />
          ) : null}
        </div>
      </header>
      <main className="app-shell__main">
        <Outlet />
      </main>
    </div>
  )
}
