package com.bogotickets.controller;

import com.bogotickets.dto.RegistroRequest;
import com.bogotickets.dto.RespuestaError;
import com.bogotickets.dto.SesionUsuario;
import com.bogotickets.model.Cliente;
import com.bogotickets.security.SesionActual;
import com.bogotickets.security.UsuarioSesion;
import com.bogotickets.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Sesión y registro. El inicio y el cierre de sesión (POST /api/auth/login y
// POST /api/auth/logout) los atiende Spring Security: ver SeguridadConfig.
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final ClienteService clientes;

    public AuthController(ClienteService clientes) {
        this.clientes = clientes;
    }

    // La página la llama al abrir para saber si ya hay una sesión activa
    @GetMapping("/sesion")
    public ResponseEntity<?> sesion() {
        UsuarioSesion usuario = SesionActual.usuario();
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RespuestaError(401, "No ha iniciado sesión"));
        }
        return ResponseEntity.ok(SesionUsuario.de(usuario));
    }

    // Un visitante crea su cuenta de cliente
    @PostMapping("/registro")
    public ResponseEntity<Cliente> registrarse(@Valid @RequestBody RegistroRequest datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientes.crear(datos));
    }
}
