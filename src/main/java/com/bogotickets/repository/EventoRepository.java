package com.bogotickets.repository;

import com.bogotickets.model.Evento;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class EventoRepository {

    private static final RowMapper<Evento> MAPEO = (rs, fila) -> new Evento(
            rs.getInt("id_evento"),
            rs.getInt("id_escenario"),
            rs.getInt("id_tipo_evento"),
            rs.getString("nombre"),
            rs.getObject("fecha", LocalDate.class),
            rs.getObject("hora_inicio", LocalTime.class),
            rs.getObject("hora_fin", LocalTime.class),
            rs.getInt("edad_minima"),
            rs.getObject("fecha_registro", LocalDate.class));

    private final JdbcTemplate jdbc;

    public EventoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Los dos filtros son opcionales: el WHERE se arma solo con los que llegan
    // y sus valores van como parámetros (?), nunca pegados al texto del SQL
    public List<Evento> listar(Integer idEscenario, Integer idTipoEvento) {
        StringBuilder sql = new StringBuilder("SELECT * FROM evento WHERE 1 = 1");
        List<Object> parametros = new ArrayList<>();
        if (idEscenario != null) {
            sql.append(" AND id_escenario = ?");
            parametros.add(idEscenario);
        }
        if (idTipoEvento != null) {
            sql.append(" AND id_tipo_evento = ?");
            parametros.add(idTipoEvento);
        }
        sql.append(" ORDER BY fecha, hora_inicio");
        return jdbc.query(sql.toString(), MAPEO, parametros.toArray());
    }

    public Optional<Evento> buscarPorId(int id) {
        return jdbc.query("SELECT * FROM evento WHERE id_evento = ?", MAPEO, id)
                .stream().findFirst();
    }

    // trg_evento_horario rechaza el evento si se cruza con otro del mismo escenario
    public Evento crear(Evento e) {
        return jdbc.queryForObject("""
                INSERT INTO evento (id_escenario, id_tipo_evento, nombre, fecha, hora_inicio, hora_fin, edad_minima)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING *""",
                MAPEO, e.idEscenario(), e.idTipoEvento(), e.nombre(), e.fecha(), e.horaInicio(), e.horaFin(),
                e.edadMinima());
    }

    // El escenario no se cambia: las tarifas y boletas del evento dependen de él.
    // Un cambio de edad mínima no afecta las boletas ya vendidas.
    public Optional<Evento> actualizar(int id, Evento e) {
        return jdbc.query("""
                UPDATE evento
                SET    nombre = ?, id_tipo_evento = ?, fecha = ?, hora_inicio = ?, hora_fin = ?, edad_minima = ?
                WHERE  id_evento = ?
                RETURNING *""",
                MAPEO, e.nombre(), e.idTipoEvento(), e.fecha(), e.horaInicio(), e.horaFin(), e.edadMinima(), id)
                .stream().findFirst();
    }

    public boolean eliminar(int id) {
        return jdbc.update("DELETE FROM evento WHERE id_evento = ?", id) > 0;
    }
}
