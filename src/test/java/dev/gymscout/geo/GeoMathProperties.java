package dev.gymscout.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.Comparator;
import java.util.List;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.DoubleRange;
import net.jqwik.api.constraints.Size;

/** Distance ordering is monotonic: walking further along a bearing never gets you closer. */
class GeoMathProperties {

  @Property
  void goingFurtherOnABearingIncreasesDistance(
      @ForAll @DoubleRange(min = -60, max = 60) double lat,
      @ForAll @DoubleRange(min = -179, max = 179) double lon,
      @ForAll @DoubleRange(min = 0, max = 360, maxIncluded = false) double bearing,
      @ForAll @DoubleRange(min = 1, max = 500_000) double near,
      @ForAll @DoubleRange(min = 1, max = 500_000) double extra) {
    var start = new GeoPoint(lat, lon);
    double d1 = GeoMath.distanceMeters(start, GeoMath.destination(start, bearing, near));
    double d2 = GeoMath.distanceMeters(start, GeoMath.destination(start, bearing, near + extra));
    assertThat(d2).isGreaterThan(d1);
    assertThat(d1).isCloseTo(near, within(near * 1e-6 + 0.01));
  }

  @Property
  void sortingByDistanceGivesNonDecreasingDistances(
      @ForAll @Size(min = 1, max = 30) List<@DoubleRange(min = 0, max = 30_000) Double> radii,
      @ForAll @DoubleRange(min = 0, max = 360, maxIncluded = false) double bearing) {
    var origin = new GeoPoint(30.0444, 31.2357);
    var sorted =
        radii.stream()
            .map(r -> GeoMath.destination(origin, bearing + r % 90, r))
            .sorted(Comparator.comparingDouble(p -> GeoMath.distanceMeters(origin, p)))
            .mapToDouble(p -> GeoMath.distanceMeters(origin, p))
            .toArray();
    for (int i = 1; i < sorted.length; i++) {
      assertThat(sorted[i]).isGreaterThanOrEqualTo(sorted[i - 1]);
    }
  }

  @Property
  void distanceIsSymmetric(
      @ForAll @DoubleRange(min = -80, max = 80) double lat1,
      @ForAll @DoubleRange(min = -180, max = 180) double lon1,
      @ForAll @DoubleRange(min = -80, max = 80) double lat2,
      @ForAll @DoubleRange(min = -180, max = 180) double lon2) {
    var a = new GeoPoint(lat1, lon1);
    var b = new GeoPoint(lat2, lon2);
    assertThat(GeoMath.distanceMeters(a, b)).isCloseTo(GeoMath.distanceMeters(b, a), within(1e-6));
  }
}
