package com.bogotickets.service;

import com.bogotickets.exception.RecursoNoEncontradoException;
import com.bogotickets.model.Localidad;
import com.bogotickets.repository.LocalidadRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocalidadService {

    private final LocalidadRepository repositorio;

    public LocalidadService(LocalidadRepository repositorio) {
        this.repositorio = repositorio;
    }

    public List<Localidad> listar(Integer idEscenario) {
        return repositorio.listar(idEscenario);
    }

    public Localidad buscar(int id) {
        return repositorio.buscarPorId(id).orElseThrow(() -> noExiste(id));
    }

    // trg_localidad_validar: la suma de aforos no puede superar la capacidad del escenario
    public Localidad crear(Localidad localidad) {
        return repositorio.crear(localidad);
    }

    // Además, el aforo no puede quedar por debajo de las boletas ya vendidas
    public Localidad actualizar(int id, Localidad localidad) {
        return repositorio.actualizar(id, localidad).orElseThrow(() -> noExiste(id));
    }

    public void eliminar(int id) {
        if (!repositorio.eliminar(id)) {
            throw noExiste(id);
        }
    }

    private RecursoNoEncontradoException noExiste(int id) {
        return new RecursoNoEncontradoException("No existe la localidad " + id);
    }
}
