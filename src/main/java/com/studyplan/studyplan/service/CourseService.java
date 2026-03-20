package com.studyplan.studyplan.service;
import com.studyplan.studyplan.model.Course;
import com.studyplan.studyplan.model.User;
import com.studyplan.studyplan.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;

    public Course findOrCreate(String name, User user) {
        return courseRepository.findByNameAndUser(name, user).orElseGet(() -> {
            log.debug("Creating new course '{}' for userId={}", name, user.getId());
            Course newCourse = Course.builder()
                    .name(name)
                    .user(user)
                    .difficultyLevel(3) // valor por defecto; el algoritmo lo ajusta via IA
                    .build();
            return courseRepository.save(newCourse);
        });
    }

    public Course save(Course course) {
        return courseRepository.save(course);
    }

    public List<Course> saveAll(List<Course> courses) {
        return courseRepository.saveAll(courses);
    }

    /**
     * Retrieves all courses currently in the database.
     *
     * @return list of all courses
     */
    @Transactional(readOnly = true)
    public List<Course> findAll() {
        return courseRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Course> findByName(String name) {
        return courseRepository.findByName(name);
    }

}
