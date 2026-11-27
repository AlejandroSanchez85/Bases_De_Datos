package com.bogotickets.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

// Fila de la vista v_ocupacion_localidad (reporte del analista)
public record OcupacionLocalidad(
        Integer idEvento,
        String evento,
        LocalDate fechaEvento,
        String escenario,
        Integer idLocalidad,
        String localidad,
        BigDecimal precioVigente,
        Integer aforoMaximo,
        Integer boletasVendidas,
        Integer boletasDisponibles,
        BigDecimal porcentajeOcupacion,
        BigDecimal recaudo) {
}
