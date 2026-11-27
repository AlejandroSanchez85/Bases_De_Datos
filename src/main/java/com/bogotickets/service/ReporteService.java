package com.bogotickets.service;

import com.bogotickets.dto.ClientePublico;
import com.bogotickets.dto.ClienteSinCompras;
import com.bogotickets.dto.EventoBajaOcupacion;
import com.bogotickets.dto.HistorialPrecio;
import com.bogotickets.dto.OcupacionLocalidad;
import com.bogotickets.dto.PrecioSobrePromedio;
import com.bogotickets.repository.ReporteRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

// Funcionalidad F5 (reportes del analista) e historial de precios
@Service
public class ReporteService {

    private final ReporteRepository reportes;
    private final EventoService eventos;

    public ReporteService(ReporteRepository reportes, EventoService eventos) {
        this.reportes = reportes;
        this.eventos = eventos;
    }

    // Sin idEvento devuelve todos los eventos
    public List<OcupacionLocalidad> ocupacion(Integer idEvento) {
        if (idEvento != null) {
            eventos.buscar(idEvento);
        }
        return reportes.ocupacion(idEvento);
    }

    public List<EventoBajaOcupacion> eventosBajaOcupacion(BigDecimal umbral) {
        return reportes.eventosBajaOcupacion(umbral);
    }

    public List<PrecioSobrePromedio> preciosSobrePromedio() {
        return reportes.preciosSobrePromedio();
    }

    public List<ClienteSinCompras> clientesSinCompras() {
        return reportes.clientesSinCompras();
    }

    public List<ClientePublico> clientes() {
        return reportes.clientes();
    }

    public List<HistorialPrecio> historialPrecios(Integer idEvento) {
        if (idEvento != null) {
            eventos.buscar(idEvento);
        }
        return reportes.historialPrecios(idEvento);
    }
}
