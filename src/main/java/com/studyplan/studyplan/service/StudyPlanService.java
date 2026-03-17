package com.studyplan.studyplan.service;
import com.studyplan.studyplan.algorithm.StudyPlanGenerator;
import com.studyplan.studyplan.dto.PlanRequest;
import com.studyplan.studyplan.dto.PlanResponse;
import com.studyplan.studyplan.model.*;
import com.studyplan.studyplan.repository.ExamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StudyPlanService {

    private final CourseService       courseService;
    private final ExamRepository      examRepository;
    private final StudyPlanGenerator  studyPlanGenerator;

    public PlanResponse generatePlan(PlanRequest request) {
        log.info("Generating study plan — courses: {}, exams: {}, hours/day: {}, window: {}",
                request.getCourses().size(),
                request.getExams().size(),
                request.getHoursPerDay(),
                request.getPreferredStudyTime());

        // ------------------------------------------------------------------
        // 1. Persist courses (reuse existing if already saved)
        // ------------------------------------------------------------------
        List<Course> courses = request.getCourses().stream()
                .map(input -> courseService.findOrCreate(input.getName()))
                .collect(Collectors.toList());

        // Build a fast lookup map: courseName → Course entity
        Map<String, Course> courseByName = courses.stream()
                .collect(Collectors.toMap(Course::getName, c -> c));

        // ------------------------------------------------------------------
        // 2. Persist exams, linking each to its course
        // ------------------------------------------------------------------
        List<Exam> exams = request.getExams().stream()
                .filter(input -> courseByName.containsKey(input.getCourse()))
                .map(input -> {
                    Course course = courseByName.get(input.getCourse());
                    Exam exam = Exam.builder()
                            .course(course)
                            .examDate(input.getDate())
                            .build();
                    return examRepository.save(exam);
                })
                .collect(Collectors.toList());

        // ------------------------------------------------------------------
        // 3. Build user preferences value object
        // ------------------------------------------------------------------
        UserPreferences preferences = UserPreferences.builder()
                .hoursPerDay(request.getHoursPerDay())
                .preferredStudyTime(request.getPreferredStudyTime())
                .build();

        // ------------------------------------------------------------------
        // 4. Run the scheduling algorithm
        // ------------------------------------------------------------------
        List<StudySession> sessions = studyPlanGenerator.generate(courses, exams, preferences);

        // ------------------------------------------------------------------
        // 5. Persist the study plan and its sessions
        // ------------------------------------------------------------------
        StudyPlan plan = StudyPlan.builder().build();

        // Link every session to the plan before saving
        sessions.forEach(s -> s.setStudyPlan(plan));
        plan.getSessions().addAll(sessions);

        // ------------------------------------------------------------------
        // 6. Map to response DTO grouped by date
        // ------------------------------------------------------------------
        return buildResponse(sessions);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private PlanResponse buildResponse(List<StudySession> sessions) {

        // Group sessions by date while preserving insertion order (already sorted)
        Map<java.time.LocalDate, List<StudySession>> byDay = new LinkedHashMap<>();
        for (StudySession s : sessions) {
            byDay.computeIfAbsent(s.getDate(), k -> new ArrayList<>()).add(s);
        }

        // Build DayPlan list
        List<PlanResponse.DayPlan> dayPlans = byDay.entrySet().stream()
                .map(entry -> {
                    List<PlanResponse.SessionInfo> sessionInfos = entry.getValue().stream()
                            .map(s -> PlanResponse.SessionInfo.builder()
                                    .course(s.getCourse().getName())
                                    .startTime(s.getStartTime())
                                    .duration(s.getDuration())
                                    .build())
                            .collect(Collectors.toList());

                    return PlanResponse.DayPlan.builder()
                            .date(entry.getKey())
                            .sessions(sessionInfos)
                            .build();
                })
                .collect(Collectors.toList());

        log.info("Study plan response built: {} days, {} total sessions",
                dayPlans.size(), sessions.size());

        return PlanResponse.builder().studyPlan(dayPlans).build();
    }
}
