package com.bogotickets.repository;

import com.bogotickets.dto.BoletaDetalle;
import com.bogotickets.dto.CompraRequest;
import com.bogotickets.dto.CompraResumen;
import com.bogotickets.dto.Disponibilidad;
import com.bogotickets.dto.EventoCatalogo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Consultas de la venta: cartelera, disponibilidad, compras y boletas
@Repository
public class VentaRepository {

    private static final RowMapper<EventoCatalogo> MAPEO_CATALOGO = (rs, fila) -> new EventoCatalogo(
            rs.getInt("id_evento"),
            rs.getString("evento"),
            rs.getInt("id_tipo_evento"),
            rs.getString("tipo_evento"),
            rs.getInt("id_escenario"),
            rs.getString("escenario"),
            rs.getString("direccion"),
            rs.getObject("fecha", LocalDate.class),
            rs.getObject("hora_inicio", LocalTime.class),
            rs.getObject("hora_fin", LocalTime.class),
            rs.getBigDecimal("precio_desde"),
            rs.getBigDecimal("precio_hasta"),
            rs.getInt("aforo_total"),
            rs.getInt("boletas_disponibles"));

    private static final RowMapper<Disponibilidad> MAPEO_DISPONIBILIDAD = (rs, fila) -> new Disponibilidad(
            rs.getInt("id_localidad"),
            rs.getString("localidad"),
            rs.getBigDecimal("precio_vigente"),
            rs.getInt("aforo_maximo"),
            rs.getInt("boletas_vendidas"),
            rs.getInt("boletas_disponibles"));

    private static final RowMapper<CompraResumen> MAPEO_COMPRA = (rs, fila) -> new CompraResumen(
            rs.getInt("id_compra"),
            rs.getObject("fecha_compra", LocalDateTime.class),
            rs.getString("metodo_pago"),
            rs.getInt("id_cliente"),
            rs.getString("cliente"),
            rs.getInt("id_evento"),
            rs.getString("evento"),
            rs.getString("tipo_evento"),
            rs.getObject("fecha_evento", LocalDate.class),
            rs.getObject("hora_inicio", LocalTime.class),
            rs.getString("escenario"),
            rs.getString("localidades"),
            rs.getInt("boletas"),
            rs.getInt("boletas_vigentes"),
            rs.getBigDecimal("total"),
            rs.getString("estado"));

    private static final RowMapper<BoletaDetalle> MAPEO_BOLETA = (rs, fila) -> new BoletaDetalle(
            rs.getInt("id_boleta"),
            rs.getInt("id_compra"),
            rs.getObject("fecha_compra", LocalDateTime.class),
            rs.getString("metodo_pago"),
            rs.getInt("id_cliente"),
            rs.getString("cliente"),
            rs.getInt("id_evento"),
            rs.getString("evento"),
            rs.getString("tipo_evento"),
            rs.getObject("fecha_evento", LocalDate.class),
            rs.getObject("hora_inicio", LocalTime.class),
            rs.getString("escenario"),
            rs.getInt("id_localidad"),
            rs.getString("localidad"),
            rs.getBigDecimal("precio_pagado"),
            rs.getString("estado"));

    private final JdbcTemplate jdbc;

    public VentaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Vista v_catalogo_eventos: eventos que aún no se realizan (RF-06)
    public List<EventoCatalogo> catalogo() {
        return jdbc.query("SELECT * FROM v_catalogo_eventos ORDER BY fecha, hora_inicio", MAPEO_CATALOGO);
    }

    // Vista v_ocupacion_localidad: cupos de cada localidad en este momento (RF-07)
    public List<Disponibilidad> disponibilidad(int idEvento) {
        return jdbc.query("""
                SELECT id_localidad, localidad, precio_vigente, aforo_maximo,
                       boletas_vendidas, boletas_disponibles
                FROM   v_ocupacion_localidad
                WHERE  id_evento = ?
                ORDER  BY precio_vigente DESC""",
                MAPEO_DISPONIBILIDAD, idEvento);
    }

    // La función crea la compra y una boleta por cupo en una sola transacción.
    // Si una boleta falla (aforo, evento realizado...), PostgreSQL deshace todo
    // y la excepción llega al ManejadorErrores. Devuelve el id de la compra.
    public int registrarCompra(int idCliente, CompraRequest c) {
        return jdbc.queryForObject("SELECT fn_registrar_compra(?, ?, ?, ?, ?)", Integer.class,
                idCliente, c.idEvento(), c.idLocalidad(), c.cantidad(), c.metodoPago());
    }

    // El procedimiento pasa las boletas a Cancelada; no borra nada (RNF-07)
    public void anularCompra(int idCompra) {
        jdbc.update("CALL sp_anular_compra(?)", idCompra);
    }

    // Vista v_compras_resumen: total, boletas vigentes y estado de la compra
    public Optional<CompraResumen> buscarCompra(int idCompra) {
        return jdbc.query("SELECT * FROM v_compras_resumen WHERE id_compra = ?", MAPEO_COMPRA, idCompra)
                .stream().findFirst();
    }

    // Compras para el administrador, de la más reciente a la más antigua.
    // Los filtros son opcionales y van como parámetros (?), nunca pegados al SQL.
    public List<CompraResumen> listarCompras(Integer idEvento, Integer idCliente, int limite) {
        StringBuilder sql = new StringBuilder("SELECT * FROM v_compras_resumen WHERE 1 = 1");
        List<Object> parametros = new ArrayList<>();
        if (idEvento != null) {
            sql.append(" AND id_evento = ?");
            parametros.add(idEvento);
        }
        if (idCliente != null) {
            sql.append(" AND id_cliente = ?");
            parametros.add(idCliente);
        }
        sql.append(" ORDER BY fecha_compra DESC LIMIT ?");
        parametros.add(limite);
        return jdbc.query(sql.toString(), MAPEO_COMPRA, parametros.toArray());
    }

    // Historial de compras de un cliente (RF-09)
    public List<CompraResumen> comprasDeCliente(int idCliente) {
        return jdbc.query("""
                SELECT *
                FROM   v_compras_resumen
                WHERE  id_cliente = ?
                ORDER  BY fecha_compra DESC""",
                MAPEO_COMPRA, idCliente);
    }

    // Vista v_boletas_detalle: reemplaza el JOIN de 7 tablas
    public List<BoletaDetalle> boletasDeCompra(int idCompra) {
        return jdbc.query("SELECT * FROM v_boletas_detalle WHERE id_compra = ? ORDER BY id_boleta",
                MAPEO_BOLETA, idCompra);
    }

    public Optional<BoletaDetalle> buscarBoleta(int idBoleta) {
        return jdbc.query("SELECT * FROM v_boletas_detalle WHERE id_boleta = ?", MAPEO_BOLETA, idBoleta)
                .stream().findFirst();
    }

    public List<BoletaDetalle> boletasDeCliente(int idCliente) {
        return jdbc.query("""
                SELECT *
                FROM   v_boletas_detalle
                WHERE  id_cliente = ?
                ORDER  BY fecha_compra DESC, id_boleta""",
                MAPEO_BOLETA, idCliente);
    }
}
