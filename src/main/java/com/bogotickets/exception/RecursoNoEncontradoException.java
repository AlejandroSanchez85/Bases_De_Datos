package com.bogotickets.exception;

// La lanzan los servicios cuando el id pedido no existe. ManejadorErrores responde 404.
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
