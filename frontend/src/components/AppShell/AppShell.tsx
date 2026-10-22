import { useEffect, useRef } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import { ADMIN_PATH } from '../../router/paths'
import { AccountMenu } from '../AccountMenu/AccountMenu'
import { BrandMark } from '../BrandMark/BrandMark'
import './AppShell.css'

export function AppShell() {
  const { user, isAdmin, isBuyer, isAgency, logout } = useAuth()
  const { pathname } = useLocation()

  const mainRef = useRef<HTMLElement>(null)
  const firstRender = useRef(true)

  useEffect(() => {
    window.scrollTo?.({ top: 0 })
    // Move focus to the new page content on client-side navigation (not on first load).
    if (firstRender.current) {
      firstRender.current = false
      return
    }
    mainRef.current?.focus({ preventScroll: true })
  }, [pathname])

  return (
    <div className="app-shell">
      <a className="skip-link" href="#main-content">
        Saltar al contenido
      </a>
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
          {isAgency ? <NavLink to="/agency" end>Agencia</NavLink> : null}
          {isAgency ? <NavLink to="/agency/sales">Ventas</NavLink> : null}
          {isAdmin ? <NavLink to={ADMIN_PATH}>Administración</NavLink> : null}
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
      <main id="main-content" ref={mainRef} tabIndex={-1} className="app-shell__main">
        <div key={pathname} className="page-transition">
          <Outlet />
        </div>
      </main>
      <footer className="app-shell__footer">
        <div className="app-shell__footer-inner">
          <span>
            <strong>Compra Tu Viaje</strong> · Vuelo y hotel en un solo lugar
          </span>
          <span>Proyecto UNQ · Prácticas de Desarrollo de Software 2026</span>
        </div>
      </footer>
    </div>
  )
}
