package dev.gymscout.scouting.domain;

import java.util.List;

/** A ranked gym with the points per factor and a plain-language reason for each. */
public record ScoredGym(
    ScoutCandidate candidate,
    double score,
    double pricePoints,
    double travelPoints,
    double amenityPoints,
    List<String> reasons) {

  public ScoredGym {
    reasons = List.copyOf(reasons);
  }
}
