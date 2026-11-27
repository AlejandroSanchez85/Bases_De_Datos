package com.bogotickets.controller;

import com.bogotickets.dto.CambioPrecioRequest;
import com.bogotickets.dto.PrecioRequest;
import com.bogotickets.model.PrecioLocalidad;
import com.bogotickets.service.PrecioService;
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

// Tarifas de un evento: precio de cada localidad (tabla precio_localidad)
@RestController
@RequestMapping("/api/eventos/{idEvento}/precios")
public class PrecioController {

    private final PrecioService servicio;

    public PrecioController(PrecioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<PrecioLocalidad> listar(@PathVariable("idEvento") int idEvento) {
        return servicio.listar(idEvento);
    }

    @PostMapping
    public ResponseEntity<PrecioLocalidad> crear(@PathVariable("idEvento") int idEvento,
                                                 @Valid @RequestBody PrecioRequest datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(idEvento, datos));
    }

    @PutMapping("/{idLocalidad}")
    public PrecioLocalidad actualizar(@PathVariable("idEvento") int idEvento,
                                      @PathVariable("idLocalidad") int idLocalidad,
                                      @Valid @RequestBody CambioPrecioRequest datos) {
        return servicio.actualizar(idEvento, idLocalidad, datos.precio());
    }

    @DeleteMapping("/{idLocalidad}")
    public ResponseEntity<Void> eliminar(@PathVariable("idEvento") int idEvento,
                                         @PathVariable("idLocalidad") int idLocalidad) {
        servicio.eliminar(idEvento, idLocalidad);
        return ResponseEntity.noContent().build();
    }
}
