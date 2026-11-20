package dev.gymscout.memberships.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** The honest price of a gym: what you paid divided by how often you actually went. */
public final class CostPerVisit {

  private CostPerVisit() {}

  public static MembershipStats of(Membership m, int visits, LocalDate today) {
    if (visits < 0) {
      throw new IllegalArgumentException("visits cannot be negative");
    }
    long total = m.totalDays();
    long elapsed =
        today.isBefore(m.startsOn())
            ? 0
            : Math.min(total, ChronoUnit.DAYS.between(m.startsOn(), today) + 1);
    long costPerVisit = Math.round((double) m.priceMinor() / Math.max(visits, 1));
    double pace = elapsed == 0 ? 0 : (double) visits / elapsed;
    double projected = elapsed == 0 ? 0 : pace * total;
    long projectedCost = Math.round(m.priceMinor() / Math.max(projected, 1.0));
    return new MembershipStats(
        visits,
        elapsed,
        total,
        costPerVisit,
        Math.round(projected * 10) / 10.0,
        projectedCost,
        Math.round(pace * 7 * 10) / 10.0);
  }
}
