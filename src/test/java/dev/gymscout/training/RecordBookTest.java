package dev.gymscout.training;

import static org.assertj.core.api.Assertions.assertThat;

import dev.gymscout.training.domain.LoggedSet;
import dev.gymscout.training.domain.PersonalRecord.Kind;
import dev.gymscout.training.domain.RecordBook;
import dev.gymscout.training.domain.WorkoutSet;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecordBookTest {

  private final List<LoggedSet> history = new ArrayList<>();
  private LocalDate day = LocalDate.of(2026, 3, 2);

  private UUID session(Object... exerciseKgReps) {
    UUID id = UUID.randomUUID();
    for (int i = 0; i < exerciseKgReps.length; i += 3) {
      history.add(
          new LoggedSet(
              id,
              day,
              new WorkoutSet(
                  (String) exerciseKgReps[i],
                  BigDecimal.valueOf(((Number) exerciseKgReps[i + 1]).doubleValue()),
                  (Integer) exerciseKgReps[i + 2])));
    }
    day = day.plusDays(2);
    return id;
  }

  @Test
  void firstTimeIsABaselineNotARecord() {
    session("Squat", 80, 5);
    assertThat(RecordBook.replay(history).records()).isEmpty();
  }

  @Test
  void heavierWeightIsARecordOfBothKinds() {
    session("Squat", 80, 5);
    UUID second = session("Squat", 85, 5);
    var records = RecordBook.replay(history).recordsFor(second);
    assertThat(records)
        .extracting(r -> r.kind())
        .containsExactly(Kind.HEAVIEST_WEIGHT, Kind.ESTIMATED_ONE_REP_MAX);
    assertThat(records.getFirst().value()).isEqualTo(85.0);
    assertThat(records.getFirst().previous()).isEqualTo(80.0);
  }

  @Test
  void moreRepsAtTheSameWeightBeatsTheOneRepMaxOnly() {
    session("Bench press", 60, 5);
    UUID second = session("bench  press", 60, 8);
    var records = RecordBook.replay(history).recordsFor(second);
    assertThat(records)
        .singleElement()
        .satisfies(r -> {
          assertThat(r.kind()).isEqualTo(Kind.ESTIMATED_ONE_REP_MAX);
          assertThat(r.value()).isEqualTo(76.0); // 60 * (1 + 8/30)
          assertThat(r.previous()).isEqualTo(70.0); // 60 * (1 + 5/30)
        });
  }

  @Test
  void onlyTheBestSetPerSessionCounts() {
    session("Deadlift", 100, 5);
    UUID second = session("Deadlift", 105, 5, "Deadlift", 110, 3, "Deadlift", 115, 1);
    var records = RecordBook.replay(history).recordsFor(second);
    assertThat(records)
        .filteredOn(r -> r.kind() == Kind.HEAVIEST_WEIGHT)
        .singleElement()
        .satisfies(r -> assertThat(r.value()).isEqualTo(115.0));
  }

  @Test
  void bestsTrackEveryExercise() {
    session("Squat", 80, 5, "Row", 50, 10);
    session("Squat", 70, 10);
    var bests = RecordBook.replay(history).bests();
    assertThat(bests).extracting(b -> b.exercise()).containsExactly("Row", "Squat");
    assertThat(bests.getLast().heaviestKg()).isEqualTo(80.0);
    assertThat(bests.getLast().sessions()).isEqualTo(2);
  }
}
