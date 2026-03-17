package com.studyplan.studyplan.model;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "study_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudySession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The course to be studied during this session. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /** The study plan this session belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_plan_id")
    private StudyPlan studyPlan;

    /** Calendar date of the session. */
    @Column(name = "session_date", nullable = false)
    private LocalDate date;

    /** Clock time when the session starts (e.g., 18:00). */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /** Duration of the session in hours (typically 1). */
    @Column(nullable = false)
    private int duration;

}
