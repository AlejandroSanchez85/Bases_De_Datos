-- =====================================================================
-- BogoTickets · 05 · Roles, usuarios y permisos (dcl.sql)
-- Ejecutar después de 04_datos.sql, conectado como postgres.
--
-- Los permisos se asignan a un rol por cada actor (sin inicio de sesión)
-- y los usuarios heredan esos roles (RF-12):
--   rol_cliente        compra boletas y administra su cuenta
--   rol_administrador  catálogo, clientes y anulación de compras
--   rol_analista       solo vistas de reporte (RNF-04)
--
--   usuario_app        cuenta con la que la API (Spring Boot) se conecta.
--                      Es NOINHERIT: por sí sola solo lee v_cuentas para el
--                      inicio de sesión. En cada transacción la API ejecuta
--                      SET LOCAL ROLE con el rol de quien inició sesión, así
--                      PostgreSQL aplica a cada petición los permisos de su
--                      actor. No es una persona.
--   admin_escenarios   administrador que trabaja directamente en pgAdmin
--   usuario_reportes   analista que consulta desde pgAdmin
--
-- Nadie tiene GRANT ALL, TRUNCATE ni permiso para crear tablas. Ningún rol
-- puede borrar boletas, compras ni historial (RNF-07) ni leer contraseñas.
-- Las contraseñas de los usuarios de PostgreSQL son de ejemplo.
-- =====================================================================

-- Oculta los avisos de GRANT y REVOKE repetidos al volver a ejecutar el script
SET client_min_messages = error;


-- 1. Roles por actor y usuarios.
-- Los usuarios de PostgreSQL son del servidor, no de una base. Si ya existen
-- (por ejemplo, de otra base del mismo servidor) no se borran: se les quitan
-- los permisos que tenían en esta base y se ajustan sus atributos.
DO $$
DECLARE
    r TEXT;
BEGIN
    FOREACH r IN ARRAY ARRAY['rol_cliente', 'rol_administrador', 'rol_analista',
                             'usuario_app', 'admin_escenarios', 'usuario_reportes'] LOOP
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = r) THEN
            EXECUTE format('DROP OWNED BY %I', r);
        ELSE
            EXECUTE format('CREATE ROLE %I', r);
        END IF;
    END LOOP;

    -- La versión anterior del proyecto llamaba rol_lectura al rol del analista
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'rol_lectura') THEN
        DROP OWNED BY rol_lectura;
        REVOKE rol_lectura FROM usuario_app, usuario_reportes;
    END IF;
END $$;

ALTER ROLE rol_cliente       NOLOGIN;
ALTER ROLE rol_administrador NOLOGIN;
ALTER ROLE rol_analista      NOLOGIN;

ALTER ROLE usuario_app      LOGIN NOINHERIT PASSWORD 'Cambiar_App_2026';
ALTER ROLE admin_escenarios LOGIN INHERIT   PASSWORD 'Cambiar_Adm_2026';
ALTER ROLE usuario_reportes LOGIN INHERIT   PASSWORD 'Cambiar_Rep_2026';

-- Se quitan y se vuelven a dar para que la pertenencia quede con los
-- atributos de arriba (usuario_app no hereda: tiene que hacer SET ROLE)
REVOKE rol_cliente, rol_administrador, rol_analista FROM usuario_app, admin_escenarios, usuario_reportes;
GRANT rol_cliente, rol_administrador, rol_analista TO usuario_app;
GRANT rol_administrador TO admin_escenarios;
GRANT rol_analista      TO usuario_reportes;


-- 2. Nada por defecto: se quitan los permisos que PostgreSQL da a PUBLIC
REVOKE ALL     ON ALL TABLES   IN SCHEMA public FROM PUBLIC;
REVOKE EXECUTE ON ALL ROUTINES IN SCHEMA public FROM PUBLIC;
REVOKE CREATE  ON SCHEMA public FROM PUBLIC;

DO $$
BEGIN
    EXECUTE format('GRANT CONNECT ON DATABASE %I TO usuario_app, admin_escenarios, usuario_reportes',
                   current_database());
END $$;
GRANT USAGE ON SCHEMA public TO usuario_app, rol_cliente, rol_administrador, rol_analista;


-- 3. usuario_app sin rol activo: solo el inicio de sesión
GRANT SELECT ON v_cuentas TO usuario_app;


-- 4. rol_cliente: registro, catálogo, compra e historial propio (RF-05 a RF-09)
-- La contraseña se puede escribir (registro) pero no leer: el SELECT es por columnas.
GRANT SELECT ON escenario, localidad, tipo_evento, evento, precio_localidad TO rol_cliente;
GRANT SELECT (id_cliente, documento, nombre_completo, correo, telefono, fecha_registro)
      ON cliente TO rol_cliente;
GRANT INSERT, DELETE ON cliente TO rol_cliente;          -- DELETE: solo cuentas sin compras (FK)
GRANT UPDATE (nombre_completo, correo, telefono) ON cliente TO rol_cliente;
GRANT SELECT, INSERT ON compra, boleta TO rol_cliente;
GRANT SELECT ON v_catalogo_eventos, v_ocupacion_localidad, v_boletas_detalle, v_compras_resumen
      TO rol_cliente;
GRANT EXECUTE ON FUNCTION fn_registrar_compra(INTEGER, INTEGER, INTEGER, INTEGER, VARCHAR) TO rol_cliente;


-- 5. rol_administrador: catálogo, clientes y anulación de compras (RF-01 a RF-04, RF-13)
GRANT SELECT, INSERT, DELETE ON escenario, localidad, tipo_evento, evento, precio_localidad
      TO rol_administrador;                    -- las FK RESTRICT protegen lo que tiene ventas
GRANT UPDATE (nombre, direccion, capacidad_maxima)                   ON escenario        TO rol_administrador;
GRANT UPDATE (nombre, aforo_maximo)                                  ON localidad        TO rol_administrador;
GRANT UPDATE (nombre, descripcion)                                   ON tipo_evento      TO rol_administrador;
GRANT UPDATE (nombre, id_tipo_evento, fecha, hora_inicio, hora_fin)  ON evento           TO rol_administrador;
GRANT UPDATE (precio)                                                ON precio_localidad TO rol_administrador;
GRANT SELECT (id_cliente, documento, nombre_completo, correo, telefono, fecha_registro)
      ON cliente TO rol_administrador;
GRANT INSERT, DELETE ON cliente TO rol_administrador;
GRANT UPDATE (nombre_completo, correo, telefono) ON cliente TO rol_administrador;
GRANT SELECT, INSERT ON historial_precios TO rol_administrador;     -- el trigger de auditoría escribe aquí
GRANT SELECT ON compra, boleta TO rol_administrador;
GRANT UPDATE (estado) ON boleta TO rol_administrador;               -- anular, nunca borrar
GRANT SELECT ON v_ocupacion_localidad, v_boletas_detalle, v_boletas_vigentes, v_compras_resumen
      TO rol_administrador;
GRANT EXECUTE ON PROCEDURE sp_anular_compra(INTEGER) TO rol_administrador;


-- 6. rol_analista: solo vistas, ninguna tabla base (RNF-04)
GRANT SELECT ON v_catalogo_eventos, v_ocupacion_localidad, v_boletas_detalle, v_clientes_publico
      TO rol_analista;


-- Verificación: usuarios, si inician sesión, si heredan y qué roles tienen
SELECT r.rolname                                          AS usuario_o_rol,
       r.rolcanlogin                                      AS inicia_sesion,
       r.rolinherit                                       AS hereda,
       COALESCE(string_agg(g.rolname, ', ' ORDER BY g.rolname), '—') AS roles
FROM   pg_roles r
LEFT   JOIN pg_auth_members m ON m.member = r.oid
LEFT   JOIN pg_roles g        ON g.oid    = m.roleid
WHERE  r.rolname IN ('usuario_app', 'admin_escenarios', 'usuario_reportes',
                     'rol_cliente', 'rol_administrador', 'rol_analista')
GROUP  BY r.rolname, r.rolcanlogin, r.rolinherit
ORDER  BY r.rolcanlogin DESC, r.rolname;
