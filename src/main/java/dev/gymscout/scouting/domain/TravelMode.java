package dev.gymscout.scouting.domain;

/** How the user gets to the gym. */
public enum TravelMode {
  DRIVE(25.0),
  WALK(4.8);

  private final double straightLineKmh;

  TravelMode(double straightLineKmh) {
    this.straightLineKmh = straightLineKmh;
  }

  /** Average door-to-door speed used when no router answers (city traffic for driving). */
  public double straightLineKmh() {
    return straightLineKmh;
  }
}
