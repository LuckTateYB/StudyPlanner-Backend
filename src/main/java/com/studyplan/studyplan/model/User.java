package com.studyplan.studyplan.model;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad raíz del sistema.
 * Todo recurso del sistema (cursos, evaluaciones, plan de estudio)
 * pertenece a un usuario y está aislado por su identidad.
 *
 * Nota de arquitectura:
 * El campo passwordHash está incluido para preparar la entidad
 * para futura integración con Spring Security sin refactorizar el modelo.
 * Por ahora no se usa en autenticación.
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Hash de la contraseña del usuario.
     * Preparado para Spring Security. No se expone en ningún DTO de respuesta.
     */
    @NotBlank(message = "La contraseña es obligatoria")
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /**
     * Preferencias de estudio embebidas.
     * Se persisten como columnas directas en la tabla 'users'.
     * Incluye hoursPerDay y preferredStudyTime.
     */
    @Valid
    @Embedded
    private UserPreferences preferences;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Cursos registrados por este usuario.
     * La eliminación del usuario elimina todos sus cursos en cascada.
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Course> courses = new ArrayList<>();

    /**
     * Plan de estudio activo del usuario.
     * Un usuario tiene como máximo un plan activo en todo momento.
     */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private StudyPlan studyPlan;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}