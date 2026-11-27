package com.bogotickets.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Una compra con sus boletas. total = suma de lo pagado por todas sus boletas.
public record CompraDetalle(
        Integer idCompra,
        Integer idCliente,
        String cliente,
        LocalDateTime fechaCompra,
        String metodoPago,
        BigDecimal total,
        List<BoletaDetalle> boletas) {
}
