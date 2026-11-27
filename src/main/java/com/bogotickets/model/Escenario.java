package com.bogotickets.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Fila de la tabla escenario. El id lo genera la base: se ignora al crear.
public record Escenario(
        Integer idEscenario,
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotBlank(message = "La dirección es obligatoria") String direccion,
        @NotNull(message = "La capacidad máxima es obligatoria") Integer capacidadMaxima) {
}
