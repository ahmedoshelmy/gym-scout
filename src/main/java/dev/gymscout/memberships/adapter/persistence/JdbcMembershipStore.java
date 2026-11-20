package dev.gymscout.memberships.adapter.persistence;

import dev.gymscout.memberships.application.MembershipStore;
import dev.gymscout.memberships.domain.Membership;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcMembershipStore implements MembershipStore {

  private static final String COLUMNS =
      "id, gym_id, plan_name, price_minor, currency, starts_on, ends_on";

  private final JdbcClient jdbc;

  JdbcMembershipStore(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void save(Membership m) {
    jdbc.sql(
            """
            insert into memberships (%s)
            values (:id, :gym, :plan, :price, :currency, :starts, :ends)
            """
                .formatted(COLUMNS))
        .param("id", m.id())
        .param("gym", m.gymId())
        .param("plan", m.planName())
        .param("price", m.priceMinor())
        .param("currency", m.currency())
        .param("starts", m.startsOn())
        .param("ends", m.endsOn())
        .update();
  }

  @Override
  public Optional<Membership> find(UUID id) {
    return jdbc.sql("select %s from memberships where id = :id".formatted(COLUMNS))
        .param("id", id)
        .query(JdbcMembershipStore::row)
        .optional();
  }

  @Override
  public Optional<Membership> activeOn(LocalDate day) {
    return jdbc.sql(
            """
            select %s from memberships
            where starts_on <= :day and ends_on >= :day
            order by starts_on desc limit 1
            """
                .formatted(COLUMNS))
        .param("day", day)
        .query(JdbcMembershipStore::row)
        .optional();
  }

  @Override
  public List<Membership> all() {
    return jdbc.sql("select %s from memberships order by starts_on desc".formatted(COLUMNS))
        .query(JdbcMembershipStore::row)
        .list();
  }

  @Override
  public boolean addVisit(UUID membershipId, LocalDate on, UUID sessionId, String source) {
    return jdbc.sql(
                """
                insert into membership_visits (id, membership_id, visited_on, session_id, source)
                values (:id, :membership, :on, :session, :source)
                on conflict (session_id) do nothing
                """)
            .param("id", UUID.randomUUID())
            .param("membership", membershipId)
            .param("on", on)
            .param("session", sessionId)
            .param("source", source)
            .update()
        == 1;
  }

  @Override
  public int visitCount(UUID membershipId) {
    return jdbc.sql("select count(*) from membership_visits where membership_id = :id")
        .param("id", membershipId)
        .query(Integer.class)
        .single();
  }

  @Override
  public List<LocalDate> visitDates(UUID membershipId) {
    return jdbc.sql(
            """
            select visited_on from membership_visits where membership_id = :id
            order by visited_on desc limit 60
            """)
        .param("id", membershipId)
        .query(LocalDate.class)
        .list();
  }

  private static Membership row(ResultSet rs, int n) throws SQLException {
    return new Membership(
        rs.getObject("id", UUID.class),
        rs.getObject("gym_id", UUID.class),
        rs.getString("plan_name"),
        rs.getLong("price_minor"),
        rs.getString("currency"),
        rs.getObject("starts_on", LocalDate.class),
        rs.getObject("ends_on", LocalDate.class));
  }
}
