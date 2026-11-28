package com.bogotickets.exception;

import java.util.Map;

// Cuando falla un UNIQUE, CHECK o FOREIGN KEY, PostgreSQL informa el nombre de la
// restricción (uq_, chk_, fk_ de sql/01_tablas.sql). Aquí se traduce a un mensaje
// para el usuario. Los triggers no aparecen: ya traen su propio mensaje en español.
final class MensajesRestriccion {

    private MensajesRestriccion() {
    }

    // UNIQUE (23505) y CHECK (23514)
    static final Map<String, String> DATOS = Map.ofEntries(
            Map.entry("uq_escenario_nombre", "Ya existe un escenario con ese nombre"),
            Map.entry("uq_localidad_escenario_nombre", "El escenario ya tiene una localidad con ese nombre"),
            Map.entry("uq_tipo_evento_nombre", "Ya existe un tipo de evento con ese nombre"),
            Map.entry("uq_evento_escenario_fecha_hora", "El escenario ya tiene un evento que empieza ese día a esa hora"),
            Map.entry("pk_precio_localidad", "La localidad ya tiene precio en este evento"),
            Map.entry("uq_cliente_documento", "Ya hay un cliente registrado con ese tipo y número de documento"),
            Map.entry("uq_cliente_correo", "Ya hay un cliente registrado con ese correo"),
            Map.entry("chk_escenario_nombre", "El nombre del escenario no puede quedar vacío"),
            Map.entry("chk_escenario_direccion", "La dirección no puede quedar vacía"),
            Map.entry("chk_escenario_capacidad", "La capacidad máxima debe ser mayor que cero"),
            Map.entry("chk_localidad_nombre", "El nombre de la localidad no puede quedar vacío"),
            Map.entry("chk_localidad_aforo", "El aforo máximo debe ser mayor que cero"),
            Map.entry("chk_tipo_evento_nombre", "El nombre del tipo de evento no puede quedar vacío"),
            Map.entry("chk_evento_nombre", "El nombre del evento no puede quedar vacío"),
            Map.entry("chk_evento_fecha", "La fecha del evento no puede ser anterior a la fecha en que se registró"),
            Map.entry("chk_evento_horario", "La hora de fin debe ser posterior a la hora de inicio"),
            Map.entry("chk_precio_valor", "El precio no puede ser negativo"),
            Map.entry("chk_cliente_tipo_documento", "El tipo de documento debe ser CC, TI, CE, PA o PPT"),
            Map.entry("chk_cliente_documento", "El número de documento debe tener de 5 a 20 caracteres, solo números y letras mayúsculas"),
            Map.entry("chk_cliente_nombres", "Los nombres no pueden quedar vacíos"),
            Map.entry("chk_cliente_apellidos", "Los apellidos no pueden quedar vacíos"),
            Map.entry("chk_cliente_nacimiento", "Para crear una cuenta se necesitan al menos 14 años cumplidos"),
            Map.entry("chk_cliente_cc_mayor_edad", "La cédula de ciudadanía es para mayores de 18 años: un menor se registra con tarjeta de identidad (TI)"),
            Map.entry("chk_cliente_ti_menor_edad", "La tarjeta de identidad es para menores de 18 años: un adulto se registra con cédula de ciudadanía (CC) o de extranjería (CE)"),
            Map.entry("chk_cliente_genero", "El género debe ser masculino, femenino u otro"),
            Map.entry("chk_cliente_indicativo", "El indicativo debe tener la forma +57"),
            Map.entry("chk_cliente_celular", "El celular debe tener de 7 a 15 dígitos"),
            Map.entry("chk_cliente_direccion", "La dirección no puede quedar vacía"),
            Map.entry("chk_evento_edad_minima", "La edad mínima debe estar entre 0 (todo público) y 18 años"),
            Map.entry("chk_cliente_correo", "El correo no es válido: debe tener la forma usuario@dominio y estar en minúsculas"),
            Map.entry("chk_cliente_correo_dominio", "Los correos @bogotickets.co son de los empleados: use otro correo"),
            Map.entry("chk_compra_metodo", "El método de pago debe ser TARJETA, PSE o EFECTIVO"));

    // FOREIGN KEY (23503) al crear o actualizar: el registro al que se apunta no existe
    static final Map<String, String> REFERENCIA_INEXISTENTE = Map.of(
            "fk_localidad_escenario", "El escenario indicado no existe",
            "fk_evento_escenario", "El escenario indicado no existe",
            "fk_evento_tipo", "El tipo de evento indicado no existe",
            "fk_precio_evento", "El evento indicado no existe",
            "fk_precio_localidad", "La localidad indicada no existe",
            "fk_compra_cliente", "El cliente indicado no existe",
            "fk_cliente_ciudad", "La ciudad indicada no existe");

    // FOREIGN KEY (23503, o 23001 en PostgreSQL 18) al eliminar: otros registros
    // dependen de este (ON DELETE RESTRICT)
    static final Map<String, String> TIENE_DEPENDIENTES = Map.of(
            "fk_evento_escenario", "No se puede eliminar el escenario: tiene eventos programados",
            "fk_evento_tipo", "No se puede eliminar el tipo: hay eventos de ese tipo",
            "fk_compra_cliente", "No se puede eliminar la cuenta: el cliente ya tiene compras registradas",
            "fk_boleta_precio_localidad", "No se puede eliminar: ya se vendieron boletas con esa tarifa",
            "fk_historial_localidad", "No se puede eliminar: la localidad tiene cambios registrados en el historial de precios",
            "fk_historial_evento", "No se puede eliminar el evento: tiene cambios de precio registrados en el historial");
}
