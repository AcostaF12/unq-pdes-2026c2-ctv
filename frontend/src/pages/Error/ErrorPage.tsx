import './ErrorPage.css'

interface ErrorPageProps {
  message?: string
}

export function ErrorPage({ message }: ErrorPageProps) {
  return (
    <div className="page-center">
      <div className="error-page">
        <div className="error-page__scene" aria-hidden="true">
          <svg className="error-page__plane" viewBox="0 0 64 64" fill="none">
            <path
              d="M4 34 L44 22 L60 8 L54 26 L34 40 L44 54 L34 52 L26 40 L14 44 L10 38 Z"
              fill="var(--color-ink)"
            />
          </svg>
          <div className="error-page__ground" />
        </div>

        <p className="error-page__eyebrow">Error</p>
        <h1 className="error-page__title">Algo se cayo en el camino</h1>
        <p className="error-page__message">
          {message ?? 'No pudimos completar esta operación. Intentá de nuevo en unos minutos.'}
        </p>

        <button
          type="button"
          className="error-page__cta"
          onClick={() => window.location.reload()}
        >
          Reintentar
        </button>
      </div>
    </div>
  )
}
