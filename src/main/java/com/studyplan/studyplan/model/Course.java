package com.studyplan.studyplan.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Usuario propietario del curso.
     * Cada curso está aislado por identidad de usuario.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank(message = "El nombre del curso es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    @Column(nullable = false)
    private String name;

    /**
     * Nivel de dificultad del curso en escala 1–5.
     * Usado por el algoritmo con peso 0.4 en el score de prioridad.
     */
    @NotNull(message = "El nivel de dificultad es obligatorio")
    @Min(value = 1, message = "La dificultad mínima es 1")
    @Max(value = 5, message = "La dificultad máxima es 5")
    @Column(name = "difficulty_level", nullable = false)
    private Integer difficultyLevel;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Exam> exams = new ArrayList<>();

}
