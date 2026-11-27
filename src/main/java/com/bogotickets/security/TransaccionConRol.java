package com.bogotickets.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

// La API se conecta como usuario_app, que por sí solo no tiene permisos sobre las
// tablas (NOINHERIT). Cada método @Transactional de los servicios abre una transacción
// y aquí se le asigna el rol de PostgreSQL de quien hizo la petición:
//   SET LOCAL ROLE rol_cliente | rol_administrador | rol_analista
// Así la base aplica los GRANT de sql/05_usuarios_permisos.sql a cada actor. LOCAL
// hace que el rol dure solo hasta el COMMIT o ROLLBACK: la conexión vuelve limpia al pool.
@Configuration
public class TransaccionConRol {

    @Bean
    public JdbcTransactionManager transactionManager(DataSource dataSource) {
        return new JdbcTransactionManager(dataSource) {
            @Override
            protected void prepareTransactionalConnection(Connection con, TransactionDefinition definicion)
                    throws SQLException {
                super.prepareTransactionalConnection(con, definicion);
                UsuarioSesion usuario = SesionActual.usuario();
                // Sin sesión solo se permite registrarse, que es una acción del cliente
                String rol = usuario == null ? "rol_cliente" : usuario.rolBaseDatos();
                try (Statement st = con.createStatement()) {
                    st.execute("SET LOCAL ROLE " + rol);    // valor fijo, nunca viene de la petición
                }
                // Los triggers de auditoría guardan este correo en historial_precios.usuario
                try (PreparedStatement ps = con.prepareStatement("SELECT set_config('bogotickets.usuario', ?, true)")) {
                    ps.setString(1, usuario == null ? "registro web" : usuario.getUsername());
                    ps.execute();
                }
            }
        };
    }
}
