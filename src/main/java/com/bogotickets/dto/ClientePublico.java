package com.bogotickets.dto;

import java.time.LocalDate;

// Fila de la vista v_clientes_publico: sin documento ni teléfono, correo oculto
public record ClientePublico(
        Integer idCliente,
        String nombreCompleto,
        String correoOculto,
        LocalDate fechaRegistro,
        Integer compras,
        Integer boletasVigentes) {
}
