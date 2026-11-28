package com.bogotickets.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

// Fila de la tabla cliente (sin la contraseña). Los formatos del documento, el correo
// y el celular, y las reglas de edad, los valida la base con CHECK; aquí solo se exige
// que lleguen. El tipo y número de documento y la fecha de nacimiento no se cambian
// en el PUT: identifican a la persona y respaldan la restricción de edad (RN-25).
public record Cliente(
        Integer idCliente,
        @NotBlank(message = "El tipo de documento es obligatorio") String tipoDocumento,
        @NotBlank(message = "El número de documento es obligatorio") String numeroDocumento,
        @NotBlank(message = "Los nombres son obligatorios") String nombres,
        @NotBlank(message = "Los apellidos son obligatorios") String apellidos,
        @NotNull(message = "La fecha de nacimiento es obligatoria") LocalDate fechaNacimiento,
        @NotBlank(message = "El género es obligatorio") String genero,
        @NotBlank(message = "El correo es obligatorio") String correo,
        @NotBlank(message = "El indicativo es obligatorio") String indicativo,
        @NotBlank(message = "El celular es obligatorio") String celular,
        @NotNull(message = "La ciudad de residencia es obligatoria") Integer idCiudad,
        @NotBlank(message = "La dirección es obligatoria") String direccion,
        LocalDate fechaRegistro) {
}
