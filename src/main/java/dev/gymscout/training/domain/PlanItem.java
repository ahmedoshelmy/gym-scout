package dev.gymscout.training.domain;

/** One line of a workout plan: an exercise with target sets and reps. */
public record PlanItem(String exercise, int targetSets, int targetReps) {

  public PlanItem {
    if (exercise == null || exercise.isBlank()) {
      throw new IllegalArgumentException("exercise is required");
    }
    exercise = exercise.strip();
    if (targetSets < 1 || targetSets > 20 || targetReps < 1 || targetReps > 100) {
      throw new IllegalArgumentException("targets must be 1-20 sets of 1-100 reps");
    }
  }
}
