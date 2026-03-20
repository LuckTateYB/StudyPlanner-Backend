package com.studyplan.studyplan.repository;

import com.studyplan.studyplan.model.Course;
import com.studyplan.studyplan.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    // Busca por nombre sin filtrar por usuario — útil para búsquedas generales
    Optional<Course> findByName(String name);

    // Busca por nombre dentro del contexto de un usuario específico.
    // Evita que un usuario reutilice el curso de otro usuario con el mismo nombre.
    Optional<Course> findByNameAndUser(String name, User user);

    boolean existsByName(String name);

}
