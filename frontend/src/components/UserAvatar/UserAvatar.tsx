import { userInitials } from './userInitials'
import './UserAvatar.css'

type UserAvatarSize = 'sm' | 'md' | 'lg'
type UserAvatarTone = 'onDark' | 'onLight'

interface UserAvatarProps {
  firstName: string
  lastName: string
  size?: UserAvatarSize
  tone?: UserAvatarTone
}

function sizeClassName(size: UserAvatarSize): string {
  switch (size) {
    case 'sm':
      return 'user-avatar--sm'
    case 'md':
      return 'user-avatar--md'
    case 'lg':
      return 'user-avatar--lg'
    default: {
      const exhaustive: never = size
      return exhaustive
    }
  }
}

function toneClassName(tone: UserAvatarTone): string {
  switch (tone) {
    case 'onDark':
      return 'user-avatar--on-dark'
    case 'onLight':
      return 'user-avatar--on-light'
    default: {
      const exhaustive: never = tone
      return exhaustive
    }
  }
}

export function UserAvatar({
  firstName,
  lastName,
  size = 'sm',
  tone = 'onLight',
}: UserAvatarProps) {
  return (
    <span
      className={['user-avatar', sizeClassName(size), toneClassName(tone)].join(' ')}
      aria-hidden="true"
    >
      {userInitials(firstName, lastName)}
    </span>
  )
}
