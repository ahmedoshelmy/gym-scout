package dev.gymscout.training.domain;

/** Best numbers so far for one exercise. */
public record ExerciseBest(
    String exercise, double heaviestKg, double bestOneRepMax, int sessions) {}
