package dev.gymscout.training.application;

import java.time.LocalDate;
import java.util.UUID;

/** A logged session in a list. */
public record SessionSummary(
    UUID id, LocalDate performedOn, UUID gymId, String notes, int sets, double volumeKg) {}
