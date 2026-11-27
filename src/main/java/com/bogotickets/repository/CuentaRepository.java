package com.bogotickets.repository;

import com.bogotickets.security.UsuarioSesion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// Inicio de sesión. Corre fuera de una transacción, como usuario_app sin rol activo:
// lo único que ese usuario puede leer es la vista v_cuentas.
@Repository
public class CuentaRepository {

    private final JdbcTemplate jdbc;

    public CuentaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UsuarioSesion> buscarPorCorreo(String correo) {
        return jdbc.query("""
                SELECT correo, contrasena, rol, id_cliente, nombre_completo
                FROM   v_cuentas
                WHERE  correo = ?""",
                (rs, fila) -> new UsuarioSesion(
                        rs.getString("correo"),
                        rs.getString("contrasena"),
                        rs.getString("rol"),
                        rs.getObject("id_cliente", Integer.class),
                        rs.getString("nombre_completo")),
                correo).stream().findFirst();
    }
}
