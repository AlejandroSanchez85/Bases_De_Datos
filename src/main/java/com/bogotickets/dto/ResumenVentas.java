package com.bogotickets.dto;

import java.math.BigDecimal;
import java.util.List;

// Tablero del analista: cifras generales y ventas agrupadas (GET /api/reportes/resumen)
public record ResumenVentas(
        BigDecimal recaudo,
        Integer boletasVendidas,
        Integer boletasCanceladas,
        Integer compras,
        Integer clientesConCompras,
        Integer eventosALaVenta,
        List<VentaAgrupada> porTipoEvento,
        List<VentaAgrupada> porEscenario,
        List<VentaAgrupada> porMes) {
}
