package dev.gymscout.scouting.domain;

/** Points available for each factor; they add up to 100 so the score reads as a percentage. */
public record ScoringWeights(double price, double travel, double amenities) {

  public static final ScoringWeights DEFAULT = new ScoringWeights(50, 35, 15);

  public ScoringWeights {
    if (price < 0 || travel < 0 || amenities < 0 || price + travel + amenities <= 0) {
      throw new IllegalArgumentException("weights must be non-negative and not all zero");
    }
  }

  public double total() {
    return price + travel + amenities;
  }
}
