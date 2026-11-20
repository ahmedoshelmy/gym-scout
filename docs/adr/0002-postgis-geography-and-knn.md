# 2. PostGIS geography columns and KNN ordering instead of app-side haversine

Date: 2026-11-21 · Status: accepted

## Context

The core query is "gyms within R metres of a point, nearest first", plus "gyms within C metres of the line from
home to work". The naive approach loads every gym and computes haversine distances in Java. That is fine for a
few hundred rows, but it scans the whole table, cannot use an index, and duplicates geometry code (corridors,
line distance) that a spatial database already has.

Options:

1. **App-side haversine** over all rows (or a bounding-box prefilter in SQL). Simple, portable, O(n) per search.
2. **PostGIS `geometry(Point,4326)`** – fast, but distances are in degrees unless every query casts or
   transforms to a projected CRS.
3. **PostGIS `geography(Point,4326)`** – distances and radii in metres on the spheroid, GiST-indexable.

## Decision

Store gym locations as `geography(Point,4326)` with a GiST index (`gyms_location_gist`). Queries:

- Radius filter: `ST_DWithin(location, :point, :radius)` – index-assisted, metres.
- Ordering: `ORDER BY location <-> :point` – the KNN operator walks the GiST index nearest-first, so
  `LIMIT n` stops early instead of sorting everything.
- Reported distance: `ST_Distance(location, :point)` in metres.
- Commute: build `ST_MakeLine(home, work)::geography` and filter with `ST_DWithin(location, line, :corridor)`.

Haversine stays in the `geo` shared kernel only for the offline straight-line travel estimate and for property
tests.

## Consequences

- Searches use the index and scale to the whole OpenStreetMap gym set for a country.
- The database must be PostGIS (`postgis/postgis` image in compose and Testcontainers); plain H2 or vanilla
  Postgres cannot run the tests. The integration test asserts the KNN order and the radius filter against a real
  PostGIS container.
- Coordinates are always (lon, lat) in SQL and (lat, lon) in the API; the repository is the only place that
  converts.
