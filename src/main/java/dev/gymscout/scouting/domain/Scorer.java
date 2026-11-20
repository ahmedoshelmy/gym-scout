package dev.gymscout.scouting.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Ranks candidates relative to the best one in the set: the cheapest per visit gets all price
 * points, the quickest to reach gets all travel points, and the others get a share in proportion.
 * Relative scoring keeps the numbers meaningful whether the user searches 1 km or 20 km.
 */
public final class Scorer {

  /** Softens travel ratios so 1 min versus 2 min is not treated as "twice as far". */
  static final double TRAVEL_SOFTENING_MINUTES = 5;

  private Scorer() {}

  public static List<ScoredGym> rank(
      List<ScoutCandidate> candidates, Set<String> wanted, ScoringWeights weights) {
    long cheapest =
        candidates.stream()
            .flatMap(c -> c.price().stream())
            .mapToLong(PlanPrice::perVisitMinor)
            .min()
            .orElse(0);
    double quickest = candidates.stream().mapToDouble(c -> c.travel().minutes()).min().orElse(0);
    return candidates.stream()
        .map(c -> score(c, cheapest, quickest, wanted, weights))
        .sorted(
            Comparator.comparingDouble(ScoredGym::score)
                .reversed()
                .thenComparingDouble(s -> s.candidate().travel().minutes())
                .thenComparing(s -> s.candidate().name()))
        .toList();
  }

  static ScoredGym score(
      ScoutCandidate c, long cheapest, double quickest, Set<String> wanted, ScoringWeights w) {
    var reasons = new ArrayList<String>();
    double scale = 100 / w.total();

    double price = 0;
    if (c.price().isPresent()) {
      PlanPrice p = c.price().get();
      price = p.perVisitMinor() == 0 ? 1 : Math.min(1, (double) cheapest / p.perVisitMinor());
      reasons.add(priceReason(p, cheapest));
    } else {
      reasons.add("No price known yet: add a plan to compare it");
    }

    double travel =
        (quickest + TRAVEL_SOFTENING_MINUTES) / (c.travel().minutes() + TRAVEL_SOFTENING_MINUTES);
    reasons.add(travelReason(c.travel()));

    double amenity = 1;
    if (!wanted.isEmpty()) {
      var have = new TreeSet<>(wanted);
      have.retainAll(c.amenities());
      var missing = new TreeSet<>(wanted);
      missing.removeAll(c.amenities());
      amenity = (double) have.size() / wanted.size();
      reasons.add(amenityReason(have, missing));
    }

    double pricePoints = round(price * w.price() * scale);
    double travelPoints = round(travel * w.travel() * scale);
    double amenityPoints = round(amenity * w.amenities() * scale);
    return new ScoredGym(
        c,
        round(pricePoints + travelPoints + amenityPoints),
        pricePoints,
        travelPoints,
        amenityPoints,
        reasons);
  }

  private static String priceReason(PlanPrice p, long cheapest) {
    String money = money(p.perVisitMinor(), p.currency());
    if (p.perVisitMinor() <= cheapest) {
      return "Cheapest per visit: %s with the %s".formatted(money, p.planName());
    }
    long above = Math.round(100.0 * (p.perVisitMinor() - cheapest) / Math.max(cheapest, 1));
    return "%s per visit with the %s, %d%% above the cheapest"
        .formatted(money, p.planName(), above);
  }

  private static String travelReason(TravelEstimate t) {
    String how = t.estimated() ? " (straight-line estimate)" : "";
    return "%.0f min away, %.1f km%s".formatted(t.minutes(), t.meters() / 1000, how);
  }

  private static String amenityReason(Set<String> have, Set<String> missing) {
    if (missing.isEmpty()) {
      return "Has everything you asked for: " + String.join(", ", have);
    }
    if (have.isEmpty()) {
      return "Has none of: " + String.join(", ", missing);
    }
    return "Has %s; missing %s".formatted(String.join(", ", have), String.join(", ", missing));
  }

  static String money(long minor, String currency) {
    return String.format(Locale.ROOT, "%s %d.%02d", currency, minor / 100, minor % 100);
  }

  private static double round(double value) {
    return Math.round(value * 10) / 10.0;
  }
}
