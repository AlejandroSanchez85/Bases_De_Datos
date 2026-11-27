package com.bogotickets.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

// Cuerpo de POST /api/eventos/{idEvento}/precios: a qué localidad se le pone precio
public record PrecioRequest(
        @NotNull(message = "La localidad es obligatoria") Integer idLocalidad,
        @NotNull(message = "El precio es obligatorio") BigDecimal precio) {
}
