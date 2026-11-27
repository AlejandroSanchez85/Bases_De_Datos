package com.bogotickets.service;

import com.bogotickets.exception.RecursoNoEncontradoException;
import com.bogotickets.model.TipoEvento;
import com.bogotickets.repository.TipoEventoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TipoEventoService {

    private final TipoEventoRepository repositorio;

    public TipoEventoService(TipoEventoRepository repositorio) {
        this.repositorio = repositorio;
    }

    public List<TipoEvento> listar() {
        return repositorio.listar();
    }

    public TipoEvento buscar(int id) {
        return repositorio.buscarPorId(id).orElseThrow(() -> noExiste(id));
    }

    public TipoEvento crear(TipoEvento tipo) {
        return repositorio.crear(tipo);
    }

    public TipoEvento actualizar(int id, TipoEvento tipo) {
        return repositorio.actualizar(id, tipo).orElseThrow(() -> noExiste(id));
    }

    public void eliminar(int id) {
        if (!repositorio.eliminar(id)) {
            throw noExiste(id);
        }
    }

    private RecursoNoEncontradoException noExiste(int id) {
        return new RecursoNoEncontradoException("No existe el tipo de evento " + id);
    }
}
