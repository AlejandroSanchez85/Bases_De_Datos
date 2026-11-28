package com.bogotickets.dto;

import java.time.LocalDate;

// Fila de la vista v_clientes_publico: sin documento, celular, dirección ni fecha de
// nacimiento (solo la edad) y con el correo oculto
public record ClientePublico(
        Integer idCliente,
        String nombres,
        String apellidos,
        String correoOculto,
        String genero,
        Integer edad,
        String ciudad,
        String departamento,
        LocalDate fechaRegistro,
        Integer compras,
        Integer boletasVigentes) {
}
