package com.studyplan.studyplan.repository;

import com.studyplan.studyplan.model.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {

    boolean existsByDateAndStartTime(LocalDate date, LocalTime startTime);

}