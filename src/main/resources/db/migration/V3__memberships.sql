create table memberships (
  id          uuid primary key,
  gym_id      uuid    not null,
  plan_name   text    not null,
  price_minor bigint  not null check (price_minor >= 0),
  currency    char(3) not null,
  starts_on   date    not null,
  ends_on     date    not null,
  check (ends_on >= starts_on)
);

create table membership_visits (
  id            uuid primary key,
  membership_id uuid not null references memberships (id) on delete cascade,
  visited_on    date not null,
  session_id    uuid unique,
  source        text not null check (source in ('manual', 'session'))
);

create index membership_visits_membership on membership_visits (membership_id, visited_on);
