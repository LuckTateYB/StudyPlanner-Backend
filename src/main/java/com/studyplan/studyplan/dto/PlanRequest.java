package com.studyplan.studyplan.dto;

import com.studyplan.studyplan.model.enums.StudyTimeWindow;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanRequest {

    /** List of courses the student needs to study. */
    @NotEmpty(message = "At least one course must be provided")
    @Valid
    private List<CourseInput> courses;

    /** List of upcoming exams. */
    @NotEmpty(message = "At least one exam must be provided")
    @Valid
    private List<ExamInput> exams;

    /**
     * Number of study hours the student can dedicate per day.
     * Must be between 1 and 12 hours.
     */
    @Min(value = 1, message = "hoursPerDay must be at least 1")
    @Max(value = 12, message = "hoursPerDay must not exceed 12")
    private int hoursPerDay;

    /**
     * Preferred time block for studying each day.
     * Must be one of: MORNING, AFTERNOON, EVENING.
     */
    @NotNull(message = "preferredStudyTime must not be null")
    private StudyTimeWindow preferredStudyTime;


    /** Input DTO for a single course. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseInput {

        @NotBlank(message = "Course name must not be blank")
        private String name;
    }

    /** Input DTO for a single exam. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExamInput {
        @NotBlank(message = "Exam course name must not be blank")
        private String course;

        @NotNull(message = "Exam date must not be null")
        private java.time.LocalDate date;
    }
}
