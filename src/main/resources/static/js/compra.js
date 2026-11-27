import { api } from './api.js';
import { alActualizar, opcionesClientes } from './datos.js';
import {
    $, boton, disponibles, etiquetaEstado, fecha, fechaHora, hora, leerFormulario, limpiar, llenarSelect,
    mostrarError, mostrarExito, numero, pesos, pintarDetalle, pintarTabla,
} from './util.js';

// Módulo del cliente: catálogo, disponibilidad, compra (caso de uso principal) e historial

let eventoElegido = null;
let cupos = [];

export function iniciarCompra() {
    $('compra-cerrar').addEventListener('click', cerrarCompra);
    $('form-compra').addEventListener('submit', comprar);
    $('compra-localidad').addEventListener('change', actualizarTotal);
    $('compra-cantidad').addEventListener('input', actualizarTotal);
    $('boletas-cliente').addEventListener('change', cargarBoletas);
    $('form-boleta').addEventListener('submit', buscarBoleta);

    alActualizar(() => {
        llenarSelect($('compra-cliente'), opcionesClientes(), 'Elija un cliente');
        llenarSelect($('boletas-cliente'), opcionesClientes(), 'Elija un cliente');
    });
}

export async function cargarCatalogo() {
    limpiar($('catalogo-mensaje'));
    try {
        const eventos = await api.get('/api/catalogo');
        pintarTabla($('tabla-catalogo'), [
            { titulo: 'Evento', valor: e => e.evento },
            { titulo: 'Tipo', valor: e => e.tipoEvento },
            { titulo: 'Escenario', valor: e => e.escenario },
            { titulo: 'Fecha', valor: e => fecha(e.fecha) },
            { titulo: 'Horario', valor: e => `${hora(e.horaInicio)} a ${hora(e.horaFin)}` },
            { titulo: 'Desde', valor: e => pesos(e.precioDesde), numero: true },
            { titulo: 'Disponibles', valor: e => disponibles(e.boletasDisponibles), numero: true },
        ], eventos, {
            vacio: 'No hay eventos a la venta.',
            acciones: e => [boton('Comprar', () => abrirCompra(e), 'boton chico')],
            marcada: e => eventoElegido !== null && e.idEvento === eventoElegido.idEvento,
        });
    } catch (e) {
        mostrarError($('catalogo-mensaje'), e.message);
    }
}

async function abrirCompra(evento) {
    eventoElegido = evento;
    $('panel-compra').hidden = false;
    $('compra-evento').textContent = evento.evento;
    $('compra-info').textContent = `${evento.tipoEvento} · ${evento.escenario} (${evento.direccion}) · `
        + `${fecha(evento.fecha)}, ${hora(evento.horaInicio)} a ${hora(evento.horaFin)}`;
    $('compra-cantidad').value = 1;
    limpiar($('compra-mensaje'), $('compra-resultado'));
    await cargarDisponibilidad();
    await cargarCatalogo();
    $('panel-compra').scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function cerrarCompra() {
    eventoElegido = null;
    $('panel-compra').hidden = true;
    cargarCatalogo();
}

async function cargarDisponibilidad() {
    try {
        cupos = await api.get(`/api/eventos/${eventoElegido.idEvento}/disponibilidad`);
        pintarTabla($('tabla-disponibilidad'), [
            { titulo: 'Localidad', valor: d => d.localidad },
            { titulo: 'Precio', valor: d => pesos(d.precio), numero: true },
            { titulo: 'Aforo', valor: d => numero(d.aforoMaximo), numero: true },
            { titulo: 'Vendidas', valor: d => numero(d.boletasVendidas), numero: true },
            { titulo: 'Disponibles', valor: d => disponibles(d.boletasDisponibles), numero: true },
        ], cupos, { vacio: 'Este evento todavía no tiene precios.' });
        llenarSelect($('compra-localidad'),
            cupos.map(d => [d.idLocalidad, `${d.localidad} · ${pesos(d.precio)} · ${numero(d.boletasDisponibles)} disponibles`]));
        actualizarTotal();
    } catch (e) {
        mostrarError($('compra-mensaje'), e.message);
    }
}

// Total de referencia en pantalla; el precio que se cobra lo fija la base de datos
function actualizarTotal() {
    const localidad = cupos.find(d => d.idLocalidad === Number($('compra-localidad').value));
    const cantidad = Number($('compra-cantidad').value) || 0;
    $('compra-total').textContent = pesos(localidad ? localidad.precio * cantidad : 0);
}

// La página no revisa los cupos: los valida PostgreSQL en la compra y,
// si algo falla, aquí se muestra el mensaje que devuelve la API
async function comprar(evento) {
    evento.preventDefault();
    const pedido = { ...leerFormulario($('form-compra')), idEvento: eventoElegido.idEvento };
    limpiar($('compra-resultado'));
    try {
        const compra = await api.post('/api/compras', pedido);
        mostrarExito($('compra-mensaje'),
            `Compra #${compra.idCompra} registrada a nombre de ${compra.cliente}: `
            + `${compra.boletas.length} boleta(s) por ${pesos(compra.total)}.`);
        pintarTabla($('compra-resultado'), [
            { titulo: 'Boleta', valor: b => b.idBoleta, numero: true },
            { titulo: 'Localidad', valor: b => b.localidad },
            { titulo: 'Precio pagado', valor: b => pesos(b.precioPagado), numero: true },
            { titulo: 'Estado', valor: b => etiquetaEstado(b.estado) },
        ], compra.boletas);
    } catch (e) {
        mostrarError($('compra-mensaje'), e.message);
    }
    // Se vuelven a pedir los cupos: si la compra falló, deben seguir iguales
    await cargarDisponibilidad();
    await cargarCatalogo();
}

// ---------- Historial de compras del cliente

export function refrescarBoletas() {
    if ($('boletas-cliente').value) {
        cargarBoletas();
    }
}

async function cargarBoletas() {
    const idCliente = $('boletas-cliente').value;
    limpiar($('boletas-mensaje'), $('tabla-boletas'));
    $('boletas-resumen').textContent = '';
    if (!idCliente) {
        return;
    }
    try {
        const boletas = await api.get(`/api/clientes/${idCliente}/boletas`);
        const vigentes = boletas.filter(b => b.estado === 'Vendida').length;
        if (boletas.length > 0) {
            $('boletas-resumen').textContent =
                `${boletas.length} boleta(s): ${vigentes} vigente(s) y ${boletas.length - vigentes} cancelada(s).`;
        }
        pintarTabla($('tabla-boletas'), [
            { titulo: 'Boleta', valor: b => b.idBoleta, numero: true },
            { titulo: 'Compra', valor: b => b.idCompra, numero: true },
            { titulo: 'Fecha de compra', valor: b => fechaHora(b.fechaCompra) },
            { titulo: 'Evento', valor: b => b.evento },
            { titulo: 'Fecha del evento', valor: b => fecha(b.fechaEvento) },
            { titulo: 'Escenario', valor: b => b.escenario },
            { titulo: 'Localidad', valor: b => b.localidad },
            { titulo: 'Precio pagado', valor: b => pesos(b.precioPagado), numero: true },
            { titulo: 'Estado', valor: b => etiquetaEstado(b.estado) },
        ], boletas, { vacio: 'Este cliente todavía no ha comprado boletas.' });
    } catch (e) {
        mostrarError($('boletas-mensaje'), e.message);
    }
}

async function buscarBoleta(evento) {
    evento.preventDefault();
    const idBoleta = $('boleta-id').value.trim();
    limpiar($('boleta-mensaje'), $('boleta-detalle'));
    if (!idBoleta) {
        mostrarError($('boleta-mensaje'), 'Escriba el código de la boleta.');
        return;
    }
    try {
        const b = await api.get(`/api/boletas/${idBoleta}`);
        pintarDetalle($('boleta-detalle'), [
            ['Boleta', '#' + b.idBoleta],
            ['Estado', etiquetaEstado(b.estado)],
            ['Evento', b.evento],
            ['Tipo', b.tipoEvento],
            ['Fecha del evento', `${fecha(b.fechaEvento)}, ${hora(b.horaInicio)}`],
            ['Escenario', b.escenario],
            ['Localidad', b.localidad],
            ['Precio pagado', pesos(b.precioPagado)],
            ['Cliente', b.cliente],
            ['Compra', `#${b.idCompra} · ${fechaHora(b.fechaCompra)} · ${b.metodoPago}`],
        ]);
    } catch (e) {
        mostrarError($('boleta-mensaje'), e.message);
    }
}
