package com.bogotickets.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

// Cuerpo de PUT /api/eventos/{idEvento}/precios/{idLocalidad}: la localidad va en la URL
public record CambioPrecioRequest(
        @NotNull(message = "El precio es obligatorio") BigDecimal precio) {
}
