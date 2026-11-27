package com.bogotickets.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Cambio de precio o de aforo registrado por los triggers de auditoría.
// evento viene vacío cuando el cambio es de aforo (es de la localidad, no de un evento).
public record HistorialPrecio(
        Integer idHistorial,
        LocalDateTime fechaCambio,
        String campoModificado,
        String evento,
        String localidad,
        BigDecimal valorAnterior,
        BigDecimal valorNuevo,
        String usuario) {
}
