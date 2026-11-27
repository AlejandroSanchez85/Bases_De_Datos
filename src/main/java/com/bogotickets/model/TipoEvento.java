package com.bogotickets.model;

import jakarta.validation.constraints.NotBlank;

// Fila de la tabla tipo_evento. La descripción es opcional.
public record TipoEvento(
        Integer idTipoEvento,
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        String descripcion) {
}
