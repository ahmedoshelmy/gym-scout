package dev.gymscout.scouting.domain;

/**
 * Travel from one point to another. {@code estimated} is true when it comes from the straight-line
 * fallback rather than a road router.
 */
public record TravelEstimate(double meters, double minutes, boolean estimated) {

  /** Roads are rarely straight: crow-flies distance times this factor approximates road length. */
  public static final double DETOUR_FACTOR = 1.35;

  public TravelEstimate {
    if (meters < 0 || minutes < 0) {
      throw new IllegalArgumentException("travel cannot be negative");
    }
  }

  /** The fallback: crow-flies distance, a detour factor and an average speed for the mode. */
  public static TravelEstimate straightLine(double crowFliesMeters, TravelMode mode) {
    double road = crowFliesMeters * DETOUR_FACTOR;
    double minutes = road / 1000 / mode.straightLineKmh() * 60;
    return new TravelEstimate(road, minutes, true);
  }
}
