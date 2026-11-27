package com.bogotickets.dto;

import java.util.List;

// Una compra (resumen de v_compras_resumen) con cada una de sus boletas
public record CompraDetalle(CompraResumen compra, List<BoletaDetalle> boletas) {
}
