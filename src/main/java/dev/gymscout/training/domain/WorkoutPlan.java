package dev.gymscout.training.domain;

import java.util.List;
import java.util.UUID;

/** A named routine, for example "Push day". */
public record WorkoutPlan(UUID id, String name, List<PlanItem> items) {

  public WorkoutPlan {
    if (name == null || name.isBlank() || name.length() > 60) {
      throw new IllegalArgumentException("plan name must be 1-60 characters");
    }
    name = name.strip();
    if (items.isEmpty() || items.size() > 30) {
      throw new IllegalArgumentException("a plan needs 1-30 exercises");
    }
    items = List.copyOf(items);
  }
}
