import { useEffect, useState } from 'react'
import { api, type NearbyGym } from './lib/api'
import { ScoutView } from './components/ScoutView'
import { TrainingView } from './components/TrainingView'
import { MembershipPanel } from './components/MembershipPanel'

type Tab = 'scout' | 'training' | 'membership'

const TABS: { id: Tab; label: string }[] = [
  { id: 'scout', label: 'Scout' },
  { id: 'training', label: 'Training' },
  { id: 'membership', label: 'Membership' },
]

const CAIRO = { lat: 30.0444, lon: 31.2357 }

export default function App() {
  const [tab, setTab] = useState<Tab>('scout')
  const [gyms, setGyms] = useState<NearbyGym[]>([])
  const [error, setError] = useState<string>()

  useEffect(() => {
    api.nearbyGyms(CAIRO).then(setGyms, (e: Error) => setError(e.message))
  }, [])

  return (
    <main>
      <header className="top">
        <div>
          <h1>Gym Scout</h1>
          <p className="muted">Find a gym that fits your week, then keep showing up.</p>
        </div>
        <nav className="tabs" aria-label="Sections">
          {TABS.map((t) => (
            <button
              key={t.id}
              className={tab === t.id ? 'tab active' : 'tab'}
              aria-current={tab === t.id ? 'page' : undefined}
              onClick={() => setTab(t.id)}
            >
              {t.label}
            </button>
          ))}
        </nav>
      </header>
      {error && <p className="notice">Could not load gyms: {error}</p>}
      {tab === 'scout' && <ScoutView start={CAIRO} />}
      {tab === 'training' && <TrainingView gyms={gyms} />}
      {tab === 'membership' && <MembershipPanel gyms={gyms} />}
      <footer className="muted small">
        Demo data: invented gyms and prices. Map data © OpenStreetMap contributors.
      </footer>
    </main>
  )
}
