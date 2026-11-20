package dev.gymscout.places;

/** A gym with its distance in metres from the search point (or from the commute line). */
public record NearbyGym(Gym gym, double distanceMeters) {}
