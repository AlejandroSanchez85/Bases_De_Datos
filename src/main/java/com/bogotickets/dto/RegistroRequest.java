package com.bogotickets.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

// Cuerpo de POST /api/auth/registro (el cliente crea su cuenta) y de POST /api/clientes
// (el administrador registra un cliente). La contraseña llega en texto y la API guarda
// solo su hash BCrypt. El largo máximo es el que admite BCrypt.
public record RegistroRequest(
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
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres") String contrasena) {
}
