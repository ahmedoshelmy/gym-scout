package dev.gymscout.places.adapter.demo;

import dev.gymscout.geo.GeoPoint;
import dev.gymscout.places.adapter.osm.SyntheticGymSource;
import dev.gymscout.places.application.PlacesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Loads the invented Cairo gyms into an empty database so the map is never blank. */
@Component
@ConditionalOnProperty(name = "gymscout.demo-data", havingValue = "true", matchIfMissing = true)
class DemoSeeder {

  private static final Logger log = LoggerFactory.getLogger(DemoSeeder.class);
  private static final GeoPoint CAIRO = new GeoPoint(30.0444, 31.2357);

  private final PlacesService places;

  DemoSeeder(PlacesService places) {
    this.places = places;
  }

  @EventListener(ApplicationReadyEvent.class)
  void seed() {
    if (places.isEmpty()) {
      var result = places.importFrom(new SyntheticGymSource(), CAIRO, 30_000);
      log.info("Seeded {} invented demo gyms around Cairo", result.stored());
    }
  }
}
