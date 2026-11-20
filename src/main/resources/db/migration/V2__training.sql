create table workout_plans (
  id         uuid primary key,
  name       text        not null,
  created_at timestamptz not null default now()
);

create table workout_plan_items (
  plan_id     uuid not null references workout_plans (id) on delete cascade,
  position    int  not null,
  exercise    text not null,
  target_sets int  not null check (target_sets > 0),
  target_reps int  not null check (target_reps > 0),
  primary key (plan_id, position)
);

create table workout_sessions (
  id           uuid primary key,
  performed_on date not null,
  gym_id       uuid,
  plan_id      uuid references workout_plans (id) on delete set null,
  notes        text,
  created_at   timestamptz not null default now()
);

create index workout_sessions_performed_on on workout_sessions (performed_on);

create table workout_sets (
  session_id uuid          not null references workout_sessions (id) on delete cascade,
  position   int           not null,
  exercise   text          not null,
  weight_kg  numeric(6, 2) not null check (weight_kg >= 0),
  reps       int           not null check (reps > 0),
  primary key (session_id, position)
);

create index workout_sets_exercise on workout_sets (exercise);
