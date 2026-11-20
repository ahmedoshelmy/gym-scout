import { describe, expect, it } from 'vitest'
import { addDays, distance, isoDate, minutes, money, parseMoney } from './format'

describe('money', () => {
  it('shows whole amounts without decimals and keeps piastres otherwise', () => {
    expect(money(120000)).toBe('EGP 1,200')
    expect(money(9333)).toBe('EGP 93.33')
    expect(money(undefined)).toBe('—')
  })

  it('parses typed amounts into minor units', () => {
    expect(parseMoney('1,200')).toBe(120000)
    expect(parseMoney('93.5')).toBe(9350)
    expect(parseMoney('abc')).toBeUndefined()
    expect(parseMoney('1.234')).toBeUndefined()
  })
})

describe('units', () => {
  it('formats distance and time for people', () => {
    expect(distance(240.4)).toBe('240 m')
    expect(distance(5230)).toBe('5.2 km')
    expect(minutes(0.4)).toBe('< 1 min')
    expect(minutes(12.4)).toBe('12 min')
    expect(minutes(75)).toBe('1 h 15 min')
    expect(minutes(120)).toBe('2 h')
  })

  it('uses the local calendar day', () => {
    const late = new Date(2026, 2, 9, 23, 30)
    expect(isoDate(late)).toBe('2026-03-09')
    expect(isoDate(addDays(late, 1))).toBe('2026-03-10')
  })
})
