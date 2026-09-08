import { Link } from 'react-router-dom'
import './NotFound.css'

export function NotFound() {
  return (
    <div className="page-center">
      <div className="not-found">
        <p className="not-found__code">404</p>
        <h1 className="not-found__title">Ruta no encontrada</h1>
        <p className="not-found__message">
          Esta página no existe o que hayas escrito mal la dirección.
        </p>
        <Link className="not-found__cta" to="/trips">
          Ir al inicio
        </Link>
      </div>
    </div>
  )
}
