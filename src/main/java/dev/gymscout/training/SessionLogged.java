package dev.gymscout.training;

import java.time.LocalDate;
import java.util.UUID;

/** A workout was logged; {@code gymId} is null for home or outdoor sessions. */
public record SessionLogged(UUID sessionId, UUID gymId, LocalDate performedOn) {}
