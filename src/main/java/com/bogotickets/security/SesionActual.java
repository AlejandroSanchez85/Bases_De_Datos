package com.bogotickets.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

// Acceso al usuario que hizo la petición actual
public final class SesionActual {

    private SesionActual() {
    }

    // null si nadie ha iniciado sesión (por ejemplo, en el registro de un cliente)
    public static UsuarioSesion usuario() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioSesion usuario) {
            return usuario;
        }
        return null;
    }

    // id del cliente que inició sesión: así cada cliente solo ve y compra a su nombre (RNF-05)
    public static int idCliente() {
        UsuarioSesion usuario = usuario();
        if (usuario == null || usuario.getIdCliente() == null) {
            throw new AccessDeniedException("Solo un cliente puede hacer esta operación");
        }
        return usuario.getIdCliente();
    }
}
