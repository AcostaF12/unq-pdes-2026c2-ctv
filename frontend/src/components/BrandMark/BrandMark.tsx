import './BrandMark.css'

type BrandMarkVariant = 'header' | 'auth'

interface BrandMarkProps {
  variant?: BrandMarkVariant
  /** `light` renders a white wordmark for use on the brand-colored scene panel. */
  tone?: 'default' | 'light'
}

export function BrandMark({ variant = 'header', tone = 'default' }: BrandMarkProps) {
  return (
    <span className={`brand-mark brand-mark--${variant}${tone === 'light' ? ' brand-mark--light' : ''}`}>
      <img className="brand-mark__mark" src="/logo.png" alt="" width={48} height={48} />
      <span className="brand-mark__text">
        <span className="brand-mark__code">CTV</span>
        <span className="brand-mark__name">Compra Tu Viaje</span>
      </span>
    </span>
  )
}
