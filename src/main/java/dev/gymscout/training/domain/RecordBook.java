package dev.gymscout.training.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Replays sets in the order they were done and notes every personal record. A record needs a
 * previous best: the first time an exercise is logged sets the baseline, it is not a record. Within
 * one session only the best set per exercise counts, so five heavier warm-up-to-top sets make one
 * record, not five.
 */
public final class RecordBook {

  /** Ignores float noise like 100.0000001 vs 100. */
  static final double EPSILON = 1e-6;

  private final Map<String, ExerciseBest> bests = new LinkedHashMap<>();
  private final List<PersonalRecord> records = new ArrayList<>();

  /** {@code sets} must be in chronological order, grouped by session. */
  public static RecordBook replay(List<LoggedSet> sets) {
    var book = new RecordBook();
    var session = new ArrayList<LoggedSet>();
    for (LoggedSet set : sets) {
      if (!session.isEmpty() && !session.getFirst().sessionId().equals(set.sessionId())) {
        book.applySession(session);
        session.clear();
      }
      session.add(set);
    }
    if (!session.isEmpty()) {
      book.applySession(session);
    }
    return book;
  }

  private void applySession(List<LoggedSet> session) {
    UUID sessionId = session.getFirst().sessionId();
    var byExercise = new LinkedHashMap<String, List<WorkoutSet>>();
    session.forEach(
        s -> byExercise.computeIfAbsent(s.set().key(), k -> new ArrayList<>()).add(s.set()));
    byExercise.forEach((key, sets) -> {
      String name = sets.getFirst().exercise();
      double heaviest = sets.stream().mapToDouble(s -> s.weightKg().doubleValue()).max().orElse(0);
      double oneRep = sets.stream().mapToDouble(WorkoutSet::estimatedOneRepMax).max().orElse(0);
      ExerciseBest before = bests.get(key);
      if (before == null) {
        bests.put(key, new ExerciseBest(name, heaviest, oneRep, 1));
        return;
      }
      var on = session.getFirst().performedOn();
      if (heaviest > before.heaviestKg() + EPSILON) {
        records.add(
            new PersonalRecord(
                sessionId,
                on,
                name,
                PersonalRecord.Kind.HEAVIEST_WEIGHT,
                heaviest,
                before.heaviestKg()));
      }
      if (oneRep > before.bestOneRepMax() + EPSILON) {
        records.add(
            new PersonalRecord(
                sessionId,
                on,
                name,
                PersonalRecord.Kind.ESTIMATED_ONE_REP_MAX,
                round(oneRep),
                round(before.bestOneRepMax())));
      }
      bests.put(
          key,
          new ExerciseBest(
              name,
              Math.max(heaviest, before.heaviestKg()),
              Math.max(oneRep, before.bestOneRepMax()),
              before.sessions() + 1));
    });
  }

  public List<PersonalRecord> records() {
    return List.copyOf(records);
  }

  public List<PersonalRecord> recordsFor(UUID sessionId) {
    return records.stream().filter(r -> r.sessionId().equals(sessionId)).toList();
  }

  public List<ExerciseBest> bests() {
    return bests.values().stream()
        .map(b ->
            new ExerciseBest(b.exercise(), b.heaviestKg(), round(b.bestOneRepMax()), b.sessions()))
        .sorted(Comparator.comparing(ExerciseBest::exercise))
        .toList();
  }

  private static double round(double v) {
    return Math.round(v * 10) / 10.0;
  }
}
