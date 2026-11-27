package com.bogotickets.service;

import com.bogotickets.exception.RecursoNoEncontradoException;
import com.bogotickets.model.Escenario;
import com.bogotickets.repository.EscenarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EscenarioService {

    private final EscenarioRepository repositorio;

    public EscenarioService(EscenarioRepository repositorio) {
        this.repositorio = repositorio;
    }

    public List<Escenario> listar() {
        return repositorio.listar();
    }

    public Escenario buscar(int id) {
        return repositorio.buscarPorId(id).orElseThrow(() -> noExiste(id));
    }

    public Escenario crear(Escenario escenario) {
        return repositorio.crear(escenario);
    }

    // trg_escenario_capacidad impide bajar la capacidad por debajo de la suma de aforos
    public Escenario actualizar(int id, Escenario escenario) {
        return repositorio.actualizar(id, escenario).orElseThrow(() -> noExiste(id));
    }

    // Borra también sus localidades (CASCADE); falla si tiene eventos (RESTRICT)
    public void eliminar(int id) {
        if (!repositorio.eliminar(id)) {
            throw noExiste(id);
        }
    }

    private RecursoNoEncontradoException noExiste(int id) {
        return new RecursoNoEncontradoException("No existe el escenario " + id);
    }
}
