package com.studyplan.studyplan.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "exams")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The course this exam belongs to.
     * Loaded eagerly since it is always needed alongside the exam.
     */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /**
     * The date on which the exam is scheduled.
     * Must be a future date at the time of creation.
     */
    @NotNull(message = "Exam date must not be null")
    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;

}
