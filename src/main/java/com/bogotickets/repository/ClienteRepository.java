package com.bogotickets.repository;

import com.bogotickets.dto.RegistroRequest;
import com.bogotickets.model.Cliente;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class ClienteRepository {

    // La columna contrasena no está aquí: ningún rol tiene permiso de leerla
    private static final String COLUMNAS = "id_cliente, documento, nombre_completo, correo, telefono, fecha_registro";

    private static final RowMapper<Cliente> MAPEO = (rs, fila) -> new Cliente(
            rs.getInt("id_cliente"),
            rs.getString("documento"),
            rs.getString("nombre_completo"),
            rs.getString("correo"),
            rs.getString("telefono"),
            rs.getObject("fecha_registro", LocalDate.class));

    private final JdbcTemplate jdbc;

    public ClienteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Cliente> listar() {
        return jdbc.query("SELECT " + COLUMNAS + " FROM cliente ORDER BY id_cliente", MAPEO);
    }

    public Optional<Cliente> buscarPorId(int id) {
        return jdbc.query("SELECT " + COLUMNAS + " FROM cliente WHERE id_cliente = ?", MAPEO, id)
                .stream().findFirst();
    }

    // Recibe la contraseña ya convertida en hash BCrypt
    public Cliente crear(RegistroRequest c, String hashContrasena) {
        return jdbc.queryForObject("""
                INSERT INTO cliente (documento, nombre_completo, correo, telefono, contrasena)
                VALUES (?, ?, ?, ?, ?)
                RETURNING %s""".formatted(COLUMNAS),
                MAPEO, c.documento(), c.nombreCompleto(), c.correo(), c.telefono(), hashContrasena);
    }

    // El documento no se cambia: identifica a la persona
    public Optional<Cliente> actualizar(int id, Cliente c) {
        return jdbc.query("""
                UPDATE cliente
                SET    nombre_completo = ?, correo = ?, telefono = ?
                WHERE  id_cliente = ?
                RETURNING %s""".formatted(COLUMNAS),
                MAPEO, c.nombreCompleto(), c.correo(), c.telefono(), id)
                .stream().findFirst();
    }

    // Si el cliente ya compró, la FK fk_compra_cliente (RESTRICT) impide borrarlo
    public boolean eliminar(int id) {
        return jdbc.update("DELETE FROM cliente WHERE id_cliente = ?", id) > 0;
    }
}
