package dev.gymscout.training.application;

import dev.gymscout.training.domain.ExerciseBest;
import dev.gymscout.training.domain.PersonalRecord;
import dev.gymscout.training.domain.WeekVolume;
import java.util.List;

/** Weekly volume, best numbers per exercise and the latest personal records (newest first). */
public record Progress(
    List<WeekVolume> weeklyVolume, List<ExerciseBest> bests, List<PersonalRecord> recentRecords) {}
