package com.bogotickets.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Fila de la tabla localidad (zona de un escenario con aforo propio)
public record Localidad(
        Integer idLocalidad,
        @NotNull(message = "El escenario es obligatorio") Integer idEscenario,
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotNull(message = "El aforo máximo es obligatorio") Integer aforoMaximo) {
}
