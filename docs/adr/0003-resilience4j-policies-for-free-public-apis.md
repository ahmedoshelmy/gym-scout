# 3. Resilience4j policies for free public APIs

Date: 2026-11-21 · Status: accepted

## Context

Live mode uses two free, community-run services: the OpenStreetMap Overpass API (gyms tagged
`leisure=fitness_centre`) and the public OSRM demo server (drive/walk times). Both have usage policies: identify
yourself, keep to about one request per second, back off on `429 Too Many Requests`, and do not hammer a server
that is struggling. Both are also sometimes slow or down, and the app must still answer.

## Decision

Every outbound call goes through one `PoliteCaller` per upstream built from Resilience4j, in this order
(outermost first), so every retry goes back through the breaker, the limiter and the bulkhead:

1. **Retry** (3 attempts, exponential backoff from 500 ms up to 10 s) – only on 429, 5xx and I/O errors, never
   on other 4xx.
2. **Circuit breaker** (window 10, minimum 4 calls, opens at 50 % failures for 30 s) – stop calling a server
   that is failing; fail fast instead.
3. **Rate limiter** (1 permit per second, wait up to 5 s) – applied to each attempt, so retries are polite too.
4. **Bulkhead** (2 concurrent calls) – a slow upstream cannot tie up request threads.

Plus:

- A fixed `User-Agent: gym-scout-demo` header and a 10 s read timeout.
- **Fallbacks**: if OSRM fails or the breaker is open, travel times fall back to the straight-line estimate
  (crow-flies distance × 1.35 detour factor at an average speed per mode) and the result is flagged
  `travelEstimated` so the UI says so. Overpass is only called by an explicit import; when it fails, search
  keeps working on the gyms already stored.
- **Caching**: OSRM answers are cached in Caffeine (20 000 entries, TTL) per rounded origin/destination pair, so
  panning the map or repeating a search does not call the server again.
- **Metrics**: retry, breaker, limiter, bulkhead and cache statistics are exported through Micrometer
  (`/actuator/prometheus`).
- **Off by default**: the synthetic gym source and straight-line router are the defaults; tests use WireMock
  with hand-written fixtures and never touch the network.

## Consequences

- Behaviour under failure is tested: WireMock tests cover retry on 429/503, the breaker opening, and the
  straight-line fallback.
- Imports of a large area are slow by design (one request per second); that is the price of using free
  infrastructure fairly.
- All policy numbers live in `OutboundProperties` and can be tuned per environment.
