package com.bogotickets.exception;

import com.bogotickets.dto.RespuestaError;
import jakarta.servlet.http.HttpServletRequest;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

// Convierte las excepciones en respuestas {"estado": ..., "mensaje": ...} con el
// código HTTP adecuado, para que la interfaz web muestre el mensaje al usuario.
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<RespuestaError> noEncontrado(RecursoNoEncontradoException e) {
        return responder(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // Falta un campo marcado con @NotNull o @NotBlank en el cuerpo (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaError> datosIncompletos(MethodArgumentNotValidException e) {
        String mensaje = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .sorted()
                .collect(Collectors.joining(". "));
        return responder(HttpStatus.BAD_REQUEST, mensaje);
    }

    // JSON mal escrito o un valor que no corresponde al tipo (texto en un número, fecha inválida)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespuestaError> jsonInvalido(HttpMessageNotReadableException e) {
        return responder(HttpStatus.BAD_REQUEST, "Los datos enviados no tienen el formato correcto");
    }

    // Un id de la URL o un parámetro que debía ser número (por ejemplo /api/eventos/abc)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<RespuestaError> parametroInvalido(MethodArgumentTypeMismatchException e) {
        return responder(HttpStatus.BAD_REQUEST, "El valor «" + e.getValue() + "» no es válido para " + e.getName());
    }

    // Cualquier otra excepción. Si la causa es un error de PostgreSQL se traduce por su
    // SQLSTATE; si es un error de Spring (ruta inexistente, método no permitido) se
    // conserva su código.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaError> otroError(Exception e, HttpServletRequest peticion) {
        PSQLException errorBd = buscarErrorPostgres(e);
        if (errorBd != null) {
            return errorBaseDatos(errorBd, peticion.getMethod());
        }
        if (e instanceof ErrorResponse errorSpring) {
            HttpStatusCode codigo = errorSpring.getStatusCode();
            String mensaje = switch (codigo.value()) {
                case 404 -> "La ruta solicitada no existe";
                case 405 -> "La ruta no admite el método " + peticion.getMethod();
                case 415 -> "Los datos deben enviarse en formato JSON";
                default -> "La petición no es válida";
            };
            return responder(codigo, mensaje);
        }
        log.error("Error no controlado en {} {}", peticion.getMethod(), peticion.getRequestURI(), e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Error inesperado en el servidor");
    }

    /*
     * SQLSTATE de PostgreSQL -> código HTTP
     *   P0001  RAISE EXCEPTION de un trigger o función (regla de negocio)   409
     *   P0002  no existe (sp_anular_compra con una compra inexistente)       404
     *   23505  UNIQUE repetido,  23503 y 23001  FOREIGN KEY                  409
     *   23514  CHECK,  23502  NOT NULL,  22xxx  dato inválido                400
     *   42501  el usuario de la base no tiene ese permiso                    403
     *   08xxx, 28xxx, 3D000  no se pudo conectar a la base                   503
     */
    private ResponseEntity<RespuestaError> errorBaseDatos(PSQLException e, String metodoHttp) {
        String estado = e.getSQLState() == null ? "" : e.getSQLState();
        ServerErrorMessage detalle = e.getServerErrorMessage();
        String mensaje = detalle != null ? detalle.getMessage() : e.getMessage();
        String restriccion = detalle != null ? detalle.getConstraint() : null;
        String columna = detalle != null ? detalle.getColumn() : null;
        log.info("PostgreSQL respondió {}: {}", estado, mensaje);

        return switch (estado) {
            case "P0001" -> responder(HttpStatus.CONFLICT, mensaje);
            case "P0002" -> responder(HttpStatus.NOT_FOUND, mensaje);
            case "22023" -> responder(HttpStatus.BAD_REQUEST, mensaje);   // cantidad de boletas fuera de 1..10
            case "23505" -> responder(HttpStatus.CONFLICT,
                    MensajesRestriccion.DATOS.getOrDefault(restriccion, "Ya existe un registro con esos datos"));
            case "23503", "23001" -> errorLlaveForanea(estado, restriccion, metodoHttp);
            case "23514" -> responder(HttpStatus.BAD_REQUEST,
                    MensajesRestriccion.DATOS.getOrDefault(restriccion, "Un dato no cumple la restricción " + restriccion));
            case "23502" -> responder(HttpStatus.BAD_REQUEST, "Falta el dato obligatorio: " + columna);
            case "22001" -> responder(HttpStatus.BAD_REQUEST, "Un texto supera la longitud permitida");
            case "22003" -> responder(HttpStatus.BAD_REQUEST, "Un número supera el valor permitido");
            case "42501" -> responder(HttpStatus.FORBIDDEN,
                    "El usuario de la base de datos no tiene permiso para esta operación");
            case "28P01", "28000" -> responder(HttpStatus.SERVICE_UNAVAILABLE,
                    "PostgreSQL rechazó el usuario o la contraseña de application.properties");
            case "3D000" -> responder(HttpStatus.SERVICE_UNAVAILABLE,
                    "No existe la base de datos indicada en application.properties");
            default -> {
                if (estado.startsWith("08")) {
                    yield responder(HttpStatus.SERVICE_UNAVAILABLE,
                            "No hay conexión con PostgreSQL: revise que el servicio esté encendido y la URL de application.properties");
                }
                if (estado.startsWith("22")) {
                    yield responder(HttpStatus.BAD_REQUEST, "Un dato no tiene el formato correcto");
                }
                log.error("Error de PostgreSQL sin traducir ({})", estado, e);
                yield responder(HttpStatus.INTERNAL_SERVER_ERROR, "Error en la base de datos: " + mensaje);
            }
        };
    }

    // La misma FK falla al crear (el registro relacionado no existe) o al eliminar
    // (otros registros dependen de este); el método HTTP dice cuál de los dos fue.
    // PostgreSQL 18 responde 23001 en vez de 23503 cuando lo impide un ON DELETE RESTRICT.
    private static ResponseEntity<RespuestaError> errorLlaveForanea(String estado, String restriccion, String metodoHttp) {
        if ("DELETE".equals(metodoHttp) || "23001".equals(estado)) {
            return responder(HttpStatus.CONFLICT, MensajesRestriccion.TIENE_DEPENDIENTES.getOrDefault(restriccion,
                    "No se puede eliminar: otros registros dependen de este"));
        }
        return responder(HttpStatus.CONFLICT, MensajesRestriccion.REFERENCIA_INEXISTENTE.getOrDefault(restriccion,
                "Uno de los registros relacionados no existe"));
    }

    // Recorre las causas de la excepción hasta encontrar la que lanzó el driver de PostgreSQL
    private static PSQLException buscarErrorPostgres(Throwable e) {
        while (e != null) {
            if (e instanceof PSQLException psql) {
                return psql;
            }
            e = e.getCause();
        }
        return null;
    }

    private static ResponseEntity<RespuestaError> responder(HttpStatusCode codigo, String mensaje) {
        return ResponseEntity.status(codigo).body(new RespuestaError(codigo.value(), mensaje));
    }
}
