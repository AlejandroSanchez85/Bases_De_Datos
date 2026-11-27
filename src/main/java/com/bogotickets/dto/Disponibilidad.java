package com.bogotickets.dto;

import java.math.BigDecimal;

// Cupos de una localidad en un evento (columnas de v_ocupacion_localidad que ve el cliente)
public record Disponibilidad(
        Integer idLocalidad,
        String localidad,
        BigDecimal precio,
        Integer aforoMaximo,
        Integer boletasVendidas,
        Integer boletasDisponibles) {
}
