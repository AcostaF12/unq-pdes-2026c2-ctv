import type { ReactNode } from 'react'
import './HeroScene.css'

interface HeroSceneProps {
  eyebrow: string
  title: string
  subtitle?: string
  /** Optional content under the copy (e.g. a call to action). */
  children?: ReactNode
  /** Shorter banner for secondary pages; the catalog uses the tall one under the search card. */
  compact?: boolean
}

/** Decorative sky (sun, clouds, route, plane, waves); sizes itself to the positioned parent. */
export function SceneBackdrop() {
  return (
    <div className="hero__scene" aria-hidden="true">
      <span className="hero__wash" />
      <span className="hero__sun" />
      <span className="hero__star hero__star--a" />
      <span className="hero__star hero__star--b" />
      <span className="hero__star hero__star--c" />
      <span className="hero__cloud hero__cloud--a" />
      <span className="hero__cloud hero__cloud--b" />
      <span className="hero__cloud hero__cloud--c" />
      <div className="hero__sky">
        <svg className="hero__route" viewBox="0 0 400 160" preserveAspectRatio="none" fill="none">
          <path className="hero__route-line" d="M-10 130 C 90 20, 190 150, 270 70 S 380 20, 420 60" />
        </svg>
        <div className="hero__flight">
          <svg className="hero__plane" viewBox="0 0 64 64" fill="currentColor">
            <path d="M62 30 38 26 26 6c-.4-.7-1.1-1-1.9-1h-3c-.9 0-1.5.9-1.2 1.7L24 24 10 22l-4-6c-.4-.6-1-.9-1.7-.9H2c-.8 0-1.4.8-1.1 1.6L4 30 .9 38.200c-.3.8.3 1.600 1.100 1.600h2.300c.7 0 1.300-.3 1.700-.9L10 33l14-2-5.100 16.300c-.3.800.3 1.700 1.200 1.700h3c.8 0 1.500-.4 1.900-1L38 34l24-4c1.400-.2 2-1.100 2-2s-.6-1.800-2-2z" />
          </svg>
        </div>
      </div>
      <svg className="hero__skyline" viewBox="0 0 1200 120" preserveAspectRatio="none">
        <path className="hero__skyline-back" d="M0 62C180 18 360 96 600 58S1010 14 1200 60V120H0Z" />
        <path className="hero__skyline-front" d="M0 88C230 52 450 108 700 80S1050 52 1200 86V120H0Z" />
      </svg>
    </div>
  )
}

/**
 * Decorative travel scene used as the page banner: a brand-colored sky with a drifting
 * cloud layer, a plane crossing a dashed route and a city skyline. Purely visual.
 */
export function HeroScene({ eyebrow, title, subtitle, children, compact = false }: HeroSceneProps) {
  return (
    <section className={compact ? 'hero hero--compact' : 'hero'}>
      <SceneBackdrop />
      <div className="hero__copy">
        <p className="hero__eyebrow">{eyebrow}</p>
        <h1 className="hero__title">{title}</h1>
        {subtitle ? <p className="hero__subtitle">{subtitle}</p> : null}
        {children}
      </div>
    </section>
  )
}
