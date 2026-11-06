/** Deterministic poster palette from an anime id / title (API has no cover art). */

const PALETTES = [
  ['#1a0a12', '#ff4733', '#f5c451'],
  ['#0a1620', '#2dd4bf', '#94a3b8'],
  ['#140c08', '#fb7185', '#fdba74'],
  ['#0c1020', '#60a5fa', '#c4b5fd'],
  ['#101008', '#a3e635', '#fde047'],
  ['#180810', '#f43f5e', '#fda4af'],
  ['#081410', '#34d399', '#6ee7b7'],
  ['#1a1008', '#f97316', '#fcd34d'],
  ['#100818', '#e879f9', '#67e8f9'],
  ['#0e0e14', '#ef4444', '#e2e8f0'],
]

export function posterPalette(seed: string | number) {
  const s = String(seed)
  let hash = 0
  for (let i = 0; i < s.length; i++) hash = (hash * 31 + s.charCodeAt(i)) >>> 0
  const [deep, mid, light] = PALETTES[hash % PALETTES.length]
  const angle = 120 + (hash % 80)
  return { deep, mid, light, angle, hash }
}

export function posterInitials(title: string) {
  const parts = title.trim().split(/\s+/).filter(Boolean)
  if (parts.length === 0) return 'MAL'
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase()
  return (parts[0][0] + parts[1][0]).toUpperCase()
}
