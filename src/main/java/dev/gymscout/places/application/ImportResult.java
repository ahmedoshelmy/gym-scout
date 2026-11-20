package dev.gymscout.places.application;

/** What an import did: how many gyms the source returned and how many were stored. */
public record ImportResult(String source, int fetched, int stored) {}
