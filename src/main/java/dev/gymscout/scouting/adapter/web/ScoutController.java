package dev.gymscout.scouting.adapter.web;

import dev.gymscout.geo.GeoPoint;
import dev.gymscout.places.AmenityTags;
import dev.gymscout.scouting.application.CommuteQuery;
import dev.gymscout.scouting.application.ScoutQuery;
import dev.gymscout.scouting.application.ScoutReport;
import dev.gymscout.scouting.application.ScoutService;
import dev.gymscout.scouting.domain.ScoringWeights;
import dev.gymscout.scouting.domain.TravelMode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scout")
@Tag(name = "Scouting", description = "Ranked, explained gym suggestions")
class ScoutController {

  private final ScoutService scout;

  ScoutController(ScoutService scout) {
    this.scout = scout;
  }

  @GetMapping
  @Operation(summary = "Rank gyms near a point by price per visit, travel time and amenities")
  ScoutResponse near(
      @RequestParam double lat,
      @RequestParam double lon,
      @RequestParam(defaultValue = "5000") double radius,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "3") double visitsPerWeek,
      @RequestParam(defaultValue = "") List<String> amenities,
      @RequestParam(defaultValue = "DRIVE") TravelMode mode) {
    return ScoutResponse.of(
        scout.near(
            new ScoutQuery(
                new GeoPoint(lat, lon),
                radius,
                limit,
                visitsPerWeek,
                wanted(amenities),
                mode,
                ScoringWeights.DEFAULT)));
  }

  @GetMapping("/commute")
  @Operation(summary = "Rank gyms along home to work by the detour they add")
  ScoutResponse commute(
      @RequestParam double homeLat,
      @RequestParam double homeLon,
      @RequestParam double workLat,
      @RequestParam double workLon,
      @RequestParam(defaultValue = "1500") double corridor,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "3") double visitsPerWeek,
      @RequestParam(defaultValue = "") List<String> amenities,
      @RequestParam(defaultValue = "DRIVE") TravelMode mode) {
    return ScoutResponse.of(
        scout.alongCommute(
            new CommuteQuery(
                new GeoPoint(homeLat, homeLon),
                new GeoPoint(workLat, workLon),
                corridor,
                limit,
                visitsPerWeek,
                wanted(amenities),
                mode,
                ScoringWeights.DEFAULT)));
  }

  private static Set<String> wanted(List<String> raw) {
    return AmenityTags.normalize(raw);
  }

  record ScoutResponse(boolean travelEstimated, List<Result> results) {

    static ScoutResponse of(ScoutReport report) {
      return new ScoutResponse(
          report.travelEstimated(), report.results().stream().map(Result::of).toList());
    }
  }

  record Result(
      UUID gymId,
      String name,
      double lat,
      double lon,
      double distanceMeters,
      double score,
      double pricePoints,
      double travelPoints,
      double amenityPoints,
      Long perVisitMinor,
      String currency,
      String bestPlan,
      double travelMinutes,
      boolean travelEstimated,
      List<String> amenities,
      List<String> reasons) {

    static Result of(ScoutReport.Entry e) {
      var r = e.ranked();
      var c = r.candidate();
      var price = c.price();
      return new Result(
          c.gymId(),
          c.name(),
          e.location().lat(),
          e.location().lon(),
          Math.round(e.distanceMeters()),
          r.score(),
          r.pricePoints(),
          r.travelPoints(),
          r.amenityPoints(),
          price.map(p -> p.perVisitMinor()).orElse(null),
          price.map(p -> p.currency()).orElse(null),
          price.map(p -> p.planName()).orElse(null),
          Math.round(c.travel().minutes() * 10) / 10.0,
          c.travel().estimated(),
          c.amenities().stream().sorted().toList(),
          r.reasons());
    }
  }
}
