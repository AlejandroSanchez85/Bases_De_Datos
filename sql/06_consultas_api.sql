-- =====================================================================
-- BogoTickets · 06 · Consultas que usa la API
-- Son las consultas con subconsultas y vistas que ejecutan los endpoints
-- (src/main/java/com/bogotickets/repository). Sirven para revisarlas en
-- pgAdmin: cada una se puede seleccionar y ejecutar sola.
-- Los ? de la API aparecen aquí con un valor de ejemplo.
-- =====================================================================


-- ---------------------------------------------------------------------
-- SUBCONSULTA 1 (escalar en WHERE)
-- GET /api/reportes/precios-sobre-promedio
-- Tarifas más caras que el promedio de todas las tarifas. La subconsulta
-- calcula el promedio una sola vez y cada fila se compara contra él.
-- ---------------------------------------------------------------------
SELECT e.id_evento, e.nombre AS evento, l.nombre AS localidad, pl.precio,
       (SELECT ROUND(AVG(precio), 2) FROM precio_localidad) AS promedio
FROM   precio_localidad pl
JOIN   evento    e ON e.id_evento    = pl.id_evento
JOIN   localidad l ON l.id_localidad = pl.id_localidad
WHERE  pl.precio > (SELECT AVG(precio) FROM precio_localidad)
ORDER  BY pl.precio DESC, e.nombre;


-- ---------------------------------------------------------------------
-- SUBCONSULTA 2 (NOT EXISTS)
-- GET /api/reportes/clientes-sin-compras
-- Clientes registrados que nunca han comprado (P7). Para cada cliente,
-- NOT EXISTS busca si hay alguna compra suya; si no hay, aparece.
-- ---------------------------------------------------------------------
SELECT c.id_cliente, c.nombre_completo, c.fecha_registro
FROM   cliente c
WHERE  NOT EXISTS (SELECT 1 FROM compra co WHERE co.id_cliente = c.id_cliente)
ORDER  BY c.id_cliente;


-- ---------------------------------------------------------------------
-- SUBCONSULTA 3 (en FROM, sobre la vista de reporte)
-- GET /api/reportes/eventos-baja-ocupacion?umbral=50
-- Eventos con ocupación general menor al umbral (RF-11). La subconsulta
-- suma por evento los cupos y las ventas de todas sus localidades; la
-- consulta externa filtra por el porcentaje calculado.
-- ---------------------------------------------------------------------
SELECT t.id_evento, t.evento, t.fecha_evento, t.aforo_total, t.boletas_vendidas,
       t.porcentaje_ocupacion
FROM  (SELECT id_evento, evento, fecha_evento,
              SUM(aforo_maximo)     AS aforo_total,
              SUM(boletas_vendidas) AS boletas_vendidas,
              ROUND(100.0 * SUM(boletas_vendidas) / SUM(aforo_maximo), 2) AS porcentaje_ocupacion
       FROM   v_ocupacion_localidad
       GROUP  BY id_evento, evento, fecha_evento) t
WHERE  t.porcentaje_ocupacion < 50
ORDER  BY t.porcentaje_ocupacion, t.fecha_evento;


-- ---------------------------------------------------------------------
-- SUBCONSULTA 4 (escalar en SELECT)
-- GET /api/compras/{id} y POST /api/compras (datos de la compra)
-- Para cada compra, la subconsulta suma lo pagado por sus boletas.
-- ---------------------------------------------------------------------
SELECT co.id_compra, co.id_cliente, c.nombre_completo, co.fecha_compra, co.metodo_pago,
       (SELECT COALESCE(SUM(b.precio_pagado), 0)
        FROM   boleta b
        WHERE  b.id_compra = co.id_compra) AS total
FROM   compra co
JOIN   cliente c ON c.id_cliente = co.id_cliente
WHERE  co.id_compra = 62;


-- ---------------------------------------------------------------------
-- VISTA v_ocupacion_localidad
-- GET /api/eventos/{id}/disponibilidad  (cupos en tiempo real, RF-07).
-- El cliente no ve el recaudo, por eso solo se piden estas columnas.
-- ---------------------------------------------------------------------
SELECT id_localidad, localidad, precio_vigente, aforo_maximo,
       boletas_vendidas, boletas_disponibles
FROM   v_ocupacion_localidad
WHERE  id_evento = 11
ORDER  BY precio_vigente DESC;

-- GET /api/reportes/ocupacion?idEvento=11  (ocupación y recaudo, RF-10)
-- Sin idEvento se quita el WHERE y se ordena por fecha_evento, evento.
SELECT *
FROM   v_ocupacion_localidad
WHERE  id_evento = 11
ORDER  BY precio_vigente DESC;


-- ---------------------------------------------------------------------
-- VISTA v_catalogo_eventos
-- GET /api/catalogo  (eventos a la venta en orden de fecha, RF-06)
-- ---------------------------------------------------------------------
SELECT *
FROM   v_catalogo_eventos
ORDER  BY fecha, hora_inicio;


-- ---------------------------------------------------------------------
-- VISTA v_boletas_detalle
-- GET /api/clientes/{id}/boletas  (historial de compras, RF-09)
-- GET /api/boletas/{id}           (datos completos de una boleta, P11)
-- ---------------------------------------------------------------------
SELECT *
FROM   v_boletas_detalle
WHERE  id_cliente = 3
ORDER  BY fecha_compra DESC, id_boleta;

SELECT * FROM v_boletas_detalle WHERE id_boleta = 1;


-- ---------------------------------------------------------------------
-- VISTA v_clientes_publico
-- GET /api/reportes/clientes  (comportamiento de compra sin datos sensibles)
-- ---------------------------------------------------------------------
SELECT *
FROM   v_clientes_publico
ORDER  BY boletas_vigentes DESC, id_cliente;


-- ---------------------------------------------------------------------
-- FUNCIÓN Y PROCEDIMIENTO
-- POST  /api/compras             -> SELECT fn_registrar_compra(cliente, evento, localidad, cantidad, método)
-- PATCH /api/compras/{id}/anular -> CALL sp_anular_compra(id)
-- (ver ejemplos ejecutables en 07_pruebas_reglas.sql)
-- ---------------------------------------------------------------------


-- ---------------------------------------------------------------------
-- Historial de cambios de precio y aforo (RNF-06)
-- GET /api/historial-precios  (con ?idEvento= se agrega WHERE h.id_evento = ?)
-- El LEFT JOIN conserva los cambios de aforo, que no tienen evento.
-- ---------------------------------------------------------------------
SELECT h.id_historial, h.fecha_cambio, h.campo_modificado, e.nombre AS evento,
       l.nombre AS localidad, h.valor_anterior, h.valor_nuevo, h.usuario
FROM   historial_precios h
JOIN   localidad l ON l.id_localidad = h.id_localidad
LEFT   JOIN evento e ON e.id_evento = h.id_evento
ORDER  BY h.fecha_cambio DESC;
