package com.bogotickets.dto;

// Cuerpo de todas las respuestas de error: {"estado": 409, "mensaje": "..."}
public record RespuestaError(int estado, String mensaje) {
}
