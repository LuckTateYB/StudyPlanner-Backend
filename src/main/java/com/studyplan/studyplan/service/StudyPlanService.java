package com.studyplan.studyplan.service;

import com.studyplan.studyplan.algorithm.StudyPlanGenerator;
import com.studyplan.studyplan.dto.PlanRequest;
import com.studyplan.studyplan.dto.PlanResponse;
import com.studyplan.studyplan.model.*;
import com.studyplan.studyplan.repository.ExamRepository;
import com.studyplan.studyplan.repository.StudyPlanRepository;
import com.studyplan.studyplan.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StudyPlanService {

    private final CourseService      courseService;
    private final ExamRepository     examRepository;
    private final StudyPlanGenerator studyPlanGenerator;
    private final StudyPlanRepository studyPlanRepository;
    private final UserRepository     userRepository;

    public PlanResponse generatePlan(PlanRequest request) {
        log.info("Generating plan — userId={}, courses={}, hours/day={}",
                request.getUserId(), request.getCourses().size(), request.getHoursPerDay());

        // ------------------------------------------------------------------
        // 1. Resolver el usuario desde el userId del request.
        //    Cuando se implemente JWT, esta línea se reemplaza por:
        //    User user = (User) SecurityContextHolder.getContext()
        //                       .getAuthentication().getPrincipal();
        // ------------------------------------------------------------------
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException(
                        "User not found with id: " + request.getUserId()));

        // ------------------------------------------------------------------
        // 2. Persistir cursos asociados al usuario
        // ------------------------------------------------------------------
        List<Course> courses = request.getCourses().stream()
                .map(input -> courseService.findOrCreate(input.getName(), user))
                .collect(Collectors.toList());

        Map<String, Course> courseByName = courses.stream()
                .collect(Collectors.toMap(Course::getName, c -> c));


        // ------------------------------------------------------------------
        // 3. Persistir exámenes vinculados a sus cursos
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
        // 4. Construir preferencias del usuario
        // ------------------------------------------------------------------
        UserPreferences preferences = UserPreferences.builder()
                .hoursPerDay(request.getHoursPerDay())
                .preferredStudyTime(request.getPreferredStudyTime())
                .build();
        // ------------------------------------------------------------------
        // 5. Ejecutar el algoritmo de planificación
        // ------------------------------------------------------------------
        List<StudySession> sessions = studyPlanGenerator.generate(courses, exams, preferences);

        // ------------------------------------------------------------------
        // 6. Persistir el plan vinculado al usuario.
        //    UNIQUE sobre user_id garantiza un solo plan activo por usuario.
        //    Si ya existe uno, se elimina antes de guardar el nuevo.
        // ------------------------------------------------------------------
        studyPlanRepository.findByUser(user).ifPresent(studyPlanRepository::delete);

        StudyPlan plan = StudyPlan.builder()
                .user(user)
                .dailyAvailableHours(request.getHoursPerDay())
                .startDate(LocalDate.now())
                .endDate(sessions.isEmpty() ? LocalDate.now()
                        : sessions.get(sessions.size() - 1).getDate())
                .build();

        sessions.forEach(s -> s.setStudyPlan(plan));
        plan.getSessions().addAll(sessions);
        studyPlanRepository.save(plan);

        // ------------------------------------------------------------------
        // 7. Construir y retornar el response agrupado por día
        // ------------------------------------------------------------------
        return buildResponse(sessions);
    }

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
