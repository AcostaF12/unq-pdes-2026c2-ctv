import { Link } from 'react-router-dom'
import { ADMIN_FLIGHTS_PATH, ADMIN_HOTELS_PATH, ADMIN_PACKAGES_PATH } from '../../router/paths'

const STEPS = [
  { id: 'hotels', to: ADMIN_HOTELS_PATH, label: 'Hotel' },
  { id: 'flights', to: ADMIN_FLIGHTS_PATH, label: 'Vuelos' },
  { id: 'packages', to: ADMIN_PACKAGES_PATH, label: 'Paquete' },
] as const

export function AdminFlow({ current }: { current: (typeof STEPS)[number]['id'] }) {
  return (
    <ol className="admin-flow" aria-label="Armado del paquete">
      {STEPS.map((step, index) => (
        <li key={step.id}>
          {step.id === current ? (
            <span aria-current="step">{`${index + 1}. ${step.label}`}</span>
          ) : (
            <Link to={step.to}>{`${index + 1}. ${step.label}`}</Link>
          )}
        </li>
      ))}
    </ol>
  )
}
