package dev.gymscout.training.domain;

import java.time.LocalDate;

/** Total load moved (kg x reps) in the ISO week starting on {@code weekStart} (a Monday). */
public record WeekVolume(LocalDate weekStart, double volumeKg, int sets) {}
