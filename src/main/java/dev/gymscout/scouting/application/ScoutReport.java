package dev.gymscout.scouting.application;

import dev.gymscout.geo.GeoPoint;
import dev.gymscout.scouting.domain.ScoredGym;
import java.util.List;

/** Ranked results plus where each gym sits, for the map. */
public record ScoutReport(List<Entry> results, boolean travelEstimated) {

  public ScoutReport {
    results = List.copyOf(results);
  }

  public record Entry(ScoredGym ranked, GeoPoint location, double distanceMeters) {}
}
