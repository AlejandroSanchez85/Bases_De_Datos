package com.bogotickets.controller;

import com.bogotickets.model.Localidad;
import com.bogotickets.service.LocalidadService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/localidades")
public class LocalidadController {

    private final LocalidadService servicio;

    public LocalidadController(LocalidadService servicio) {
        this.servicio = servicio;
    }

    // GET /api/localidades?idEscenario=4 filtra por escenario
    @GetMapping
    public List<Localidad> listar(@RequestParam(name = "idEscenario", required = false) Integer idEscenario) {
        return servicio.listar(idEscenario);
    }

    @GetMapping("/{id}")
    public Localidad buscar(@PathVariable("id") int id) {
        return servicio.buscar(id);
    }

    @PostMapping
    public ResponseEntity<Localidad> crear(@Valid @RequestBody Localidad localidad) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(localidad));
    }

    @PutMapping("/{id}")
    public Localidad actualizar(@PathVariable("id") int id, @Valid @RequestBody Localidad localidad) {
        return servicio.actualizar(id, localidad);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") int id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
