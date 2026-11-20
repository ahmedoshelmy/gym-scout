package dev.gymscout.memberships.domain;

/**
 * How the membership is paying off. {@code costPerVisitMinor} is price over visits so far (the full
 * price while there are none); {@code projected*} extrapolates the current pace to the end date.
 */
public record MembershipStats(
    int visits,
    long daysElapsed,
    long totalDays,
    long costPerVisitMinor,
    double projectedVisits,
    long projectedCostPerVisitMinor,
    double visitsPerWeek) {}
