package com.bogotickets.dto;

import com.bogotickets.security.UsuarioSesion;

// Datos de la sesión que recibe la página después de iniciar sesión
public record SesionUsuario(String correo, String nombre, String rol, Integer idCliente) {

    public static SesionUsuario de(UsuarioSesion usuario) {
        return new SesionUsuario(usuario.getUsername(), usuario.getNombre(), usuario.getRol(), usuario.getIdCliente());
    }
}
