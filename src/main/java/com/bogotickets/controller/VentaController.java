package com.bogotickets.controller;

import com.bogotickets.dto.BoletaDetalle;
import com.bogotickets.dto.CompraDetalle;
import com.bogotickets.dto.CompraRequest;
import com.bogotickets.dto.CompraResumen;
import com.bogotickets.dto.Disponibilidad;
import com.bogotickets.dto.EventoCatalogo;
import com.bogotickets.service.VentaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Venta de boletas. La cartelera y la compra son del cliente; la lista de ventas,
// la anulación y las consultas de boletas son del administrador (ver SeguridadConfig).
@RestController
@RequestMapping("/api")
public class VentaController {

    private final VentaService servicio;

    public VentaController(VentaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/catalogo")
    public List<EventoCatalogo> catalogo() {
        return servicio.catalogo();
    }

    @GetMapping("/eventos/{id}/disponibilidad")
    public List<Disponibilidad> disponibilidad(@PathVariable("id") int idEvento) {
        return servicio.disponibilidad(idEvento);
    }

    @PostMapping("/compras")
    public ResponseEntity<CompraDetalle> comprar(@Valid @RequestBody CompraRequest datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.comprar(datos));
    }

    // Últimas 200 compras; filtros opcionales: /api/compras?idEvento=11&idCliente=5
    @GetMapping("/compras")
    public List<CompraResumen> listarCompras(@RequestParam(name = "idEvento", required = false) Integer idEvento,
                                             @RequestParam(name = "idCliente", required = false) Integer idCliente) {
        return servicio.listarCompras(idEvento, idCliente);
    }

    @GetMapping("/compras/{id}")
    public CompraDetalle buscarCompra(@PathVariable("id") int idCompra) {
        return servicio.buscarCompra(idCompra);
    }

    @PatchMapping("/compras/{id}/anular")
    public CompraDetalle anular(@PathVariable("id") int idCompra) {
        return servicio.anular(idCompra);
    }

    @GetMapping("/boletas/{id}")
    public BoletaDetalle buscarBoleta(@PathVariable("id") int idBoleta) {
        return servicio.buscarBoleta(idBoleta);
    }

    @GetMapping("/clientes/{id}/boletas")
    public List<BoletaDetalle> boletasDeCliente(@PathVariable("id") int idCliente) {
        return servicio.boletasDeCliente(idCliente);
    }
}
