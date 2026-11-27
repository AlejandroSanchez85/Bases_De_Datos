package com.bogotickets.repository;

import com.bogotickets.dto.BoletaDetalle;
import com.bogotickets.dto.CompraDetalle;
import com.bogotickets.dto.CompraRequest;
import com.bogotickets.dto.Disponibilidad;
import com.bogotickets.dto.EventoCatalogo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

// Consultas del módulo del cliente: catálogo, disponibilidad, compras y boletas
@Repository
public class VentaRepository {

    private static final RowMapper<EventoCatalogo> MAPEO_CATALOGO = (rs, fila) -> new EventoCatalogo(
            rs.getInt("id_evento"),
            rs.getString("evento"),
            rs.getString("tipo_evento"),
            rs.getString("escenario"),
            rs.getString("direccion"),
            rs.getObject("fecha", LocalDate.class),
            rs.getObject("hora_inicio", LocalTime.class),
            rs.getObject("hora_fin", LocalTime.class),
            rs.getBigDecimal("precio_desde"),
            rs.getInt("boletas_disponibles"));

    private static final RowMapper<Disponibilidad> MAPEO_DISPONIBILIDAD = (rs, fila) -> new Disponibilidad(
            rs.getInt("id_localidad"),
            rs.getString("localidad"),
            rs.getBigDecimal("precio_vigente"),
            rs.getInt("aforo_maximo"),
            rs.getInt("boletas_vendidas"),
            rs.getInt("boletas_disponibles"));

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
    public int registrarCompra(CompraRequest c) {
        return jdbc.queryForObject("SELECT fn_registrar_compra(?, ?, ?, ?, ?)", Integer.class,
                c.idCliente(), c.idEvento(), c.idLocalidad(), c.cantidad(), c.metodoPago());
    }

    // El procedimiento pasa las boletas a Cancelada; no borra nada (RNF-07)
    public void anularCompra(int idCompra) {
        jdbc.update("CALL sp_anular_compra(?)", idCompra);
    }

    public Optional<CompraDetalle> buscarCompra(int idCompra) {
        List<BoletaDetalle> boletas = jdbc.query(
                "SELECT * FROM v_boletas_detalle WHERE id_compra = ? ORDER BY id_boleta",
                MAPEO_BOLETA, idCompra);

        // El total sale de una subconsulta en el SELECT sobre las boletas de la compra
        return jdbc.query("""
                SELECT co.id_compra, co.id_cliente, c.nombre_completo, co.fecha_compra, co.metodo_pago,
                       (SELECT COALESCE(SUM(b.precio_pagado), 0)
                        FROM   boleta b
                        WHERE  b.id_compra = co.id_compra) AS total
                FROM   compra co
                JOIN   cliente c ON c.id_cliente = co.id_cliente
                WHERE  co.id_compra = ?""",
                (rs, fila) -> new CompraDetalle(
                        rs.getInt("id_compra"),
                        rs.getInt("id_cliente"),
                        rs.getString("nombre_completo"),
                        rs.getObject("fecha_compra", LocalDateTime.class),
                        rs.getString("metodo_pago"),
                        rs.getBigDecimal("total"),
                        boletas),
                idCompra).stream().findFirst();
    }

    // Vista v_boletas_detalle: reemplaza el JOIN de 7 tablas
    public Optional<BoletaDetalle> buscarBoleta(int idBoleta) {
        return jdbc.query("SELECT * FROM v_boletas_detalle WHERE id_boleta = ?", MAPEO_BOLETA, idBoleta)
                .stream().findFirst();
    }

    // Historial de compras del cliente (RF-09)
    public List<BoletaDetalle> boletasDeCliente(int idCliente) {
        return jdbc.query("""
                SELECT *
                FROM   v_boletas_detalle
                WHERE  id_cliente = ?
                ORDER  BY fecha_compra DESC, id_boleta""",
                MAPEO_BOLETA, idCliente);
    }
}
