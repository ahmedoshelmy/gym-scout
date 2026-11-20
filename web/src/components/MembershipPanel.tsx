import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { api, type MembershipView, type NearbyGym } from '../lib/api'
import { addDays, isoDate, money, parseMoney } from '../lib/format'

export function MembershipPanel({ gyms }: { gyms: NearbyGym[] }) {
  const [current, setCurrent] = useState<MembershipView | null>()
  const [error, setError] = useState<string>()
  const [gymId, setGymId] = useState('')
  const [planName, setPlanName] = useState('Monthly unlimited')
  const [price, setPrice] = useState('')
  const [startsOn, setStartsOn] = useState(() => isoDate(new Date()))
  const [days, setDays] = useState(30)

  const load = useCallback(() => {
    api.currentMembership().then((m) => setCurrent(m ?? null), (e: Error) => setError(e.message))
  }, [])
  useEffect(load, [load])

  const gym = gyms.find((g) => g.gym.id === gymId)?.gym

  const choosePlan = (id: string) => {
    const plan = gym?.plans.find((p) => p.id === id)
    if (plan) {
      setPlanName(plan.name)
      setPrice(String(plan.priceMinor / 100))
      setDays(plan.durationDays)
    }
  }

  const start = async (e: FormEvent) => {
    e.preventDefault()
    const priceMinor = parseMoney(price)
    if (!gymId || priceMinor === undefined) {
      setError('Pick a gym and enter the price you paid')
      return
    }
    try {
      const view = await api.startMembership({
        gymId,
        planName,
        priceMinor,
        currency: 'EGP',
        startsOn,
        endsOn: isoDate(addDays(new Date(`${startsOn}T12:00:00`), days - 1)),
      })
      setCurrent(view)
      setError(undefined)
    } catch (err) {
      setError((err as Error).message)
    }
  }

  const visit = async () => {
    if (!current) return
    try {
      setCurrent(await api.addVisit(current.membership.id))
    } catch (err) {
      setError((err as Error).message)
    }
  }

  if (current === undefined) return <p className="muted">Loading…</p>

  if (current) {
    const { membership: m, stats: s } = current
    const progress = Math.round((s.daysElapsed / s.totalDays) * 100)
    return (
      <section className="panel membership">
        <h2>
          {current.gymName} · {m.planName}
        </h2>
        <p className="muted">
          {m.startsOn} → {m.endsOn} · paid {money(m.priceMinor, m.currency)}
        </p>
        <div className="stats">
          <div>
            <span className="big">{s.visits}</span>
            <span className="muted">visits</span>
          </div>
          <div>
            <span className="big">{money(s.costPerVisitMinor, m.currency)}</span>
            <span className="muted">per visit so far</span>
          </div>
          <div>
            <span className="big">{money(s.projectedCostPerVisitMinor, m.currency)}</span>
            <span className="muted">per visit at this pace ({s.projectedVisits} visits)</span>
          </div>
          <div>
            <span className="big">{s.visitsPerWeek}</span>
            <span className="muted">visits / week</span>
          </div>
        </div>
        <div className="progress" aria-label={`${progress}% of the membership used`}>
          <span style={{ width: `${progress}%` }} />
        </div>
        <p className="small muted">
          Day {s.daysElapsed} of {s.totalDays}. Workouts logged at this gym count as visits automatically.
        </p>
        <button className="primary" onClick={visit}>
          I went today
        </button>
        {error && <p className="notice">{error}</p>}
      </section>
    )
  }

  return (
    <form className="panel membership" onSubmit={start}>
      <h2>Start tracking a membership</h2>
      <p className="muted">See what each visit really costs you.</p>
      <label>
        Gym
        <select value={gymId} onChange={(e) => setGymId(e.target.value)}>
          <option value="">Choose…</option>
          {gyms.map((g) => (
            <option key={g.gym.id} value={g.gym.id}>
              {g.gym.name}
            </option>
          ))}
        </select>
      </label>
      {gym && gym.plans.length > 0 && (
        <label>
          Listed plans
          <select defaultValue="" onChange={(e) => choosePlan(e.target.value)}>
            <option value="">Custom</option>
            {gym.plans.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name} · {money(p.priceMinor, p.currency)}
              </option>
            ))}
          </select>
        </label>
      )}
      <div className="row">
        <label>
          Plan
          <input value={planName} onChange={(e) => setPlanName(e.target.value)} />
        </label>
        <label>
          Price paid (EGP)
          <input inputMode="decimal" value={price} onChange={(e) => setPrice(e.target.value)} />
        </label>
      </div>
      <div className="row">
        <label>
          Starts
          <input type="date" value={startsOn} onChange={(e) => setStartsOn(e.target.value)} />
        </label>
        <label>
          Days
          <input type="number" min={1} max={3660} value={days} onChange={(e) => setDays(Number(e.target.value))} />
        </label>
      </div>
      <button type="submit" className="primary">
        Start
      </button>
      {error && <p className="notice">{error}</p>}
    </form>
  )
}
