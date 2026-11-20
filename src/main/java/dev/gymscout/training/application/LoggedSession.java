package dev.gymscout.training.application;

import dev.gymscout.training.domain.PersonalRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Result of logging a session: its id and the records it set. */
public record LoggedSession(UUID id, LocalDate performedOn, List<PersonalRecord> records) {}
