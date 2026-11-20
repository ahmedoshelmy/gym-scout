package dev.gymscout.memberships.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/** A paid membership at one gym for a fixed period (both ends inclusive). */
public record Membership(
    UUID id,
    UUID gymId,
    String planName,
    long priceMinor,
    String currency,
    LocalDate startsOn,
    LocalDate endsOn) {

  public Membership {
    Objects.requireNonNull(gymId, "gymId");
    if (planName == null || planName.isBlank()) {
      throw new IllegalArgumentException("plan name is required");
    }
    if (priceMinor < 0) {
      throw new IllegalArgumentException("price cannot be negative");
    }
    if (currency == null || !currency.matches("[A-Z]{3}")) {
      throw new IllegalArgumentException("currency must be an ISO-4217 code like EGP");
    }
    if (startsOn == null || endsOn == null || endsOn.isBefore(startsOn)) {
      throw new IllegalArgumentException("the membership must end on or after its start");
    }
  }

  public boolean covers(LocalDate day) {
    return !day.isBefore(startsOn) && !day.isAfter(endsOn);
  }

  public long totalDays() {
    return ChronoUnit.DAYS.between(startsOn, endsOn) + 1;
  }
}
