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
    // A la base solo llega el hash BCrypt de la contraseña (RN-23). Las reglas de edad y
    // de documento (RN-26, RN-27) las revisa la base con CHECK.
    public Cliente crear(RegistroRequest d) {
        RegistroRequest limpio = new RegistroRequest(d.tipoDocumento().trim().toUpperCase(),
                sinSeparadores(d.numeroDocumento()).toUpperCase(), d.nombres().trim(), d.apellidos().trim(),
                d.fechaNacimiento(), d.genero().trim().toUpperCase(), d.correo().trim().toLowerCase(),
                d.indicativo().trim(), sinSeparadores(d.celular()), d.idCiudad(), d.direccion().trim(),
                d.contrasena());
        return repositorio.crear(limpio, codificador.encode(d.contrasena()));
    }

    public Cliente actualizar(int id, Cliente c) {
        Cliente limpio = new Cliente(id, c.tipoDocumento(), c.numeroDocumento(), c.nombres().trim(),
                c.apellidos().trim(), c.fechaNacimiento(), c.genero().trim().toUpperCase(),
                c.correo().trim().toLowerCase(), c.indicativo().trim(), sinSeparadores(c.celular()),
                c.idCiudad(), c.direccion().trim(), null);
        return repositorio.actualizar(id, limpio).orElseThrow(() -> noExiste(id));
    }

    // Solo se puede eliminar una cuenta sin compras (RF-16)
    public void eliminar(int id) {
        if (!repositorio.eliminar(id)) {
            throw noExiste(id);
        }
    }

    // "1.020.456.789" -> "1020456789" y "311 482 0931" -> "3114820931"
    private static String sinSeparadores(String texto) {
        return texto.replaceAll("[\\s.-]", "");
    }

    private RecursoNoEncontradoException noExiste(int id) {
        return new RecursoNoEncontradoException("No existe el cliente " + id);
    }
}
