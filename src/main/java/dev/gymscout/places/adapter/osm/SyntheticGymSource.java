package dev.gymscout.places.adapter.osm;

import dev.gymscout.geo.GeoMath;
import dev.gymscout.geo.GeoPoint;
import dev.gymscout.places.application.GymSource;
import dev.gymscout.places.domain.GymCandidate;
import dev.gymscout.places.domain.PlanOffer;
import java.util.List;
import java.util.Set;

/**
 * Invented gyms around central Cairo with invented prices. The default source, so the app, the
 * tests and CI never call a public server. Names, prices and exact spots are made up.
 */
public final class SyntheticGymSource implements GymSource {

  private static final List<GymCandidate> GYMS =
      List.of(
          gym(
              "nile-strength-club",
              "Nile Strength Club",
              30.0478,
              31.2336,
              Set.of("weights", "cardio", "sauna"),
              "Mo-Su 06:00-24:00",
              monthly(1_200_00),
              pack(12, 900_00),
              yearly(11_000_00)),
          gym(
              "zamalek-iron-house",
              "Zamalek Iron House",
              30.0626,
              31.2197,
              Set.of("weights", "crossfit"),
              "Mo-Sa 07:00-23:00",
              monthly(1_500_00),
              pack(8, 800_00)),
          gym(
              "garden-city-swim-fit",
              "Garden City Swim & Fit",
              30.0359,
              31.2318,
              Set.of("pool", "cardio", "classes", "sauna"),
              "Mo-Su 06:00-22:00",
              monthly(2_400_00),
              yearly(22_000_00)),
          gym(
              "dokki-barbell-yard",
              "Dokki Barbell Yard",
              30.0385,
              31.2108,
              Set.of("weights", "parking"),
              "Mo-Su 08:00-02:00",
              monthly(750_00),
              pack(10, 450_00)),
          gym(
              "tahrir-training-rooms",
              "Tahrir Training Rooms",
              30.0441,
              31.2380,
              Set.of("weights", "cardio", "open-24h"),
              "24/7",
              monthly(950_00)),
          gym(
              "mohandessin-flex-arena",
              "Mohandessin Flex Arena",
              30.0561,
              31.2016,
              Set.of("weights", "cardio", "classes", "yoga", "women-only"),
              "Mo-Su 07:00-23:00",
              monthly(1_100_00),
              pack(12, 700_00)),
          gym(
              "pharaoh-fitness-lab",
              "Pharaoh Fitness Lab",
              30.0131,
              31.2089,
              Set.of("weights", "cardio", "accessible"),
              "Mo-Su 06:00-23:00",
              monthly(850_00),
              yearly(8_000_00)),
          gym(
              "maadi-move-studio",
              "Maadi Move Studio",
              29.9602,
              31.2569,
              Set.of("yoga", "classes", "parking"),
              "Mo-Sa 08:00-22:00",
              pack(8, 1_000_00),
              monthly(1_600_00)),
          gym(
              "heliopolis-pulse-gym",
              "Heliopolis Pulse Gym",
              30.0911,
              31.3225,
              Set.of("weights", "cardio", "pool", "parking"),
              "Mo-Su 06:00-24:00",
              monthly(1_800_00),
              yearly(16_500_00)),
          gym(
              "nasr-city-power-hub",
              "Nasr City Power Hub",
              30.0566,
              31.3301,
              Set.of("weights", "crossfit", "open-24h"),
              "24/7",
              monthly(1_000_00)),
          gym(
              "downtown-kettlebell-loft",
              "Downtown Kettlebell Loft",
              30.0505,
              31.2440,
              Set.of("weights", "classes"),
              null,
              pack(10, 600_00)),
          gym(
              "roda-island-rowing-gym",
              "Roda Island Rowing Gym",
              30.0201,
              31.2265,
              Set.of("cardio", "pool"),
              "Mo-Su 06:00-21:00"));

  @Override
  public String sourceName() {
    return "seed";
  }

  @Override
  public List<GymCandidate> fetchAround(GeoPoint center, int radiusMeters) {
    return GYMS.stream()
        .filter(g -> GeoMath.distanceMeters(center, g.location()) <= radiusMeters)
        .toList();
  }

  private static GymCandidate gym(
      String slug,
      String name,
      double lat,
      double lon,
      Set<String> amenities,
      String hours,
      PlanOffer... plans) {
    return new GymCandidate(
        "seed:" + slug, name, new GeoPoint(lat, lon), "Cairo", amenities, hours, List.of(plans));
  }

  private static PlanOffer monthly(long priceMinor) {
    return new PlanOffer("Monthly unlimited", priceMinor, "EGP", 30, null);
  }

  private static PlanOffer pack(int visits, long priceMinor) {
    return new PlanOffer(visits + "-visit pack", priceMinor, "EGP", 30, visits);
  }

  private static PlanOffer yearly(long priceMinor) {
    return new PlanOffer("Yearly unlimited", priceMinor, "EGP", 365, null);
  }
}
