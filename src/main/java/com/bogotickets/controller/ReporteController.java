package com.bogotickets.controller;

import com.bogotickets.dto.ClientePublico;
import com.bogotickets.dto.ClienteSinCompras;
import com.bogotickets.dto.EventoBajaOcupacion;
import com.bogotickets.dto.HistorialPrecio;
import com.bogotickets.dto.OcupacionLocalidad;
import com.bogotickets.dto.PrecioSobrePromedio;
import com.bogotickets.service.ReporteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

// Reportes del analista de ventas (solo lectura) e historial de precios
@RestController
@RequestMapping("/api")
public class ReporteController {

    private final ReporteService servicio;

    public ReporteController(ReporteService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/reportes/ocupacion")
    public List<OcupacionLocalidad> ocupacion(@RequestParam(name = "idEvento", required = false) Integer idEvento) {
        return servicio.ocupacion(idEvento);
    }

    @GetMapping("/reportes/eventos-baja-ocupacion")
    public List<EventoBajaOcupacion> eventosBajaOcupacion(
            @RequestParam(name = "umbral", defaultValue = "50") BigDecimal umbral) {
        return servicio.eventosBajaOcupacion(umbral);
    }

    @GetMapping("/reportes/precios-sobre-promedio")
    public List<PrecioSobrePromedio> preciosSobrePromedio() {
        return servicio.preciosSobrePromedio();
    }

    @GetMapping("/reportes/clientes-sin-compras")
    public List<ClienteSinCompras> clientesSinCompras() {
        return servicio.clientesSinCompras();
    }

    @GetMapping("/reportes/clientes")
    public List<ClientePublico> clientes() {
        return servicio.clientes();
    }

    @GetMapping("/historial-precios")
    public List<HistorialPrecio> historialPrecios(@RequestParam(name = "idEvento", required = false) Integer idEvento) {
        return servicio.historialPrecios(idEvento);
    }
}
