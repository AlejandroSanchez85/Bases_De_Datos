package com.bogotickets.service;

import com.bogotickets.dto.CompraDetalle;
import com.bogotickets.dto.CompraResumen;
import com.bogotickets.exception.RecursoNoEncontradoException;
import com.bogotickets.model.Cliente;
import com.bogotickets.repository.VentaRepository;
import com.bogotickets.security.SesionActual;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Cuenta del cliente que inició sesión. El id nunca llega en la petición: sale de la
// sesión, así un cliente no puede ver ni cambiar los datos de otro (RNF-05).
@Service
@Transactional
public class CuentaService {

    private final ClienteService clientes;
    private final VentaRepository ventas;

    public CuentaService(ClienteService clientes, VentaRepository ventas) {
        this.clientes = clientes;
        this.ventas = ventas;
    }

    public Cliente datos() {
        return clientes.buscar(SesionActual.idCliente());
    }

    public Cliente actualizar(Cliente datos) {
        return clientes.actualizar(SesionActual.idCliente(), datos);
    }

    public void eliminar() {
        clientes.eliminar(SesionActual.idCliente());
    }

    public List<CompraResumen> compras() {
        return ventas.comprasDeCliente(SesionActual.idCliente());
    }

    // Una compra de otro cliente responde 404, igual que una que no existe
    public CompraDetalle compra(int idCompra) {
        CompraResumen compra = ventas.buscarCompra(idCompra)
                .filter(c -> c.idCliente() == SesionActual.idCliente())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la compra " + idCompra + " en su cuenta"));
        return new CompraDetalle(compra, ventas.boletasDeCompra(idCompra));
    }
}
