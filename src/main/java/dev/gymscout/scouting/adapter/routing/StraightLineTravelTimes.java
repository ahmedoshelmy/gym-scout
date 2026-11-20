package dev.gymscout.scouting.adapter.routing;

import dev.gymscout.geo.GeoMath;
import dev.gymscout.geo.GeoPoint;
import dev.gymscout.scouting.application.TravelTimes;
import dev.gymscout.scouting.domain.TravelEstimate;
import dev.gymscout.scouting.domain.TravelMode;
import java.util.List;

/** Offline travel times: crow-flies distance, a detour factor and an average speed. */
public final class StraightLineTravelTimes implements TravelTimes {

  @Override
  public List<TravelEstimate> from(GeoPoint origin, List<GeoPoint> destinations, TravelMode mode) {
    return destinations.stream()
        .map(d -> TravelEstimate.straightLine(GeoMath.distanceMeters(origin, d), mode))
        .toList();
  }
}
