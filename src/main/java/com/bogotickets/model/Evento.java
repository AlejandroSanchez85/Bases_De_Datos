package com.bogotickets.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

// Fila de la tabla evento. fechaRegistro la pone la base (hoy) al crear.
public record Evento(
        Integer idEvento,
        @NotNull(message = "El escenario es obligatorio") Integer idEscenario,
        @NotNull(message = "El tipo de evento es obligatorio") Integer idTipoEvento,
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotNull(message = "La fecha es obligatoria") LocalDate fecha,
        @NotNull(message = "La hora de inicio es obligatoria") LocalTime horaInicio,
        @NotNull(message = "La hora de fin es obligatoria") LocalTime horaFin,
        LocalDate fechaRegistro) {
}
