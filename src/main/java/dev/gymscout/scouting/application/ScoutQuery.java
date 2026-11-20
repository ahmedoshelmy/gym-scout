package dev.gymscout.scouting.application;

import dev.gymscout.geo.GeoPoint;
import dev.gymscout.scouting.domain.ScoringWeights;
import dev.gymscout.scouting.domain.TravelMode;
import java.util.Set;

/** A search near one point. */
public record ScoutQuery(
    GeoPoint origin,
    double radiusMeters,
    int limit,
    double visitsPerWeek,
    Set<String> wantedAmenities,
    TravelMode mode,
    ScoringWeights weights) {

  public ScoutQuery {
    wantedAmenities = Set.copyOf(wantedAmenities);
  }
}
