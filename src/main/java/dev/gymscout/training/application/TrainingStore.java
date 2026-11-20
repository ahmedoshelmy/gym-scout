package dev.gymscout.training.application;

import dev.gymscout.training.domain.LoggedSet;
import dev.gymscout.training.domain.WorkoutPlan;
import dev.gymscout.training.domain.WorkoutSet;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Storage port for plans, sessions and sets. */
public interface TrainingStore {

  void saveSession(
      UUID id, LocalDate performedOn, UUID gymId, UUID planId, String notes, List<WorkoutSet> sets);

  /** Every set ever logged, ordered by date, then by when the session was logged, then position. */
  List<LoggedSet> allSetsChronological();

  List<SessionSummary> recentSessions(int limit);

  void savePlan(WorkoutPlan plan);

  List<WorkoutPlan> plans();

  Optional<WorkoutPlan> plan(UUID id);
}
