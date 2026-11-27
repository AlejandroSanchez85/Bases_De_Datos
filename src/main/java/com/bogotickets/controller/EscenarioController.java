package com.bogotickets.controller;

import com.bogotickets.model.Escenario;
import com.bogotickets.service.EscenarioService;
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
@RequestMapping("/api/escenarios")
public class EscenarioController {

    private final EscenarioService servicio;

    public EscenarioController(EscenarioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<Escenario> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    public Escenario buscar(@PathVariable("id") int id) {
        return servicio.buscar(id);
    }

    @PostMapping
    public ResponseEntity<Escenario> crear(@Valid @RequestBody Escenario escenario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(escenario));
    }

    @PutMapping("/{id}")
    public Escenario actualizar(@PathVariable("id") int id, @Valid @RequestBody Escenario escenario) {
        return servicio.actualizar(id, escenario);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") int id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
