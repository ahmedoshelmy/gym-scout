package dev.gymscout.memberships;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.gymscout.memberships.domain.CostPerVisit;
import dev.gymscout.memberships.domain.Membership;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CostPerVisitTest {

  private static final Membership QUARTER =
      new Membership(
          UUID.randomUUID(),
          UUID.randomUUID(),
          "Quarterly",
          3_000_00,
          "EGP",
          LocalDate.of(2026, 1, 1),
          LocalDate.of(2026, 3, 31));

  @Test
  void costPerVisitIsPriceOverVisitsSoFar() {
    var stats = CostPerVisit.of(QUARTER, 12, LocalDate.of(2026, 1, 30));
    assertThat(stats.costPerVisitMinor()).isEqualTo(250_00);
    assertThat(stats.daysElapsed()).isEqualTo(30);
    assertThat(stats.totalDays()).isEqualTo(90);
    assertThat(stats.projectedVisits()).isEqualTo(36.0);
    assertThat(stats.projectedCostPerVisitMinor()).isEqualTo(83_33);
    assertThat(stats.visitsPerWeek()).isEqualTo(2.8);
  }

  @Test
  void noVisitsYetMeansTheWholePriceIsTheCostOfTheFirstVisit() {
    var stats = CostPerVisit.of(QUARTER, 0, LocalDate.of(2026, 1, 10));
    assertThat(stats.costPerVisitMinor()).isEqualTo(3_000_00);
  }

  @Test
  void beforeTheStartNothingIsProjected() {
    var stats = CostPerVisit.of(QUARTER, 0, LocalDate.of(2025, 12, 20));
    assertThat(stats.daysElapsed()).isZero();
    assertThat(stats.projectedVisits()).isZero();
  }

  @Test
  void afterTheEndElapsedStopsAtTheTotal() {
    assertThat(CostPerVisit.of(QUARTER, 40, LocalDate.of(2026, 6, 1)).daysElapsed()).isEqualTo(90);
  }

  @Test
  void membershipMustEndAfterItStarts() {
    assertThatThrownBy(() ->
            new Membership(
                null,
                UUID.randomUUID(),
                "Bad",
                1,
                "EGP",
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 1, 1)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
