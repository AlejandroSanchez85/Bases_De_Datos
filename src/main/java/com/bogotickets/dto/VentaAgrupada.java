package com.bogotickets.dto;

import java.math.BigDecimal;

// Boletas vendidas y recaudo de un grupo: un tipo de evento, un escenario o un mes
public record VentaAgrupada(String nombre, Integer boletas, BigDecimal recaudo) {
}
