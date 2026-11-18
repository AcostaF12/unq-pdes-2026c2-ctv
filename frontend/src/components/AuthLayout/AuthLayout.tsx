import type { ReactNode } from 'react'
import { BrandMark } from '../BrandMark/BrandMark'
import { SceneBackdrop } from '../HeroScene/HeroScene'
import './AuthLayout.css'

/** Split screen for login/register: brand scene on the left, the form on the right. */
export function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <div className="auth">
      <aside className="auth__scene" aria-hidden="true">
        <SceneBackdrop />
        <div className="auth__brand">
          <BrandMark variant="auth" tone="light" />
        </div>
        <div className="auth__pitch">
          <p className="auth__headline">Tu próximo viaje empieza acá.</p>
          <ul className="auth__perks">
            <li>Vuelo y hotel en un solo paso</li>
            <li>Guardá tus favoritos y comprá cuando quieras</li>
            <li>Todas tus reservas en un solo lugar</li>
          </ul>
        </div>
      </aside>
      <div className="auth__panel">{children}</div>
    </div>
  )
}
