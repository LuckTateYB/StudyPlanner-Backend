package com.studyplan.studyplan.service;
import com.studyplan.studyplan.dto.PlanRequest;
import com.studyplan.studyplan.model.*;
import com.studyplan.studyplan.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StuddySessionService {

    private final StudySessionRepository repository;
    private final CourseRepository courseRepository;

    public StudySession updateSession(Long id, PlanRequest.UpdateSessionRequest request) {

        StudySession session = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (request.getDate() != null) {
            session.setDate(request.getDate());
        }

        if (request.getStartTime() != null) {
            session.setStartTime(request.getStartTime());
        }

        if (request.getDuration() != null) {
            session.setDuration(request.getDuration());
        }

        if (request.getCourseId() != null) {
            Course course = courseRepository.findById(request.getCourseId())
                    .orElseThrow();
            session.setCourse(course);
        }

        return repository.save(session);
    }
}
