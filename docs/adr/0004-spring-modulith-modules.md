# 4. A modular monolith with Spring Modulith

Date: 2026-11-21 · Status: accepted

## Context

Gym Scout has four bounded contexts: `places` (gyms, plans, amenities), `scouting` (search and explainable
scoring), `training` (sessions, sets, records, volume) and `memberships` (current gym, visits, cost per visit).
Microservices would add network hops, deployments and distributed failure modes for an app with one user and one
database. A plain package-by-layer monolith, on the other hand, lets every class reach into every other.

## Decision

One Spring Boot application, one top-level package per context, verified by Spring Modulith:

- Each module exposes a small API in its root package (services, events, view records); `domain`,
  `application` and `adapter` sub-packages are internal.
- `ModularityTests` calls `ApplicationModules.of(...).verify()`, which fails the build on cycles or on access to
  another module's internals. ArchUnit (`LayeringTest`) adds the hexagonal rules inside a module: the domain has no
  Spring or adapter dependencies, the application layer never depends on adapters.
- `geo` and `outbound` are shared kernels declared `@ApplicationModule(type = OPEN)`: helpers with no business
  state, usable by every module.
- Cross-module side effects use events: `training` publishes `SessionLogged`; `memberships` listens with
  `@ApplicationModuleListener` and records a visit when the session was at the member gym. The event publication
  registry stores the event in the same transaction (a transactional outbox) and republishes outstanding events
  on restart; the visit insert is idempotent (`ON CONFLICT (session_id) DO NOTHING`), so at-least-once delivery is
  safe.

## Consequences

- One deployable and one database, with boundaries that are checked, not just hoped for.
- A module could be extracted into its own service later: its API and events are already the seam.
- Tests can bootstrap a single module (`@ApplicationModuleTest`) when needed.
