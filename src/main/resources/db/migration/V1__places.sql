-- Gyms live as PostGIS geography points: distances come back in metres on the WGS84 spheroid,
-- and the GiST index serves both radius filters (ST_DWithin) and nearest-first ordering (<->).
create extension if not exists postgis;

create table gyms (
  id            uuid primary key,
  name          text        not null,
  source        text        not null check (source in ('seed', 'osm', 'manual')),
  external_ref  text        unique,
  location      geography(Point, 4326) not null,
  address       text,
  amenities     text[]      not null default '{}',
  opening_hours text,
  created_at    timestamptz not null default now()
);

create index gyms_location_gist on gyms using gist (location);

create table membership_plans (
  id              uuid primary key,
  gym_id          uuid    not null references gyms (id) on delete cascade,
  name            text    not null,
  price_minor     bigint  not null check (price_minor >= 0),
  currency        char(3) not null,
  duration_days   int     not null check (duration_days > 0),
  visits_included int     check (visits_included is null or visits_included > 0)
);

create index membership_plans_gym on membership_plans (gym_id);
