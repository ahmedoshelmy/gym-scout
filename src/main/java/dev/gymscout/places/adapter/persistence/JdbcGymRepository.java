package dev.gymscout.places.adapter.persistence;

import dev.gymscout.geo.GeoPoint;
import dev.gymscout.places.Gym;
import dev.gymscout.places.MembershipPlan;
import dev.gymscout.places.NearbyGym;
import dev.gymscout.places.application.GymRepository;
import dev.gymscout.places.domain.GymCandidate;
import dev.gymscout.places.domain.PlanOffer;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * PostGIS-backed gym storage. Points are {@code geography(Point,4326)}, so {@code ST_DWithin} and
 * {@code ST_Distance} work in metres; {@code <->} gives index-assisted nearest-first ordering
 * through the GiST index.
 */
@Repository
class JdbcGymRepository implements GymRepository {

  private static final String POINT = "ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography";

  private static final String COLUMNS =
      """
      g.id, g.name, g.source, g.address, g.amenities, g.opening_hours,
      ST_Y(g.location::geometry) as lat, ST_X(g.location::geometry) as lon
      """;

  private final JdbcClient jdbc;

  JdbcGymRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public UUID upsert(GymCandidate c, String source) {
    return jdbc.sql(
            """
            insert into gyms (id, name, source, external_ref, location, address, amenities,
                              opening_hours)
            values (:id, :name, :source, :ref, %s, :address, :amenities, :hours)
            on conflict (external_ref) do update
              set name = excluded.name, location = excluded.location, address = excluded.address,
                  amenities = excluded.amenities, opening_hours = excluded.opening_hours
            returning id
            """
                .formatted(POINT))
        .param("id", UUID.randomUUID())
        .param("name", c.name())
        .param("source", source)
        .param("ref", c.externalRef())
        .param("lat", c.location().lat())
        .param("lon", c.location().lon())
        .param("address", c.address())
        .param("amenities", c.amenities().stream().sorted().toArray(String[]::new))
        .param("hours", c.openingHours())
        .query(UUID.class)
        .single();
  }

  @Override
  public Optional<MembershipPlan> addPlan(UUID gymId, PlanOffer offer) {
    UUID id = UUID.randomUUID();
    int inserted =
        jdbc.sql(
                """
                insert into membership_plans
                  (id, gym_id, name, price_minor, currency, duration_days, visits_included)
                select :id, :gym, :name, :price, :currency, :days, :visits
                where not exists (
                  select 1 from membership_plans where gym_id = :gym and lower(name) = lower(:name))
                """)
            .param("id", id)
            .param("gym", gymId)
            .param("name", offer.name())
            .param("price", offer.priceMinor())
            .param("currency", offer.currency())
            .param("days", offer.durationDays())
            .param("visits", offer.visitsIncluded(), Types.INTEGER)
            .update();
    return inserted == 0
        ? Optional.empty()
        : Optional.of(
            new MembershipPlan(
                id,
                offer.name(),
                offer.priceMinor(),
                offer.currency(),
                offer.durationDays(),
                offer.visitsIncluded()));
  }

  @Override
  public List<NearbyGym> nearest(GeoPoint point, double radiusMeters, int limit) {
    var rows =
        jdbc.sql(
                """
                select %s, ST_Distance(g.location, %s) as distance
                from gyms g
                where ST_DWithin(g.location, %s, :radius)
                order by g.location <-> %s, g.name
                limit :limit
                """
                    .formatted(COLUMNS, POINT, POINT, POINT))
            .param("lat", point.lat())
            .param("lon", point.lon())
            .param("radius", radiusMeters)
            .param("limit", limit)
            .query(JdbcGymRepository::row)
            .list();
    return withPlans(rows);
  }

  @Override
  public List<NearbyGym> alongLine(GeoPoint a, GeoPoint b, double corridorMeters, int limit) {
    var rows =
        jdbc.sql(
                """
                with line as (
                  select ST_MakeLine(ST_SetSRID(ST_MakePoint(:aLon, :aLat), 4326),
                                     ST_SetSRID(ST_MakePoint(:bLon, :bLat), 4326))::geography as g)
                select %s, ST_Distance(g.location, line.g) as distance
                from gyms g, line
                where ST_DWithin(g.location, line.g, :corridor)
                order by distance, g.name
                limit :limit
                """
                    .formatted(COLUMNS))
            .param("aLat", a.lat())
            .param("aLon", a.lon())
            .param("bLat", b.lat())
            .param("bLon", b.lon())
            .param("corridor", corridorMeters)
            .param("limit", limit)
            .query(JdbcGymRepository::row)
            .list();
    return withPlans(rows);
  }

  @Override
  public List<Gym> findAll(List<UUID> ids) {
    var rows =
        jdbc.sql("select %s, 0.0 as distance from gyms g where g.id in (:ids)".formatted(COLUMNS))
            .param("ids", ids)
            .query(JdbcGymRepository::row)
            .list();
    return withPlans(rows).stream().map(NearbyGym::gym).toList();
  }

  @Override
  public long count() {
    return jdbc.sql("select count(*) from gyms").query(Long.class).single();
  }

  private List<NearbyGym> withPlans(List<Row> rows) {
    if (rows.isEmpty()) {
      return List.of();
    }
    Map<UUID, List<MembershipPlan>> plans =
        jdbc
            .sql(
                """
                select id, gym_id, name, price_minor, currency, duration_days, visits_included
                from membership_plans where gym_id in (:ids)
                order by price_minor, name
                """)
            .param("ids", rows.stream().map(Row::id).toList())
            .query((rs, n) ->
                Map.entry(
                    rs.getObject("gym_id", UUID.class),
                    new MembershipPlan(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getLong("price_minor"),
                        rs.getString("currency"),
                        rs.getInt("duration_days"),
                        rs.getObject("visits_included", Integer.class))))
            .list()
            .stream()
            .collect(
                Collectors.groupingBy(
                    Map.Entry::getKey,
                    LinkedHashMap::new,
                    Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    return rows.stream()
        .map(r -> new NearbyGym(r.toGym(plans.getOrDefault(r.id(), List.of())), r.distance()))
        .toList();
  }

  private static Row row(ResultSet rs, int rowNum) throws SQLException {
    return new Row(
        rs.getObject("id", UUID.class),
        rs.getString("name"),
        rs.getString("source"),
        rs.getString("address"),
        strings(rs.getArray("amenities")),
        rs.getString("opening_hours"),
        new GeoPoint(rs.getDouble("lat"), rs.getDouble("lon")),
        rs.getDouble("distance"));
  }

  private static List<String> strings(Array array) throws SQLException {
    return array == null ? List.of() : Arrays.asList((String[]) array.getArray());
  }

  private record Row(
      UUID id,
      String name,
      String source,
      String address,
      List<String> amenities,
      String openingHours,
      GeoPoint location,
      double distance) {

    Gym toGym(List<MembershipPlan> plans) {
      return new Gym(id, name, location, address, amenities, openingHours, source, plans);
    }
  }
}
