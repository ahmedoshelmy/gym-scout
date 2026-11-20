package dev.gymscout.scouting.application;

import dev.gymscout.geo.GeoPoint;
import dev.gymscout.scouting.domain.ScoringWeights;
import dev.gymscout.scouting.domain.TravelMode;
import java.util.Set;

/** A search along the way from home to work: travel cost is the detour the gym adds. */
public record CommuteQuery(
    GeoPoint home,
    GeoPoint work,
    double corridorMeters,
    int limit,
    double visitsPerWeek,
    Set<String> wantedAmenities,
    TravelMode mode,
    ScoringWeights weights) {

  public CommuteQuery {
    wantedAmenities = Set.copyOf(wantedAmenities);
  }
}
