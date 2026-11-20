package dev.gymscout.training.domain;

import java.time.LocalDate;
import java.util.UUID;

/** A lift beat the lifter's previous best. */
public record PersonalRecord(
    UUID sessionId, LocalDate on, String exercise, Kind kind, double value, double previous) {

  /** Heaviest load lifted, or best estimated one-rep max (a lighter set with more reps can win). */
  public enum Kind {
    HEAVIEST_WEIGHT,
    ESTIMATED_ONE_REP_MAX
  }
}
