package com.bogotickets.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

// Resultado de la subconsulta en FROM de /api/reportes/eventos-baja-ocupacion
public record EventoBajaOcupacion(
        Integer idEvento,
        String evento,
        LocalDate fechaEvento,
        Integer aforoTotal,
        Integer boletasVendidas,
        BigDecimal porcentajeOcupacion) {
}
