-- =====================================================================
-- BogoTickets · 05 · Roles, usuarios y permisos (dcl.sql)
-- Ejecutar después de 04_datos.sql, conectado como postgres.
--
-- Los permisos se asignan a un rol por cada actor (sin inicio de sesión)
-- y los usuarios heredan esos roles (RF-12):
--   rol_cliente        compra boletas y administra su cuenta
--   rol_administrador  catálogo: escenarios, localidades, eventos y precios
--   rol_lectura        analista: solo vistas de reporte (RNF-04)
--
--   usuario_app        cuenta con la que la API (Spring Boot) se conecta a
--                      la base. Atiende los tres módulos de la web, por eso
--                      hereda los tres roles. No es una persona.
--   admin_escenarios   administrador de escenarios que trabaja en pgAdmin
--   usuario_reportes   analista de ventas que consulta desde pgAdmin
--
-- Nadie tiene GRANT ALL, TRUNCATE ni permiso para crear tablas. Ningún rol
-- puede borrar boletas, compras ni historial (RNF-07).
-- Las contraseñas son de ejemplo: cámbielas en una instalación real.
-- =====================================================================

SET client_min_messages = warning;

-- Limpieza para poder volver a ejecutar el script
DO $$
DECLARE
    r TEXT;
BEGIN
    FOREACH r IN ARRAY ARRAY['usuario_app', 'admin_escenarios', 'usuario_reportes',
                             'rol_cliente', 'rol_administrador', 'rol_lectura'] LOOP
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = r) THEN
            EXECUTE format('DROP OWNED BY %I', r);
            EXECUTE format('DROP ROLE %I', r);
        END IF;
    END LOOP;
END $$;


-- 1. Roles por actor y usuarios
CREATE ROLE rol_cliente       NOLOGIN;
CREATE ROLE rol_administrador NOLOGIN;
CREATE ROLE rol_lectura       NOLOGIN;

CREATE USER usuario_app      PASSWORD 'Cambiar_App_2026';
CREATE USER admin_escenarios PASSWORD 'Cambiar_Adm_2026';
CREATE USER usuario_reportes PASSWORD 'Cambiar_Rep_2026';

GRANT rol_cliente, rol_administrador, rol_lectura TO usuario_app;
GRANT rol_administrador TO admin_escenarios;
GRANT rol_lectura       TO usuario_reportes;


-- 2. Nada por defecto: se quitan los permisos que PostgreSQL da a PUBLIC
REVOKE ALL     ON ALL TABLES   IN SCHEMA public FROM PUBLIC;
REVOKE EXECUTE ON ALL ROUTINES IN SCHEMA public FROM PUBLIC;
REVOKE CREATE  ON SCHEMA public FROM PUBLIC;

DO $$
BEGIN
    EXECUTE format('GRANT CONNECT ON DATABASE %I TO usuario_app, admin_escenarios, usuario_reportes',
                   current_database());
END $$;
GRANT USAGE ON SCHEMA public TO rol_cliente, rol_administrador, rol_lectura;


-- 3. rol_cliente: catálogo, registro, compra e historial (RF-05 a RF-09)
GRANT SELECT ON escenario, localidad, tipo_evento, evento, precio_localidad TO rol_cliente;
GRANT SELECT, INSERT, DELETE ON cliente TO rol_cliente;   -- DELETE: solo cuentas sin compras (FK)
GRANT UPDATE (nombre_completo, correo, telefono) ON cliente TO rol_cliente;
GRANT SELECT, INSERT ON compra, boleta TO rol_cliente;
GRANT SELECT ON v_catalogo_eventos, v_ocupacion_localidad, v_boletas_detalle TO rol_cliente;
GRANT EXECUTE ON FUNCTION fn_registrar_compra(INTEGER, INTEGER, INTEGER, INTEGER, VARCHAR) TO rol_cliente;


-- 4. rol_administrador: catálogo y anulación de compras (RF-01 a RF-04, RF-13)
GRANT SELECT, INSERT, DELETE ON escenario, localidad, tipo_evento, evento, precio_localidad
      TO rol_administrador;                    -- las FK RESTRICT protegen lo que tiene ventas
GRANT UPDATE (nombre, direccion, capacidad_maxima)                   ON escenario        TO rol_administrador;
GRANT UPDATE (nombre, aforo_maximo)                                  ON localidad        TO rol_administrador;
GRANT UPDATE (nombre, descripcion)                                   ON tipo_evento      TO rol_administrador;
GRANT UPDATE (nombre, id_tipo_evento, fecha, hora_inicio, hora_fin)  ON evento           TO rol_administrador;
GRANT UPDATE (precio)                                                ON precio_localidad TO rol_administrador;
GRANT SELECT, INSERT ON historial_precios TO rol_administrador;     -- el trigger de auditoría escribe aquí
GRANT SELECT ON compra, boleta TO rol_administrador;
GRANT UPDATE (estado) ON boleta TO rol_administrador;               -- anular, nunca borrar
GRANT SELECT ON v_ocupacion_localidad, v_boletas_detalle, v_boletas_vigentes TO rol_administrador;
GRANT EXECUTE ON PROCEDURE sp_anular_compra(INTEGER) TO rol_administrador;


-- 5. rol_lectura: solo vistas, ninguna tabla base (RNF-04)
GRANT SELECT ON v_catalogo_eventos, v_ocupacion_localidad, v_boletas_detalle, v_clientes_publico
      TO rol_lectura;


-- Verificación: usuarios, si inician sesión y qué roles heredan
SELECT r.rolname                                          AS usuario_o_rol,
       r.rolcanlogin                                      AS inicia_sesion,
       COALESCE(string_agg(g.rolname, ', ' ORDER BY g.rolname), '—') AS hereda_de
FROM   pg_roles r
LEFT   JOIN pg_auth_members m ON m.member = r.oid
LEFT   JOIN pg_roles g        ON g.oid    = m.roleid
WHERE  r.rolname IN ('usuario_app', 'admin_escenarios', 'usuario_reportes',
                     'rol_cliente', 'rol_administrador', 'rol_lectura')
GROUP  BY r.rolname, r.rolcanlogin
ORDER  BY r.rolcanlogin DESC, r.rolname;
