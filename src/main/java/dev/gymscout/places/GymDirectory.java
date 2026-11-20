package dev.gymscout.places;

import dev.gymscout.geo.GeoPoint;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spatial queries over the gym catalogue, answered by PostGIS. */
public interface GymDirectory {

  /** Gyms within {@code radiusMeters} of {@code point}, nearest first. */
  List<NearbyGym> nearest(GeoPoint point, double radiusMeters, int limit);

  /** Gyms within {@code corridorMeters} of the straight line from {@code a} to {@code b}. */
  List<NearbyGym> alongLine(GeoPoint a, GeoPoint b, double corridorMeters, int limit);

  Optional<Gym> find(UUID id);

  /** Same as {@link #find} but for many ids at once. */
  List<Gym> findAll(List<UUID> ids);
}
