package com.studyplan.studyplan.dto;

import com.studyplan.studyplan.model.enums.StudyTimeWindow;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanRequest {

    /** Lista de cursos del estudiante */
    @NotEmpty(message = "At least one course must be provided")
    @Valid
    private List<CourseInput> courses;

    /** Lista de próximos exámenes */
    @NotEmpty(message = "At least one exam must be provided")
    @Valid
    private List<ExamInput> exams;

    /**
     * Número de horas de estudio que el estudiante puede dedicar por día
     * * Debe estar entre 1 y 12 horas.
     */
    @Min(value = 1, message = "hoursPerDay must be at least 1")
    @Max(value = 12, message = "hoursPerDay must not exceed 12")
    private int hoursPerDay;

    /**
     * Bloque horario preferido para estudiar cada día.
     * Debe ser uno de los siguientes: MAÑANA, TARDE o NOCHE.
     */
    @NotNull(message = "preferredStudyTime must not be null")
    private StudyTimeWindow preferredStudyTime;


    /** Introduzca el DTO para un solo curso */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseInput {

        @NotBlank(message = "Course name must not be blank")
        private String name;
    }

    /** Introduzca el DTO para un único examen */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExamInput {
        @NotBlank(message = "Exam course name must not be blank")
        private String course;

        @NotNull(message = "Exam date must not be null")
        private java.time.LocalDate date;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateSessionRequest {
        private LocalDate date;
        private LocalTime startTime;
        private Integer duration;
        private Long courseId;
    }
}
