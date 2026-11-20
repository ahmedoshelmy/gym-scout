// Display helpers. Money arrives as integer minor units (piastres for EGP).

export function money(minor: number | undefined, currency = 'EGP'): string {
  if (minor === undefined || minor === null) return '—'
  const major = minor / 100
  const digits = Number.isInteger(major) ? 0 : 2
  return `${currency} ${major.toLocaleString('en-US', { minimumFractionDigits: digits, maximumFractionDigits: 2 })}`
}

export function parseMoney(text: string): number | undefined {
  const clean = text.replace(/[^0-9.]/g, '')
  if (!clean || !/^\d+(\.\d{0,2})?$/.test(clean)) return undefined
  return Math.round(Number(clean) * 100)
}

export function distance(meters: number): string {
  return meters < 1000 ? `${Math.round(meters)} m` : `${(meters / 1000).toFixed(1)} km`
}

export function minutes(value: number): string {
  if (value < 1) return '< 1 min'
  if (value < 60) return `${Math.round(value)} min`
  const h = Math.floor(value / 60)
  const m = Math.round(value % 60)
  return m === 0 ? `${h} h` : `${h} h ${m} min`
}

/** Local calendar date as yyyy-mm-dd (not UTC, so late-evening logs land on the right day). */
export function isoDate(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

export function addDays(date: Date, days: number): Date {
  const copy = new Date(date)
  copy.setDate(copy.getDate() + days)
  return copy
}
