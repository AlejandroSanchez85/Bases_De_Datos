package com.bogotickets.dto;

import java.util.List;

// Boletas vendidas y recaudo según quién compra: género, rango de edad y
// departamento de residencia (GET /api/reportes/perfil-compradores)
public record PerfilCompradores(
        List<VentaAgrupada> porGenero,
        List<VentaAgrupada> porRangoEdad,
        List<VentaAgrupada> porDepartamento) {
}
