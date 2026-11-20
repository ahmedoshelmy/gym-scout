package dev.gymscout.places.domain;

import dev.gymscout.geo.GeoPoint;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A gym as an external source describes it, before it is stored. {@code externalRef} is stable per
 * source (for example {@code osm:node/42}) so a re-import updates instead of duplicating.
 */
public record GymCandidate(
    String externalRef,
    String name,
    GeoPoint location,
    String address,
    Set<String> amenities,
    String openingHours,
    List<PlanOffer> plans) {

  public GymCandidate {
    Objects.requireNonNull(externalRef, "externalRef");
    Objects.requireNonNull(location, "location");
    name = name == null || name.isBlank() ? "Unnamed gym" : name.strip();
    amenities = Set.copyOf(amenities);
    plans = List.copyOf(plans);
  }
}
