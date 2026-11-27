package com.bogotickets.service;

import com.bogotickets.exception.RecursoNoEncontradoException;
import com.bogotickets.model.Evento;
import com.bogotickets.repository.EventoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EventoService {

    private final EventoRepository repositorio;

    public EventoService(EventoRepository repositorio) {
        this.repositorio = repositorio;
    }

    public List<Evento> listar(Integer idEscenario, Integer idTipoEvento) {
        return repositorio.listar(idEscenario, idTipoEvento);
    }

    public Evento buscar(int id) {
        return repositorio.buscarPorId(id).orElseThrow(() -> noExiste(id));
    }

    public Evento crear(Evento evento) {
        return repositorio.crear(evento);
    }

    public Evento actualizar(int id, Evento evento) {
        return repositorio.actualizar(id, evento).orElseThrow(() -> noExiste(id));
    }

    // Borra también sus tarifas (CASCADE) si no vendió boletas
    public void eliminar(int id) {
        if (!repositorio.eliminar(id)) {
            throw noExiste(id);
        }
    }

    private RecursoNoEncontradoException noExiste(int id) {
        return new RecursoNoEncontradoException("No existe el evento " + id);
    }
}
