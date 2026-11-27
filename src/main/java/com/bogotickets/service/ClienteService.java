package com.bogotickets.service;

import com.bogotickets.exception.RecursoNoEncontradoException;
import com.bogotickets.model.Cliente;
import com.bogotickets.repository.ClienteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository repositorio;

    public ClienteService(ClienteRepository repositorio) {
        this.repositorio = repositorio;
    }

    public List<Cliente> listar() {
        return repositorio.listar();
    }

    public Cliente buscar(int id) {
        return repositorio.buscarPorId(id).orElseThrow(() -> noExiste(id));
    }

    public Cliente crear(Cliente cliente) {
        return repositorio.crear(cliente);
    }

    public Cliente actualizar(int id, Cliente cliente) {
        return repositorio.actualizar(id, cliente).orElseThrow(() -> noExiste(id));
    }

    // Solo se puede eliminar una cuenta sin compras (RF-16)
    public void eliminar(int id) {
        if (!repositorio.eliminar(id)) {
            throw noExiste(id);
        }
    }

    private RecursoNoEncontradoException noExiste(int id) {
        return new RecursoNoEncontradoException("No existe el cliente " + id);
    }
}
