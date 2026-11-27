package com.bogotickets.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

// Fila de la vista v_boletas_detalle
public record BoletaDetalle(
        Integer idBoleta,
        Integer idCompra,
        LocalDateTime fechaCompra,
        String metodoPago,
        Integer idCliente,
        String cliente,
        Integer idEvento,
        String evento,
        String tipoEvento,
        LocalDate fechaEvento,
        LocalTime horaInicio,
        String escenario,
        Integer idLocalidad,
        String localidad,
        BigDecimal precioPagado,
        String estado) {
}
