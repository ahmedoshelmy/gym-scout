package dev.gymscout.places.adapter.osm;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import dev.gymscout.geo.GeoPoint;
import dev.gymscout.outbound.PoliteCaller;
import dev.gymscout.outbound.TestPolicies;
import dev.gymscout.outbound.UpstreamUnavailableException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/** The live Overpass adapter against a WireMock stand-in; the fixture is hand-written. */
class OverpassGymSourceTest {

  private static final String FIXTURE =
      """
      {"elements": [
        {"type": "node", "id": 101, "lat": 30.05, "lon": 31.24,
         "tags": {"leisure": "fitness_centre", "name": "Example Iron Gym", "sport": "fitness",
                  "addr:street": "Example Street", "addr:housenumber": "7", "addr:city": "Cairo"}},
        {"type": "way", "id": 202, "center": {"lat": 30.06, "lon": 31.25},
         "tags": {"leisure": "fitness_centre", "sport": "swimming", "sauna": "yes"}},
        {"type": "relation", "id": 303, "tags": {"name": "No position"}}
      ]}
      """;

  private WireMockServer server;
  private OverpassGymSource source;

  @BeforeEach
  void start() {
    server = new WireMockServer(options().dynamicPort());
    server.start();
    var http =
        RestClient.builder()
            .baseUrl(server.baseUrl() + "/api/interpreter")
            .defaultHeader("User-Agent", "gym-scout-demo")
            .build();
    source =
        new OverpassGymSource(
            http, new PoliteCaller("overpass", TestPolicies.fast(), new SimpleMeterRegistry()));
  }

  @AfterEach
  void stop() {
    server.stop();
  }

  @Test
  void parsesNodesAndWaysAndSkipsElementsWithoutAPosition() {
    stubOk();
    var gyms = source.fetchAround(new GeoPoint(30.05, 31.24), 2000);
    assertThat(gyms).hasSize(2);
    var node = gyms.getFirst();
    assertThat(node.externalRef()).isEqualTo("osm:node/101");
    assertThat(node.name()).isEqualTo("Example Iron Gym");
    assertThat(node.address()).isEqualTo("7 Example Street, Cairo");
    assertThat(node.amenities()).containsExactly("weights");
    var way = gyms.getLast();
    assertThat(way.externalRef()).isEqualTo("osm:way/202");
    assertThat(way.name()).isEqualTo("Unnamed gym");
    assertThat(way.location()).isEqualTo(new GeoPoint(30.06, 31.25));
    assertThat(way.amenities()).containsExactlyInAnyOrder("pool", "sauna");
    server.verify(
        postRequestedFor(urlEqualTo("/api/interpreter"))
            .withHeader("User-Agent", equalTo("gym-scout-demo"))
            .withRequestBody(containing("fitness_centre")));
  }

  @Test
  void retriesWhenTheServerSaysTooManyRequests() {
    server.stubFor(
        post(urlEqualTo("/api/interpreter"))
            .inScenario("busy")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withStatus(429).withHeader("Retry-After", "0"))
            .willSetStateTo("calm"));
    server.stubFor(
        post(urlEqualTo("/api/interpreter"))
            .inScenario("busy")
            .whenScenarioStateIs("calm")
            .willReturn(json(FIXTURE)));
    assertThat(source.fetchAround(new GeoPoint(30.05, 31.24), 2000)).hasSize(2);
    server.verify(2, postRequestedFor(urlEqualTo("/api/interpreter")));
  }

  @Test
  void doesNotRetryOurOwnBadRequest() {
    server.stubFor(post(urlEqualTo("/api/interpreter")).willReturn(aResponse().withStatus(400)));
    assertThatThrownBy(() -> source.fetchAround(new GeoPoint(30.05, 31.24), 2000))
        .isInstanceOf(UpstreamUnavailableException.class);
    server.verify(1, postRequestedFor(urlEqualTo("/api/interpreter")));
  }

  private void stubOk() {
    server.stubFor(post(urlEqualTo("/api/interpreter")).willReturn(json(FIXTURE)));
  }

  private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder json(
      String body) {
    return aResponse().withHeader("Content-Type", "application/json").withBody(body);
  }
}
