package com.bogotickets.dto;

import java.time.LocalDate;

// Resultado de la subconsulta NOT EXISTS de /api/reportes/clientes-sin-compras
public record ClienteSinCompras(
        Integer idCliente,
        String nombres,
        String apellidos,
        String ciudad,
        LocalDate fechaRegistro) {
}
