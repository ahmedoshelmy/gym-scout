package dev.gymscout.training.adapter.web;

import dev.gymscout.training.application.LogSession;
import dev.gymscout.training.application.LoggedSession;
import dev.gymscout.training.application.Progress;
import dev.gymscout.training.application.SessionSummary;
import dev.gymscout.training.application.TrainingService;
import dev.gymscout.training.domain.PlanItem;
import dev.gymscout.training.domain.WorkoutPlan;
import dev.gymscout.training.domain.WorkoutSet;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/training")
@Tag(name = "Training", description = "Workouts, personal records and weekly volume")
class TrainingController {

  private final TrainingService training;

  TrainingController(TrainingService training) {
    this.training = training;
  }

  @PostMapping("/sessions")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Log a workout; the response lists any personal records it set")
  LoggedSession log(@Valid @RequestBody SessionRequest body) {
    return training.log(
        new LogSession(
            body.performedOn(),
            body.gymId(),
            body.planId(),
            body.notes(),
            body.sets().stream()
                .map(s -> new WorkoutSet(s.exercise(), s.weightKg(), s.reps()))
                .toList()));
  }

  @GetMapping("/sessions")
  List<SessionSummary> sessions(@RequestParam(defaultValue = "20") int limit) {
    return training.recentSessions(limit);
  }

  @GetMapping("/progress")
  @Operation(summary = "Weekly volume, bests per exercise and recent personal records")
  Progress progress(@RequestParam(defaultValue = "8") int weeks) {
    return training.progress(weeks);
  }

  @PostMapping("/plans")
  @ResponseStatus(HttpStatus.CREATED)
  WorkoutPlan createPlan(@Valid @RequestBody PlanRequest body) {
    return training.createPlan(
        new WorkoutPlan(
            null,
            body.name(),
            body.items().stream()
                .map(i -> new PlanItem(i.exercise(), i.targetSets(), i.targetReps()))
                .toList()));
  }

  @GetMapping("/plans")
  List<WorkoutPlan> plans() {
    return training.plans();
  }

  record SessionRequest(
      LocalDate performedOn,
      UUID gymId,
      UUID planId,
      String notes,
      @NotEmpty List<@Valid SetRequest> sets) {}

  record SetRequest(@NotBlank String exercise, @NotNull BigDecimal weightKg, int reps) {}

  record PlanRequest(@NotBlank String name, @NotEmpty List<@Valid ItemRequest> items) {}

  record ItemRequest(@NotBlank String exercise, int targetSets, int targetReps) {}
}
