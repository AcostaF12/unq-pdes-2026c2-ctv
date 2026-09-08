import { useEffect, useId, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { PROFILE_PATH } from '../../router/paths'
import { Button } from '../Button/Button'
import { UserAvatar } from '../UserAvatar/UserAvatar'
import './AccountMenu.css'

interface AccountMenuProps {
  firstName: string
  lastName: string
  username: string
  onLogout: () => void
}

export function AccountMenu({ firstName, lastName, username, onLogout }: AccountMenuProps) {
  const [open, setOpen] = useState(false)
  const rootRef = useRef<HTMLDivElement>(null)
  const menuId = useId()
  const fullName = `${firstName} ${lastName}`.trim()

  useEffect(() => {
    if (!open) {
      return
    }

    const onPointerDown = (event: MouseEvent) => {
      if (rootRef.current && !rootRef.current.contains(event.target as Node)) {
        setOpen(false)
      }
    }

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setOpen(false)
      }
    }

    document.addEventListener('mousedown', onPointerDown)
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('mousedown', onPointerDown)
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [open])

  return (
    <div className="account-menu" ref={rootRef}>
      <button
        type="button"
        className="account-menu__trigger"
        aria-label={`${fullName}, menú de cuenta`}
        aria-expanded={open}
        aria-haspopup="menu"
        aria-controls={menuId}
        onClick={() => setOpen((value) => !value)}
      >
        <UserAvatar firstName={firstName} lastName={lastName} tone="onDark" />
        <span className="account-menu__meta">
          <span className="account-menu__name">{fullName}</span>
          <span className="account-menu__hint">@{username}</span>
        </span>
        <span className="account-menu__caret" aria-hidden="true" />
      </button>
      {open ? (
        <div className="account-menu__panel" id={menuId} role="menu" aria-label="Cuenta">
          <div className="account-menu__identity">
            <UserAvatar firstName={firstName} lastName={lastName} size="md" />
            <p className="account-menu__identity-copy">
              <span className="account-menu__identity-name">{fullName}</span>
              <span className="account-menu__identity-user">@{username}</span>
            </p>
          </div>
          <Link
            className="account-menu__item"
            role="menuitem"
            to={PROFILE_PATH}
            onClick={() => setOpen(false)}
          >
            Mi cuenta
          </Link>
          <Link
            className="account-menu__item"
            role="menuitem"
            to={`${PROFILE_PATH}#preferencias`}
            onClick={() => setOpen(false)}
          >
            Preferencias
          </Link>
          <Link
            className="account-menu__item"
            role="menuitem"
            to={`${PROFILE_PATH}#password`}
            onClick={() => setOpen(false)}
          >
            Contraseña
          </Link>
          <Button
            type="button"
            variant="ghost"
            className="account-menu__logout"
            role="menuitem"
            onClick={onLogout}
          >
            Salir
          </Button>
        </div>
      ) : null}
    </div>
  )
}
