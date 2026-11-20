package dev.gymscout.places.adapter.web;

import dev.gymscout.geo.GeoPoint;
import dev.gymscout.places.Gym;
import dev.gymscout.places.MembershipPlan;
import dev.gymscout.places.NearbyGym;
import dev.gymscout.places.application.ImportResult;
import dev.gymscout.places.application.PlacesService;
import dev.gymscout.places.domain.PlanOffer;
import dev.gymscout.shared.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/places")
@Tag(name = "Places", description = "Gyms on the map")
class PlacesController {

  private final PlacesService places;

  PlacesController(PlacesService places) {
    this.places = places;
  }

  @GetMapping("/gyms")
  @Operation(summary = "Gyms within a radius, nearest first (PostGIS KNN)")
  List<NearbyGym> nearby(
      @RequestParam double lat,
      @RequestParam double lon,
      @RequestParam(defaultValue = "5000") double radius,
      @RequestParam(defaultValue = "20") int limit) {
    return places.nearest(new GeoPoint(lat, lon), radius, limit);
  }

  @GetMapping("/gyms/{id}")
  Gym gym(@PathVariable UUID id) {
    return places.find(id).orElseThrow(() -> new NotFoundException("No gym " + id));
  }

  @PostMapping("/gyms/{id}/plans")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Record a price a gym offers (map data has no prices)")
  MembershipPlan addPlan(@PathVariable UUID id, @Valid @RequestBody PlanRequest body) {
    return places.addPlan(
        id,
        new PlanOffer(
            body.name(),
            body.priceMinor(),
            body.currency(),
            body.durationDays(),
            body.visitsIncluded()));
  }

  @PostMapping("/import")
  @Operation(summary = "Import gyms around a point from the configured source")
  ImportResult importAround(@Valid @RequestBody ImportRequest body) {
    return places.importAround(new GeoPoint(body.lat(), body.lon()), body.radiusMeters());
  }

  record PlanRequest(
      @NotBlank String name,
      @Min(0) long priceMinor,
      @NotBlank String currency,
      @Min(1) int durationDays,
      Integer visitsIncluded) {}

  record ImportRequest(
      @NotNull Double lat, @NotNull Double lon, @Min(100) @Max(10_000) int radiusMeters) {}
}
