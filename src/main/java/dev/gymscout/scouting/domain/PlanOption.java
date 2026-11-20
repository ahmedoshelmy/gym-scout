package dev.gymscout.scouting.domain;

/** A plan as the scorer sees it; {@code visitsIncluded} null means unlimited. */
public record PlanOption(
    String name, long priceMinor, String currency, int durationDays, Integer visitsIncluded) {}
