package dev.gymscout.training.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/** Weekly training volume for the last N weeks, oldest first, with empty weeks as zero. */
public final class WeeklyVolume {

  private WeeklyVolume() {}

  public static List<WeekVolume> lastWeeks(List<LoggedSet> sets, LocalDate today, int weeks) {
    if (weeks < 1 || weeks > 104) {
      throw new IllegalArgumentException("weeks must be between 1 and 104");
    }
    LocalDate thisWeek = mondayOf(today);
    var result = new ArrayList<WeekVolume>(weeks);
    for (int i = weeks - 1; i >= 0; i--) {
      LocalDate start = thisWeek.minusWeeks(i);
      LocalDate end = start.plusDays(7);
      double volume = 0;
      int count = 0;
      for (LoggedSet s : sets) {
        if (!s.performedOn().isBefore(start) && s.performedOn().isBefore(end)) {
          volume += s.set().volumeKg();
          count++;
        }
      }
      result.add(new WeekVolume(start, Math.round(volume * 10) / 10.0, count));
    }
    return result;
  }

  public static LocalDate mondayOf(LocalDate day) {
    return day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
  }
}
