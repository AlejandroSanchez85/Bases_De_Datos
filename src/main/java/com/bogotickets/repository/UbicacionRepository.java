package com.bogotickets.repository;

import com.bogotickets.dto.Ubicacion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

// Países, departamentos y ciudades para los formularios de cliente
@Repository
public class UbicacionRepository {

    private final JdbcTemplate jdbc;

    public UbicacionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Vista v_ubicaciones: une ciudad, departamento y país
    public List<Ubicacion> listar() {
        return jdbc.query("SELECT * FROM v_ubicaciones ORDER BY pais, departamento, ciudad",
                (rs, fila) -> new Ubicacion(
                        rs.getInt("id_ciudad"),
                        rs.getString("ciudad"),
                        rs.getInt("id_departamento"),
                        rs.getString("departamento"),
                        rs.getInt("id_pais"),
                        rs.getString("pais"),
                        rs.getString("indicativo")));
    }
}
