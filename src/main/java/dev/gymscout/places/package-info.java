/**
 * Places: gyms as PostGIS points with amenities, opening hours and membership plans, imported from
 * a {@link dev.gymscout.places.application.GymSource} (synthetic by default, OpenStreetMap Overpass
 * when enabled). Other modules query through {@link dev.gymscout.places.GymDirectory}.
 */
@ApplicationModule(displayName = "Places")
package dev.gymscout.places;

import org.springframework.modulith.ApplicationModule;
