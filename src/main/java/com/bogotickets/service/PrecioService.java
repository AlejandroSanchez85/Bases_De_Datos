package com.bogotickets.service;

import com.bogotickets.dto.PrecioRequest;
import com.bogotickets.exception.RecursoNoEncontradoException;
import com.bogotickets.model.PrecioLocalidad;
import com.bogotickets.repository.PrecioRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PrecioService {

    private final PrecioRepository precios;
    private final EventoService eventos;

    public PrecioService(PrecioRepository precios, EventoService eventos) {
        this.precios = precios;
        this.eventos = eventos;
    }

    public List<PrecioLocalidad> listar(int idEvento) {
        eventos.buscar(idEvento);                       // 404 si el evento no existe
        return precios.listarPorEvento(idEvento);
    }

    public PrecioLocalidad crear(int idEvento, PrecioRequest datos) {
        eventos.buscar(idEvento);
        precios.crear(idEvento, datos.idLocalidad(), datos.precio());
        return buscar(idEvento, datos.idLocalidad());
    }

    // Funcionalidad F3: el cambio queda en historial_precios por el trigger de auditoría
    public PrecioLocalidad actualizar(int idEvento, int idLocalidad, BigDecimal precio) {
        if (!precios.actualizar(idEvento, idLocalidad, precio)) {
            throw noExiste(idEvento, idLocalidad);
        }
        return buscar(idEvento, idLocalidad);
    }

    public void eliminar(int idEvento, int idLocalidad) {
        if (!precios.eliminar(idEvento, idLocalidad)) {
            throw noExiste(idEvento, idLocalidad);
        }
    }

    private PrecioLocalidad buscar(int idEvento, int idLocalidad) {
        return precios.buscar(idEvento, idLocalidad).orElseThrow(() -> noExiste(idEvento, idLocalidad));
    }

    private RecursoNoEncontradoException noExiste(int idEvento, int idLocalidad) {
        return new RecursoNoEncontradoException(
                "El evento " + idEvento + " no tiene precio para la localidad " + idLocalidad);
    }
}
