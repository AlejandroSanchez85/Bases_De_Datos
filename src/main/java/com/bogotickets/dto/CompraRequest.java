package com.bogotickets.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Cuerpo de POST /api/compras: qué localidad de qué evento, cuántas boletas y cómo paga.
// El cliente no va en el cuerpo: es el que inició sesión.
public record CompraRequest(
        @NotNull(message = "El evento es obligatorio") Integer idEvento,
        @NotNull(message = "La localidad es obligatoria") Integer idLocalidad,
        @NotNull(message = "La cantidad es obligatoria") Integer cantidad,
        @NotBlank(message = "El método de pago es obligatorio") String metodoPago) {
}
