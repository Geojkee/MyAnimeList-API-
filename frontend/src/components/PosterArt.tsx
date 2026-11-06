import type { CSSProperties } from 'react'
import { posterInitials, posterPalette } from '../lib/poster'

type Props = {
  seed: string | number
  title: string
  type?: string
  className?: string
  size?: 'tile' | 'hero' | 'row'
}

export function PosterArt({ seed, title, type, className = '', size = 'tile' }: Props) {
  const { deep, mid, light, angle, hash } = posterPalette(seed)
  const initials = posterInitials(title)
  const pattern = hash % 3

  const style = {
    '--p-deep': deep,
    '--p-mid': mid,
    '--p-light': light,
    '--p-angle': `${angle}deg`,
  } as CSSProperties

  return (
    <div
      className={`poster ${size === 'hero' ? 'poster--hero' : ''} ${size === 'row' ? 'poster--row' : ''} ${className}`}
      style={style}
      aria-hidden
    >
      <div className={`poster__wash poster__wash--${pattern}`} />
      <div className="poster__grain" />
      <div className="poster__mark">{initials}</div>
      {type && <span className="poster__type">{type}</span>}
      <div className="poster__title">{title}</div>
    </div>
  )
}
