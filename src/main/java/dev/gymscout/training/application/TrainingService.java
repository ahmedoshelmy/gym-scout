package dev.gymscout.training.application;

import dev.gymscout.places.GymDirectory;
import dev.gymscout.shared.NotFoundException;
import dev.gymscout.training.SessionLogged;
import dev.gymscout.training.domain.RecordBook;
import dev.gymscout.training.domain.WeeklyVolume;
import dev.gymscout.training.domain.WorkoutPlan;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logs sessions and derives progress. Records are recomputed by replaying history rather than
 * stored, so editing or back-dating a session can never leave a stale record behind.
 */
@Service
public class TrainingService {

  private final TrainingStore store;
  private final GymDirectory gyms;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  public TrainingService(
      TrainingStore store, GymDirectory gyms, ApplicationEventPublisher events, Clock clock) {
    this.store = store;
    this.gyms = gyms;
    this.events = events;
    this.clock = clock;
  }

  @Transactional
  public LoggedSession log(LogSession cmd) {
    LocalDate today = LocalDate.now(clock);
    LocalDate on = cmd.performedOn() == null ? today : cmd.performedOn();
    if (on.isAfter(today)) {
      throw new IllegalArgumentException("a session cannot be in the future");
    }
    if (cmd.gymId() != null && gyms.find(cmd.gymId()).isEmpty()) {
      throw new NotFoundException("No gym " + cmd.gymId());
    }
    if (cmd.planId() != null && store.plan(cmd.planId()).isEmpty()) {
      throw new NotFoundException("No plan " + cmd.planId());
    }
    UUID id = UUID.randomUUID();
    store.saveSession(id, on, cmd.gymId(), cmd.planId(), cmd.notes(), cmd.sets());
    var records = RecordBook.replay(store.allSetsChronological()).recordsFor(id);
    events.publishEvent(new SessionLogged(id, cmd.gymId(), on));
    return new LoggedSession(id, on, records);
  }

  @Transactional(readOnly = true)
  public Progress progress(int weeks) {
    var sets = store.allSetsChronological();
    var book = RecordBook.replay(sets);
    var records = book.records();
    var recent = records.reversed().stream().limit(20).toList();
    return new Progress(
        WeeklyVolume.lastWeeks(sets, LocalDate.now(clock), weeks), book.bests(), recent);
  }

  public List<SessionSummary> recentSessions(int limit) {
    return store.recentSessions(Math.clamp(limit, 1, 100));
  }

  @Transactional
  public WorkoutPlan createPlan(WorkoutPlan draft) {
    var plan = new WorkoutPlan(UUID.randomUUID(), draft.name(), draft.items());
    store.savePlan(plan);
    return plan;
  }

  public List<WorkoutPlan> plans() {
    return store.plans();
  }
}
