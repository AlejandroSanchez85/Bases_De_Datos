package com.bogotickets.service;

import com.bogotickets.dto.RegistroRequest;
import com.bogotickets.exception.RecursoNoEncontradoException;
import com.bogotickets.model.Cliente;
import com.bogotickets.repository.ClienteRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ClienteService {

    private final ClienteRepository repositorio;
    private final PasswordEncoder codificador;

    public ClienteService(ClienteRepository repositorio, PasswordEncoder codificador) {
        this.repositorio = repositorio;
        this.codificador = codificador;
    }

    public List<Cliente> listar() {
        return repositorio.listar();
    }

    public Cliente buscar(int id) {
        return repositorio.buscarPorId(id).orElseThrow(() -> noExiste(id));
    }

    // Registro de una cuenta: lo usa el cliente (registro) y el administrador.
    // A la base solo llega el hash BCrypt de la contraseña (RN-23).
    public Cliente crear(RegistroRequest datos) {
        RegistroRequest limpio = new RegistroRequest(datos.documento().trim(), datos.nombreCompleto().trim(),
                datos.correo().trim().toLowerCase(), datos.telefono().trim(), datos.contrasena());
        return repositorio.crear(limpio, codificador.encode(datos.contrasena()));
    }

    public Cliente actualizar(int id, Cliente cliente) {
        Cliente limpio = new Cliente(id, cliente.documento(), cliente.nombreCompleto().trim(),
                cliente.correo().trim().toLowerCase(), cliente.telefono().trim(), null);
        return repositorio.actualizar(id, limpio).orElseThrow(() -> noExiste(id));
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
