package com.bogotickets.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

// Usuario que inició sesión. Además del correo, la contraseña y el rol que usa
// Spring Security, guarda su nombre y, si es cliente, su id_cliente.
public class UsuarioSesion extends User {

    private final String rol;           // CLIENTE, ADMINISTRADOR o ANALISTA
    private final Integer idCliente;    // null para los empleados
    private final String nombre;

    public UsuarioSesion(String correo, String contrasena, String rol, Integer idCliente, String nombre) {
        super(correo, contrasena, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
        this.rol = rol;
        this.idCliente = idCliente;
        this.nombre = nombre;
    }

    // Rol de PostgreSQL con el que se ejecutan sus consultas (sql/05_usuarios_permisos.sql)
    public String rolBaseDatos() {
        return switch (rol) {
            case "CLIENTE" -> "rol_cliente";
            case "ADMINISTRADOR" -> "rol_administrador";
            case "ANALISTA" -> "rol_analista";
            default -> throw new IllegalStateException("Rol desconocido: " + rol);
        };
    }

    public String getRol() {
        return rol;
    }

    public Integer getIdCliente() {
        return idCliente;
    }

    public String getNombre() {
        return nombre;
    }
}
