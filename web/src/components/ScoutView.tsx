import { useEffect, useState } from 'react'
import { CircleMarker, MapContainer, TileLayer, Tooltip, useMapEvents } from 'react-leaflet'
import { api, type LatLon, type ScoutResponse } from '../lib/api'
import { distance, minutes, money } from '../lib/format'
import { AMENITIES, scoutQuery, toggle, type SearchState } from '../lib/query'

type Placing = 'home' | 'work'

function ClickToPlace({ onPick }: { onPick: (p: LatLon) => void }) {
  useMapEvents({ click: (e) => onPick({ lat: e.latlng.lat, lon: e.latlng.lng }) })
  return null
}

export function ScoutView({ start }: { start: LatLon }) {
  const [search, setSearch] = useState<SearchState>({
    home: start,
    commute: false,
    radiusKm: 6,
    corridorM: 1500,
    visitsPerWeek: 3,
    amenities: [],
    mode: 'DRIVE',
  })
  const [placing, setPlacing] = useState<Placing>('home')
  const [report, setReport] = useState<ScoutResponse>()
  const [selected, setSelected] = useState<string>()
  const [error, setError] = useState<string>()

  const query = scoutQuery(search)
  useEffect(() => {
    let live = true
    api.scout(query).then(
      (r) => {
        if (live) {
          setReport(r)
          setError(undefined)
        }
      },
      (e: Error) => live && setError(e.message),
    )
    return () => {
      live = false
    }
  }, [query])

  const pick = (p: LatLon) =>
    setSearch((s) => (placing === 'work' && s.commute ? { ...s, work: p } : { ...s, home: p }))

  const results = report?.results ?? []

  return (
    <section className="scout">
      <div className="panel controls">
        <h2>What fits your week?</h2>
        <label>
          Visits per week: <strong>{search.visitsPerWeek}</strong>
          <input
            type="range"
            min={1}
            max={7}
            value={search.visitsPerWeek}
            onChange={(e) => setSearch({ ...search, visitsPerWeek: Number(e.target.value) })}
          />
        </label>
        <div className="row">
          <label className="check">
            <input
              type="checkbox"
              checked={search.commute}
              onChange={(e) => {
                setSearch({ ...search, commute: e.target.checked })
                setPlacing(e.target.checked ? 'work' : 'home')
              }}
            />
            On my way to work
          </label>
          <select
            aria-label="Travel mode"
            value={search.mode}
            onChange={(e) => setSearch({ ...search, mode: e.target.value as SearchState['mode'] })}
          >
            <option value="DRIVE">Driving</option>
            <option value="WALK">Walking</option>
          </select>
        </div>
        {search.commute ? (
          <>
            <div className="row">
              <button className={placing === 'home' ? 'chip on' : 'chip'} onClick={() => setPlacing('home')}>
                Place home
              </button>
              <button className={placing === 'work' ? 'chip on' : 'chip'} onClick={() => setPlacing('work')}>
                Place work
              </button>
            </div>
            <label>
              Max detour from the route: <strong>{distance(search.corridorM)}</strong>
              <input
                type="range"
                min={300}
                max={5000}
                step={100}
                value={search.corridorM}
                onChange={(e) => setSearch({ ...search, corridorM: Number(e.target.value) })}
              />
            </label>
            {!search.work && <p className="hint">Click the map to place work.</p>}
          </>
        ) : (
          <label>
            Within <strong>{search.radiusKm} km</strong>
            <input
              type="range"
              min={1}
              max={30}
              value={search.radiusKm}
              onChange={(e) => setSearch({ ...search, radiusKm: Number(e.target.value) })}
            />
          </label>
        )}
        <fieldset className="chips">
          <legend>Must have</legend>
          {AMENITIES.map((a) => (
            <button
              key={a}
              className={search.amenities.includes(a) ? 'chip on' : 'chip'}
              aria-pressed={search.amenities.includes(a)}
              onClick={() => setSearch({ ...search, amenities: toggle(search.amenities, a) })}
            >
              {a}
            </button>
          ))}
        </fieldset>
        <p className="hint">Click the map to move {placing === 'work' && search.commute ? 'work' : 'home'}.</p>
      </div>

      <div className="map-wrap panel">
        <MapContainer center={[start.lat, start.lon]} zoom={12} className="map" scrollWheelZoom>
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <ClickToPlace onPick={pick} />
          <CircleMarker center={[search.home.lat, search.home.lon]} radius={9} pathOptions={{ className: 'pin-home' }}>
            <Tooltip permanent direction="top">Home</Tooltip>
          </CircleMarker>
          {search.commute && search.work && (
            <CircleMarker center={[search.work.lat, search.work.lon]} radius={9} pathOptions={{ className: 'pin-work' }}>
              <Tooltip permanent direction="top">Work</Tooltip>
            </CircleMarker>
          )}
          {results.map((r, i) => (
            <CircleMarker
              key={r.gymId}
              center={[r.lat, r.lon]}
              radius={selected === r.gymId ? 11 : 7}
              pathOptions={{ className: i < 3 ? 'pin-gym top' : 'pin-gym' }}
              eventHandlers={{ click: () => setSelected(r.gymId) }}
            >
              <Tooltip direction="top">
                #{i + 1} {r.name} · {r.score}
              </Tooltip>
            </CircleMarker>
          ))}
        </MapContainer>
      </div>

      <div className="panel results">
        <h2>
          {results.length} gyms ranked
          {report?.travelEstimated && <span className="badge">travel times estimated</span>}
        </h2>
        {error && <p className="notice">{error}</p>}
        {results.length === 0 && !error && <p className="muted">No gyms in range. Widen the search.</p>}
        <ol className="ranked">
          {results.map((r) => (
            <li
              key={r.gymId}
              className={selected === r.gymId ? 'result selected' : 'result'}
              onClick={() => setSelected(r.gymId)}
            >
              <div className="result-head">
                <strong>{r.name}</strong>
                <span className="score" title="Score out of 100">
                  {r.score}
                </span>
              </div>
              <div className="bar" aria-hidden="true">
                <span className="seg price" style={{ width: `${r.pricePoints}%` }} />
                <span className="seg travel" style={{ width: `${r.travelPoints}%` }} />
                <span className="seg amenity" style={{ width: `${r.amenityPoints}%` }} />
              </div>
              <div className="facts">
                <span>{r.perVisitMinor !== undefined ? `${money(r.perVisitMinor, r.currency)} / visit` : 'no price yet'}</span>
                <span>{minutes(r.travelMinutes)}</span>
                <span>{distance(r.distanceMeters)}</span>
              </div>
              {selected === r.gymId && (
                <ul className="reasons">
                  {r.reasons.map((reason) => (
                    <li key={reason}>{reason}</li>
                  ))}
                  {r.amenities.length > 0 && <li className="muted">Amenities: {r.amenities.join(', ')}</li>}
                </ul>
              )}
            </li>
          ))}
        </ol>
        <p className="legend small">
          <span className="seg price" /> price per visit <span className="seg travel" /> travel{' '}
          <span className="seg amenity" /> amenities
        </p>
      </div>
    </section>
  )
}
