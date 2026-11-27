package com.bogotickets.dto;

import java.math.BigDecimal;

// Resultado de la subconsulta escalar de /api/reportes/precios-sobre-promedio
public record PrecioSobrePromedio(
        Integer idEvento,
        String evento,
        String localidad,
        BigDecimal precio,
        BigDecimal promedio) {
}
