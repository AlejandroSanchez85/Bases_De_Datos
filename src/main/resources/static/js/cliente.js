import { api } from './api.js';
import { armarCamposPersona, cargarUbicaciones, llenarCamposPersona, textoCiudad } from './persona.js';
import {
    $, boton, crear, cuadroDia, edad, etiquetaEstado, fecha, fechaHora, fechaLarga, hora, hoy, leerFormulario, limpiar,
    llenarSelect, mostrarError, mostrarExito, mostrarVista, normalizar, numero, pesos, pintarTabla, textoEdadMinima,
} from './util.js';

// Módulo del cliente: cartelera, compra (caso de uso principal), mis compras y mi cuenta

const MAXIMO_BOLETAS = 10;

let catalogo = [];
let tipoElegido = '';
let eventoActual = null;
let cupos = [];
let localidadElegida = null;
let cantidad = 1;
let compras = [];
let filtroCompras = 'proximas';
let alEliminarCuenta = null;

export function iniciarCliente(funcionAlEliminarCuenta) {
    alEliminarCuenta = funcionAlEliminarCuenta;
    $('cartelera-buscar').addEventListener('input', pintarCartelera);
    $('cartelera-escenario').addEventListener('change', pintarCartelera);
    $('cartelera-fecha').addEventListener('change', pintarCartelera);
    $('evento-volver').addEventListener('click', () => abrirVistaCliente('cartelera'));
    $('compra-menos').addEventListener('click', () => cambiarCantidad(-1));
    $('compra-mas').addEventListener('click', () => cambiarCantidad(1));
    $('compra-metodo').addEventListener('change', () => limpiar($('compra-mensaje')));
    $('compra-confirmar').addEventListener('click', comprar);
    document.querySelectorAll('#mis-compras-filtro button').forEach(b => b.addEventListener('click', () => {
        filtroCompras = b.dataset.filtro;
        pintarCompras();
    }));
    $('form-cuenta').addEventListener('submit', guardarCuenta);
    $('cuenta-eliminar').addEventListener('click', eliminarCuenta);
}

export function abrirVistaCliente(vista) {
    mostrarVista(vista);
    if (vista === 'cartelera') {
        cargarCartelera();
    } else if (vista === 'mis-compras') {
        cargarCompras();
    } else if (vista === 'cuenta') {
        cargarCuenta();
    }
}

// ---------- Cartelera

async function cargarCartelera() {
    limpiar($('cartelera-mensaje'));
    try {
        catalogo = await api.get('/api/catalogo');
    } catch (e) {
        mostrarError($('cartelera-mensaje'), e.message);
        return;
    }
    // Los filtros se arman con los tipos y escenarios que tienen eventos a la venta
    const tipos = [...new Map(catalogo.map(e => [e.idTipoEvento, e.tipoEvento])).entries()]
        .sort((a, b) => a[1].localeCompare(b[1]));
    if (!tipos.some(([id]) => id === tipoElegido)) {
        tipoElegido = '';
    }
    const chips = $('cartelera-tipos');
    chips.replaceChildren();
    for (const [id, nombre] of [['', 'Todos'], ...tipos]) {
        const total = id === '' ? catalogo.length : catalogo.filter(e => e.idTipoEvento === id).length;
        const chip = crear('button', { type: 'button', texto: `${nombre} (${total})` });
        chip.classList.toggle('activo', id === tipoElegido);
        chip.addEventListener('click', () => {
            tipoElegido = id;
            chips.querySelectorAll('button').forEach(b => b.classList.toggle('activo', b === chip));
            pintarCartelera();
        });
        chips.append(chip);
    }
    const escenarios = [...new Map(catalogo.map(e => [e.idEscenario, e.escenario])).entries()]
        .sort((a, b) => a[1].localeCompare(b[1]));
    llenarSelect($('cartelera-escenario'), escenarios, 'Todos los escenarios');
    pintarCartelera();
}

function pintarCartelera() {
    const texto = normalizar($('cartelera-buscar').value.trim());
    const escenario = $('cartelera-escenario').value;
    const dias = Number($('cartelera-fecha').value);
    const hasta = dias ? hoy(dias) : null;

    const eventos = catalogo.filter(e =>
        (tipoElegido === '' || e.idTipoEvento === tipoElegido)
        && (escenario === '' || String(e.idEscenario) === escenario)
        && (hasta === null || e.fecha <= hasta)
        && (texto === '' || normalizar(`${e.evento} ${e.escenario} ${e.tipoEvento}`).includes(texto)));

    $('cartelera-cuenta').textContent = `${eventos.length} de ${catalogo.length} eventos a la venta`;
    const rejilla = $('cartelera-eventos');
    rejilla.replaceChildren();
    if (eventos.length === 0) {
        rejilla.append(crear('p', { clase: 'vacio', texto: 'No hay eventos con esos filtros.' }));
        return;
    }
    for (const e of eventos) {
        rejilla.append(crear('article', { clase: 'tarjeta-evento', onclick: () => abrirEvento(e) },
            crear('div', { clase: 'banda tipo-' + e.idTipoEvento },
                crear('div', {},
                    crear('span', { clase: 'tipo', texto: e.tipoEvento }),
                    crear('span', { clase: 'edad-minima', texto: textoEdadMinima(e.edadMinima) })),
                cuadroDia(e.fecha)),
            crear('div', { clase: 'cuerpo-evento' },
                crear('h3', { texto: e.evento }),
                crear('p', { clase: 'lugar', texto: `${e.escenario} · ${hora(e.horaInicio)}` }),
                crear('div', { clase: 'pie-evento' },
                    crear('span', { clase: 'precio-desde' }, 'Desde', crear('strong', { texto: pesos(e.precioDesde) })),
                    estadoCupos(e.boletasDisponibles, e.aforoTotal)))));
    }
}

// Agotado, últimas boletas (menos del 10 % del aforo) o disponible
function estadoCupos(disponibles, aforo) {
    if (disponibles === 0) {
        return crear('span', { clase: 'etiqueta agotada', texto: 'Agotado' });
    }
    if (disponibles < aforo * 0.1 || disponibles <= 20) {
        return crear('span', { clase: 'etiqueta ultimas', texto: 'Últimas boletas' });
    }
    return crear('span', { clase: 'etiqueta disponible', texto: 'Disponible' });
}

// ---------- Detalle del evento y compra

async function abrirEvento(evento) {
    eventoActual = evento;
    localidadElegida = null;
    cantidad = 1;
    mostrarVista('evento', 'cartelera');
    const portada = $('evento-portada');
    portada.className = 'portada-evento tipo-' + evento.idTipoEvento;
    portada.replaceChildren(
        crear('div', {},
            crear('span', { clase: 'tipo', texto: evento.tipoEvento.toUpperCase() }),
            crear('h2', { texto: evento.evento }),
            crear('p', { texto: `${fechaLarga(evento.fecha)} · ${hora(evento.horaInicio)} a ${hora(evento.horaFin)}` }),
            crear('p', { texto: `${evento.escenario} · ${evento.direccion}` }),
            crear('p', { texto: evento.edadMinima > 0
                ? `Edad mínima: ${evento.edadMinima} años cumplidos el día del evento`
                : 'Todo público' })),
        cuadroDia(evento.fecha));
    // La página solo informa la edad mínima: la regla la revisa PostgreSQL al comprar (RN-25)
    $('compra-edad').hidden = evento.edadMinima === 0;
    $('compra-edad').textContent = `Este evento es para mayores de ${evento.edadMinima} años. La base de datos `
        + 'lo verifica con la fecha de nacimiento de su cuenta.';
    $('compra-formulario').hidden = false;
    $('compra-confirmacion').hidden = true;
    limpiar($('compra-mensaje'));
    await cargarCupos();
}

async function cargarCupos() {
    try {
        cupos = await api.get(`/api/eventos/${eventoActual.idEvento}/disponibilidad`);
    } catch (e) {
        mostrarError($('compra-mensaje'), e.message);
        return;
    }
    const lista = $('evento-localidades');
    lista.replaceChildren();
    for (const c of cupos) {
        const agotada = c.boletasDisponibles === 0;
        const texto = agotada ? 'Agotada' : `${numero(c.boletasDisponibles)} disponibles de ${numero(c.aforoMaximo)}`;
        const opcion = crear('button', {
            type: 'button', clase: 'localidad' + (c.idLocalidad === localidadElegida ? ' activa' : ''),
            onclick: () => elegirLocalidad(c.idLocalidad),
        },
            crear('span', { clase: 'nombre', texto: c.localidad }),
            crear('span', { clase: 'precio', texto: pesos(c.precio) }),
            crear('span', { clase: 'cupos' }, agotada
                ? crear('span', { clase: 'etiqueta agotada', texto })
                : (c.boletasDisponibles <= 20 ? crear('span', { clase: 'etiqueta ultimas', texto: `Quedan ${c.boletasDisponibles}` }) : texto)));
        opcion.disabled = agotada;
        lista.append(opcion);
    }
    actualizarResumen();
}

function elegirLocalidad(idLocalidad) {
    localidadElegida = idLocalidad;
    limpiar($('compra-mensaje'));
    document.querySelectorAll('#evento-localidades .localidad').forEach((b, i) =>
        b.classList.toggle('activa', cupos[i].idLocalidad === idLocalidad));
    actualizarResumen();
}

function cambiarCantidad(cambio) {
    cantidad = Math.min(MAXIMO_BOLETAS, Math.max(1, cantidad + cambio));
    limpiar($('compra-mensaje'));
    actualizarResumen();
}

// El total es de referencia: el precio que se cobra lo fija la base de datos (RN-16)
function actualizarResumen() {
    const elegida = cupos.find(c => c.idLocalidad === localidadElegida);
    $('compra-localidad').textContent = elegida ? `${elegida.localidad} · ${pesos(elegida.precio)} c/u` : 'Elija una localidad';
    $('compra-cantidad').textContent = cantidad;
    $('compra-total').textContent = pesos(elegida ? elegida.precio * cantidad : 0);
    $('compra-confirmar').disabled = !elegida;
}

// La página no revisa los cupos: los valida PostgreSQL en fn_registrar_compra y,
// si una boleta falla, se muestra el mensaje que devuelve la API
async function comprar() {
    limpiar($('compra-mensaje'));
    $('compra-confirmar').disabled = true;
    try {
        const detalle = await api.post('/api/compras', {
            idEvento: eventoActual.idEvento,
            idLocalidad: localidadElegida,
            cantidad,
            metodoPago: $('compra-metodo').value,
        });
        mostrarConfirmacion(detalle);
    } catch (e) {
        mostrarError($('compra-mensaje'), e.message);
    }
    // Se vuelven a pedir los cupos: si la compra falló, deben seguir iguales
    await cargarCupos();
}

function mostrarConfirmacion({ compra, boletas }) {
    $('compra-formulario').hidden = true;
    const caja = $('compra-confirmacion');
    caja.hidden = false;
    caja.replaceChildren(crear('div', { clase: 'confirmacion' },
        crear('h3', { texto: '¡Compra confirmada!' }),
        crear('p', { texto: `Compra #${compra.idCompra} · ${boletas.length} boleta(s) en ${compra.localidades}` }),
        crear('p', { texto: `Total pagado: ${pesos(compra.total)} · ${compra.metodoPago}` }),
        crear('div', { clase: 'codigos' }, ...boletas.map(b => crear('span', { clase: 'codigo', texto: 'Boleta #' + b.idBoleta }))),
        crear('div', { clase: 'acciones' },
            boton('Ver mis compras', () => abrirVistaCliente('mis-compras'), 'boton'),
            boton('Comprar más boletas', () => {
                $('compra-formulario').hidden = false;
                caja.hidden = true;
            }, 'boton secundario'))));
}

// ---------- Mis compras

async function cargarCompras() {
    limpiar($('mis-compras-mensaje'));
    try {
        compras = await api.get('/api/cuenta/compras');
    } catch (e) {
        mostrarError($('mis-compras-mensaje'), e.message);
        return;
    }
    const vigentes = compras.filter(c => c.estado === 'Vigente');
    const boletas = vigentes.reduce((suma, c) => suma + c.boletasVigentes, 0);
    $('mis-compras-resumen').textContent =
        `${compras.length} compras · ${numero(boletas)} boletas vigentes`;
    pintarCompras();
}

function pintarCompras() {
    document.querySelectorAll('#mis-compras-filtro button').forEach(b =>
        b.classList.toggle('activo', b.dataset.filtro === filtroCompras));
    const hoyTexto = hoy();
    const filtradas = compras.filter(c => {
        if (filtroCompras === 'anuladas') {
            return c.estado === 'Anulada';
        }
        const futura = c.fechaEvento >= hoyTexto;
        return c.estado === 'Vigente' && (filtroCompras === 'proximas' ? futura : !futura);
    });
    // Los próximos eventos se muestran del más cercano al más lejano
    if (filtroCompras === 'proximas') {
        filtradas.sort((a, b) => a.fechaEvento.localeCompare(b.fechaEvento));
    }

    const lista = $('mis-compras-lista');
    lista.replaceChildren();
    if (filtradas.length === 0) {
        const textos = {
            proximas: 'No tiene boletas para eventos próximos. Mire la cartelera.',
            realizadas: 'Todavía no ha asistido a eventos con BogoTickets.',
            anuladas: 'No tiene compras anuladas.',
        };
        lista.append(crear('p', { clase: 'vacio', texto: textos[filtroCompras] }));
        return;
    }
    for (const c of filtradas) {
        const estado = c.estado === 'Anulada' ? 'Anulada' : (c.fechaEvento < hoyTexto ? 'Realizado' : 'Vigente');
        const detalle = crear('div', { clase: 'boletas-compra', hidden: '' });
        const ver = boton('Ver boletas', () => alternarBoletas(c.idCompra, detalle, ver));
        lista.append(crear('article', { clase: 'compra' + (c.estado === 'Anulada' ? ' anulada' : '') },
            cuadroDia(c.fechaEvento),
            crear('div', {},
                crear('h3', { texto: c.evento }),
                crear('p', { clase: 'datos', texto: `${c.escenario} · ${hora(c.horaInicio)} · ${c.localidades}` }),
                crear('p', { clase: 'datos', texto: `Compra #${c.idCompra} del ${fechaHora(c.fechaCompra)} · ${c.metodoPago}` })),
            crear('div', { clase: 'lado' },
                crear('strong', { texto: pesos(c.total) }),
                crear('span', { texto: `${c.boletas} boleta(s)` }),
                etiquetaEstado(estado),
                ver),
            detalle));
    }
}

async function alternarBoletas(idCompra, contenedor, botonVer) {
    if (!contenedor.hidden) {
        contenedor.hidden = true;
        botonVer.textContent = 'Ver boletas';
        return;
    }
    try {
        const { boletas } = await api.get(`/api/cuenta/compras/${idCompra}`);
        pintarTabla(contenedor, [
            { titulo: 'Código', valor: b => '#' + b.idBoleta },
            { titulo: 'Localidad', valor: b => b.localidad },
            { titulo: 'Fecha del evento', valor: b => `${fecha(b.fechaEvento)}, ${hora(b.horaInicio)}` },
            { titulo: 'Precio pagado', valor: b => pesos(b.precioPagado), numero: true },
            { titulo: 'Estado', valor: b => etiquetaEstado(b.estado) },
        ], boletas);
        contenedor.hidden = false;
        botonVer.textContent = 'Ocultar';
    } catch (e) {
        mostrarError($('mis-compras-mensaje'), e.message);
    }
}

// ---------- Mi cuenta

async function cargarCuenta() {
    limpiar($('cuenta-mensaje'), $('cuenta-eliminar-mensaje'));
    try {
        await cargarUbicaciones();
        const cliente = await api.get('/api/cuenta');
        armarCamposPersona($('cuenta-campos'), { identidadFija: true });
        llenarCamposPersona($('cuenta-campos'), cliente);
        $('cuenta-registro').textContent = `Cliente desde el ${fecha(cliente.fechaRegistro)} · `
            + `${edad(cliente.fechaNacimiento)} años · ${textoCiudad(cliente.idCiudad)}`;
    } catch (e) {
        mostrarError($('cuenta-mensaje'), e.message);
    }
}

async function guardarCuenta(evento) {
    evento.preventDefault();
    try {
        const cliente = await api.put('/api/cuenta', leerFormulario($('form-cuenta')));
        $('usuario-nombre').textContent = `${cliente.nombres} ${cliente.apellidos}`;
        mostrarExito($('cuenta-mensaje'), 'Sus datos quedaron actualizados. Si cambió el correo, úselo la próxima vez que inicie sesión.');
    } catch (e) {
        mostrarError($('cuenta-mensaje'), e.message);
    }
}

async function eliminarCuenta() {
    if (!confirm('¿Eliminar su cuenta? Esta acción no se puede deshacer.')) {
        return;
    }
    try {
        await api.eliminar('/api/cuenta');
        alEliminarCuenta('Su cuenta fue eliminada.');
    } catch (e) {
        mostrarError($('cuenta-eliminar-mensaje'), e.message);
    }
}
