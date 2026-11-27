package com.bogotickets.repository;

import com.bogotickets.model.Localidad;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class LocalidadRepository {

    private static final RowMapper<Localidad> MAPEO = (rs, fila) -> new Localidad(
            rs.getInt("id_localidad"),
            rs.getInt("id_escenario"),
            rs.getString("nombre"),
            rs.getInt("aforo_maximo"));

    private final JdbcTemplate jdbc;

    public LocalidadRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Sin escenario devuelve todas las localidades
    public List<Localidad> listar(Integer idEscenario) {
        if (idEscenario == null) {
            return jdbc.query("SELECT * FROM localidad ORDER BY id_escenario, id_localidad", MAPEO);
        }
        return jdbc.query("SELECT * FROM localidad WHERE id_escenario = ? ORDER BY id_localidad",
                MAPEO, idEscenario);
    }

    public Optional<Localidad> buscarPorId(int id) {
        return jdbc.query("SELECT * FROM localidad WHERE id_localidad = ?", MAPEO, id)
                .stream().findFirst();
    }

    public Localidad crear(Localidad l) {
        return jdbc.queryForObject("""
                INSERT INTO localidad (id_escenario, nombre, aforo_maximo)
                VALUES (?, ?, ?)
                RETURNING *""",
                MAPEO, l.idEscenario(), l.nombre(), l.aforoMaximo());
    }

    // El escenario de una localidad no se cambia; si cambia el aforo,
    // el trigger trg_auditar_aforo lo registra en historial_precios
    public Optional<Localidad> actualizar(int id, Localidad l) {
        return jdbc.query("""
                UPDATE localidad
                SET    nombre = ?, aforo_maximo = ?
                WHERE  id_localidad = ?
                RETURNING *""",
                MAPEO, l.nombre(), l.aforoMaximo(), id)
                .stream().findFirst();
    }

    public boolean eliminar(int id) {
        return jdbc.update("DELETE FROM localidad WHERE id_localidad = ?", id) > 0;
    }
}
