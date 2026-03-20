package com.studyplan.studyplan.model;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "study_plans",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_study_plans_user", columnNames = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Usuario propietario del plan.
     * Relación OneToOne: un usuario, un plan activo.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /**
     * Horas disponibles por día al momento de generar el plan.
     * Tomadas desde UserPreferences.hoursPerDay en el momento de la generación.
     */
    @Column(name = "daily_available_hours", nullable = false)
    private Double dailyAvailableHours;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @OneToMany(mappedBy = "studyPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StudySession> sessions = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.generatedAt = LocalDateTime.now();
    }
}