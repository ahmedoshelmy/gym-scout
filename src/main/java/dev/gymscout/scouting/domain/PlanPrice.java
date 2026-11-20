package dev.gymscout.scouting.domain;

/** The cheapest way to pay for one visit at a gym, given how often the user trains. */
public record PlanPrice(String planName, long perVisitMinor, String currency, double visitsUsed) {}
