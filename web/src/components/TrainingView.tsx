import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { api, type LoggedSession, type NearbyGym, type Progress, type SessionSummary } from '../lib/api'
import { isoDate } from '../lib/format'

type Row = { exercise: string; weightKg: string; reps: string }

const EMPTY: Row = { exercise: '', weightKg: '', reps: '' }

const KIND_LABEL = { HEAVIEST_WEIGHT: 'heaviest', ESTIMATED_ONE_REP_MAX: 'est. 1RM' } as const

export function TrainingView({ gyms }: { gyms: NearbyGym[] }) {
  const [rows, setRows] = useState<Row[]>([{ exercise: 'Back squat', weightKg: '', reps: '5' }])
  const [today] = useState(() => isoDate(new Date()))
  const [date, setDate] = useState(today)
  const [gymId, setGymId] = useState('')
  const [notes, setNotes] = useState('')
  const [progress, setProgress] = useState<Progress>()
  const [sessions, setSessions] = useState<SessionSummary[]>([])
  const [logged, setLogged] = useState<LoggedSession>()
  const [error, setError] = useState<string>()

  const refresh = useCallback(() => {
    api.progress(8).then(setProgress, (e: Error) => setError(e.message))
    api.sessions().then(setSessions, (e: Error) => setError(e.message))
  }, [])
  useEffect(refresh, [refresh])

  const update = (i: number, patch: Partial<Row>) =>
    setRows(rows.map((r, j) => (j === i ? { ...r, ...patch } : r)))

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setError(undefined)
    const sets = rows
      .filter((r) => r.exercise.trim() && r.reps)
      .map((r) => ({ exercise: r.exercise.trim(), weightKg: Number(r.weightKg || 0), reps: Number(r.reps) }))
    if (sets.length === 0) {
      setError('Add at least one set')
      return
    }
    try {
      const result = await api.logSession({ performedOn: date, gymId: gymId || undefined, notes: notes || undefined, sets })
      setLogged(result)
      setRows([{ ...EMPTY }])
      setNotes('')
      refresh()
    } catch (err) {
      setError((err as Error).message)
    }
  }

  const maxVolume = Math.max(1, ...(progress?.weeklyVolume.map((w) => w.volumeKg) ?? [1]))
  const gymName = (id?: string) => gyms.find((g) => g.gym.id === id)?.gym.name

  return (
    <section className="two-col">
      <form className="panel" onSubmit={submit}>
        <h2>Log a workout</h2>
        <div className="row">
          <label>
            Date
            <input type="date" value={date} max={today} onChange={(e) => setDate(e.target.value)} />
          </label>
          <label>
            Gym
            <select value={gymId} onChange={(e) => setGymId(e.target.value)}>
              <option value="">Home / outdoors</option>
              {gyms.map((g) => (
                <option key={g.gym.id} value={g.gym.id}>
                  {g.gym.name}
                </option>
              ))}
            </select>
          </label>
        </div>
        <table className="sets">
          <thead>
            <tr>
              <th>Exercise</th>
              <th>kg</th>
              <th>Reps</th>
              <th aria-label="Remove" />
            </tr>
          </thead>
          <tbody>
            {rows.map((r, i) => (
              <tr key={i}>
                <td>
                  <input aria-label={`Exercise ${i + 1}`} value={r.exercise} onChange={(e) => update(i, { exercise: e.target.value })} />
                </td>
                <td>
                  <input aria-label={`Weight ${i + 1}`} inputMode="decimal" value={r.weightKg} onChange={(e) => update(i, { weightKg: e.target.value })} />
                </td>
                <td>
                  <input aria-label={`Reps ${i + 1}`} inputMode="numeric" value={r.reps} onChange={(e) => update(i, { reps: e.target.value })} />
                </td>
                <td>
                  <button type="button" className="ghost" aria-label="Remove set" onClick={() => setRows(rows.filter((_, j) => j !== i))}>
                    ×
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <div className="row">
          <button type="button" className="ghost" onClick={() => setRows([...rows, { ...(rows.at(-1) ?? EMPTY) }])}>
            + Set
          </button>
          <input aria-label="Notes" placeholder="Notes (optional)" value={notes} onChange={(e) => setNotes(e.target.value)} />
        </div>
        <button type="submit" className="primary">
          Log workout
        </button>
        {error && <p className="notice">{error}</p>}
        {logged && (
          <div className={logged.records.length > 0 ? 'pr-banner' : 'muted'}>
            {logged.records.length > 0
              ? logged.records.map((r) => (
                  <div key={r.exercise + r.kind}>
                    New record: {r.exercise} {KIND_LABEL[r.kind]} {r.value} kg (was {r.previous})
                  </div>
                ))
              : 'Logged. No new records this time.'}
          </div>
        )}
      </form>

      <div className="panel">
        <h2>Weekly volume (kg × reps)</h2>
        <div className="bars" role="img" aria-label="Weekly training volume for the last 8 weeks">
          {progress?.weeklyVolume.map((w) => (
            <div key={w.weekStart} className="bar-col" title={`${w.weekStart}: ${w.volumeKg} kg in ${w.sets} sets`}>
              <div className="bar-fill" style={{ height: `${(w.volumeKg / maxVolume) * 100}%` }} />
              <span className="small muted">{w.weekStart.slice(5)}</span>
            </div>
          ))}
        </div>
        <h2>Personal records</h2>
        {progress?.recentRecords.length ? (
          <ul className="plain">
            {progress.recentRecords.map((r) => (
              <li key={r.sessionId + r.exercise + r.kind}>
                <strong>{r.exercise}</strong> {KIND_LABEL[r.kind]} {r.value} kg <span className="muted">(was {r.previous}, {r.on})</span>
              </li>
            ))}
          </ul>
        ) : (
          <p className="muted">Records appear from your second session of an exercise.</p>
        )}
        <h2>Bests</h2>
        <table className="data">
          <thead>
            <tr>
              <th>Exercise</th>
              <th>Heaviest</th>
              <th>Est. 1RM</th>
              <th>Sessions</th>
            </tr>
          </thead>
          <tbody>
            {progress?.bests.map((b) => (
              <tr key={b.exercise}>
                <td>{b.exercise}</td>
                <td>{b.heaviestKg} kg</td>
                <td>{b.bestOneRepMax} kg</td>
                <td>{b.sessions}</td>
              </tr>
            ))}
          </tbody>
        </table>
        <h2>Recent sessions</h2>
        <ul className="plain">
          {sessions.map((s) => (
            <li key={s.id}>
              {s.performedOn} · {s.sets} sets · {Math.round(s.volumeKg)} kg{gymName(s.gymId) ? ` · ${gymName(s.gymId)}` : ''}
            </li>
          ))}
        </ul>
      </div>
    </section>
  )
}
