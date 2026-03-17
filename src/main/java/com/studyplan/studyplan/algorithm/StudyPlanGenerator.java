package com.studyplan.studyplan.algorithm;
import com.studyplan.studyplan.model.*;
import com.studyplan.studyplan.model.enums.StudyTimeWindow;
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
    private static final double DIFFICULTY_WEIGHT   = 0.6;
    private static final double PROXIMITY_WEIGHT    = 0.4;

    /** Base multiplier: sessions per unit of difficulty level. */
    private static final int SESSIONS_PER_DIFFICULTY = 2;

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
        // Step 3: Calculate priority for each course and sort
        // -------------------------------------------------------------------
        LocalDate today = LocalDate.now();
        List<PrioritizedCourse> prioritized = courses.stream()
                .map(course -> {
                    double priority = calculatePriority(course, examDateByCourseName, today);
                    return new PrioritizedCourse(course, priority,
                            examDateByCourseName.get(course.getName()));
                })
                .sorted(Comparator.comparingDouble(PrioritizedCourse::priority).reversed())
                .toList();

        log.debug("Prioritized courses: {}",
                prioritized.stream().map(p -> p.course().getName() + "=" + p.priority()).toList());

        // -------------------------------------------------------------------
        // Step 4: Determine sessions needed per course (based on difficulty)
        // -------------------------------------------------------------------
        Map<Course, Integer> sessionsNeeded = new LinkedHashMap<>();
        for (PrioritizedCourse pc : prioritized) {
            int sessions = pc.course().getDifficultyLevel() * SESSIONS_PER_DIFFICULTY;
            sessionsNeeded.put(pc.course(), sessions);
            log.debug("Course '{}' needs {} sessions (difficulty {})",
                    pc.course().getName(), sessions, pc.course().getDifficultyLevel());
        }

        // -------------------------------------------------------------------
        // Step 5 & 6: Distribute sessions across days, respecting deadlines
        // -------------------------------------------------------------------
        return distributeSessions(prioritized, sessionsNeeded, preferences, today);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================
    private double calculatePriority(
            Course course,
            Map<String, LocalDate> examDateByCourseName,
            LocalDate today) {

        double difficulty = course.getDifficultyLevel();

        LocalDate examDate = examDateByCourseName.get(course.getName());
        double examProximity = 0.0;
        if (examDate != null) {
            long daysUntilExam = ChronoUnit.DAYS.between(today, examDate);
            if (daysUntilExam > 0) {
                // Closer exam → higher proximity score
                examProximity = 1.0 / daysUntilExam;
            } else {
                // Exam is today or already past → high proximity
                examProximity = 1.0;
            }
        }

        return (difficulty * DIFFICULTY_WEIGHT) + (examProximity * PROXIMITY_WEIGHT);
    }

    private List<StudySession> distributeSessions(
            List<PrioritizedCourse> prioritized,
            Map<Course, Integer>    sessionsNeeded,
            UserPreferences         preferences,
            LocalDate               today) {

        // day → number of session slots already occupied
        Map<LocalDate, Integer> slotUsedByDay = new LinkedHashMap<>();
        List<StudySession> allSessions = new ArrayList<>();

        for (PrioritizedCourse pc : prioritized) {
            Course    course      = pc.course();
            LocalDate deadline    = pc.examDate() != null ? pc.examDate() : today.plusDays(60);
            int       totalNeeded = sessionsNeeded.get(course);

            // Find days in [today+1 ... deadline-1] and schedule sessions
            LocalDate day = today.plusDays(1);
            int scheduledCount = 0;

            while (scheduledCount < totalNeeded && day.isBefore(deadline)) {
                int usedToday = slotUsedByDay.getOrDefault(day, 0);

                if (usedToday < preferences.getHoursPerDay()) {
                    // Calculate start time: base start + offset for each occupied slot
                    LocalTime startTime = preferences.getPreferredStudyTime()
                            .getStartTime()
                            .plusHours(usedToday);

                    // Build and record the session
                    StudySession session = StudySession.builder()
                            .course(course)
                            .date(day)
                            .startTime(startTime)
                            .duration(1)
                            .build();

                    allSessions.add(session);
                    slotUsedByDay.put(day, usedToday + 1);
                    scheduledCount++;
                }

                // Move to next day when current day is full
                if (slotUsedByDay.getOrDefault(day, 0) >= preferences.getHoursPerDay()) {
                    day = day.plusDays(1);
                }
            }

            if (scheduledCount < totalNeeded) {
                log.warn("Could only schedule {}/{} sessions for '{}' before exam deadline {}",
                        scheduledCount, totalNeeded, course.getName(), deadline);
            }
        }

        // Sort all sessions by date then start time for a clean chronological output
        allSessions.sort(Comparator.comparing(StudySession::getDate)
                .thenComparing(StudySession::getStartTime));

        log.info("Study plan generated: {} total sessions across {} days",
                allSessions.size(),
                allSessions.stream().map(StudySession::getDate).distinct().count());

        return allSessions;
    }

    // -------------------------------------------------------------------------
    // Inner record used only within the algorithm
    // -------------------------------------------------------------------------
    private record PrioritizedCourse(Course course, double priority, LocalDate examDate) {}

}
