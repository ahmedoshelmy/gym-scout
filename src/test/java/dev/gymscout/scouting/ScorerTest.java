package dev.gymscout.scouting;

import static org.assertj.core.api.Assertions.assertThat;

import dev.gymscout.scouting.domain.PlanPrice;
import dev.gymscout.scouting.domain.ScoredGym;
import dev.gymscout.scouting.domain.Scorer;
import dev.gymscout.scouting.domain.ScoringWeights;
import dev.gymscout.scouting.domain.ScoutCandidate;
import dev.gymscout.scouting.domain.TravelEstimate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ScorerTest {

  static ScoutCandidate gym(String name, Long perVisit, double minutes, String... amenities) {
    return new ScoutCandidate(
        UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
        name,
        Optional.ofNullable(perVisit).map(p -> new PlanPrice("Monthly", p, "EGP", 12)),
        new TravelEstimate(minutes * 400, minutes, false),
        Set.of(amenities));
  }

  @Test
  void bestOnEveryFactorScoresOneHundred() {
    var ranked =
        Scorer.rank(
            List.of(gym("Cheap and close", 50_00L, 5, "pool"), gym("Pricey and far", 100_00L, 25)),
            Set.of("pool"),
            ScoringWeights.DEFAULT);
    assertThat(ranked.getFirst().score()).isEqualTo(100.0);
    assertThat(ranked.getFirst().candidate().name()).isEqualTo("Cheap and close");
  }

  @Test
  void pointsAreProportionalToTheBestInTheSet() {
    var ranked =
        Scorer.rank(
            List.of(gym("A", 50_00L, 5), gym("B", 100_00L, 5)), Set.of(), ScoringWeights.DEFAULT);
    ScoredGym b = ranked.getLast();
    assertThat(b.pricePoints()).isEqualTo(25.0);
    assertThat(b.travelPoints()).isEqualTo(35.0);
    assertThat(b.amenityPoints()).isEqualTo(15.0);
    assertThat(b.reasons().getFirst()).contains("EGP 100.00 per visit").contains("100% above");
  }

  @Test
  void everyFactorExplainsItself() {
    var ranked =
        Scorer.rank(
            List.of(gym("Swim club", 80_00L, 12, "pool")),
            Set.of("pool", "sauna"),
            ScoringWeights.DEFAULT);
    assertThat(ranked.getFirst().reasons())
        .containsExactly(
            "Cheapest per visit: EGP 80.00 with the Monthly",
            "12 min away, 4.8 km",
            "Has pool; missing sauna");
  }

  @Test
  void unknownPriceGetsNoPricePointsAndSaysSo() {
    var ranked =
        Scorer.rank(
            List.of(gym("Priced", 90_00L, 10), gym("Mystery", null, 10)),
            Set.of(),
            ScoringWeights.DEFAULT);
    ScoredGym mystery = ranked.getLast();
    assertThat(mystery.candidate().name()).isEqualTo("Mystery");
    assertThat(mystery.pricePoints()).isZero();
    assertThat(mystery.reasons().getFirst()).startsWith("No price known yet");
  }

  @Test
  void estimatedTravelIsFlagged() {
    var estimated =
        new ScoutCandidate(
            UUID.randomUUID(),
            "Far",
            Optional.empty(),
            TravelEstimate.straightLine(2000, dev.gymscout.scouting.domain.TravelMode.WALK),
            Set.of());
    var ranked = Scorer.rank(List.of(estimated), Set.of(), ScoringWeights.DEFAULT);
    assertThat(ranked.getFirst().reasons().get(1)).endsWith("(straight-line estimate)");
  }
}
