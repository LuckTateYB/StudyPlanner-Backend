package com.studyplan.studyplan.model;
import com.studyplan.studyplan.model.enums.StudyTimeWindow;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferences {

    @NotNull(message = "Las horas por día son obligatorias")
    @DecimalMin(value = "0.5", message = "Mínimo 0.5 horas por día")
    @DecimalMax(value = "16.0", message = "Máximo 16 horas por día")
    @Column(name = "hours_per_day", nullable = false)
    private Double hoursPerDay;

    /**
     * Bloque horario preferido para estudiar.
     * Valores posibles: MORNING, AFTERNOON, EVENING.
     * Almacenado como String en la base de datos para legibilidad.
     */
    @NotNull(message = "El bloque horario preferido es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_study_time", nullable = false)
    private StudyTimeWindow preferredStudyTime;

}
