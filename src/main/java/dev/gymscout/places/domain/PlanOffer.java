package dev.gymscout.places.domain;

import java.util.Objects;

/** A plan price to store for a gym; validated here so every adapter gets the same rules. */
public record PlanOffer(
    String name, long priceMinor, String currency, int durationDays, Integer visitsIncluded) {

  public PlanOffer {
    Objects.requireNonNull(name, "name");
    name = name.strip();
    if (name.isEmpty() || name.length() > 80) {
      throw new IllegalArgumentException("plan name must be 1-80 characters");
    }
    if (priceMinor < 0) {
      throw new IllegalArgumentException("price cannot be negative");
    }
    if (currency == null || !currency.matches("[A-Z]{3}")) {
      throw new IllegalArgumentException("currency must be an ISO-4217 code like EGP");
    }
    if (durationDays < 1 || durationDays > 3660) {
      throw new IllegalArgumentException("duration must be 1-3660 days");
    }
    if (visitsIncluded != null && visitsIncluded < 1) {
      throw new IllegalArgumentException("visits included must be positive or empty (unlimited)");
    }
  }
}
