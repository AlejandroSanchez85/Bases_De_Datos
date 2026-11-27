-- =====================================================================
-- BogoTickets · 03 · Vistas
-- Ejecutar después de 02_funciones_triggers.sql.
-- =====================================================================

DROP VIEW IF EXISTS v_catalogo_eventos, v_ocupacion_localidad, v_boletas_detalle,
                    v_clientes_publico, v_boletas_vigentes, v_compras_resumen,
                    v_cuentas CASCADE;


-- ---------------------------------------------------------------------
-- VISTA: v_ocupacion_localidad (reporte)
-- Una fila por cada localidad con precio en un evento: boletas vendidas,
-- disponibles, % de ocupación y dinero recaudado.
-- Parte de precio_localidad con LEFT JOIN a boleta para que las
-- localidades sin ventas también aparezcan (con 0). Solo cuentan las
-- boletas 'Vendida': una cancelada libera el cupo y no suma recaudo.
-- El recaudo suma precio_pagado (lo que pagó cada cliente), no el precio
-- vigente, porque el precio pudo cambiar después de la venta.
-- Usada en: GET /api/eventos/{id}/disponibilidad y GET /api/reportes/ocupacion
-- ---------------------------------------------------------------------
CREATE VIEW v_ocupacion_localidad AS
SELECT e.id_evento,
       e.nombre                                               AS evento,
       e.fecha                                                AS fecha_evento,
       s.nombre                                               AS escenario,
       l.id_localidad,
       l.nombre                                               AS localidad,
       pl.precio                                              AS precio_vigente,
       l.aforo_maximo,
       COUNT(b.id_boleta)                                     AS boletas_vendidas,
       l.aforo_maximo - COUNT(b.id_boleta)                    AS boletas_disponibles,
       ROUND(100.0 * COUNT(b.id_boleta) / l.aforo_maximo, 2)  AS porcentaje_ocupacion,
       COALESCE(SUM(b.precio_pagado), 0)                      AS recaudo
FROM   precio_localidad pl
JOIN   evento    e ON e.id_evento    = pl.id_evento
JOIN   escenario s ON s.id_escenario = e.id_escenario
JOIN   localidad l ON l.id_localidad = pl.id_localidad
LEFT   JOIN boleta b ON b.id_evento    = pl.id_evento
                    AND b.id_localidad = pl.id_localidad
                    AND b.estado       = 'Vendida'
GROUP  BY e.id_evento, e.nombre, e.fecha, s.nombre,
          l.id_localidad, l.nombre, pl.precio, l.aforo_maximo;


-- ---------------------------------------------------------------------
-- VISTA: v_catalogo_eventos (simplificación)
-- Eventos que todavía no se han realizado, con su tipo, escenario, rango
-- de precios y cupos disponibles (RF-06, RF-07).
-- Se apoya en v_ocupacion_localidad para no repetir el conteo de cupos.
-- Un evento sin tarifas no aparece: todavía no está a la venta.
-- Usada en: GET /api/catalogo (cartelera del cliente)
-- ---------------------------------------------------------------------
CREATE VIEW v_catalogo_eventos AS
SELECT e.id_evento,
       e.nombre                    AS evento,
       t.id_tipo_evento,
       t.nombre                    AS tipo_evento,
       s.id_escenario,
       s.nombre                    AS escenario,
       s.direccion,
       e.fecha,
       e.hora_inicio,
       e.hora_fin,
       MIN(o.precio_vigente)       AS precio_desde,
       MAX(o.precio_vigente)       AS precio_hasta,
       SUM(o.aforo_maximo)         AS aforo_total,
       SUM(o.boletas_disponibles)  AS boletas_disponibles
FROM   evento e
JOIN   tipo_evento t ON t.id_tipo_evento = e.id_tipo_evento
JOIN   escenario   s ON s.id_escenario   = e.id_escenario
JOIN   v_ocupacion_localidad o ON o.id_evento = e.id_evento
WHERE  e.fecha >= CURRENT_DATE
GROUP  BY e.id_evento, e.nombre, t.id_tipo_evento, t.nombre, s.id_escenario,
          s.nombre, s.direccion, e.fecha, e.hora_inicio, e.hora_fin;


-- ---------------------------------------------------------------------
-- VISTA: v_boletas_detalle (simplificación)
-- Datos completos de cada boleta: compra, cliente, evento, tipo,
-- escenario y localidad. Reemplaza un JOIN de 7 tablas (RF-09, P11).
-- Usada en: GET /api/boletas/{id} y GET /api/clientes/{id}/boletas
-- ---------------------------------------------------------------------
CREATE VIEW v_boletas_detalle AS
SELECT b.id_boleta,
       co.id_compra,
       co.fecha_compra,
       co.metodo_pago,
       c.id_cliente,
       c.nombre_completo  AS cliente,
       e.id_evento,
       e.nombre           AS evento,
       t.nombre           AS tipo_evento,
       e.fecha            AS fecha_evento,
       e.hora_inicio,
       s.nombre           AS escenario,
       l.id_localidad,
       l.nombre           AS localidad,
       b.precio_pagado,
       b.estado
FROM   boleta b
JOIN   compra      co ON co.id_compra     = b.id_compra
JOIN   cliente     c  ON c.id_cliente     = co.id_cliente
JOIN   evento      e  ON e.id_evento      = b.id_evento
JOIN   tipo_evento t  ON t.id_tipo_evento = e.id_tipo_evento
JOIN   escenario   s  ON s.id_escenario   = e.id_escenario
JOIN   localidad   l  ON l.id_localidad   = b.id_localidad;


-- ---------------------------------------------------------------------
-- VISTA: v_clientes_publico (seguridad)
-- Clientes sin documento ni teléfono y con el correo oculto. El analista
-- la consulta en lugar de la tabla cliente (RNF-04).
-- Las cantidades se calculan con subconsultas en el SELECT para no
-- multiplicar filas al unir compras y boletas.
-- Usada en: GET /api/reportes/clientes
-- ---------------------------------------------------------------------
CREATE VIEW v_clientes_publico AS
SELECT c.id_cliente,
       c.nombre_completo,
       LEFT(c.correo, 1) || '***@' || SPLIT_PART(c.correo, '@', 2)  AS correo_oculto,
       c.fecha_registro,
       (SELECT COUNT(*)
        FROM   compra co
        WHERE  co.id_cliente = c.id_cliente)                         AS compras,
       (SELECT COUNT(*)
        FROM   boleta b
        JOIN   compra co ON co.id_compra = b.id_compra
        WHERE  co.id_cliente = c.id_cliente AND b.estado = 'Vendida') AS boletas_vigentes
FROM   cliente c;


-- ---------------------------------------------------------------------
-- VISTA: v_boletas_vigentes (actualizable, WITH CHECK OPTION)
-- Solo las boletas 'Vendida'. Por esta vista no se puede registrar ni
-- dejar una boleta en otro estado. La usa el administrador en pgAdmin.
-- ---------------------------------------------------------------------
CREATE VIEW v_boletas_vigentes AS
SELECT id_boleta, id_compra, id_evento, id_localidad, precio_pagado, estado
FROM   boleta
WHERE  estado = 'Vendida'
WITH CHECK OPTION;


-- ---------------------------------------------------------------------
-- VISTA: v_compras_resumen (simplificación)
-- Una fila por compra: cliente, evento, localidades, cuántas boletas tiene
-- y cuántas siguen vigentes, total pagado y estado. Reemplaza un JOIN de
-- 7 tablas con GROUP BY que necesitan dos pantallas.
-- Una compra es Anulada cuando ninguna de sus boletas sigue Vendida.
-- Usada en: GET /api/cuenta/compras (Mis compras), GET /api/compras
-- (ventas del administrador) y GET /api/compras/{id}
-- ---------------------------------------------------------------------
CREATE VIEW v_compras_resumen AS
SELECT co.id_compra,
       co.fecha_compra,
       co.metodo_pago,
       c.id_cliente,
       c.nombre_completo                                       AS cliente,
       e.id_evento,
       e.nombre                                                AS evento,
       t.nombre                                                AS tipo_evento,
       e.fecha                                                 AS fecha_evento,
       e.hora_inicio,
       s.nombre                                                AS escenario,
       string_agg(DISTINCT l.nombre, ', ')                     AS localidades,
       COUNT(b.id_boleta)                                      AS boletas,
       COUNT(b.id_boleta) FILTER (WHERE b.estado = 'Vendida')  AS boletas_vigentes,
       SUM(b.precio_pagado)                                    AS total,
       CASE WHEN COUNT(b.id_boleta) FILTER (WHERE b.estado = 'Vendida') = 0
            THEN 'Anulada' ELSE 'Vigente' END                  AS estado
FROM   compra co
JOIN   cliente     c ON c.id_cliente     = co.id_cliente
JOIN   boleta      b ON b.id_compra      = co.id_compra
JOIN   evento      e ON e.id_evento      = b.id_evento
JOIN   tipo_evento t ON t.id_tipo_evento = e.id_tipo_evento
JOIN   escenario   s ON s.id_escenario   = e.id_escenario
JOIN   localidad   l ON l.id_localidad   = b.id_localidad
GROUP  BY co.id_compra, c.id_cliente, e.id_evento, t.id_tipo_evento, s.id_escenario;


-- ---------------------------------------------------------------------
-- VISTA: v_cuentas (seguridad)
-- Junta las cuentas de empleados y de clientes para el inicio de sesión:
-- correo, contraseña cifrada (BCrypt), rol y, si es cliente, su id.
-- usuario_app solo puede leer esta vista mientras nadie ha iniciado
-- sesión; las tablas cliente y empleado no le muestran la contraseña a
-- ningún rol (ver 05_usuarios_permisos.sql).
-- Usada en: POST /api/auth/login
-- ---------------------------------------------------------------------
CREATE VIEW v_cuentas AS
SELECT correo, contrasena, rol, NULL::INTEGER AS id_cliente, nombre_completo
FROM   empleado
UNION ALL
SELECT correo, contrasena, 'CLIENTE', id_cliente, nombre_completo
FROM   cliente;


-- Verificación: las 7 vistas
SELECT table_name AS vista,
       CASE WHEN is_updatable = 'YES' THEN 'sí' ELSE 'no' END AS actualizable
FROM   information_schema.views
WHERE  table_schema = 'public'
ORDER  BY table_name;
