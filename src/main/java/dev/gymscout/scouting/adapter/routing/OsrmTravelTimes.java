package dev.gymscout.scouting.adapter.routing;

import com.github.benmanes.caffeine.cache.Cache;
import dev.gymscout.geo.GeoPoint;
import dev.gymscout.outbound.PoliteCaller;
import dev.gymscout.outbound.UpstreamUnavailableException;
import dev.gymscout.scouting.application.TravelTimes;
import dev.gymscout.scouting.domain.TravelEstimate;
import dev.gymscout.scouting.domain.TravelMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

/**
 * Driving times from an OSRM server's table service, one origin to many destinations per call.
 * Answers are cached per rounded origin/destination pair (Caffeine), so panning the map does not
 * re-ask the public server. When OSRM is unavailable the straight-line estimate answers instead and
 * is not cached. Walking always uses the estimate (the public demo server only routes cars).
 */
public final class OsrmTravelTimes implements TravelTimes {

  private static final Logger log = LoggerFactory.getLogger(OsrmTravelTimes.class);

  /** Keeps URLs short and each call cheap for the public server. */
  static final int MAX_DESTINATIONS_PER_CALL = 50;

  private final RestClient http;
  private final PoliteCaller caller;
  private final Cache<RouteKey, TravelEstimate> cache;
  private final StraightLineTravelTimes fallback = new StraightLineTravelTimes();

  public OsrmTravelTimes(
      RestClient http, PoliteCaller caller, Cache<RouteKey, TravelEstimate> cache) {
    this.http = http;
    this.caller = caller;
    this.cache = cache;
  }

  /** Cache key: both ends rounded to about a metre. */
  public record RouteKey(GeoPoint from, GeoPoint to) {}

  @Override
  public List<TravelEstimate> from(GeoPoint origin, List<GeoPoint> destinations, TravelMode mode) {
    if (mode != TravelMode.DRIVE) {
      return fallback.from(origin, destinations, mode);
    }
    var results = new ArrayList<TravelEstimate>(destinations.size());
    var missing = new ArrayList<GeoPoint>();
    for (GeoPoint d : destinations) {
      TravelEstimate hit = cache.getIfPresent(key(origin, d));
      results.add(hit);
      if (hit == null) {
        missing.add(d);
      }
    }
    for (int start = 0; start < missing.size(); start += MAX_DESTINATIONS_PER_CALL) {
      fill(
          origin,
          missing.subList(start, Math.min(missing.size(), start + MAX_DESTINATIONS_PER_CALL)));
    }
    for (int i = 0; i < results.size(); i++) {
      if (results.get(i) == null) {
        TravelEstimate fresh = cache.getIfPresent(key(origin, destinations.get(i)));
        results.set(
            i,
            fresh != null
                ? fresh
                : fallback.from(origin, List.of(destinations.get(i)), mode).getFirst());
      }
    }
    return results;
  }

  private void fill(GeoPoint origin, List<GeoPoint> batch) {
    var points = new ArrayList<GeoPoint>(batch.size() + 1);
    points.add(origin);
    points.addAll(batch);
    String coordinates =
        points.stream()
            .map(p -> String.format(Locale.ROOT, "%.6f,%.6f", p.lon(), p.lat()))
            .collect(Collectors.joining(";"));
    try {
      JsonNode body =
          caller.call(() ->
              http.get()
                  .uri(
                      "/table/v1/driving/{coordinates}?sources=0&annotations=duration,distance",
                      coordinates)
                  .retrieve()
                  .body(JsonNode.class));
      store(origin, batch, body);
    } catch (UpstreamUnavailableException e) {
      log.warn("OSRM unavailable, using straight-line estimates: {}", e.getMessage());
    }
  }

  private void store(GeoPoint origin, List<GeoPoint> batch, JsonNode body) {
    if (body == null || !"Ok".equals(body.path("code").asString())) {
      return;
    }
    JsonNode durations = body.path("durations").path(0);
    JsonNode distances = body.path("distances").path(0);
    for (int i = 0; i < batch.size(); i++) {
      JsonNode seconds = durations.path(i + 1);
      JsonNode meters = distances.path(i + 1);
      if (seconds.isNumber() && meters.isNumber()) {
        cache.put(
            key(origin, batch.get(i)),
            new TravelEstimate(meters.asDouble(), seconds.asDouble() / 60, false));
      }
    }
  }

  private static RouteKey key(GeoPoint from, GeoPoint to) {
    return new RouteKey(from.rounded(), to.rounded());
  }
}
