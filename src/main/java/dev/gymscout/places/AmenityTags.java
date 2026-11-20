package dev.gymscout.places;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Turns OpenStreetMap tags into the small amenity vocabulary the scorer understands. OSM tagging is
 * free-form, so this only trusts well-known keys and ignores the rest.
 */
public final class AmenityTags {

  /** Everything the scorer can match on. */
  public static final Set<String> VOCABULARY =
      Set.of(
          "weights",
          "cardio",
          "pool",
          "sauna",
          "classes",
          "yoga",
          "crossfit",
          "women-only",
          "open-24h",
          "parking",
          "accessible");

  private AmenityTags() {}

  public static Set<String> fromOsm(Map<String, String> tags) {
    var found = new TreeSet<String>();
    for (String sport : tags.getOrDefault("sport", "").split(";")) {
      switch (sport.strip()) {
        case "swimming" -> found.add("pool");
        case "fitness", "weightlifting", "powerlifting", "bodybuilding" -> found.add("weights");
        case "crossfit" -> found.add("crossfit");
        case "yoga" -> found.add("yoga");
        case "aerobics", "pilates", "zumba" -> found.add("classes");
        default -> {
          // unknown sports are not amenities we score
        }
      }
    }
    if ("yes".equals(tags.get("sauna"))) {
      found.add("sauna");
    }
    if ("yes".equals(tags.get("swimming_pool"))) {
      found.add("pool");
    }
    if ("yes".equals(tags.get("female")) || "only".equals(tags.get("female"))) {
      found.add("women-only");
    }
    if ("24/7".equals(tags.get("opening_hours"))) {
      found.add("open-24h");
    }
    if ("yes".equals(tags.get("wheelchair"))) {
      found.add("accessible");
    }
    if (tags.containsKey("parking") && !"no".equals(tags.get("parking"))) {
      found.add("parking");
    }
    return found;
  }

  /** Keeps only known amenity names, lower-cased; used for user input too. */
  public static Set<String> normalize(Iterable<String> raw) {
    var clean = new TreeSet<String>();
    for (String value : raw) {
      String key = value.strip().toLowerCase(Locale.ROOT);
      if (VOCABULARY.contains(key)) {
        clean.add(key);
      }
    }
    return clean;
  }
}
