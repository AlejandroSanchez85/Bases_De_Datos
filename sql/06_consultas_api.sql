-- =====================================================================
-- BogoTickets · 06 · Consultas que usa la API
-- Son las consultas con subconsultas y vistas que ejecutan los endpoints
-- (src/main/java/com/bogotickets/repository). Sirven para revisarlas en
-- pgAdmin: cada una se puede seleccionar y ejecutar sola.
-- Los ? de la API aparecen aquí con un valor de ejemplo.
--
-- La API se conecta como usuario_app y ejecuta cada petición dentro de una
-- transacción con el rol de quien inició sesión (SET LOCAL ROLE). El bloque
-- del final muestra cómo se ve eso en SQL.
-- =====================================================================


-- ---------------------------------------------------------------------
-- SUBCONSULTA 1 (escalar en WHERE)
-- GET /api/reportes/precios-sobre-promedio            (rol_analista)
-- Tarifas más caras que el promedio de todas las tarifas. La subconsulta
-- calcula el promedio una sola vez y cada fila se compara contra él.
-- Usa v_ocupacion_localidad (una fila por tarifa) porque el analista solo
-- puede leer vistas.
-- ---------------------------------------------------------------------
SELECT id_evento, evento, localidad, precio_vigente AS precio,
       (SELECT ROUND(AVG(precio_vigente), 2) FROM v_ocupacion_localidad) AS promedio
FROM   v_ocupacion_localidad
WHERE  precio_vigente > (SELECT AVG(precio_vigente) FROM v_ocupacion_localidad)
ORDER  BY precio_vigente DESC, evento;


-- ---------------------------------------------------------------------
-- SUBCONSULTA 2 (NOT EXISTS)
-- GET /api/reportes/clientes-sin-compras              (rol_analista)
-- Clientes registrados que nunca han comprado (P7). Para cada cliente,
-- NOT EXISTS busca si tiene alguna boleta; toda compra tiene al menos una,
-- así que los que quedan nunca han comprado.
-- ---------------------------------------------------------------------
SELECT c.id_cliente, c.nombres, c.apellidos, c.ciudad, c.fecha_registro
FROM   v_clientes_publico c
WHERE  NOT EXISTS (SELECT 1 FROM v_boletas_detalle b WHERE b.id_cliente = c.id_cliente)
ORDER  BY c.id_cliente;


-- ---------------------------------------------------------------------
-- SUBCONSULTA 3 (en FROM, sobre la vista de reporte)
-- GET /api/reportes/eventos-baja-ocupacion?umbral=50  (rol_analista)
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
-- SUBCONSULTA 4 (escalar en SELECT) y totales del tablero
-- GET /api/reportes/resumen                           (rol_analista)
-- Una sola fila con el recaudo, las boletas vendidas y anuladas, y el
-- número de eventos a la venta, que sale de la subconsulta del SELECT.
-- ---------------------------------------------------------------------
SELECT COALESCE(SUM(precio_pagado) FILTER (WHERE estado = 'Vendida'), 0) AS recaudo,
       COUNT(*) FILTER (WHERE estado = 'Vendida')                       AS vendidas,
       COUNT(*) FILTER (WHERE estado = 'Cancelada')                     AS canceladas,
       COUNT(DISTINCT id_compra)                                        AS compras,
       COUNT(DISTINCT id_cliente)                                       AS clientes,
       (SELECT COUNT(*) FROM v_catalogo_eventos)                        AS a_la_venta
FROM   v_boletas_detalle;

-- Ventas agrupadas del mismo tablero: por tipo de evento (también se agrupa
-- por escenario y por mes cambiando la primera columna)
SELECT tipo_evento AS nombre, COUNT(*) AS boletas, SUM(precio_pagado) AS recaudo
FROM   v_boletas_detalle
WHERE  estado = 'Vendida'
GROUP  BY 1
ORDER  BY recaudo DESC;

SELECT to_char(fecha_compra, 'YYYY-MM') AS nombre, COUNT(*) AS boletas, SUM(precio_pagado) AS recaudo
FROM   v_boletas_detalle
WHERE  estado = 'Vendida'
GROUP  BY 1
ORDER  BY 1;


-- ---------------------------------------------------------------------
-- PERFIL DE COMPRADORES
-- GET /api/reportes/perfil-compradores                (rol_analista)
-- Boletas vendidas según quién compra. v_clientes_publico entrega la edad
-- y la ciudad, no la fecha de nacimiento ni la dirección. El rango de edad
-- se arma con CASE y se ordena por la menor edad de cada rango.
-- ---------------------------------------------------------------------
SELECT CASE WHEN c.edad < 18 THEN '14 a 17 años'
            WHEN c.edad < 25 THEN '18 a 24 años'
            WHEN c.edad < 35 THEN '25 a 34 años'
            WHEN c.edad < 45 THEN '35 a 44 años'
            WHEN c.edad < 60 THEN '45 a 59 años'
            ELSE '60 años o más' END AS nombre,
       COUNT(*) AS boletas, SUM(b.precio_pagado) AS recaudo
FROM   v_boletas_detalle b
JOIN   v_clientes_publico c ON c.id_cliente = b.id_cliente
WHERE  b.estado = 'Vendida'
GROUP  BY 1
ORDER  BY MIN(c.edad);

-- Por género (INITCAP muestra «Femenino») y por departamento de residencia
SELECT INITCAP(c.genero) AS nombre, COUNT(*) AS boletas, SUM(b.precio_pagado) AS recaudo
FROM   v_boletas_detalle b
JOIN   v_clientes_publico c ON c.id_cliente = b.id_cliente
WHERE  b.estado = 'Vendida'
GROUP  BY 1
ORDER  BY boletas DESC;

SELECT c.departamento AS nombre, COUNT(*) AS boletas, SUM(b.precio_pagado) AS recaudo
FROM   v_boletas_detalle b
JOIN   v_clientes_publico c ON c.id_cliente = b.id_cliente
WHERE  b.estado = 'Vendida'
GROUP  BY 1
ORDER  BY recaudo DESC
LIMIT  10;


-- ---------------------------------------------------------------------
-- VISTA v_ubicaciones
-- GET /api/ubicaciones  (selectores País -> Departamento -> Ciudad del
-- registro y de Mi cuenta, y lista de indicativos del celular)
-- ---------------------------------------------------------------------
SELECT *
FROM   v_ubicaciones
ORDER  BY pais, departamento, ciudad;


-- ---------------------------------------------------------------------
-- VISTA v_ocupacion_localidad
-- GET /api/eventos/{id}/disponibilidad  (cupos en tiempo real, RF-07)
-- El cliente no ve el recaudo, por eso solo se piden estas columnas.
-- ---------------------------------------------------------------------
SELECT id_localidad, localidad, precio_vigente, aforo_maximo,
       boletas_vendidas, boletas_disponibles
FROM   v_ocupacion_localidad
WHERE  id_evento = 15
ORDER  BY precio_vigente DESC;

-- GET /api/reportes/ocupacion?idEvento=15  (ocupación y recaudo, RF-10)
-- Sin idEvento se quita el WHERE y se ordena por fecha_evento, evento.
SELECT *
FROM   v_ocupacion_localidad
WHERE  id_evento = 15
ORDER  BY precio_vigente DESC;


-- ---------------------------------------------------------------------
-- VISTA v_catalogo_eventos
-- GET /api/catalogo  (cartelera del cliente en orden de fecha, RF-06)
-- ---------------------------------------------------------------------
SELECT *
FROM   v_catalogo_eventos
ORDER  BY fecha, hora_inicio;


-- ---------------------------------------------------------------------
-- VISTA v_compras_resumen
-- GET /api/cuenta/compras        (Mis compras del cliente que inició sesión)
-- GET /api/compras?idEvento=15   (ventas del administrador, últimas 200)
-- GET /api/compras/{id}          (encabezado de una compra)
-- ---------------------------------------------------------------------
SELECT *
FROM   v_compras_resumen
WHERE  id_cliente = 1
ORDER  BY fecha_compra DESC;

SELECT *
FROM   v_compras_resumen
WHERE  id_evento = 15
ORDER  BY fecha_compra DESC
LIMIT  200;


-- ---------------------------------------------------------------------
-- VISTA v_boletas_detalle
-- GET /api/compras/{id} y /api/cuenta/compras/{id} (boletas de una compra)
-- GET /api/clientes/{id}/boletas                    (historial, RF-09)
-- GET /api/boletas/{id}                             (una boleta, P11)
-- ---------------------------------------------------------------------
SELECT * FROM v_boletas_detalle WHERE id_compra = 1 ORDER BY id_boleta;

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
-- VISTA v_cuentas
-- POST /api/auth/login  (lo único que lee usuario_app sin cambiar de rol)
-- La API compara la contraseña escrita con este hash usando BCrypt.
-- ---------------------------------------------------------------------
SELECT correo, contrasena, rol, id_cliente, nombre_completo
FROM   v_cuentas
WHERE  correo = 'admin@bogotickets.co';


-- ---------------------------------------------------------------------
-- FUNCIÓN Y PROCEDIMIENTO
-- POST  /api/compras             -> SELECT fn_registrar_compra(cliente de la sesión, evento, localidad, cantidad, método)
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


-- ---------------------------------------------------------------------
-- Así ejecuta la API una petición del analista (TransaccionConRol.java):
-- abre la transacción, toma el rol del actor y deja el correo de quien hizo
-- la petición para la auditoría. Al terminar, el rol vuelve a usuario_app.
-- ---------------------------------------------------------------------
BEGIN;
SET LOCAL ROLE rol_analista;
SELECT set_config('bogotickets.usuario', 'analista@bogotickets.co', true);
SELECT current_user AS rol_activo, COUNT(*) AS tarifas FROM v_ocupacion_localidad;
-- SELECT * FROM cliente;   -- con este rol daría: permission denied for table cliente
COMMIT;
