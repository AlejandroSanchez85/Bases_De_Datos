-- =====================================================================
-- BogoTickets · 07 · Pruebas de las reglas de negocio
-- Ejecutar cada prueba por separado: seleccionar sus líneas y pulsar F5.
-- Las pruebas 1 a 12 deben terminar en ERROR (eso demuestra la regla) y
-- no modifican datos. La 13 sí funciona y se deshace con ROLLBACK.
-- Requiere los scripts 01 a 05 recién ejecutados.
-- =====================================================================


-- Prueba 1 · Compra que falla a mitad de camino (RN-07, caso principal)
-- El palco del evento 11 tiene 6 de 8 boletas vendidas. Se piden 3: las
-- dos primeras entran, la tercera supera el aforo y se deshace TODO.
-- a) Conteo antes (77 compras, 104 boletas):
SELECT (SELECT COUNT(*) FROM compra) AS compras, (SELECT COUNT(*) FROM boleta) AS boletas;
-- b) Compra de 3 boletas -> ERROR: Localidad agotada: «Palco Presidencial» ...
SELECT fn_registrar_compra(5, 11, 15, 3);
-- c) Volver a ejecutar el conteo de a): sigue en 77 y 104.


-- Prueba 2 · No se venden boletas de un evento realizado (RN-17)
-- ERROR: El evento «Clásico Capitalino ...» ya se realizó (2026-09-20) ...
SELECT fn_registrar_compra(5, 1, 3, 1);


-- Prueba 3 · Máximo 10 boletas por compra (RN-19)
-- ERROR: Una compra debe tener entre 1 y 10 boletas
SELECT fn_registrar_compra(5, 11, 13, 11);


-- Prueba 4 · La localidad debe ser del escenario del evento (RN-15)
-- El evento 12 es en El Campín y la localidad 6 es del Movistar Arena.
-- ERROR: La localidad 6 no pertenece al escenario del evento 12
INSERT INTO precio_localidad (id_evento, id_localidad, precio) VALUES (12, 6, 100000);


-- Prueba 5 · Dos eventos no se cruzan en el mismo escenario (RNF-02)
-- ERROR: El escenario ya tiene «Gran Final del Fútbol ...» ese día de 18:00 a 20:00
INSERT INTO evento (id_escenario, id_tipo_evento, nombre, fecha, hora_inicio, hora_fin)
VALUES (1, 1, 'Concierto de Fin de Año', '2026-12-16', '19:00', '22:00');


-- Prueba 6 · Las localidades no superan la capacidad del escenario (RN-05)
-- ERROR: Las localidades sumarían 1808 cupos y el escenario solo admite 1700
INSERT INTO localidad (id_escenario, nombre, aforo_maximo) VALUES (4, 'Gradería', 600);


-- Prueba 7 · El aforo no baja de lo ya vendido (RN-18)
-- ERROR: No se puede dejar el aforo en 5: un evento ya vendió 8 boletas ...
UPDATE localidad SET aforo_maximo = 5 WHERE id_localidad = 15;


-- Prueba 8 · Las boletas no se borran (RNF-07)
-- ERROR: No se permite eliminar registros de boleta: cambie su estado a Cancelada
DELETE FROM boleta WHERE id_boleta = 1;


-- Prueba 9 · No se anula una compra de un evento que ya pasó (RN-20)
-- ERROR: No se puede cancelar la boleta 55: «Obra de Teatro ...» ya se realizó
CALL sp_anular_compra(1);


-- Prueba 10 · La compra no es anterior al registro del cliente (RN-12)
-- La clienta 12 se registró el 2026-09-28.
-- ERROR: La compra (2026-09-01) no puede ser anterior al registro del cliente (2026-09-28)
INSERT INTO compra (id_cliente, fecha_compra) VALUES (12, '2026-09-01 10:00');


-- Prueba 11 · Una boleta cancelada no se reactiva (RN-20)
-- ERROR: La boleta 9 está cancelada y no se puede reactivar
UPDATE boleta SET estado = 'Vendida' WHERE id_boleta = 9;


-- Prueba 12 · Solo se venden localidades con precio en el evento (RN-08)
-- En la Gran Final (evento 12) no se vende la Gramilla (localidad 1).
-- ERROR: La localidad 1 no tiene precio en el evento 12: no está a la venta
SELECT fn_registrar_compra(5, 12, 1, 1);


-- Prueba 13 · Auditoría automática de precios (RNF-06)
-- Seleccionar desde BEGIN hasta el SELECT y pulsar F5: aparece el cambio
-- registrado por el trigger. Después ejecutar ROLLBACK para deshacerlo.
BEGIN;
UPDATE precio_localidad SET precio = 150000 WHERE id_evento = 11 AND id_localidad = 13;
SELECT id_historial, campo_modificado, valor_anterior, valor_nuevo, usuario, fecha_cambio
FROM   historial_precios ORDER BY id_historial DESC LIMIT 1;

ROLLBACK;
