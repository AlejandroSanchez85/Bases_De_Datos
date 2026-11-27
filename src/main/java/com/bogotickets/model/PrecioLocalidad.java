package com.bogotickets.model;

import java.math.BigDecimal;

// Tarifa de una localidad en un evento (tabla precio_localidad + nombre de la localidad)
public record PrecioLocalidad(
        Integer idEvento,
        Integer idLocalidad,
        String localidad,
        BigDecimal precio) {
}
