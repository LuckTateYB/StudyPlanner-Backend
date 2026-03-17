package com.studyplan.studyplan.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Display name of the course (e.g., "Mathematics", "Programming"). */
    @NotBlank(message = "Course name must not be blank")
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * Difficulty level estimated by the AI.
     * Range: 1 (very easy) to 5 (very hard).
     * Default is 3 (medium) before AI estimation.
     */
    @Column(name = "difficulty_level")
    @Builder.Default
    private int difficultyLevel = 3;

}
