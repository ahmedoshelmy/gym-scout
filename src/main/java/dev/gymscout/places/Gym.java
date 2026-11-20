package dev.gymscout.places;

import dev.gymscout.geo.GeoPoint;
import java.util.List;
import java.util.UUID;

/** A gym as other modules see it. */
public record Gym(
    UUID id,
    String name,
    GeoPoint location,
    String address,
    List<String> amenities,
    String openingHours,
    String source,
    List<MembershipPlan> plans) {

  public Gym {
    amenities = List.copyOf(amenities);
    plans = List.copyOf(plans);
  }
}
