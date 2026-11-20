package dev.gymscout.memberships.application;

import dev.gymscout.memberships.domain.Membership;
import dev.gymscout.memberships.domain.MembershipStats;
import java.time.LocalDate;
import java.util.List;

/** A membership with its gym's name, stats and visit dates (newest first). */
public record MembershipView(
    Membership membership, String gymName, MembershipStats stats, List<LocalDate> visits) {}
