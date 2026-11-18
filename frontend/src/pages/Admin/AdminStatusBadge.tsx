interface AdminStatusBadgeProps {
  active: boolean
  activeLabel: string
  inactiveLabel: string
}

export function AdminStatusBadge({ active, activeLabel, inactiveLabel }: AdminStatusBadgeProps) {
  return (
    <span className={`admin-badge ${active ? 'admin-badge--ok' : 'admin-badge--off'}`}>
      {active ? activeLabel : inactiveLabel}
    </span>
  )
}
