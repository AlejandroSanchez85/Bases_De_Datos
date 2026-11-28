package com.bogotickets.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

// Fila de la vista v_catalogo_eventos
public record EventoCatalogo(
        Integer idEvento,
        String evento,
        Integer idTipoEvento,
        String tipoEvento,
        Integer idEscenario,
        String escenario,
        String direccion,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        Integer edadMinima,
        BigDecimal precioDesde,
        BigDecimal precioHasta,
        Integer aforoTotal,
        Integer boletasDisponibles) {
}
