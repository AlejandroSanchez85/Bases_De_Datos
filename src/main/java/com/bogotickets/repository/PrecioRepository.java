package com.bogotickets.repository;

import com.bogotickets.model.PrecioLocalidad;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public class PrecioRepository {

    private static final RowMapper<PrecioLocalidad> MAPEO = (rs, fila) -> new PrecioLocalidad(
            rs.getInt("id_evento"),
            rs.getInt("id_localidad"),
            rs.getString("localidad"),
            rs.getBigDecimal("precio"));

    private static final String SELECT_TARIFAS = """
            SELECT pl.id_evento, pl.id_localidad, l.nombre AS localidad, pl.precio
            FROM   precio_localidad pl
            JOIN   localidad l ON l.id_localidad = pl.id_localidad
            """;

    private final JdbcTemplate jdbc;

    public PrecioRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<PrecioLocalidad> listarPorEvento(int idEvento) {
        return jdbc.query(SELECT_TARIFAS + "WHERE pl.id_evento = ? ORDER BY pl.precio DESC",
                MAPEO, idEvento);
    }

    public Optional<PrecioLocalidad> buscar(int idEvento, int idLocalidad) {
        return jdbc.query(SELECT_TARIFAS + "WHERE pl.id_evento = ? AND pl.id_localidad = ?",
                MAPEO, idEvento, idLocalidad).stream().findFirst();
    }

    // trg_precio_localidad verifica que la localidad sea del escenario del evento
    public void crear(int idEvento, int idLocalidad, BigDecimal precio) {
        jdbc.update("INSERT INTO precio_localidad (id_evento, id_localidad, precio) VALUES (?, ?, ?)",
                idEvento, idLocalidad, precio);
    }

    // trg_auditar_precio guarda el valor anterior y el nuevo en historial_precios.
    // Las boletas ya vendidas conservan su precio_pagado.
    public boolean actualizar(int idEvento, int idLocalidad, BigDecimal precio) {
        return jdbc.update("UPDATE precio_localidad SET precio = ? WHERE id_evento = ? AND id_localidad = ?",
                precio, idEvento, idLocalidad) > 0;
    }

    public boolean eliminar(int idEvento, int idLocalidad) {
        return jdbc.update("DELETE FROM precio_localidad WHERE id_evento = ? AND id_localidad = ?",
                idEvento, idLocalidad) > 0;
    }
}
