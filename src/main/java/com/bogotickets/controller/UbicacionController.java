package com.bogotickets.controller;

import com.bogotickets.dto.Ubicacion;
import com.bogotickets.service.UbicacionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Catálogo de residencia. Es público porque lo usa el formulario de registro, antes
// de iniciar sesión.
@RestController
@RequestMapping("/api/ubicaciones")
public class UbicacionController {

    private final UbicacionService servicio;

    public UbicacionController(UbicacionService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<Ubicacion> listar() {
        return servicio.listar();
    }
}
