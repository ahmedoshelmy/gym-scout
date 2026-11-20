package dev.gymscout.places.application;

import dev.gymscout.geo.GeoPoint;
import dev.gymscout.places.Gym;
import dev.gymscout.places.GymDirectory;
import dev.gymscout.places.MembershipPlan;
import dev.gymscout.places.NearbyGym;
import dev.gymscout.places.domain.GymCandidate;
import dev.gymscout.places.domain.PlanOffer;
import dev.gymscout.shared.NotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Imports gyms from the configured source and answers spatial queries. */
@Service
public class PlacesService implements GymDirectory {

  /** Search radius cap: beyond 50 km "near me" stops meaning anything. */
  public static final double MAX_RADIUS_METERS = 50_000;

  public static final int MAX_RESULTS = 100;

  private final GymRepository gyms;
  private final GymSource source;

  public PlacesService(GymRepository gyms, GymSource source) {
    this.gyms = gyms;
    this.source = source;
  }

  /** Pulls gyms around a point from the configured source (no transaction during the call). */
  public ImportResult importAround(GeoPoint center, int radiusMeters) {
    return importFrom(source, center, radiusMeters);
  }

  public ImportResult importFrom(GymSource from, GeoPoint center, int radiusMeters) {
    checkRadius(radiusMeters);
    List<GymCandidate> found = from.fetchAround(center, radiusMeters);
    int stored = 0;
    for (GymCandidate candidate : found) {
      UUID id = gyms.upsert(candidate, from.sourceName());
      candidate.plans().forEach(plan -> gyms.addPlan(id, plan));
      stored++;
    }
    return new ImportResult(from.sourceName(), found.size(), stored);
  }

  @Transactional
  public MembershipPlan addPlan(UUID gymId, PlanOffer offer) {
    find(gymId).orElseThrow(() -> new NotFoundException("No gym " + gymId));
    return gyms.addPlan(gymId, offer)
        .orElseThrow(() ->
            new IllegalArgumentException("This gym already has a plan named " + offer.name()));
  }

  public boolean isEmpty() {
    return gyms.count() == 0;
  }

  @Override
  public List<NearbyGym> nearest(GeoPoint point, double radiusMeters, int limit) {
    checkRadius(radiusMeters);
    return gyms.nearest(point, radiusMeters, clampLimit(limit));
  }

  @Override
  public List<NearbyGym> alongLine(GeoPoint a, GeoPoint b, double corridorMeters, int limit) {
    if (corridorMeters <= 0 || corridorMeters > 10_000) {
      throw new IllegalArgumentException("corridor must be between 1 m and 10 km");
    }
    return gyms.alongLine(a, b, corridorMeters, clampLimit(limit));
  }

  @Override
  public Optional<Gym> find(UUID id) {
    return gyms.findAll(List.of(id)).stream().findFirst();
  }

  @Override
  public List<Gym> findAll(List<UUID> ids) {
    return ids.isEmpty() ? List.of() : gyms.findAll(ids);
  }

  private static void checkRadius(double radiusMeters) {
    if (radiusMeters <= 0 || radiusMeters > MAX_RADIUS_METERS) {
      throw new IllegalArgumentException("radius must be between 1 m and 50 km");
    }
  }

  private static int clampLimit(int limit) {
    return Math.clamp(limit, 1, MAX_RESULTS);
  }
}
