package dev.gymscout.memberships.application;

import dev.gymscout.memberships.domain.CostPerVisit;
import dev.gymscout.memberships.domain.Membership;
import dev.gymscout.places.Gym;
import dev.gymscout.places.GymDirectory;
import dev.gymscout.shared.NotFoundException;
import dev.gymscout.training.SessionLogged;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Starts memberships, records visits and works out what each visit cost. */
@Service
public class MembershipService {

  private static final Logger log = LoggerFactory.getLogger(MembershipService.class);

  private final MembershipStore store;
  private final GymDirectory gyms;
  private final Clock clock;

  public MembershipService(MembershipStore store, GymDirectory gyms, Clock clock) {
    this.store = store;
    this.gyms = gyms;
    this.clock = clock;
  }

  @Transactional
  public MembershipView start(Membership draft) {
    gyms.find(draft.gymId()).orElseThrow(() -> new NotFoundException("No gym " + draft.gymId()));
    var m =
        new Membership(
            UUID.randomUUID(),
            draft.gymId(),
            draft.planName(),
            draft.priceMinor(),
            draft.currency(),
            draft.startsOn(),
            draft.endsOn());
    store.save(m);
    return view(m);
  }

  @Transactional(readOnly = true)
  public Optional<MembershipView> current() {
    return store.activeOn(LocalDate.now(clock)).map(this::view);
  }

  @Transactional(readOnly = true)
  public List<MembershipView> all() {
    return store.all().stream().map(this::view).toList();
  }

  @Transactional
  public MembershipView addVisit(UUID membershipId, LocalDate on) {
    Membership m =
        store
            .find(membershipId)
            .orElseThrow(() -> new NotFoundException("No membership " + membershipId));
    LocalDate day = on == null ? LocalDate.now(clock) : on;
    if (!m.covers(day)) {
      throw new IllegalArgumentException("that day is outside the membership period");
    }
    store.addVisit(m.id(), day, null, "manual");
    return view(m);
  }

  /**
   * A logged workout at the member gym is a visit. Runs after the training transaction commits,
   * from the Modulith event registry, so a crash here is retried on restart and never loses the
   * session; the unique session id makes the retry harmless.
   */
  @ApplicationModuleListener
  void on(SessionLogged event) {
    if (event.gymId() == null) {
      return;
    }
    store
        .activeOn(event.performedOn())
        .filter(m -> m.gymId().equals(event.gymId()))
        .ifPresent(m -> {
          if (store.addVisit(m.id(), event.performedOn(), event.sessionId(), "session")) {
            log.info("Counted session {} as a visit", event.sessionId());
          }
        });
  }

  private MembershipView view(Membership m) {
    String name = gyms.find(m.gymId()).map(Gym::name).orElse("Unknown gym");
    var stats = CostPerVisit.of(m, store.visitCount(m.id()), LocalDate.now(clock));
    return new MembershipView(m, name, stats, store.visitDates(m.id()));
  }
}
