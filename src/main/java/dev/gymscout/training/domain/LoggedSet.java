package dev.gymscout.training.domain;

import java.time.LocalDate;
import java.util.UUID;

/** A stored set with the session it belongs to, for replays. */
public record LoggedSet(UUID sessionId, LocalDate performedOn, WorkoutSet set) {}
