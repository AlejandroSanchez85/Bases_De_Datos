package com.bogotickets.repository;

import com.bogotickets.model.Escenario;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class EscenarioRepository {

    // Convierte cada fila del resultado en un objeto Escenario
    private static final RowMapper<Escenario> MAPEO = (rs, fila) -> new Escenario(
            rs.getInt("id_escenario"),
            rs.getString("nombre"),
            rs.getString("direccion"),
            rs.getInt("capacidad_maxima"));

    private final JdbcTemplate jdbc;

    public EscenarioRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Escenario> listar() {
        return jdbc.query("SELECT * FROM escenario ORDER BY id_escenario", MAPEO);
    }

    public Optional<Escenario> buscarPorId(int id) {
        return jdbc.query("SELECT * FROM escenario WHERE id_escenario = ?", MAPEO, id)
                .stream().findFirst();
    }

    // RETURNING devuelve la fila guardada, con el id que generó PostgreSQL
    public Escenario crear(Escenario e) {
        return jdbc.queryForObject("""
                INSERT INTO escenario (nombre, direccion, capacidad_maxima)
                VALUES (?, ?, ?)
                RETURNING *""",
                MAPEO, e.nombre(), e.direccion(), e.capacidadMaxima());
    }

    // Vacío si el id no existe (el UPDATE no afectó ninguna fila)
    public Optional<Escenario> actualizar(int id, Escenario e) {
        return jdbc.query("""
                UPDATE escenario
                SET    nombre = ?, direccion = ?, capacidad_maxima = ?
                WHERE  id_escenario = ?
                RETURNING *""",
                MAPEO, e.nombre(), e.direccion(), e.capacidadMaxima(), id)
                .stream().findFirst();
    }

    public boolean eliminar(int id) {
        return jdbc.update("DELETE FROM escenario WHERE id_escenario = ?", id) > 0;
    }
}
