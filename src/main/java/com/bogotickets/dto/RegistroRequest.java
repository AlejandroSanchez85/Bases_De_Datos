package com.bogotickets.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Cuerpo de POST /api/auth/registro (el cliente crea su cuenta) y de POST /api/clientes
// (el administrador registra un cliente). La contraseña llega en texto y la API guarda
// solo su hash BCrypt. El largo máximo es el que admite BCrypt.
public record RegistroRequest(
        @NotBlank(message = "El documento es obligatorio") String documento,
        @NotBlank(message = "El nombre es obligatorio") String nombreCompleto,
        @NotBlank(message = "El correo es obligatorio") String correo,
        @NotBlank(message = "El teléfono es obligatorio") String telefono,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres") String contrasena) {
}
