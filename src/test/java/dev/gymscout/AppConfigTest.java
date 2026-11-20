package dev.gymscout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class AppConfigTest {

  @Test
  void clockUsesTheConfiguredZoneNotTheHostOne() {
    var clock = new AppConfig().clock("Africa/Cairo");
    assertThat(clock.getZone()).isEqualTo(ZoneId.of("Africa/Cairo"));
  }

  @Test
  void justAfterLocalMidnightIsAlreadyTomorrowInCairoWhileUtcIsStillToday() {
    // 22:30 UTC on 1 March is 00:30 on 2 March in Cairo (UTC+2 in winter).
    var instant = Instant.parse("2026-03-01T22:30:00Z");
    var cairo = new AppConfig().clock("Africa/Cairo");
    var cairoToday = LocalDate.ofInstant(instant, cairo.getZone());
    assertThat(cairoToday).isEqualTo(LocalDate.of(2026, 3, 2));
    assertThat(LocalDate.ofInstant(instant, ZoneId.of("UTC"))).isEqualTo(LocalDate.of(2026, 3, 1));
  }

  @Test
  void unknownZoneFailsFast() {
    assertThatThrownBy(() -> new AppConfig().clock("Mars/Olympus"))
        .isInstanceOf(DateTimeException.class);
  }
}
