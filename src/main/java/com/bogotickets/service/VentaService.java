package com.bogotickets.service;

import com.bogotickets.dto.BoletaDetalle;
import com.bogotickets.dto.CompraDetalle;
import com.bogotickets.dto.CompraRequest;
import com.bogotickets.dto.CompraResumen;
import com.bogotickets.dto.Disponibilidad;
import com.bogotickets.dto.EventoCatalogo;
import com.bogotickets.exception.RecursoNoEncontradoException;
import com.bogotickets.repository.VentaRepository;
import com.bogotickets.security.SesionActual;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class VentaService {

    private static final int LIMITE_COMPRAS = 200;

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

    // Funcionalidad F1 (caso de uso principal). Compra a nombre del cliente que inició
    // sesión. Las reglas las valida PostgreSQL dentro de fn_registrar_compra; si alguna
    // falla no se guarda nada y el error sigue hacia ManejadorErrores con el mensaje de la base.
    public CompraDetalle comprar(CompraRequest datos) {
        int idCompra = ventas.registrarCompra(SesionActual.idCliente(), datos);
        return buscarCompra(idCompra);
    }

    public List<CompraResumen> listarCompras(Integer idEvento, Integer idCliente) {
        return ventas.listarCompras(idEvento, idCliente, LIMITE_COMPRAS);
    }

    public CompraDetalle buscarCompra(int idCompra) {
        CompraResumen compra = ventas.buscarCompra(idCompra)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la compra " + idCompra));
        return new CompraDetalle(compra, ventas.boletasDeCompra(idCompra));
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

    // Funcionalidad F6 vista por el administrador: boletas de cualquier cliente
    public List<BoletaDetalle> boletasDeCliente(int idCliente) {
        clientes.buscar(idCliente);
        return ventas.boletasDeCliente(idCliente);
    }
}
