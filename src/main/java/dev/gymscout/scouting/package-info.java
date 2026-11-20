/**
 * Scouting: ranks gyms near a point or along a home-to-work commute by price per visit, travel
 * minutes and amenities, with the reason for every point awarded. Travel times come from a {@link
 * dev.gymscout.scouting.application.TravelTimes} port (straight-line estimate by default, OSRM when
 * enabled).
 */
@ApplicationModule(displayName = "Scouting")
package dev.gymscout.scouting;

import org.springframework.modulith.ApplicationModule;
