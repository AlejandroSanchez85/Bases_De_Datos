package com.bogotickets.repository;

import com.bogotickets.model.TipoEvento;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TipoEventoRepository {

    private static final RowMapper<TipoEvento> MAPEO = (rs, fila) -> new TipoEvento(
            rs.getInt("id_tipo_evento"),
            rs.getString("nombre"),
            rs.getString("descripcion"));

    private final JdbcTemplate jdbc;

    public TipoEventoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<TipoEvento> listar() {
        return jdbc.query("SELECT * FROM tipo_evento ORDER BY nombre", MAPEO);
    }

    public Optional<TipoEvento> buscarPorId(int id) {
        return jdbc.query("SELECT * FROM tipo_evento WHERE id_tipo_evento = ?", MAPEO, id)
                .stream().findFirst();
    }

    public TipoEvento crear(TipoEvento t) {
        return jdbc.queryForObject("""
                INSERT INTO tipo_evento (nombre, descripcion)
                VALUES (?, ?)
                RETURNING *""",
                MAPEO, t.nombre(), t.descripcion());
    }

    public Optional<TipoEvento> actualizar(int id, TipoEvento t) {
        return jdbc.query("""
                UPDATE tipo_evento
                SET    nombre = ?, descripcion = ?
                WHERE  id_tipo_evento = ?
                RETURNING *""",
                MAPEO, t.nombre(), t.descripcion(), id)
                .stream().findFirst();
    }

    public boolean eliminar(int id) {
        return jdbc.update("DELETE FROM tipo_evento WHERE id_tipo_evento = ?", id) > 0;
    }
}
