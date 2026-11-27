package com.bogotickets.controller;

import com.bogotickets.dto.CompraDetalle;
import com.bogotickets.dto.CompraResumen;
import com.bogotickets.model.Cliente;
import com.bogotickets.service.CuentaService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Módulo del cliente: su cuenta y sus compras (solo las propias)
@RestController
@RequestMapping("/api/cuenta")
public class CuentaController {

    private final CuentaService servicio;

    public CuentaController(CuentaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public Cliente datos() {
        return servicio.datos();
    }

    @PutMapping
    public Cliente actualizar(@Valid @RequestBody Cliente datos) {
        return servicio.actualizar(datos);
    }

    // Si la cuenta se elimina, también se cierra la sesión
    @DeleteMapping
    public ResponseEntity<Void> eliminar(HttpServletRequest peticion) throws ServletException {
        servicio.eliminar();
        peticion.logout();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/compras")
    public List<CompraResumen> compras() {
        return servicio.compras();
    }

    @GetMapping("/compras/{id}")
    public CompraDetalle compra(@PathVariable("id") int idCompra) {
        return servicio.compra(idCompra);
    }
}
