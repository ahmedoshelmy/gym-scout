package dev.gymscout.places.adapter.osm;

import dev.gymscout.geo.GeoPoint;
import dev.gymscout.outbound.PoliteCaller;
import dev.gymscout.places.AmenityTags;
import dev.gymscout.places.application.GymSource;
import dev.gymscout.places.domain.GymCandidate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

/**
 * Live gyms from the OpenStreetMap Overpass API ({@code leisure=fitness_centre}). Every call goes
 * through a {@link PoliteCaller}: one request per second, retries on 429/5xx, breaker. Data is ©
 * OpenStreetMap contributors (ODbL).
 */
public final class OverpassGymSource implements GymSource {

  private final RestClient http;
  private final PoliteCaller caller;

  public OverpassGymSource(RestClient http, PoliteCaller caller) {
    this.http = http;
    this.caller = caller;
  }

  @Override
  public String sourceName() {
    return "osm";
  }

  @Override
  public List<GymCandidate> fetchAround(GeoPoint center, int radiusMeters) {
    String query =
        "[out:json][timeout:25];nwr[\"leisure\"=\"fitness_centre\"](around:%d,%.6f,%.6f);out center 100;"
            .formatted(radiusMeters, center.lat(), center.lon());
    var form = new LinkedMultiValueMap<String, String>();
    form.add("data", query);
    JsonNode body =
        caller.call(() ->
            http.post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class));
    return parse(body);
  }

  /** Maps Overpass elements to candidates; nodes carry lat/lon, ways and relations a center. */
  static List<GymCandidate> parse(JsonNode body) {
    var gyms = new ArrayList<GymCandidate>();
    if (body == null) {
      return gyms;
    }
    for (JsonNode element : body.path("elements")) {
      JsonNode at = element.has("lat") ? element : element.path("center");
      if (!at.has("lat") || !at.has("lon")) {
        continue;
      }
      Map<String, String> tags = new LinkedHashMap<>();
      element.path("tags").properties().forEach(e -> tags.put(e.getKey(), e.getValue().asString()));
      gyms.add(
          new GymCandidate(
              "osm:" + element.path("type").asString() + "/" + element.path("id").asLong(),
              tags.get("name"),
              new GeoPoint(at.path("lat").asDouble(), at.path("lon").asDouble()),
              address(tags),
              AmenityTags.fromOsm(tags),
              tags.get("opening_hours"),
              List.of()));
    }
    return gyms;
  }

  private static String address(Map<String, String> tags) {
    String street = tags.get("addr:street");
    String number = tags.get("addr:housenumber");
    String city = tags.get("addr:city");
    var parts = new ArrayList<String>();
    if (street != null) {
      parts.add(number == null ? street : number + " " + street);
    }
    if (city != null) {
      parts.add(city);
    }
    return parts.isEmpty() ? null : String.join(", ", parts);
  }
}
