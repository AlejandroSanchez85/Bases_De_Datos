import { api } from './api.js';
import { alActualizar, opcionesEventos } from './datos.js';
import {
    $, barraOcupacion, fecha, filaTotales, limpiar, llenarSelect, mostrarError, numero, pesos, pintarTabla,
    porcentaje,
} from './util.js';

// Módulo del analista de ventas: solo consulta (F5)

let reporte = 'ocupacion';

const CARGAR = {
    'ocupacion': cargarOcupacion,
    'baja': cargarBajaOcupacion,
    'promedio': cargarSobrePromedio,
    'sin-compras': cargarSinCompras,
    'clientes': cargarClientes,
};

export function iniciarReportes() {
    document.querySelectorAll('#reportes-menu button').forEach(b =>
        b.addEventListener('click', () => mostrarReporte(b.dataset.reporte)));
    $('ocupacion-evento').addEventListener('change', cargarOcupacion);
    $('form-baja').addEventListener('submit', evento => {
        evento.preventDefault();
        cargarBajaOcupacion();
    });
    alActualizar(() => llenarSelect($('ocupacion-evento'), opcionesEventos(), 'Todos los eventos'));
}

// Se llama cada vez que se abre la pestaña Reportes
export function mostrarReportes() {
    mostrarReporte(reporte);
}

function mostrarReporte(nuevo) {
    reporte = nuevo;
    document.querySelectorAll('#reportes-menu button').forEach(b =>
        b.classList.toggle('activo', b.dataset.reporte === reporte));
    document.querySelectorAll('.tarjeta.reporte').forEach(t => {
        t.hidden = t.dataset.reporte !== reporte;
    });
    CARGAR[reporte]();
}

async function cargarOcupacion() {
    const idEvento = $('ocupacion-evento').value;
    limpiar($('ocupacion-mensaje'));
    $('ocupacion-resumen').textContent = '';
    try {
        const filas = await api.get(idEvento ? `/api/reportes/ocupacion?idEvento=${idEvento}` : '/api/reportes/ocupacion');
        const aforo = filas.reduce((suma, f) => suma + f.aforoMaximo, 0);
        const vendidas = filas.reduce((suma, f) => suma + f.boletasVendidas, 0);
        const recaudo = filas.reduce((suma, f) => suma + Number(f.recaudo), 0);
        const ocupacion = aforo > 0 ? Math.round(10000 * vendidas / aforo) / 100 : 0;
        if (filas.length > 0) {
            $('ocupacion-resumen').textContent =
                `${numero(vendidas)} de ${numero(aforo)} cupos vendidos (${porcentaje(ocupacion)}) · Recaudo: ${pesos(recaudo)}`;
        }

        // Con todos los eventos se agrega la columna del evento
        const columnas = idEvento ? [] : [
            { titulo: 'Evento', valor: f => f.evento },
            { titulo: 'Fecha', valor: f => fecha(f.fechaEvento) },
        ];
        columnas.push(
            { titulo: 'Localidad', valor: f => f.localidad },
            { titulo: 'Precio vigente', valor: f => pesos(f.precioVigente), numero: true },
            { titulo: 'Aforo', valor: f => numero(f.aforoMaximo), numero: true },
            { titulo: 'Vendidas', valor: f => numero(f.boletasVendidas), numero: true },
            { titulo: 'Disponibles', valor: f => numero(f.boletasDisponibles), numero: true },
            { titulo: 'Ocupación', valor: f => barraOcupacion(f.porcentajeOcupacion) },
            { titulo: 'Recaudo', valor: f => pesos(f.recaudo), numero: true },
        );
        const vacias = Array(columnas.length - 6).fill({ texto: '' });
        pintarTabla($('ocupacion-tabla'), columnas, filas, {
            vacio: 'El evento no tiene precios asignados.',
            pie: filaTotales([
                { texto: 'Total' }, ...vacias,
                { texto: numero(aforo), numero: true },
                { texto: numero(vendidas), numero: true },
                { texto: numero(aforo - vendidas), numero: true },
                { texto: porcentaje(ocupacion) },
                { texto: pesos(recaudo), numero: true },
            ]),
        });
    } catch (e) {
        mostrarError($('ocupacion-mensaje'), e.message);
    }
}

async function cargarBajaOcupacion() {
    const umbral = $('baja-umbral').value.trim();
    limpiar($('baja-mensaje'));
    try {
        const filas = await api.get('/api/reportes/eventos-baja-ocupacion'
            + (umbral ? `?umbral=${encodeURIComponent(umbral)}` : ''));
        pintarTabla($('baja-tabla'), [
            { titulo: 'Evento', valor: f => f.evento },
            { titulo: 'Fecha', valor: f => fecha(f.fechaEvento) },
            { titulo: 'Aforo total', valor: f => numero(f.aforoTotal), numero: true },
            { titulo: 'Vendidas', valor: f => numero(f.boletasVendidas), numero: true },
            { titulo: 'Ocupación', valor: f => barraOcupacion(f.porcentajeOcupacion) },
        ], filas, { vacio: 'Ningún evento está por debajo de ese porcentaje.' });
    } catch (e) {
        mostrarError($('baja-mensaje'), e.message);
    }
}

async function cargarSobrePromedio() {
    limpiar($('promedio-mensaje'));
    $('promedio-resumen').textContent = '';
    try {
        const filas = await api.get('/api/reportes/precios-sobre-promedio');
        if (filas.length > 0) {
            $('promedio-resumen').textContent =
                `Promedio de todas las tarifas: ${pesos(filas[0].promedio)} · ${filas.length} tarifas lo superan`;
        }
        pintarTabla($('promedio-tabla'), [
            { titulo: 'Evento', valor: f => f.evento },
            { titulo: 'Localidad', valor: f => f.localidad },
            { titulo: 'Precio', valor: f => pesos(f.precio), numero: true },
        ], filas, { vacio: 'Ninguna tarifa supera el promedio.' });
    } catch (e) {
        mostrarError($('promedio-mensaje'), e.message);
    }
}

async function cargarSinCompras() {
    limpiar($('sin-compras-mensaje'));
    try {
        const filas = await api.get('/api/reportes/clientes-sin-compras');
        pintarTabla($('sin-compras-tabla'), [
            { titulo: 'Id', valor: f => f.idCliente, numero: true },
            { titulo: 'Cliente', valor: f => f.nombreCompleto },
            { titulo: 'Fecha de registro', valor: f => fecha(f.fechaRegistro) },
        ], filas, { vacio: 'Todos los clientes han comprado al menos una vez.' });
    } catch (e) {
        mostrarError($('sin-compras-mensaje'), e.message);
    }
}

async function cargarClientes() {
    limpiar($('clientes-mensaje'));
    try {
        const filas = await api.get('/api/reportes/clientes');
        pintarTabla($('clientes-tabla'), [
            { titulo: 'Id', valor: f => f.idCliente, numero: true },
            { titulo: 'Cliente', valor: f => f.nombreCompleto },
            { titulo: 'Correo', valor: f => f.correoOculto },
            { titulo: 'Registro', valor: f => fecha(f.fechaRegistro) },
            { titulo: 'Compras', valor: f => numero(f.compras), numero: true },
            { titulo: 'Boletas vigentes', valor: f => numero(f.boletasVigentes), numero: true },
        ], filas);
    } catch (e) {
        mostrarError($('clientes-mensaje'), e.message);
    }
}
