package com.bogotickets.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

// Fila de la vista v_compras_resumen: una compra con su evento, totales y estado
public record CompraResumen(
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
        String localidades,
        Integer boletas,
        Integer boletasVigentes,
        BigDecimal total,
        String estado) {
}
