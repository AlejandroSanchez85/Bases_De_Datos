import { api } from './api.js';
import {
    $, barraOcupacion, crear, fecha, filaTotales, limpiar, llenarSelect, mostrarError, mostrarVista, numero, pesos,
    pintarTabla, porcentaje,
} from './util.js';

// Módulo del analista de ventas: solo consulta (F5). El rol rol_analista solo lee
// vistas, por eso la lista de eventos sale del mismo reporte de ocupación.

let reporte = 'resumen';
let eventosCargados = false;

const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];
const GENEROS = { MASCULINO: 'Masculino', FEMENINO: 'Femenino', OTRO: 'Otro' };

// «Bogotá» o «Chía, Cundinamarca»
const lugar = f => (f.departamento.startsWith(f.ciudad) ? f.ciudad : `${f.ciudad}, ${f.departamento}`);

const CARGAR = {
    'resumen': cargarResumen,
    'ocupacion': cargarOcupacion,
    'baja': cargarBajaOcupacion,
    'promedio': cargarSobrePromedio,
    'perfil': cargarPerfil,
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
}

// Se llama al iniciar sesión como analista
export function mostrarReportes() {
    mostrarVista('reportes');
    eventosCargados = false;
    mostrarReporte(reporte);
}

function mostrarReporte(nuevo) {
    reporte = nuevo;
    document.querySelectorAll('#reportes-menu button').forEach(b =>
        b.classList.toggle('activo', b.dataset.reporte === reporte));
    document.querySelectorAll('#vista-reportes .reporte').forEach(t => {
        t.hidden = t.dataset.reporte !== reporte;
    });
    CARGAR[reporte]();
}

// ---------- Resumen: cifras generales y ventas agrupadas

async function cargarResumen() {
    limpiar($('resumen-mensaje'));
    let r;
    try {
        r = await api.get('/api/reportes/resumen');
    } catch (e) {
        mostrarError($('resumen-mensaje'), e.message);
        return;
    }
    const cifras = [
        ['Recaudo', pesos(r.recaudo)],
        ['Boletas vendidas', numero(r.boletasVendidas)],
        ['Compras', numero(r.compras)],
        ['Clientes que han comprado', numero(r.clientesConCompras)],
        ['Eventos a la venta', numero(r.eventosALaVenta)],
        ['Boletas anuladas', numero(r.boletasCanceladas)],
    ];
    $('resumen-cifras').replaceChildren(...cifras.map(([texto, valor]) =>
        crear('div', { clase: 'cifra' }, crear('span', { texto }), crear('strong', { texto: valor }))));
    pintarBarras($('resumen-tipos'), r.porTipoEvento);
    pintarBarras($('resumen-escenarios'), r.porEscenario);

    // Columnas por mes: la altura es proporcional a las boletas vendidas
    const maximo = Math.max(...r.porMes.map(m => m.boletas), 1);
    $('resumen-meses').replaceChildren(...r.porMes.map(m => {
        const [anio, mes] = m.nombre.split('-');
        const columna = crear('div', { clase: 'columna', title: pesos(m.recaudo) });
        columna.style.height = Math.max(4, 160 * m.boletas / maximo) + 'px';
        return crear('div', { clase: 'mes' }, crear('strong', { texto: numero(m.boletas) }), columna,
            crear('span', { texto: `${MESES[Number(mes) - 1]} ${anio.slice(2)}` }));
    }));
}

// Barras horizontales con el recaudo de cada grupo
// medida: 'recaudo' (pesos) o 'boletas' (cantidad); el otro valor queda en el título
function pintarBarras(contenedor, grupos, medida = 'recaudo') {
    const maximo = Math.max(...grupos.map(g => Number(g[medida])), 1);
    contenedor.replaceChildren(crear('div', { clase: 'barras' }, ...grupos.map(g => {
        const relleno = crear('span');
        relleno.style.width = (100 * Number(g[medida]) / maximo) + '%';
        const texto = medida === 'recaudo' ? pesos(g.recaudo) : `${numero(g.boletas)} boletas`;
        const titulo = medida === 'recaudo' ? `${numero(g.boletas)} boletas` : pesos(g.recaudo);
        return crear('div', { clase: 'barra-fila' },
            crear('span', { texto: g.nombre }),
            crear('div', { clase: 'pista' }, relleno),
            crear('span', { clase: 'valor', texto, title: titulo }));
    })));
}

// ---------- Perfil de compradores: género, edad y residencia de quien compra

async function cargarPerfil() {
    limpiar($('perfil-mensaje'));
    try {
        const perfil = await api.get('/api/reportes/perfil-compradores');
        pintarBarras($('perfil-genero'), perfil.porGenero, 'boletas');
        pintarBarras($('perfil-edad'), perfil.porRangoEdad, 'boletas');
        pintarBarras($('perfil-departamento'), perfil.porDepartamento);
    } catch (e) {
        mostrarError($('perfil-mensaje'), e.message);
    }
}

// ---------- Ocupación y recaudo por localidad

async function cargarOcupacion() {
    limpiar($('ocupacion-mensaje'));
    $('ocupacion-resumen').textContent = '';
    if (!eventosCargados) {
        try {
            const todas = await api.get('/api/reportes/ocupacion');
            const eventos = [...new Map(todas.map(f => [f.idEvento, `${f.evento} · ${fecha(f.fechaEvento)}`])).entries()];
            llenarSelect($('ocupacion-evento'), eventos, 'Todos los eventos');
            eventosCargados = true;
        } catch (e) {
            mostrarError($('ocupacion-mensaje'), e.message);
            return;
        }
    }
    const idEvento = $('ocupacion-evento').value;
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
            { titulo: 'Nombres', valor: f => f.nombres },
            { titulo: 'Apellidos', valor: f => f.apellidos },
            { titulo: 'Ciudad', valor: f => f.ciudad },
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
            { titulo: 'Cliente', valor: f => `${f.nombres} ${f.apellidos}` },
            { titulo: 'Correo', valor: f => f.correoOculto },
            { titulo: 'Género', valor: f => GENEROS[f.genero] },
            { titulo: 'Edad', valor: f => f.edad, numero: true },
            { titulo: 'Residencia', valor: f => lugar(f) },
            { titulo: 'Registro', valor: f => fecha(f.fechaRegistro) },
            { titulo: 'Compras', valor: f => numero(f.compras), numero: true },
            { titulo: 'Boletas vigentes', valor: f => numero(f.boletasVigentes), numero: true },
        ], filas);
    } catch (e) {
        mostrarError($('clientes-mensaje'), e.message);
    }
}
