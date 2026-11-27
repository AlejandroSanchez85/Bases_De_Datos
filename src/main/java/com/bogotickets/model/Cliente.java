package com.bogotickets.model;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

// Fila de la tabla cliente. El formato del documento, correo y teléfono lo
// valida la base con CHECK; aquí solo se exige que lleguen.
public record Cliente(
        Integer idCliente,
        @NotBlank(message = "El documento es obligatorio") String documento,
        @NotBlank(message = "El nombre es obligatorio") String nombreCompleto,
        @NotBlank(message = "El correo es obligatorio") String correo,
        @NotBlank(message = "El teléfono es obligatorio") String telefono,
        LocalDate fechaRegistro) {
}
