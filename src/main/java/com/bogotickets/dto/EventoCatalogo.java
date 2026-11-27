package com.bogotickets.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

// Fila de la vista v_catalogo_eventos
public record EventoCatalogo(
        Integer idEvento,
        String evento,
        String tipoEvento,
        String escenario,
        String direccion,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        BigDecimal precioDesde,
        Integer boletasDisponibles) {
}
