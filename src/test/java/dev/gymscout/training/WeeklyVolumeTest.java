package dev.gymscout.training;

import static org.assertj.core.api.Assertions.assertThat;

import dev.gymscout.training.domain.LoggedSet;
import dev.gymscout.training.domain.WeekVolume;
import dev.gymscout.training.domain.WeeklyVolume;
import dev.gymscout.training.domain.WorkoutSet;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WeeklyVolumeTest {

  private static LoggedSet set(LocalDate on, int kg, int reps) {
    return new LoggedSet(
        UUID.randomUUID(), on, new WorkoutSet("Squat", BigDecimal.valueOf(kg), reps));
  }

  @Test
  void groupsByIsoWeekAndKeepsEmptyWeeks() {
    LocalDate thursday = LocalDate.of(2026, 3, 12);
    var weeks =
        WeeklyVolume.lastWeeks(
            List.of(
                set(LocalDate.of(2026, 3, 9), 100, 5), // Monday this week
                set(LocalDate.of(2026, 3, 12), 50, 10),
                set(LocalDate.of(2026, 2, 27), 80, 5)), // two weeks back
            thursday,
            3);
    assertThat(weeks)
        .containsExactly(
            new WeekVolume(LocalDate.of(2026, 2, 23), 400.0, 1),
            new WeekVolume(LocalDate.of(2026, 3, 2), 0.0, 0),
            new WeekVolume(LocalDate.of(2026, 3, 9), 1000.0, 2));
  }

  @Test
  void sundayBelongsToTheWeekThatStartedOnMonday() {
    assertThat(WeeklyVolume.mondayOf(LocalDate.of(2026, 3, 15)))
        .isEqualTo(LocalDate.of(2026, 3, 9));
  }
}
