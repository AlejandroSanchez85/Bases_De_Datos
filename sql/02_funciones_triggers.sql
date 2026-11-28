-- =====================================================================
-- BogoTickets · 02 · Funciones, procedimiento y triggers
-- Reglas de negocio que comparan varias filas o tablas y por eso no
-- caben en un CHECK. Ejecutar después de 01_tablas.sql.
-- Todos los errores de regla usan RAISE EXCEPTION (SQLSTATE P0001) con
-- un mensaje pensado para mostrarse al usuario final (RNF-11).
-- =====================================================================


-- ---------------------------------------------------------------------
-- Localidad: RN-05 la suma de aforos no supera la capacidad del escenario
--            RN-18 el aforo no puede quedar por debajo de lo ya vendido
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_localidad_validar() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    v_capacidad  INTEGER;
    v_otras      INTEGER;
    v_vendidas   INTEGER;
BEGIN
    SELECT capacidad_maxima INTO v_capacidad
    FROM   escenario WHERE id_escenario = NEW.id_escenario;

    SELECT COALESCE(SUM(aforo_maximo), 0) INTO v_otras
    FROM   localidad
    WHERE  id_escenario = NEW.id_escenario
      AND  id_localidad IS DISTINCT FROM NEW.id_localidad;

    IF v_otras + NEW.aforo_maximo > v_capacidad THEN
        RAISE EXCEPTION 'Las localidades sumarían % cupos y el escenario solo admite %',
              v_otras + NEW.aforo_maximo, v_capacidad;
    END IF;

    IF TG_OP = 'UPDATE' THEN
        -- mayor número de boletas vendidas de esta localidad en un mismo evento
        SELECT COALESCE(MAX(n), 0) INTO v_vendidas
        FROM  (SELECT COUNT(*) AS n
               FROM   boleta
               WHERE  id_localidad = NEW.id_localidad AND estado = 'Vendida'
               GROUP  BY id_evento) t;

        IF NEW.aforo_maximo < v_vendidas THEN
            RAISE EXCEPTION 'No se puede dejar el aforo en %: un evento ya vendió % boletas en esta localidad',
                  NEW.aforo_maximo, v_vendidas;
        END IF;
    END IF;
    RETURN NEW;
END $$;

CREATE OR REPLACE TRIGGER trg_localidad_validar
BEFORE INSERT OR UPDATE OF aforo_maximo, id_escenario ON localidad
FOR EACH ROW EXECUTE FUNCTION fn_localidad_validar();


-- ---------------------------------------------------------------------
-- Escenario: RN-05 la capacidad no puede bajar de la suma de sus localidades
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_escenario_validar_capacidad() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    v_suma INTEGER;
BEGIN
    SELECT COALESCE(SUM(aforo_maximo), 0) INTO v_suma
    FROM   localidad WHERE id_escenario = NEW.id_escenario;

    IF NEW.capacidad_maxima < v_suma THEN
        RAISE EXCEPTION 'La capacidad no puede ser % porque sus localidades ya suman % cupos',
              NEW.capacidad_maxima, v_suma;
    END IF;
    RETURN NEW;
END $$;

CREATE OR REPLACE TRIGGER trg_escenario_capacidad
BEFORE UPDATE OF capacidad_maxima ON escenario
FOR EACH ROW EXECUTE FUNCTION fn_escenario_validar_capacidad();


-- ---------------------------------------------------------------------
-- Evento: RNF-02 dos eventos del mismo escenario no se cruzan en horario
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_evento_validar_horario() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    v_otro    VARCHAR(150);
    v_inicio  TIME;
    v_fin     TIME;
BEGIN
    SELECT nombre, hora_inicio, hora_fin INTO v_otro, v_inicio, v_fin
    FROM   evento
    WHERE  id_escenario = NEW.id_escenario
      AND  fecha = NEW.fecha
      AND  id_evento IS DISTINCT FROM NEW.id_evento
      AND  NEW.hora_inicio < hora_fin           -- los rangos se solapan
      AND  hora_inicio < NEW.hora_fin
    LIMIT 1;

    IF FOUND THEN
        RAISE EXCEPTION 'El escenario ya tiene «%» ese día de % a %', v_otro,
              to_char(v_inicio, 'HH24:MI'), to_char(v_fin, 'HH24:MI');
    END IF;
    RETURN NEW;
END $$;

CREATE OR REPLACE TRIGGER trg_evento_horario
BEFORE INSERT OR UPDATE OF id_escenario, fecha, hora_inicio, hora_fin ON evento
FOR EACH ROW EXECUTE FUNCTION fn_evento_validar_horario();


-- ---------------------------------------------------------------------
-- Precio: RN-15 la localidad debe pertenecer al escenario del evento
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_precio_validar_localidad() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (SELECT 1
                   FROM   evento e
                   JOIN   localidad l ON l.id_escenario = e.id_escenario
                   WHERE  e.id_evento = NEW.id_evento
                     AND  l.id_localidad = NEW.id_localidad) THEN
        RAISE EXCEPTION 'La localidad % no pertenece al escenario del evento %',
              NEW.id_localidad, NEW.id_evento;
    END IF;
    RETURN NEW;
END $$;

CREATE OR REPLACE TRIGGER trg_precio_localidad
BEFORE INSERT OR UPDATE OF id_evento, id_localidad ON precio_localidad
FOR EACH ROW EXECUTE FUNCTION fn_precio_validar_localidad();


-- ---------------------------------------------------------------------
-- Compra: RN-12 no puede ser anterior al registro del cliente
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_compra_validar() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    v_registro DATE;
BEGIN
    SELECT fecha_registro INTO v_registro
    FROM   cliente WHERE id_cliente = NEW.id_cliente;

    IF NEW.fecha_compra::date < v_registro THEN
        RAISE EXCEPTION 'La compra (%) no puede ser anterior al registro del cliente (%)',
              NEW.fecha_compra::date, v_registro;
    END IF;
    RETURN NEW;
END $$;

CREATE OR REPLACE TRIGGER trg_compra_validar
BEFORE INSERT OR UPDATE OF id_cliente, fecha_compra ON compra
FOR EACH ROW EXECUTE FUNCTION fn_compra_validar();


-- ---------------------------------------------------------------------
-- Boleta, al venderla:
--   RNF-03 la compra no es anterior a la creación del evento
--   RN-17  no se venden boletas de un evento que ya se realizó
--   RN-25  el comprador tiene la edad mínima del evento el día del evento
--   RN-08  solo se venden localidades con precio en el evento
--   RN-16  el precio pagado es el precio vigente de la localidad
--   RN-07  no se supera el aforo (RNF-01). El bloqueo por evento y
--          localidad hace esperar a las compras simultáneas (RNF-10).
-- Boleta, al modificarla:
--   RNF-07 solo cambia el estado; RN-20 una cancelada no se reactiva y
--          no se cancela una boleta de un evento que ya pasó
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_boleta_validar() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    v_fecha_compra    DATE;
    v_nacimiento      DATE;
    v_evento          VARCHAR(150);
    v_fecha_evento    DATE;
    v_registro_evento DATE;
    v_edad_minima     INTEGER;
    v_edad            INTEGER;
    v_localidad       VARCHAR(50);
    v_aforo           INTEGER;
    v_vendidas        INTEGER;
BEGIN
    SELECT nombre, fecha, fecha_registro, edad_minima
    INTO   v_evento, v_fecha_evento, v_registro_evento, v_edad_minima
    FROM   evento WHERE id_evento = NEW.id_evento;

    IF TG_OP = 'UPDATE' THEN
        IF (NEW.id_compra, NEW.id_evento, NEW.id_localidad, NEW.precio_pagado)
           IS DISTINCT FROM (OLD.id_compra, OLD.id_evento, OLD.id_localidad, OLD.precio_pagado) THEN
            RAISE EXCEPTION 'De una boleta vendida solo se puede cambiar el estado';
        END IF;
        IF OLD.estado = 'Cancelada' AND NEW.estado = 'Vendida' THEN
            RAISE EXCEPTION 'La boleta % está cancelada y no se puede reactivar', OLD.id_boleta;
        END IF;
        IF OLD.estado = 'Vendida' AND NEW.estado = 'Cancelada' AND v_fecha_evento < CURRENT_DATE THEN
            RAISE EXCEPTION 'No se puede cancelar la boleta %: «%» ya se realizó', OLD.id_boleta, v_evento;
        END IF;
        RETURN NEW;
    END IF;

    SELECT co.fecha_compra::date, c.fecha_nacimiento
    INTO   v_fecha_compra, v_nacimiento
    FROM   compra co
    JOIN   cliente c ON c.id_cliente = co.id_cliente
    WHERE  co.id_compra = NEW.id_compra;

    IF v_fecha_compra < v_registro_evento THEN
        RAISE EXCEPTION 'La compra (%) es anterior a la creación del evento «%» (%)',
              v_fecha_compra, v_evento, v_registro_evento;
    END IF;
    IF v_fecha_compra > v_fecha_evento THEN
        RAISE EXCEPTION 'El evento «%» ya se realizó (%): no se venden boletas', v_evento, v_fecha_evento;
    END IF;

    -- Años cumplidos el día del evento: quien compra en noviembre con 17 años
    -- puede ir a un evento de marzo si ya tendrá 18
    v_edad := EXTRACT(YEAR FROM age(v_fecha_evento, v_nacimiento));
    IF v_edad < v_edad_minima THEN
        RAISE EXCEPTION '«%» exige una edad mínima de % años y el comprador tendrá % años el día del evento',
              v_evento, v_edad_minima, v_edad;
    END IF;

    IF NEW.precio_pagado IS NULL THEN
        SELECT precio INTO NEW.precio_pagado
        FROM   precio_localidad
        WHERE  id_evento = NEW.id_evento AND id_localidad = NEW.id_localidad;

        IF NOT FOUND THEN
            RAISE EXCEPTION 'La localidad % no tiene precio en el evento %: no está a la venta',
                  NEW.id_localidad, NEW.id_evento;
        END IF;
    END IF;

    IF NEW.estado = 'Vendida' THEN
        PERFORM pg_advisory_xact_lock(NEW.id_evento, NEW.id_localidad);

        SELECT nombre, aforo_maximo INTO v_localidad, v_aforo
        FROM   localidad WHERE id_localidad = NEW.id_localidad;

        SELECT COUNT(*) INTO v_vendidas
        FROM   boleta
        WHERE  id_evento = NEW.id_evento AND id_localidad = NEW.id_localidad
          AND  estado = 'Vendida';

        IF v_vendidas >= v_aforo THEN
            RAISE EXCEPTION 'Localidad agotada: «%» de «%» no tiene más cupos (aforo %)',
                  v_localidad, v_evento, v_aforo;
        END IF;
    END IF;
    RETURN NEW;
END $$;

CREATE OR REPLACE TRIGGER trg_boleta_validar
BEFORE INSERT OR UPDATE ON boleta
FOR EACH ROW EXECUTE FUNCTION fn_boleta_validar();


-- ---------------------------------------------------------------------
-- RNF-06: auditoría automática de cambios de precio y de aforo
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_auditar_precio() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO historial_precios (id_localidad, id_evento, campo_modificado, valor_anterior, valor_nuevo)
    VALUES (NEW.id_localidad, NEW.id_evento, 'PRECIO', OLD.precio, NEW.precio);
    RETURN NULL;
END $$;

CREATE OR REPLACE TRIGGER trg_auditar_precio
AFTER UPDATE OF precio ON precio_localidad
FOR EACH ROW WHEN (OLD.precio IS DISTINCT FROM NEW.precio)
EXECUTE FUNCTION fn_auditar_precio();

CREATE OR REPLACE FUNCTION fn_auditar_aforo() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO historial_precios (id_localidad, id_evento, campo_modificado, valor_anterior, valor_nuevo)
    VALUES (NEW.id_localidad, NULL, 'AFORO_MAXIMO', OLD.aforo_maximo, NEW.aforo_maximo);
    RETURN NULL;
END $$;

CREATE OR REPLACE TRIGGER trg_auditar_aforo
AFTER UPDATE OF aforo_maximo ON localidad
FOR EACH ROW WHEN (OLD.aforo_maximo IS DISTINCT FROM NEW.aforo_maximo)
EXECUTE FUNCTION fn_auditar_aforo();


-- ---------------------------------------------------------------------
-- RNF-07: boletas, compras e historial no se borran
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_impedir_borrado() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'No se permite eliminar registros de %: %', TG_TABLE_NAME,
          CASE TG_TABLE_NAME
              WHEN 'boleta' THEN 'cambie su estado a Cancelada'
              WHEN 'compra' THEN 'anule la compra'
              ELSE 'es el registro de auditoría'
          END;
END $$;

CREATE OR REPLACE TRIGGER trg_boleta_no_borrar
BEFORE DELETE ON boleta
FOR EACH ROW EXECUTE FUNCTION fn_impedir_borrado();

CREATE OR REPLACE TRIGGER trg_compra_no_borrar
BEFORE DELETE ON compra
FOR EACH ROW EXECUTE FUNCTION fn_impedir_borrado();

CREATE OR REPLACE TRIGGER trg_historial_no_borrar
BEFORE DELETE ON historial_precios
FOR EACH ROW EXECUTE FUNCTION fn_impedir_borrado();


-- ---------------------------------------------------------------------
-- Compra de boletas (caso de uso principal, RF-08).
-- Crea la compra y una boleta por cada cupo. Todo corre en una sola
-- transacción: si una boleta falla (por ejemplo, la localidad se agota
-- en la tercera), PostgreSQL deshace la compra completa.
-- Devuelve el id de la compra creada.
-- ---------------------------------------------------------------------
DROP FUNCTION IF EXISTS fn_registrar_compra(INTEGER, INTEGER, INTEGER, INTEGER, VARCHAR);

CREATE FUNCTION fn_registrar_compra(
    p_id_cliente    INTEGER,
    p_id_evento     INTEGER,
    p_id_localidad  INTEGER,
    p_cantidad      INTEGER,
    p_metodo_pago   VARCHAR DEFAULT 'TARJETA')
RETURNS INTEGER
LANGUAGE plpgsql AS $$
DECLARE
    v_id_compra INTEGER;
BEGIN
    IF p_cantidad IS NULL OR p_cantidad NOT BETWEEN 1 AND 10 THEN       -- RN-19
        RAISE EXCEPTION 'Una compra debe tener entre 1 y 10 boletas'
              USING ERRCODE = 'invalid_parameter_value';
    END IF;

    INSERT INTO compra (id_cliente, metodo_pago)
    VALUES (p_id_cliente, p_metodo_pago)
    RETURNING id_compra INTO v_id_compra;

    FOR i IN 1 .. p_cantidad LOOP
        INSERT INTO boleta (id_compra, id_evento, id_localidad)
        VALUES (v_id_compra, p_id_evento, p_id_localidad);
    END LOOP;

    RETURN v_id_compra;
END $$;


-- ---------------------------------------------------------------------
-- Anulación de una compra (RF-13): sus boletas pasan a Cancelada y los
-- cupos quedan libres. Nada se borra (RNF-07).
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_anular_compra(INTEGER);

CREATE PROCEDURE sp_anular_compra(p_id_compra INTEGER)
LANGUAGE plpgsql AS $$
DECLARE
    v_anuladas INTEGER;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM compra WHERE id_compra = p_id_compra) THEN
        RAISE EXCEPTION 'La compra % no existe', p_id_compra USING ERRCODE = 'no_data_found';
    END IF;

    UPDATE boleta SET estado = 'Cancelada'
    WHERE  id_compra = p_id_compra AND estado = 'Vendida';
    GET DIAGNOSTICS v_anuladas = ROW_COUNT;

    IF v_anuladas = 0 THEN
        RAISE EXCEPTION 'La compra % ya estaba anulada', p_id_compra;
    END IF;
END $$;


-- Verificación: triggers creados por tabla
SELECT event_object_table AS tabla,
       trigger_name       AS trigger,
       action_timing || ' ' || string_agg(event_manipulation, ' / ' ORDER BY event_manipulation) AS momento
FROM   information_schema.triggers
WHERE  trigger_schema = 'public'
GROUP  BY event_object_table, trigger_name, action_timing
ORDER  BY tabla, trigger;
