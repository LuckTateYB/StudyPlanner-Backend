package com.studyplan.studyplan.repository;

import com.studyplan.studyplan.model.StudyPlan;
import com.studyplan.studyplan.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {

    Optional<StudyPlan> findByUser(User user);

}
