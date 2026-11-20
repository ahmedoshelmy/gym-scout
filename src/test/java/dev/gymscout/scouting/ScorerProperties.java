package dev.gymscout.scouting;

import static org.assertj.core.api.Assertions.assertThat;

import dev.gymscout.scouting.domain.ScoredGym;
import dev.gymscout.scouting.domain.Scorer;
import dev.gymscout.scouting.domain.ScoringWeights;
import java.util.List;
import java.util.Set;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.DoubleRange;
import net.jqwik.api.constraints.LongRange;

class ScorerProperties {

  /** A gym that is no worse on any factor never ranks below the one it dominates. */
  @Property
  void dominatingGymNeverScoresLower(
      @ForAll @LongRange(min = 1, max = 100_000) long price,
      @ForAll @LongRange(min = 0, max = 100_000) long extraPrice,
      @ForAll @DoubleRange(min = 0, max = 120) double minutes,
      @ForAll @DoubleRange(min = 0, max = 120) double extraMinutes,
      @ForAll boolean betterHasSauna) {
    var better =
        betterHasSauna
            ? ScorerTest.gym("Better", price, minutes, "pool", "sauna")
            : ScorerTest.gym("Better", price, minutes, "pool");
    var worse = ScorerTest.gym("Worse", price + extraPrice, minutes + extraMinutes, "pool");
    List<ScoredGym> ranked =
        Scorer.rank(List.of(worse, better), Set.of("pool", "sauna"), ScoringWeights.DEFAULT);
    double betterScore = scoreOf(ranked, "Better");
    double worseScore = scoreOf(ranked, "Worse");
    assertThat(betterScore).isGreaterThanOrEqualTo(worseScore);
  }

  @Property
  void scoresStayBetweenZeroAndOneHundred(
      @ForAll @LongRange(min = 0, max = 1_000_000) long a,
      @ForAll @LongRange(min = 0, max = 1_000_000) long b,
      @ForAll @DoubleRange(min = 0, max = 600) double ta,
      @ForAll @DoubleRange(min = 0, max = 600) double tb) {
    var ranked =
        Scorer.rank(
            List.of(ScorerTest.gym("A", a, ta), ScorerTest.gym("B", b, tb)),
            Set.of("yoga"),
            ScoringWeights.DEFAULT);
    assertThat(ranked).allSatisfy(r -> assertThat(r.score()).isBetween(0.0, 100.0));
  }

  private static double scoreOf(List<ScoredGym> ranked, String name) {
    return ranked.stream()
        .filter(r -> r.candidate().name().equals(name))
        .findFirst()
        .orElseThrow()
        .score();
  }
}
