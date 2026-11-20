package dev.gymscout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * End to end against a real PostGIS (Testcontainers): seeded gyms, KNN and radius queries, the
 * explained ranking, a workout that sets a record, and the module event that turns it into a
 * membership visit.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class GymScoutFlowIntegrationTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String CAIRO = "lat=30.0444&lon=31.2357";

  @Autowired MockMvc mvc;

  @Test
  void nearestGymsComeFirstAndStayInsideTheRadius() throws Exception {
    JsonNode gyms =
        send(get("/api/places/gyms?" + CAIRO + "&radius=3000&limit=50"), null, status().isOk());
    var distances = new ArrayList<Double>();
    gyms.forEach(g -> distances.add(g.get("distanceMeters").asDouble()));
    assertThat(distances)
        .isNotEmpty()
        .isSorted()
        .allSatisfy(d -> assertThat(d).isLessThanOrEqualTo(3000));
    assertThat(gyms.get(0).get("gym").get("name").asString()).isEqualTo("Tahrir Training Rooms");
    var names = new ArrayList<String>();
    gyms.forEach(g -> names.add(g.get("gym").get("name").asString()));
    assertThat(names).doesNotContain("Maadi Move Studio", "Heliopolis Pulse Gym");
  }

  @Test
  void limitKeepsTheNearestOnes() throws Exception {
    JsonNode all =
        send(get("/api/places/gyms?" + CAIRO + "&radius=50000&limit=100"), null, status().isOk());
    JsonNode three =
        send(get("/api/places/gyms?" + CAIRO + "&radius=50000&limit=3"), null, status().isOk());
    assertThat(three.size()).isEqualTo(3);
    for (int i = 0; i < 3; i++) {
      assertThat(three.get(i).get("gym").get("id")).isEqualTo(all.get(i).get("gym").get("id"));
    }
  }

  @Test
  void scoutRanksAndExplains() throws Exception {
    JsonNode report =
        send(
            get("/api/scout?" + CAIRO + "&radius=6000&visitsPerWeek=3&amenities=pool,sauna"),
            null,
            status().isOk());
    assertThat(report.get("travelEstimated").asBoolean()).isTrue();
    JsonNode results = report.get("results");
    assertThat(results.size()).isGreaterThan(3);
    var scores = new ArrayList<Double>();
    results.forEach(r -> scores.add(r.get("score").asDouble()));
    assertThat(scores).isSortedAccordingTo((a, b) -> Double.compare(b, a));
    assertThat(results.get(0).get("reasons").size()).isEqualTo(3);
  }

  @Test
  void commuteSearchFindsGymsNearTheLine() throws Exception {
    JsonNode report =
        send(
            get(
                "/api/scout/commute?homeLat=30.0385&homeLon=31.2050&workLat=30.0450&workLon=31.2450&corridor=1000"),
            null,
            status().isOk());
    assertThat(report.get("results").size()).isPositive();
    report
        .get("results")
        .forEach(r -> assertThat(r.get("distanceMeters").asDouble()).isLessThanOrEqualTo(1000));
  }

  @Test
  void badInputIsAProblemDetail() throws Exception {
    JsonNode problem = send(get("/api/places/gyms?lat=95&lon=31"), null, status().isBadRequest());
    assertThat(problem.get("detail").asString()).contains("latitude");
  }

  @Test
  void aWorkoutAtTheMemberGymSetsARecordAndCountsAsAVisit() throws Exception {
    JsonNode gyms =
        send(get("/api/places/gyms?lat=30.0478&lon=31.2336&radius=200"), null, status().isOk());
    String gymId = gyms.get(0).get("gym").get("id").asString();
    LocalDate today = LocalDate.now();

    send(
        post("/api/memberships"),
        """
        {"gymId": "%s", "planName": "Monthly unlimited", "priceMinor": 120000, "currency": "EGP",
         "startsOn": "%s", "endsOn": "%s"}
        """
            .formatted(gymId, today.minusDays(10), today.plusDays(19)),
        status().isCreated());

    send(
        post("/api/training/sessions"),
        session(gymId, today.minusDays(2), 80),
        status().isCreated());
    JsonNode second =
        send(post("/api/training/sessions"), session(gymId, today, 85), status().isCreated());
    List<String> kinds = new ArrayList<>();
    second.get("records").forEach(r -> kinds.add(r.get("kind").asString()));
    assertThat(kinds).contains("HEAVIEST_WEIGHT", "ESTIMATED_ONE_REP_MAX");

    JsonNode progress = send(get("/api/training/progress?weeks=4"), null, status().isOk());
    assertThat(progress.get("weeklyVolume").size()).isEqualTo(4);
    assertThat(progress.get("recentRecords").get(0).get("exercise").asString())
        .isEqualTo("Back squat");

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(() -> {
          JsonNode current = send(get("/api/memberships/current"), null, status().isOk());
          assertThat(current.get("stats").get("visits").asInt()).isEqualTo(2);
          assertThat(current.get("stats").get("costPerVisitMinor").asLong()).isEqualTo(60000);
        });
  }

  private static String session(String gymId, LocalDate on, int kg) {
    return """
    {"performedOn": "%s", "gymId": "%s",
     "sets": [{"exercise": "Back squat", "weightKg": %d, "reps": 5},
              {"exercise": "Back squat", "weightKg": %d, "reps": 5}]}
    """
        .formatted(on, gymId, kg - 10, kg);
  }

  private JsonNode send(MockHttpServletRequestBuilder request, String body, ResultMatcher expected)
      throws Exception {
    if (body != null) {
      request.contentType(MediaType.APPLICATION_JSON).content(body);
    }
    String response =
        mvc.perform(request).andExpect(expected).andReturn().getResponse().getContentAsString();
    return response.isEmpty() ? JSON.nullNode() : JSON.readTree(response);
  }
}
