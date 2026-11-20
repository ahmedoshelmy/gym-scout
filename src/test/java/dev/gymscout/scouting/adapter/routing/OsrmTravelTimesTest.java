package dev.gymscout.scouting.adapter.routing;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.tomakehurst.wiremock.WireMockServer;
import dev.gymscout.geo.GeoPoint;
import dev.gymscout.outbound.PoliteCaller;
import dev.gymscout.outbound.TestPolicies;
import dev.gymscout.scouting.domain.TravelEstimate;
import dev.gymscout.scouting.domain.TravelMode;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/** The live OSRM adapter against a WireMock stand-in: cache, retry, breaker and fallback. */
class OsrmTravelTimesTest {

  private static final GeoPoint HOME = new GeoPoint(30.0444, 31.2357);
  private static final List<GeoPoint> GYMS =
      List.of(new GeoPoint(30.0500, 31.2400), new GeoPoint(30.0600, 31.2500));
  private static final String TABLE_PATH = "/table/v1/driving/.*";

  private WireMockServer server;
  private PoliteCaller caller;
  private OsrmTravelTimes osrm;

  @BeforeEach
  void start() {
    server = new WireMockServer(options().dynamicPort());
    server.start();
    caller = new PoliteCaller("osrm", TestPolicies.fast(), new SimpleMeterRegistry());
    osrm =
        new OsrmTravelTimes(
            RestClient.builder().baseUrl(server.baseUrl()).build(),
            caller,
            Caffeine.newBuilder().maximumSize(100).build());
  }

  @AfterEach
  void stop() {
    server.stop();
  }

  @Test
  void readsTheFirstRowOfTheTableAndCachesIt() {
    server.stubFor(
        get(urlPathMatching(TABLE_PATH))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                        {"code": "Ok", "durations": [[0, 600, 1200]], "distances": [[0, 5000, 9000]]}
                        """)));
    List<TravelEstimate> first = osrm.from(HOME, GYMS, TravelMode.DRIVE);
    assertThat(first)
        .containsExactly(new TravelEstimate(5000, 10, false), new TravelEstimate(9000, 20, false));

    List<TravelEstimate> again = osrm.from(HOME, GYMS, TravelMode.DRIVE);
    assertThat(again).isEqualTo(first);
    server.verify(1, getRequestedFor(urlPathMatching(TABLE_PATH)));
  }

  @Test
  void fallsBackToStraightLineWhenTheServerKeepsFailing() {
    server.stubFor(get(urlPathMatching(TABLE_PATH)).willReturn(aResponse().withStatus(503)));
    List<TravelEstimate> result = osrm.from(HOME, GYMS, TravelMode.DRIVE);
    assertThat(result).allSatisfy(t -> assertThat(t.estimated()).isTrue());
    server.verify(3, getRequestedFor(urlPathMatching(TABLE_PATH)));
  }

  @Test
  void breakerOpensAndStopsCallingTheServer() {
    server.stubFor(get(urlPathMatching(TABLE_PATH)).willReturn(aResponse().withStatus(500)));
    osrm.from(HOME, GYMS, TravelMode.DRIVE);
    osrm.from(HOME, GYMS, TravelMode.DRIVE);
    assertThat(caller.breakerState()).isEqualTo(CircuitBreaker.State.OPEN);
    int callsSoFar = server.getAllServeEvents().size();

    List<TravelEstimate> whileOpen = osrm.from(HOME, GYMS, TravelMode.DRIVE);
    assertThat(whileOpen).allSatisfy(t -> assertThat(t.estimated()).isTrue());
    assertThat(server.getAllServeEvents()).hasSize(callsSoFar);
  }

  @Test
  void walkingNeverCallsTheCarRouter() {
    assertThat(osrm.from(HOME, GYMS, TravelMode.WALK))
        .allSatisfy(t -> assertThat(t.estimated()).isTrue());
    server.verify(0, getRequestedFor(urlPathMatching(TABLE_PATH)));
  }
}
