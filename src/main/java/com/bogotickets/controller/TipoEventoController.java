package com.bogotickets.controller;

import com.bogotickets.model.TipoEvento;
import com.bogotickets.service.TipoEventoService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-evento")
public class TipoEventoController {

    private final TipoEventoService servicio;

    public TipoEventoController(TipoEventoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<TipoEvento> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    public TipoEvento buscar(@PathVariable("id") int id) {
        return servicio.buscar(id);
    }

    @PostMapping
    public ResponseEntity<TipoEvento> crear(@Valid @RequestBody TipoEvento tipo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(tipo));
    }

    @PutMapping("/{id}")
    public TipoEvento actualizar(@PathVariable("id") int id, @Valid @RequestBody TipoEvento tipo) {
        return servicio.actualizar(id, tipo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") int id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
