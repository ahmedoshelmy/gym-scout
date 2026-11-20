package dev.gymscout.places;

import java.util.UUID;

/**
 * A price on offer at a gym. {@code visitsIncluded} is null for unlimited plans; money is in minor
 * units (piastres for EGP).
 */
public record MembershipPlan(
    UUID id,
    String name,
    long priceMinor,
    String currency,
    int durationDays,
    Integer visitsIncluded) {}
