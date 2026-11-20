package dev.gymscout.scouting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.gymscout.scouting.domain.PerVisitPrice;
import dev.gymscout.scouting.domain.PlanOption;
import java.util.List;
import org.junit.jupiter.api.Test;

class PerVisitPriceTest {

  private static final PlanOption MONTHLY = new PlanOption("Monthly", 1_200_00, "EGP", 30, null);
  private static final PlanOption PACK = new PlanOption("8-visit pack", 800_00, "EGP", 30, 8);

  @Test
  void unlimitedMonthIsDividedByTheVisitsYouActuallyMake() {
    // 3 visits a week over 30 days = 12.86 visits -> 1200 / 12.86 = 93.33
    var best = PerVisitPrice.best(List.of(MONTHLY), 3).orElseThrow();
    assertThat(best.perVisitMinor()).isEqualTo(93_33);
    assertThat(best.visitsUsed()).isCloseTo(12.857, org.assertj.core.data.Offset.offset(0.001));
  }

  @Test
  void packWinsForOccasionalVisitorsAndMonthlyWinsForRegulars() {
    // once a week: 4.29 visits; pack 800/4.29 = 186.67, monthly 1200/4.29 = 280
    assertThat(PerVisitPrice.best(List.of(MONTHLY, PACK), 1).orElseThrow().planName())
        .isEqualTo("8-visit pack");
    // five a week: 21.4 visits; pack caps at 8 -> 100, monthly 56
    assertThat(PerVisitPrice.best(List.of(MONTHLY, PACK), 5).orElseThrow().planName())
        .isEqualTo("Monthly");
  }

  @Test
  void packOnlyCountsTheVisitsItIncludes() {
    var price = PerVisitPrice.best(List.of(PACK), 7).orElseThrow();
    assertThat(price.visitsUsed()).isEqualTo(8);
    assertThat(price.perVisitMinor()).isEqualTo(100_00);
  }

  @Test
  void noPlansMeansNoPrice() {
    assertThat(PerVisitPrice.best(List.of(), 3)).isEmpty();
  }

  @Test
  void rejectsSillyVisitRates() {
    assertThatThrownBy(() -> PerVisitPrice.best(List.of(MONTHLY), 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> PerVisitPrice.best(List.of(MONTHLY), 15))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
