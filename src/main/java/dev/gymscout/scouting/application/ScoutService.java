package dev.gymscout.scouting.application;

import dev.gymscout.geo.GeoPoint;
import dev.gymscout.places.Gym;
import dev.gymscout.places.GymDirectory;
import dev.gymscout.places.NearbyGym;
import dev.gymscout.scouting.domain.PerVisitPrice;
import dev.gymscout.scouting.domain.PlanOption;
import dev.gymscout.scouting.domain.ScoredGym;
import dev.gymscout.scouting.domain.Scorer;
import dev.gymscout.scouting.domain.ScoringWeights;
import dev.gymscout.scouting.domain.ScoutCandidate;
import dev.gymscout.scouting.domain.TravelEstimate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;

/** Finds gyms with PostGIS, asks the travel port how far they are, then scores them. */
@Service
public class ScoutService {

  private final GymDirectory directory;
  private final TravelTimes travelTimes;

  public ScoutService(GymDirectory directory, TravelTimes travelTimes) {
    this.directory = directory;
    this.travelTimes = travelTimes;
  }

  public ScoutReport near(ScoutQuery q) {
    List<NearbyGym> found = directory.nearest(q.origin(), q.radiusMeters(), q.limit());
    List<TravelEstimate> travel = travelTimes.from(q.origin(), locations(found), q.mode());
    return report(found, travel, q.visitsPerWeek(), q.wantedAmenities(), q.weights());
  }

  /**
   * Detour = home to gym + gym to work - home to work. Gym-to-work is approximated by work-to-gym,
   * which is close enough for ranking and halves the router calls.
   */
  public ScoutReport alongCommute(CommuteQuery q) {
    List<NearbyGym> found = directory.alongLine(q.home(), q.work(), q.corridorMeters(), q.limit());
    var targets = new ArrayList<>(locations(found));
    targets.add(q.work());
    List<TravelEstimate> fromHome = travelTimes.from(q.home(), targets, q.mode());
    List<TravelEstimate> fromWork = travelTimes.from(q.work(), locations(found), q.mode());
    TravelEstimate direct = fromHome.getLast();
    List<TravelEstimate> detours =
        IntStream.range(0, found.size())
            .mapToObj(i -> {
              TravelEstimate h = fromHome.get(i);
              TravelEstimate w = fromWork.get(i);
              return new TravelEstimate(
                  Math.max(0, h.meters() + w.meters() - direct.meters()),
                  Math.max(0, h.minutes() + w.minutes() - direct.minutes()),
                  h.estimated() || w.estimated() || direct.estimated());
            })
            .toList();
    return report(found, detours, q.visitsPerWeek(), q.wantedAmenities(), q.weights());
  }

  private ScoutReport report(
      List<NearbyGym> found,
      List<TravelEstimate> travel,
      double visitsPerWeek,
      Set<String> wanted,
      ScoringWeights weights) {
    var candidates =
        IntStream.range(0, found.size())
            .mapToObj(i -> candidate(found.get(i).gym(), travel.get(i), visitsPerWeek))
            .toList();
    Map<UUID, NearbyGym> byId =
        found.stream().collect(Collectors.toMap(n -> n.gym().id(), Function.identity()));
    List<ScoredGym> ranked = Scorer.rank(candidates, wanted, weights);
    var entries =
        ranked.stream()
            .map(r -> {
              NearbyGym n = byId.get(r.candidate().gymId());
              return new ScoutReport.Entry(r, n.gym().location(), n.distanceMeters());
            })
            .toList();
    return new ScoutReport(entries, travel.stream().anyMatch(TravelEstimate::estimated));
  }

  private static ScoutCandidate candidate(Gym gym, TravelEstimate travel, double visitsPerWeek) {
    var plans =
        gym.plans().stream()
            .map(p ->
                new PlanOption(
                    p.name(), p.priceMinor(), p.currency(), p.durationDays(), p.visitsIncluded()))
            .toList();
    return new ScoutCandidate(
        gym.id(),
        gym.name(),
        PerVisitPrice.best(plans, visitsPerWeek),
        travel,
        new HashSet<>(gym.amenities()));
  }

  private static List<GeoPoint> locations(List<NearbyGym> found) {
    return found.stream().map(n -> n.gym().location()).toList();
  }
}
