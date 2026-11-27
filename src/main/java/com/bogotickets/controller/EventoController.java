package com.bogotickets.controller;

import com.bogotickets.model.Evento;
import com.bogotickets.service.EventoService;
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
@RequestMapping("/api/eventos")
public class EventoController {

    private final EventoService servicio;

    public EventoController(EventoService servicio) {
        this.servicio = servicio;
    }

    // Filtros opcionales: /api/eventos?idEscenario=4&idTipo=4
    @GetMapping
    public List<Evento> listar(@RequestParam(name = "idEscenario", required = false) Integer idEscenario,
                               @RequestParam(name = "idTipo", required = false) Integer idTipo) {
        return servicio.listar(idEscenario, idTipo);
    }

    @GetMapping("/{id}")
    public Evento buscar(@PathVariable("id") int id) {
        return servicio.buscar(id);
    }

    @PostMapping
    public ResponseEntity<Evento> crear(@Valid @RequestBody Evento evento) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(evento));
    }

    @PutMapping("/{id}")
    public Evento actualizar(@PathVariable("id") int id, @Valid @RequestBody Evento evento) {
        return servicio.actualizar(id, evento);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") int id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
