import './BrandMark.css'

type BrandMarkVariant = 'header' | 'auth'

interface BrandMarkProps {
  variant?: BrandMarkVariant
}

export function BrandMark({ variant = 'header' }: BrandMarkProps) {
  return (
    <span className={`brand-mark brand-mark--${variant}`}>
      <img className="brand-mark__mark" src="/logo.png" alt="" width={48} height={48} />
      <span className="brand-mark__text">
        <span className="brand-mark__code">CTV</span>
        <span className="brand-mark__name">Compra Tu Viaje</span>
      </span>
    </span>
  )
}
