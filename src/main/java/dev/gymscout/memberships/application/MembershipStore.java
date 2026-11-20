package dev.gymscout.memberships.application;

import dev.gymscout.memberships.domain.Membership;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Storage port for memberships and visits. */
public interface MembershipStore {

  void save(Membership membership);

  Optional<Membership> find(UUID id);

  /** The latest-starting membership that covers {@code day}. */
  Optional<Membership> activeOn(LocalDate day);

  List<Membership> all();

  /** Records a visit; returns false if this session was already counted. */
  boolean addVisit(UUID membershipId, LocalDate on, UUID sessionId, String source);

  int visitCount(UUID membershipId);

  List<LocalDate> visitDates(UUID membershipId);
}
