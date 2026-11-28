package com.bogotickets.dto;

// Fila de la vista v_ubicaciones: una ciudad con su departamento, su país y el
// indicativo telefónico del país
public record Ubicacion(
        Integer idCiudad,
        String ciudad,
        Integer idDepartamento,
        String departamento,
        Integer idPais,
        String pais,
        String indicativo) {
}
