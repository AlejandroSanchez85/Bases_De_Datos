package com.bogotickets.service;

import com.bogotickets.dto.BoletaDetalle;
import com.bogotickets.dto.CompraDetalle;
import com.bogotickets.dto.CompraRequest;
import com.bogotickets.dto.Disponibilidad;
import com.bogotickets.dto.EventoCatalogo;
import com.bogotickets.exception.RecursoNoEncontradoException;
import com.bogotickets.repository.VentaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VentaService {

    private final VentaRepository ventas;
    private final EventoService eventos;
    private final ClienteService clientes;

    public VentaService(VentaRepository ventas, EventoService eventos, ClienteService clientes) {
        this.ventas = ventas;
        this.eventos = eventos;
        this.clientes = clientes;
    }

    public List<EventoCatalogo> catalogo() {
        return ventas.catalogo();
    }

    // Funcionalidad F4: cupos por localidad en tiempo real
    public List<Disponibilidad> disponibilidad(int idEvento) {
        eventos.buscar(idEvento);
        return ventas.disponibilidad(idEvento);
    }

    // Funcionalidad F1 (caso de uso principal). Las reglas las valida PostgreSQL dentro
    // de fn_registrar_compra; si alguna falla no se guarda nada y el error sigue hacia
    // ManejadorErrores, que responde con el mensaje de la base.
    public CompraDetalle comprar(CompraRequest datos) {
        int idCompra = ventas.registrarCompra(datos);
        return buscarCompra(idCompra);
    }

    public CompraDetalle buscarCompra(int idCompra) {
        return ventas.buscarCompra(idCompra)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la compra " + idCompra));
    }

    // Funcionalidad F2: si la compra no existe, el procedimiento lanza P0002 (404)
    public CompraDetalle anular(int idCompra) {
        ventas.anularCompra(idCompra);
        return buscarCompra(idCompra);
    }

    public BoletaDetalle buscarBoleta(int idBoleta) {
        return ventas.buscarBoleta(idBoleta)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la boleta " + idBoleta));
    }

    // Funcionalidad F6: historial de compras del cliente
    public List<BoletaDetalle> boletasDeCliente(int idCliente) {
        clientes.buscar(idCliente);
        return ventas.boletasDeCliente(idCliente);
    }
}
