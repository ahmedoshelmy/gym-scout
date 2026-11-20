package dev.gymscout.places.application;

import dev.gymscout.geo.GeoPoint;
import dev.gymscout.places.Gym;
import dev.gymscout.places.MembershipPlan;
import dev.gymscout.places.NearbyGym;
import dev.gymscout.places.domain.GymCandidate;
import dev.gymscout.places.domain.PlanOffer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Storage port for gyms; the PostGIS adapter does the spatial filtering and ordering. */
public interface GymRepository {

  /** Inserts or updates by external reference; returns the gym id. */
  UUID upsert(GymCandidate candidate, String source);

  /** Adds a plan unless the gym already has one with the same name. */
  Optional<MembershipPlan> addPlan(UUID gymId, PlanOffer offer);

  List<NearbyGym> nearest(GeoPoint point, double radiusMeters, int limit);

  List<NearbyGym> alongLine(GeoPoint a, GeoPoint b, double corridorMeters, int limit);

  List<Gym> findAll(List<UUID> ids);

  long count();
}
