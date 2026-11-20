import { describe, expect, it } from 'vitest'
import { scoutQuery, toggle, type SearchState } from './query'

const base: SearchState = {
  home: { lat: 30.0444, lon: 31.2357 },
  commute: false,
  radiusKm: 5,
  corridorM: 1500,
  visitsPerWeek: 3,
  amenities: [],
  mode: 'DRIVE',
}

describe('scoutQuery', () => {
  it('searches around home by radius', () => {
    expect(scoutQuery(base)).toBe(
      '?lat=30.04440&lon=31.23570&radius=5000&visitsPerWeek=3&mode=DRIVE',
    )
  })

  it('switches to the commute endpoint when a work point is set', () => {
    const q = scoutQuery({ ...base, commute: true, work: { lat: 30.09, lon: 31.32 }, amenities: ['sauna', 'pool'] })
    expect(q.startsWith('/commute?homeLat=30.04440')).toBe(true)
    expect(q).toContain('workLon=31.32000')
    expect(q).toContain('corridor=1500')
    expect(q).toContain('amenities=pool%2Csauna')
    expect(q).not.toContain('radius=')
  })

  it('falls back to radius search while no work point is chosen', () => {
    expect(scoutQuery({ ...base, commute: true })).toContain('radius=5000')
  })
})

describe('toggle', () => {
  it('adds and removes', () => {
    expect(toggle(['pool'], 'sauna')).toEqual(['pool', 'sauna'])
    expect(toggle(['pool', 'sauna'], 'pool')).toEqual(['sauna'])
  })
})
