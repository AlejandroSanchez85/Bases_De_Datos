package com.bogotickets.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Cuerpo de POST /api/compras: quién compra, qué localidad de qué evento y cuántas boletas
public record CompraRequest(
        @NotNull(message = "El cliente es obligatorio") Integer idCliente,
        @NotNull(message = "El evento es obligatorio") Integer idEvento,
        @NotNull(message = "La localidad es obligatoria") Integer idLocalidad,
        @NotNull(message = "La cantidad es obligatoria") Integer cantidad,
        @NotBlank(message = "El método de pago es obligatorio") String metodoPago) {
}
