package dev.gymscout.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class GeoMathTest {

  @Test
  void oneDegreeOfLatitudeIsAbout111Km() {
    double d = GeoMath.distanceMeters(new GeoPoint(30, 31), new GeoPoint(31, 31));
    assertThat(d).isCloseTo(111_195, within(50.0));
  }

  @Test
  void rejectsImpossibleCoordinates() {
    assertThatThrownBy(() -> new GeoPoint(91, 0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new GeoPoint(0, -181)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void roundingGivesStableCacheKeys() {
    assertThat(new GeoPoint(30.0444001, 31.2357004).rounded())
        .isEqualTo(new GeoPoint(30.0444, 31.2357));
  }
}
