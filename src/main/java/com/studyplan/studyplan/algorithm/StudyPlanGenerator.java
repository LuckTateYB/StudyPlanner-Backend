package com.studyplan.studyplan.algorithm;
import com.studyplan.studyplan.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class StudyPlanGenerator {

    // Priority formula weights (must sum to 1.0)
    private static final double DIFFICULTY_WEIGHT   = 0.5;
    private static final double PROXIMITY_WEIGHT    = 0.3;
    private static final double REMAINING_RATIO_WEIGHT   = 0.2;

    private static final double AVG_HOURS_RATIO = 0.75; // e.g. 4h/day → uses 3h for planning

    private static final int MIN_SESSIONS_PER_COURSE = 3;

    private static final double MAX_COURSE_SLOT_FRACTION = 0.40;
    /**
     * Maximum consecutive slots a single course may occupy within one day.
     * Prevents a high-priority course from monopolizing an entire day.
     */
    private static final int MAX_CONSECUTIVE_SLOTS_PER_COURSE_PER_DAY = 2;

    private final DifficultyAnalyzer difficultyAnalyzer;

    // =========================================================================
    // Public API
    // =========================================================================
    public List<StudySession> generate(
            List<Course> courses,
            List<Exam>   exams,
            UserPreferences preferences) {

        log.info("Starting study plan generation for {} courses, {} exams",
                courses.size(), exams.size());

        // -------------------------------------------------------------------
        // Step 1: Get AI difficulty estimates
        // -------------------------------------------------------------------
        List<String> courseNames = courses.stream().map(Course::getName).toList();
        Map<String, Integer> difficultyMap = difficultyAnalyzer.analyzeDifficulty(courseNames);

        // Update each Course entity with the AI-estimated difficulty
        for (Course course : courses) {
            int level = difficultyMap.getOrDefault(course.getName(), 3);
            course.setDifficultyLevel(level);
        }

        // -------------------------------------------------------------------
        // Step 2: Build a lookup map: courseName → nearest exam date
        // -------------------------------------------------------------------
        Map<String, LocalDate> examDateByCourseName = exams.stream()
                .collect(Collectors.toMap(
                        e -> e.getCourse().getName(),
                        Exam::getExamDate,
                        // If a course has multiple exams, keep the earliest one
                        (d1, d2) -> d1.isBefore(d2) ? d1 : d2
                ));

        // -------------------------------------------------------------------
        // Step 3: Calculate total sessions needed via weighted capacity model
        // -------------------------------------------------------------------
        LocalDate today = LocalDate.now();

        LocalDate planningEnd = examDateByCourseName.values().stream()
                .max(Comparator.naturalOrder())
                .orElse(today.plusDays(60));

        long totalDays  = ChronoUnit.DAYS.between(today, planningEnd);
        double avgHours = Math.max(2.0, preferences.getHoursPerDay() * AVG_HOURS_RATIO);
        int totalSlots  = (int) Math.round(totalDays * avgHours);

        log.info("Capacity model — totalDays: {}, avgHours/day: {}, totalSlots: {}",
                totalDays, avgHours, totalSlots);

// Compute urgency-weighted score for each active course
        Map<Course, Double> courseScore = new LinkedHashMap<>();
        double totalScore = 0.0;

        for (Course course : courses) {
            LocalDate examDate = examDateByCourseName.get(course.getName());

            // Skip courses whose exam has already passed
            if (examDate != null && !examDate.isAfter(today)) {
                log.debug("Skipping '{}' — exam date {} is in the past.", course.getName(), examDate);
                courseScore.put(course, 0.0);
                continue;
            }

            long daysUntilExam = (examDate != null)
                    ? ChronoUnit.DAYS.between(today, examDate)
                    : totalDays; // no exam → treat as furthest deadline

            // Avoid division by zero; floor at 1 day
            double urgency = 1.0 / Math.max(1, daysUntilExam);
            double score   = course.getDifficultyLevel() * urgency;

            courseScore.put(course, score);
            totalScore += score;
        }

// Distribute totalSlots proportionally; apply min and max guards
        Map<Course, Integer> totalSessionsNeeded = new LinkedHashMap<>();
        int maxSessionsPerCourse = (int) Math.round(totalSlots * MAX_COURSE_SLOT_FRACTION);

        for (Course course : courses) {
            double score    = courseScore.getOrDefault(course, 0.0);
            int    sessions = 0;

            if (score > 0 && totalScore > 0) {
                int raw = (int) Math.round((score / totalScore) * totalSlots);
                sessions = Math.min(maxSessionsPerCourse, Math.max(MIN_SESSIONS_PER_COURSE, raw));
            }

            totalSessionsNeeded.put(course, sessions);
            log.debug("Course '{}' → score={}, sessions={} (difficulty={}, exam={})",
                    course.getName(),
                    String.format("%.4f", score),
                    sessions,
                    course.getDifficultyLevel(),
                    examDateByCourseName.get(course.getName()));
        }


        // -------------------------------------------------------------------
        // Step 4: Distribute sessions across days, respecting deadlines
        // -------------------------------------------------------------------
        return distributeSessions(courses, totalSessionsNeeded, examDateByCourseName,
                preferences, today, planningEnd);
    }

    // Core scheduling engine — day-centric
    // =========================================================================

    private List<StudySession> distributeSessions(
            List<Course>           courses,
            Map<Course, Integer>   totalSessionsNeeded,
            Map<String, LocalDate> examDateByCourseName,
            UserPreferences        preferences,
            LocalDate              today,
            LocalDate              planningEnd) {

        // Mutable remaining-sessions tracker
        Map<Course, Integer> remainingSessions = new LinkedHashMap<>(totalSessionsNeeded);
        List<StudySession>   allSessions       = new ArrayList<>();
        // Conjunto de fechas bloqueadas: días en los que el usuario tiene algún examen
        Set<LocalDate> examDays = new HashSet<>(examDateByCourseName.values());
        for (LocalDate day = today.plusDays(1); !day.isAfter(planningEnd); day = day.plusDays(1)) {

            final LocalDate currentDay = day;
            // Si el día actual tiene un examen programado -> saltar, no se estudia ese día
            if (examDays.contains(currentDay)) {
                log.debug("Día {} bloqueado por examen — no se generan sesiones de estudio.", currentDay);
                continue;
            }

            // Courses still eligible on this day (have remaining sessions AND exam not yet passed)
            List<Course> eligible = courses.stream()
                    .filter(c -> remainingSessions.getOrDefault(c, 0) > 0)
                    .filter(c -> {
                        LocalDate examDate = examDateByCourseName.get(c.getName());
                        return examDate == null || currentDay.isBefore(examDate);
                    })
                    .collect(Collectors.toList());

            if (eligible.isEmpty()) {
                log.debug("No eligible courses for day {}", currentDay);
                continue;
            }

            // Dynamic daily capacity: avoids front-loading while respecting user's max
            int remainingDays = (int) Math.max(1, ChronoUnit.DAYS.between(currentDay, planningEnd));
            int globalRemaining = remainingSessions.values().stream().mapToInt(Integer::intValue).sum();

            int idealSlotsToday = (int) Math.ceil((double) globalRemaining / remainingDays);
            int slotsAvailable  = Math.min(preferences.getHoursPerDay(), Math.max(2, idealSlotsToday));

            log.debug("Day {} — globalRemaining={}, remainingDays={}, idealSlots={}, slotsAvailable={}",
                    currentDay, globalRemaining, remainingDays, idealSlotsToday, slotsAvailable);

            // Track how many slots each course has consumed TODAY (anti-concentration guard)
            Map<Course, Integer> slotsUsedToday = new LinkedHashMap<>();

            for (int slot = 0; slot < slotsAvailable; slot++) {

                // Re-filter: respect anti-concentration cap within this day
                List<Course> candidates = eligible.stream()
                        .filter(c -> remainingSessions.getOrDefault(c, 0) > 0)
                        .filter(c -> slotsUsedToday.getOrDefault(c, 0)
                                < MAX_CONSECUTIVE_SLOTS_PER_COURSE_PER_DAY)
                        .collect(Collectors.toList());

                if (candidates.isEmpty()) {
                    // All remaining eligible courses hit the daily cap — relax the cap
                    candidates = eligible.stream()
                            .filter(c -> remainingSessions.getOrDefault(c, 0) > 0)
                            .collect(Collectors.toList());
                }

                if (candidates.isEmpty()) break;

                // Select the best course for this slot via dynamic priority
                Course best = selectBestCourse(candidates, totalSessionsNeeded,
                        remainingSessions, examDateByCourseName, currentDay);

                // Build the session
                LocalTime startTime = preferences.getPreferredStudyTime()
                        .getStartTime()
                        .plusHours(slot);

                StudySession session = StudySession.builder()
                        .course(best)
                        .date(currentDay)
                        .startTime(startTime)
                        .duration(1)
                        .build();

                allSessions.add(session);

                // Update trackers
                remainingSessions.merge(best, -1, Integer::sum);
                slotsUsedToday.merge(best, 1, Integer::sum);

                // Remove from eligible if fully scheduled
                if (remainingSessions.getOrDefault(best, 0) <= 0) {
                    eligible.remove(best);
                }
            }
        }

        // Warn about any unscheduled sessions
        remainingSessions.forEach((course, remaining) -> {
            if (remaining > 0) {
                log.warn("Course '{}' has {} unscheduled sessions after planning horizon.",
                        course.getName(), remaining);
            }
        });

        // Final sort: chronological by date then start time
        allSessions.sort(Comparator.comparing(StudySession::getDate)
                .thenComparing(StudySession::getStartTime));

        log.info("Study plan generated: {} total sessions across {} days",
                allSessions.size(),
                allSessions.stream().map(StudySession::getDate).distinct().count());

        return allSessions;
    }

    // =========================================================================
    // Dynamic priority calculation
    // =========================================================================

    /**
     * Scores a course for a specific scheduling slot.
     *
     * <pre>
     * priority = (difficulty * 0.5)
     *          + (examProximity * 0.3)
     *          + (remainingRatio * 0.2)
     * </pre>
     *
     * <ul>
     *   <li><b>difficulty</b>: normalized to [0,1] from the 1–5 scale.</li>
     *   <li><b>examProximity</b>: 1 / daysUntilExam — spikes as deadline approaches.</li>
     *   <li><b>remainingRatio</b>: sessionsRemaining / sessionsTotal — keeps partially
     *       completed courses visible even when the exam is distant.</li>
     * </ul>
     */
    private Course selectBestCourse(
            List<Course>           candidates,
            Map<Course, Integer>   totalSessionsNeeded,
            Map<Course, Integer>   remainingSessions,
            Map<String, LocalDate> examDateByCourseName,
            LocalDate              today) {

        Course bestCourse   = null;
        double bestPriority = Double.NEGATIVE_INFINITY;

        for (Course course : candidates) {
            double priority = computePriority(course, totalSessionsNeeded, remainingSessions,
                    examDateByCourseName, today);
            log.trace("Course '{}' priority on {}: {}", course.getName(), today, priority);

            if (priority > bestPriority) {
                bestPriority = priority;
                bestCourse   = course;
            }
        }

        return bestCourse;
    }

    private double computePriority(
            Course                 course,
            Map<Course, Integer>   totalSessionsNeeded,
            Map<Course, Integer>   remainingSessions,
            Map<String, LocalDate> examDateByCourseName,
            LocalDate              today) {

        // 1. Difficulty component — normalized to [0.2, 1.0]
        double normalizedDifficulty = course.getDifficultyLevel() / 5.0;

        // 2. Exam proximity component
        double examProximity = 0.0;
        LocalDate examDate = examDateByCourseName.get(course.getName());
        if (examDate != null) {
            long daysUntilExam = ChronoUnit.DAYS.between(today, examDate);
            if (daysUntilExam > 0) {
                examProximity = 1.0 / daysUntilExam;
            } else {
                examProximity = 1.0; // exam is today or past
            }
        }

        // 3. Remaining-sessions ratio component
        int total     = totalSessionsNeeded.getOrDefault(course, 1);
        int remaining = remainingSessions.getOrDefault(course, 0);
        double remainingRatio = total > 0 ? (double) remaining / total : 0.0;

        return (normalizedDifficulty * DIFFICULTY_WEIGHT)
                + (examProximity        * PROXIMITY_WEIGHT)
                + (remainingRatio       * REMAINING_RATIO_WEIGHT);
    }
}
