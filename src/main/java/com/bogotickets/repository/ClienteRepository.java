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
    private static final String COLUMNAS = """
            id_cliente, tipo_documento, numero_documento, nombres, apellidos, fecha_nacimiento, genero,
            correo, indicativo, celular, id_ciudad, direccion, fecha_registro""";

    private static final RowMapper<Cliente> MAPEO = (rs, fila) -> new Cliente(
            rs.getInt("id_cliente"),
            rs.getString("tipo_documento"),
            rs.getString("numero_documento"),
            rs.getString("nombres"),
            rs.getString("apellidos"),
            rs.getObject("fecha_nacimiento", LocalDate.class),
            rs.getString("genero"),
            rs.getString("correo"),
            rs.getString("indicativo"),
            rs.getString("celular"),
            rs.getInt("id_ciudad"),
            rs.getString("direccion"),
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
                INSERT INTO cliente (tipo_documento, numero_documento, nombres, apellidos, fecha_nacimiento,
                                     genero, correo, indicativo, celular, id_ciudad, direccion, contrasena)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING %s""".formatted(COLUMNAS),
                MAPEO, c.tipoDocumento(), c.numeroDocumento(), c.nombres(), c.apellidos(), c.fechaNacimiento(),
                c.genero(), c.correo(), c.indicativo(), c.celular(), c.idCiudad(), c.direccion(), hashContrasena);
    }

    // El documento y la fecha de nacimiento no se cambian (no tienen permiso de UPDATE)
    public Optional<Cliente> actualizar(int id, Cliente c) {
        return jdbc.query("""
                UPDATE cliente
                SET    nombres = ?, apellidos = ?, genero = ?, correo = ?, indicativo = ?, celular = ?,
                       id_ciudad = ?, direccion = ?
                WHERE  id_cliente = ?
                RETURNING %s""".formatted(COLUMNAS),
                MAPEO, c.nombres(), c.apellidos(), c.genero(), c.correo(), c.indicativo(), c.celular(),
                c.idCiudad(), c.direccion(), id)
                .stream().findFirst();
    }

    // Si el cliente ya compró, la FK fk_compra_cliente (RESTRICT) impide borrarlo
    public boolean eliminar(int id) {
        return jdbc.update("DELETE FROM cliente WHERE id_cliente = ?", id) > 0;
    }
}
