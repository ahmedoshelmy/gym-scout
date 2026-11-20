package dev.gymscout.geo;

/** Great-circle helpers for fallbacks and tests; the database does the real spatial work. */
public final class GeoMath {

  /** Mean Earth radius in metres (IUGG). */
  public static final double EARTH_RADIUS_M = 6_371_008.8;

  private GeoMath() {}

  /** Haversine distance in metres. */
  public static double distanceMeters(GeoPoint a, GeoPoint b) {
    double dLat = Math.toRadians(b.lat() - a.lat());
    double dLon = Math.toRadians(b.lon() - a.lon());
    double h =
        Math.pow(Math.sin(dLat / 2), 2)
            + Math.cos(Math.toRadians(a.lat()))
                * Math.cos(Math.toRadians(b.lat()))
                * Math.pow(Math.sin(dLon / 2), 2);
    return 2 * EARTH_RADIUS_M * Math.asin(Math.min(1, Math.sqrt(h)));
  }

  /** The point reached by going {@code meters} from {@code start} on an initial bearing. */
  public static GeoPoint destination(GeoPoint start, double bearingDegrees, double meters) {
    double delta = meters / EARTH_RADIUS_M;
    double theta = Math.toRadians(bearingDegrees);
    double phi1 = Math.toRadians(start.lat());
    double lambda1 = Math.toRadians(start.lon());
    double phi2 =
        Math.asin(
            Math.sin(phi1) * Math.cos(delta) + Math.cos(phi1) * Math.sin(delta) * Math.cos(theta));
    double lambda2 =
        lambda1
            + Math.atan2(
                Math.sin(theta) * Math.sin(delta) * Math.cos(phi1),
                Math.cos(delta) - Math.sin(phi1) * Math.sin(phi2));
    double lon = ((Math.toDegrees(lambda2) + 540) % 360) - 180;
    return new GeoPoint(Math.toDegrees(phi2), lon);
  }
}
