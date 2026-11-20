package dev.gymscout.scouting.application;

import dev.gymscout.geo.GeoPoint;
import dev.gymscout.scouting.domain.TravelEstimate;
import dev.gymscout.scouting.domain.TravelMode;
import java.util.List;

/** Travel from one origin to many destinations, in the same order as {@code destinations}. */
public interface TravelTimes {

  List<TravelEstimate> from(GeoPoint origin, List<GeoPoint> destinations, TravelMode mode);
}
