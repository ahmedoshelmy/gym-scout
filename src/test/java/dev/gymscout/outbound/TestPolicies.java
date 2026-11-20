package dev.gymscout.outbound;

import java.time.Duration;

/** Same policy shapes as production, with millisecond waits so tests stay fast. */
public final class TestPolicies {

  private TestPolicies() {}

  public static OutboundProperties fast() {
    return new OutboundProperties(
        "gym-scout-demo",
        50,
        Duration.ofSeconds(1),
        2,
        3,
        Duration.ofMillis(5),
        Duration.ofMillis(20),
        4,
        4,
        50,
        Duration.ofMinutes(1),
        Duration.ofSeconds(2));
  }
}
