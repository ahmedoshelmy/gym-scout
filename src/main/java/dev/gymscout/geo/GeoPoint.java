package dev.gymscout.geo;

/** A WGS84 coordinate. Latitude first, like people say it; PostGIS wants longitude first. */
public record GeoPoint(double lat, double lon) {

  public GeoPoint {
    if (!Double.isFinite(lat) || lat < -90 || lat > 90) {
      throw new IllegalArgumentException("latitude must be between -90 and 90");
    }
    if (!Double.isFinite(lon) || lon < -180 || lon > 180) {
      throw new IllegalArgumentException("longitude must be between -180 and 180");
    }
  }

  /** Rounds to 5 decimals (about 1 m), used as a stable cache key. */
  public GeoPoint rounded() {
    return new GeoPoint(Math.round(lat * 1e5) / 1e5, Math.round(lon * 1e5) / 1e5);
  }
}
