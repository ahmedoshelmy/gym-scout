package dev.gymscout.scouting.domain;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Everything the scorer needs about one gym. */
public record ScoutCandidate(
    UUID gymId,
    String name,
    Optional<PlanPrice> price,
    TravelEstimate travel,
    Set<String> amenities) {

  public ScoutCandidate {
    amenities = Set.copyOf(amenities);
  }
}
