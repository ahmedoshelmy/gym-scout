package dev.gymscout.training.application;

import dev.gymscout.training.domain.WorkoutSet;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Command: log one workout. {@code performedOn} null means today. */
public record LogSession(
    LocalDate performedOn, UUID gymId, UUID planId, String notes, List<WorkoutSet> sets) {

  public LogSession {
    if (sets == null || sets.isEmpty() || sets.size() > 200) {
      throw new IllegalArgumentException("a session needs 1-200 sets");
    }
    sets = List.copyOf(sets);
    if (notes != null && notes.length() > 500) {
      throw new IllegalArgumentException("notes must be at most 500 characters");
    }
  }
}
