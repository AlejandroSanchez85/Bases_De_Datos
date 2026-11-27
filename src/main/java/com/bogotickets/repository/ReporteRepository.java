package com.bogotickets.repository;

import com.bogotickets.dto.ClientePublico;
import com.bogotickets.dto.ClienteSinCompras;
import com.bogotickets.dto.EventoBajaOcupacion;
import com.bogotickets.dto.HistorialPrecio;
import com.bogotickets.dto.OcupacionLocalidad;
import com.bogotickets.dto.PrecioSobrePromedio;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// Consultas de solo lectura del analista y del historial de precios.
// Son las mismas de sql/06_consultas_api.sql.
@Repository
public class ReporteRepository {

    private static final RowMapper<OcupacionLocalidad> MAPEO_OCUPACION = (rs, fila) -> new OcupacionLocalidad(
            rs.getInt("id_evento"),
            rs.getString("evento"),
            rs.getObject("fecha_evento", LocalDate.class),
            rs.getString("escenario"),
            rs.getInt("id_localidad"),
            rs.getString("localidad"),
            rs.getBigDecimal("precio_vigente"),
            rs.getInt("aforo_maximo"),
            rs.getInt("boletas_vendidas"),
            rs.getInt("boletas_disponibles"),
            rs.getBigDecimal("porcentaje_ocupacion"),
            rs.getBigDecimal("recaudo"));

    private static final RowMapper<HistorialPrecio> MAPEO_HISTORIAL = (rs, fila) -> new HistorialPrecio(
            rs.getInt("id_historial"),
            rs.getObject("fecha_cambio", LocalDateTime.class),
            rs.getString("campo_modificado"),
            rs.getString("evento"),
            rs.getString("localidad"),
            rs.getBigDecimal("valor_anterior"),
            rs.getBigDecimal("valor_nuevo"),
            rs.getString("usuario"));

    private final JdbcTemplate jdbc;

    public ReporteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Vista v_ocupacion_localidad: vendidas, % de ocupación y recaudo (RF-10)
    public List<OcupacionLocalidad> ocupacion(Integer idEvento) {
        if (idEvento == null) {
            return jdbc.query("""
                    SELECT *
                    FROM   v_ocupacion_localidad
                    ORDER  BY fecha_evento, evento, precio_vigente DESC""",
                    MAPEO_OCUPACION);
        }
        return jdbc.query("""
                SELECT *
                FROM   v_ocupacion_localidad
                WHERE  id_evento = ?
                ORDER  BY precio_vigente DESC""",
                MAPEO_OCUPACION, idEvento);
    }

    // SUBCONSULTA EN FROM: la interna suma por evento los cupos y las ventas de
    // todas sus localidades; la externa deja los que están por debajo del umbral (RF-11)
    public List<EventoBajaOcupacion> eventosBajaOcupacion(BigDecimal umbral) {
        return jdbc.query("""
                SELECT t.id_evento, t.evento, t.fecha_evento, t.aforo_total, t.boletas_vendidas,
                       t.porcentaje_ocupacion
                FROM  (SELECT id_evento, evento, fecha_evento,
                              SUM(aforo_maximo)     AS aforo_total,
                              SUM(boletas_vendidas) AS boletas_vendidas,
                              ROUND(100.0 * SUM(boletas_vendidas) / SUM(aforo_maximo), 2) AS porcentaje_ocupacion
                       FROM   v_ocupacion_localidad
                       GROUP  BY id_evento, evento, fecha_evento) t
                WHERE  t.porcentaje_ocupacion < ?
                ORDER  BY t.porcentaje_ocupacion, t.fecha_evento""",
                (rs, fila) -> new EventoBajaOcupacion(
                        rs.getInt("id_evento"),
                        rs.getString("evento"),
                        rs.getObject("fecha_evento", LocalDate.class),
                        rs.getInt("aforo_total"),
                        rs.getInt("boletas_vendidas"),
                        rs.getBigDecimal("porcentaje_ocupacion")),
                umbral);
    }

    // SUBCONSULTA ESCALAR EN WHERE: el promedio de todas las tarifas se calcula
    // una vez y cada tarifa se compara contra él
    public List<PrecioSobrePromedio> preciosSobrePromedio() {
        return jdbc.query("""
                SELECT e.id_evento, e.nombre AS evento, l.nombre AS localidad, pl.precio,
                       (SELECT ROUND(AVG(precio), 2) FROM precio_localidad) AS promedio
                FROM   precio_localidad pl
                JOIN   evento    e ON e.id_evento    = pl.id_evento
                JOIN   localidad l ON l.id_localidad = pl.id_localidad
                WHERE  pl.precio > (SELECT AVG(precio) FROM precio_localidad)
                ORDER  BY pl.precio DESC, e.nombre""",
                (rs, fila) -> new PrecioSobrePromedio(
                        rs.getInt("id_evento"),
                        rs.getString("evento"),
                        rs.getString("localidad"),
                        rs.getBigDecimal("precio"),
                        rs.getBigDecimal("promedio")));
    }

    // SUBCONSULTA NOT EXISTS: clientes que no tienen ninguna compra
    public List<ClienteSinCompras> clientesSinCompras() {
        return jdbc.query("""
                SELECT c.id_cliente, c.nombre_completo, c.fecha_registro
                FROM   cliente c
                WHERE  NOT EXISTS (SELECT 1 FROM compra co WHERE co.id_cliente = c.id_cliente)
                ORDER  BY c.id_cliente""",
                (rs, fila) -> new ClienteSinCompras(
                        rs.getInt("id_cliente"),
                        rs.getString("nombre_completo"),
                        rs.getObject("fecha_registro", LocalDate.class)));
    }

    // Vista v_clientes_publico: comportamiento de compra sin datos sensibles (RNF-04)
    public List<ClientePublico> clientes() {
        return jdbc.query("""
                SELECT *
                FROM   v_clientes_publico
                ORDER  BY boletas_vigentes DESC, id_cliente""",
                (rs, fila) -> new ClientePublico(
                        rs.getInt("id_cliente"),
                        rs.getString("nombre_completo"),
                        rs.getString("correo_oculto"),
                        rs.getObject("fecha_registro", LocalDate.class),
                        rs.getInt("compras"),
                        rs.getInt("boletas_vigentes")));
    }

    // Cambios de precio y aforo que registraron los triggers de auditoría (RNF-06)
    public List<HistorialPrecio> historialPrecios(Integer idEvento) {
        String sql = """
                SELECT h.id_historial, h.fecha_cambio, h.campo_modificado, e.nombre AS evento,
                       l.nombre AS localidad, h.valor_anterior, h.valor_nuevo, h.usuario
                FROM   historial_precios h
                JOIN   localidad l ON l.id_localidad = h.id_localidad
                LEFT   JOIN evento e ON e.id_evento = h.id_evento
                """;
        if (idEvento == null) {
            return jdbc.query(sql + "ORDER BY h.fecha_cambio DESC", MAPEO_HISTORIAL);
        }
        return jdbc.query(sql + "WHERE h.id_evento = ? ORDER BY h.fecha_cambio DESC", MAPEO_HISTORIAL, idEvento);
    }
}
