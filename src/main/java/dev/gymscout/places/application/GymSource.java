package dev.gymscout.places.application;

import dev.gymscout.geo.GeoPoint;
import dev.gymscout.places.domain.GymCandidate;
import java.util.List;

/** Where gyms come from: synthetic seed data by default, OpenStreetMap when enabled. */
public interface GymSource {

  /** A short name stored with every imported gym ({@code seed} or {@code osm}). */
  String sourceName();

  List<GymCandidate> fetchAround(GeoPoint center, int radiusMeters);
}
