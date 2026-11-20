package dev.gymscout.training.domain;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

/** One set: an exercise, the load in kilograms (0 for bodyweight) and the reps done. */
public record WorkoutSet(String exercise, BigDecimal weightKg, int reps) {

  public WorkoutSet {
    Objects.requireNonNull(exercise, "exercise");
    exercise = exercise.strip().replaceAll("\\s+", " ");
    if (exercise.isEmpty() || exercise.length() > 60) {
      throw new IllegalArgumentException("exercise must be 1-60 characters");
    }
    if (weightKg == null
        || weightKg.signum() < 0
        || weightKg.compareTo(BigDecimal.valueOf(1000)) > 0) {
      throw new IllegalArgumentException("weight must be between 0 and 1000 kg");
    }
    if (reps < 1 || reps > 100) {
      throw new IllegalArgumentException("reps must be between 1 and 100");
    }
  }

  /** Case-insensitive identity, so "Bench press" and "bench press" are the same lift. */
  public String key() {
    return exercise.toLowerCase(Locale.ROOT);
  }

  public double volumeKg() {
    return weightKg.doubleValue() * reps;
  }

  /** Epley's estimate of the heaviest single rep: {@code w * (1 + reps / 30)}. */
  public double estimatedOneRepMax() {
    double w = weightKg.doubleValue();
    return reps == 1 ? w : w * (1 + reps / 30.0);
  }
}
