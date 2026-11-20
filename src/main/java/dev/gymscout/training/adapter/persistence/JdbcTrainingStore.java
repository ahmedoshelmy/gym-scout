package dev.gymscout.training.adapter.persistence;

import dev.gymscout.training.application.SessionSummary;
import dev.gymscout.training.application.TrainingStore;
import dev.gymscout.training.domain.LoggedSet;
import dev.gymscout.training.domain.PlanItem;
import dev.gymscout.training.domain.WorkoutPlan;
import dev.gymscout.training.domain.WorkoutSet;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcTrainingStore implements TrainingStore {

  private final JdbcClient jdbc;

  JdbcTrainingStore(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void saveSession(
      UUID id,
      LocalDate performedOn,
      UUID gymId,
      UUID planId,
      String notes,
      List<WorkoutSet> sets) {
    jdbc.sql(
            """
            insert into workout_sessions (id, performed_on, gym_id, plan_id, notes)
            values (:id, :on, :gym, :plan, :notes)
            """)
        .param("id", id)
        .param("on", performedOn)
        .param("gym", gymId)
        .param("plan", planId)
        .param("notes", notes)
        .update();
    for (int i = 0; i < sets.size(); i++) {
      WorkoutSet s = sets.get(i);
      jdbc.sql(
              """
              insert into workout_sets (session_id, position, exercise, weight_kg, reps)
              values (:id, :pos, :exercise, :weight, :reps)
              """)
          .param("id", id)
          .param("pos", i)
          .param("exercise", s.exercise())
          .param("weight", s.weightKg())
          .param("reps", s.reps())
          .update();
    }
  }

  @Override
  public List<LoggedSet> allSetsChronological() {
    return jdbc.sql(
            """
            select s.id, s.performed_on, w.exercise, w.weight_kg, w.reps
            from workout_sets w join workout_sessions s on s.id = w.session_id
            order by s.performed_on, s.created_at, s.id, w.position
            """)
        .query((rs, n) ->
            new LoggedSet(
                rs.getObject("id", UUID.class),
                rs.getObject("performed_on", LocalDate.class),
                new WorkoutSet(
                    rs.getString("exercise"), rs.getBigDecimal("weight_kg"), rs.getInt("reps"))))
        .list();
  }

  @Override
  public List<SessionSummary> recentSessions(int limit) {
    return jdbc.sql(
            """
            select s.id, s.performed_on, s.gym_id, s.notes, count(w.*) as sets,
                   coalesce(sum(w.weight_kg * w.reps), 0) as volume
            from workout_sessions s left join workout_sets w on w.session_id = s.id
            group by s.id
            order by s.performed_on desc, s.created_at desc
            limit :limit
            """)
        .param("limit", limit)
        .query((rs, n) ->
            new SessionSummary(
                rs.getObject("id", UUID.class),
                rs.getObject("performed_on", LocalDate.class),
                rs.getObject("gym_id", UUID.class),
                rs.getString("notes"),
                rs.getInt("sets"),
                rs.getDouble("volume")))
        .list();
  }

  @Override
  public void savePlan(WorkoutPlan plan) {
    jdbc.sql("insert into workout_plans (id, name) values (:id, :name)")
        .param("id", plan.id())
        .param("name", plan.name())
        .update();
    for (int i = 0; i < plan.items().size(); i++) {
      PlanItem item = plan.items().get(i);
      jdbc.sql(
              """
              insert into workout_plan_items (plan_id, position, exercise, target_sets, target_reps)
              values (:plan, :pos, :exercise, :sets, :reps)
              """)
          .param("plan", plan.id())
          .param("pos", i)
          .param("exercise", item.exercise())
          .param("sets", item.targetSets())
          .param("reps", item.targetReps())
          .update();
    }
  }

  @Override
  public List<WorkoutPlan> plans() {
    return load(null);
  }

  @Override
  public Optional<WorkoutPlan> plan(UUID id) {
    return load(id).stream().findFirst();
  }

  private List<WorkoutPlan> load(UUID onlyId) {
    record Line(UUID id, String name, PlanItem item) {}
    var lines =
        jdbc.sql(
                """
                select p.id, p.name, i.exercise, i.target_sets, i.target_reps
                from workout_plans p join workout_plan_items i on i.plan_id = p.id
                where cast(:id as uuid) is null or p.id = :id
                order by p.created_at, p.id, i.position
                """)
            .param("id", onlyId, Types.OTHER)
            .query((rs, n) ->
                new Line(
                    rs.getObject("id", UUID.class),
                    rs.getString("name"),
                    new PlanItem(
                        rs.getString("exercise"),
                        rs.getInt("target_sets"),
                        rs.getInt("target_reps"))))
            .list();
    Map<UUID, List<PlanItem>> items = new LinkedHashMap<>();
    Map<UUID, String> names = new LinkedHashMap<>();
    for (Line line : lines) {
      names.putIfAbsent(line.id(), line.name());
      items.computeIfAbsent(line.id(), k -> new ArrayList<>()).add(line.item());
    }
    return names.entrySet().stream()
        .map(e -> new WorkoutPlan(e.getKey(), e.getValue(), items.get(e.getKey())))
        .toList();
  }
}
