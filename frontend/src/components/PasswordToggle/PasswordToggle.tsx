import './PasswordToggle.css'

interface PasswordToggleProps {
  visible: boolean
  onToggle: () => void
}

export function PasswordToggle({ visible, onToggle }: PasswordToggleProps) {
  return (
    <button
      type="button"
      className="eye-toggle"
      onClick={onToggle}
      aria-pressed={visible}
      aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
      title={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
    >
      <svg
        key={String(visible)}
        className="eye-toggle__icon"
        width="22"
        height="22"
        viewBox="0 0 24 24"
        fill="none"
        aria-hidden="true"
      >
        {visible ? (
          <>
            <path
              d="M2 12C2 12 5.5 5.5 12 5.5C18.5 5.5 22 12 22 12C22 12 18.5 18.5 12 18.5C5.5 18.5 2 12 2 12Z"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
            <circle cx="12" cy="12" r="3" stroke="currentColor" strokeWidth="2" />
          </>
        ) : (
          <>
            <path
              d="M2 12C2 12 5.5 5.5 12 5.5C18.5 5.5 22 12 22 12C22 12 18.5 18.5 12 18.5C5.5 18.5 2 12 2 12Z"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
            <line x1="4" y1="20" x2="20" y2="4" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
          </>
        )}
      </svg>
    </button>
  )
}
