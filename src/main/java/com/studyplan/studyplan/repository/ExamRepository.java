package com.studyplan.studyplan.repository;

import com.studyplan.studyplan.model.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    List<Exam> findByCourse_Name(String courseName);

    Optional<Exam> findFirstByCourse_Name(String courseName);
}
