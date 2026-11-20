// Builds the scout query string from the search panel state.
import type { LatLon, TravelMode } from './api'

export type SearchState = {
  home: LatLon
  work?: LatLon
  commute: boolean
  radiusKm: number
  corridorM: number
  visitsPerWeek: number
  amenities: string[]
  mode: TravelMode
}

export const AMENITIES = [
  'weights',
  'cardio',
  'pool',
  'sauna',
  'classes',
  'yoga',
  'crossfit',
  'women-only',
  'open-24h',
  'parking',
  'accessible',
] as const

const fixed = (n: number) => n.toFixed(5)

export function scoutQuery(s: SearchState): string {
  const p = new URLSearchParams()
  if (s.commute && s.work) {
    p.set('homeLat', fixed(s.home.lat))
    p.set('homeLon', fixed(s.home.lon))
    p.set('workLat', fixed(s.work.lat))
    p.set('workLon', fixed(s.work.lon))
    p.set('corridor', String(Math.round(s.corridorM)))
  } else {
    p.set('lat', fixed(s.home.lat))
    p.set('lon', fixed(s.home.lon))
    p.set('radius', String(Math.round(s.radiusKm * 1000)))
  }
  p.set('visitsPerWeek', String(s.visitsPerWeek))
  p.set('mode', s.mode)
  if (s.amenities.length > 0) p.set('amenities', [...s.amenities].sort().join(','))
  return (s.commute && s.work ? '/commute?' : '?') + p.toString()
}

export function toggle(list: string[], value: string): string[] {
  return list.includes(value) ? list.filter((v) => v !== value) : [...list, value]
}
