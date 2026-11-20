package dev.gymscout.places;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AmenityTagsTest {

  @Test
  void mapsWellKnownOsmTags() {
    var tags =
        Map.of(
            "leisure", "fitness_centre",
            "sport", "fitness;swimming;yoga",
            "sauna", "yes",
            "opening_hours", "24/7",
            "wheelchair", "yes",
            "female", "only");
    assertThat(AmenityTags.fromOsm(tags))
        .containsExactly(
            "accessible", "open-24h", "pool", "sauna", "weights", "women-only", "yoga");
  }

  @Test
  void ignoresUnknownTagsAndNoValues() {
    assertThat(AmenityTags.fromOsm(Map.of("sport", "chess", "sauna", "no", "parking", "no")))
        .isEmpty();
  }

  @Test
  void normalizesUserInput() {
    assertThat(AmenityTags.normalize(List.of(" Pool", "SAUNA", "jacuzzi", "")))
        .containsExactly("pool", "sauna");
  }
}
