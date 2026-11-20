package dev.gymscout.scouting.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Price per visit: a plan's price divided by the visits the user will really make in it. An
 * unlimited month costs the same whether you go twice or twenty times, so the user's own rhythm
 * decides which plan wins; a pack caps the visits that count.
 */
public final class PerVisitPrice {

  private PerVisitPrice() {}

  public static Optional<PlanPrice> best(List<PlanOption> plans, double visitsPerWeek) {
    if (visitsPerWeek <= 0 || visitsPerWeek > 14) {
      throw new IllegalArgumentException("visits per week must be between 0 and 14");
    }
    return plans.stream()
        .map(plan -> price(plan, visitsPerWeek))
        .min(Comparator.comparingLong(PlanPrice::perVisitMinor).thenComparing(PlanPrice::planName));
  }

  static PlanPrice price(PlanOption plan, double visitsPerWeek) {
    double expected = visitsPerWeek * plan.durationDays() / 7.0;
    double used =
        plan.visitsIncluded() == null ? expected : Math.min(expected, plan.visitsIncluded());
    long perVisit = Math.round(plan.priceMinor() / Math.max(used, 1.0));
    return new PlanPrice(plan.name(), perVisit, plan.currency(), used);
  }
}
