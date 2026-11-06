import type { ReactNode } from 'react'

type Props = {
  title: string
  children?: ReactNode
  action?: ReactNode
}

export function EmptyState({ title, children, action }: Props) {
  return (
    <div className="empty">
      <div className="empty__orb" aria-hidden />
      <strong>{title}</strong>
      {children && <p>{children}</p>}
      {action && <div className="empty__action">{action}</div>}
    </div>
  )
}
