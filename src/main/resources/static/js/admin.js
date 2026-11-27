import { api } from './api.js';
import {
    alActualizar, datos, nombreEscenario, nombreTipo, opcionesEscenarios, opcionesEventos, opcionesTipos,
    recargarDatos,
} from './datos.js';
import {
    $, boton, crear, etiquetaEstado, fecha, fechaHora, hora, leerFormulario, limpiar, llenarSelect,
    mostrarError, mostrarExito, numero, pesos, pintarDetalle, pintarTabla,
} from './util.js';

// Módulo del administrador. El CRUD de las cinco entidades usa el mismo formulario y la
// misma tabla: cada entidad se describe abajo con su URL, sus campos y sus columnas.
// soloAlCrear marca los campos que la API no cambia en el PUT; al editar se bloquean.

const ENTIDADES = {
    escenarios: {
        nombre: 'escenario', titulo: 'Escenarios', tabla: 'escenario', url: '/api/escenarios', id: 'idEscenario',
        campos: [
            { clave: 'nombre', etiqueta: 'Nombre' },
            { clave: 'direccion', etiqueta: 'Dirección' },
            { clave: 'capacidadMaxima', etiqueta: 'Capacidad máxima', tipo: 'number' },
        ],
        columnas: [
            { titulo: 'Id', valor: e => e.idEscenario, numero: true },
            { titulo: 'Nombre', valor: e => e.nombre },
            { titulo: 'Dirección', valor: e => e.direccion },
            { titulo: 'Capacidad', valor: e => numero(e.capacidadMaxima), numero: true },
        ],
        describir: e => e.nombre,
    },
    localidades: {
        nombre: 'localidad', femenino: true, titulo: 'Localidades', tabla: 'localidad', url: '/api/localidades',
        id: 'idLocalidad',
        filtros: [{ clave: 'idEscenario', etiqueta: 'Escenario', opciones: opcionesEscenarios }],
        campos: [
            { clave: 'idEscenario', etiqueta: 'Escenario', tipo: 'select', opciones: opcionesEscenarios, soloAlCrear: true },
            { clave: 'nombre', etiqueta: 'Nombre' },
            { clave: 'aforoMaximo', etiqueta: 'Aforo máximo', tipo: 'number' },
        ],
        columnas: [
            { titulo: 'Id', valor: l => l.idLocalidad, numero: true },
            { titulo: 'Escenario', valor: l => nombreEscenario(l.idEscenario) },
            { titulo: 'Localidad', valor: l => l.nombre },
            { titulo: 'Aforo', valor: l => numero(l.aforoMaximo), numero: true },
        ],
        describir: l => `${l.nombre} de ${nombreEscenario(l.idEscenario)}`,
    },
    tipos: {
        nombre: 'tipo de evento', titulo: 'Tipos de evento', tabla: 'tipo_evento', url: '/api/tipos-evento',
        id: 'idTipoEvento',
        campos: [
            { clave: 'nombre', etiqueta: 'Nombre' },
            { clave: 'descripcion', etiqueta: 'Descripción (opcional)' },
        ],
        columnas: [
            { titulo: 'Id', valor: t => t.idTipoEvento, numero: true },
            { titulo: 'Nombre', valor: t => t.nombre },
            { titulo: 'Descripción', valor: t => t.descripcion },
        ],
        describir: t => t.nombre,
    },
    eventos: {
        nombre: 'evento', titulo: 'Eventos', tabla: 'evento', url: '/api/eventos', id: 'idEvento',
        filtros: [
            { clave: 'idEscenario', etiqueta: 'Escenario', opciones: opcionesEscenarios },
            { clave: 'idTipo', etiqueta: 'Tipo', opciones: opcionesTipos },
        ],
        campos: [
            { clave: 'idEscenario', etiqueta: 'Escenario', tipo: 'select', opciones: opcionesEscenarios, soloAlCrear: true },
            { clave: 'idTipoEvento', etiqueta: 'Tipo de evento', tipo: 'select', opciones: opcionesTipos },
            { clave: 'nombre', etiqueta: 'Nombre' },
            { clave: 'fecha', etiqueta: 'Fecha', tipo: 'date' },
            { clave: 'horaInicio', etiqueta: 'Hora de inicio', tipo: 'time' },
            { clave: 'horaFin', etiqueta: 'Hora de fin', tipo: 'time' },
        ],
        columnas: [
            { titulo: 'Id', valor: e => e.idEvento, numero: true },
            { titulo: 'Evento', valor: e => e.nombre },
            { titulo: 'Tipo', valor: e => nombreTipo(e.idTipoEvento) },
            { titulo: 'Escenario', valor: e => nombreEscenario(e.idEscenario) },
            { titulo: 'Fecha', valor: e => fecha(e.fecha) },
            { titulo: 'Horario', valor: e => `${hora(e.horaInicio)} a ${hora(e.horaFin)}` },
        ],
        describir: e => e.nombre,
    },
    clientes: {
        nombre: 'cliente', titulo: 'Clientes', tabla: 'cliente', url: '/api/clientes', id: 'idCliente',
        campos: [
            { clave: 'documento', etiqueta: 'Documento', soloAlCrear: true },
            { clave: 'nombreCompleto', etiqueta: 'Nombre completo' },
            { clave: 'correo', etiqueta: 'Correo' },
            { clave: 'telefono', etiqueta: 'Teléfono' },
        ],
        columnas: [
            { titulo: 'Id', valor: c => c.idCliente, numero: true },
            { titulo: 'Documento', valor: c => c.documento },
            { titulo: 'Nombre', valor: c => c.nombreCompleto },
            { titulo: 'Correo', valor: c => c.correo },
            { titulo: 'Teléfono', valor: c => c.telefono },
            { titulo: 'Registro', valor: c => fecha(c.fechaRegistro) },
        ],
        describir: c => c.nombreCompleto,
    },
};

let seccion = 'escenarios';
let editando = null;            // id del registro que se está editando (null = creando)
let localidadEditada = null;    // localidad cuyo precio se está cambiando

export function iniciarAdmin() {
    document.querySelectorAll('#admin-menu button').forEach(b =>
        b.addEventListener('click', () => mostrarSeccion(b.dataset.seccion)));
    $('crud-form').addEventListener('submit', guardar);
    $('crud-cancelar').addEventListener('click', () => {
        limpiar($('crud-mensaje'));
        armarFormulario();
    });
    $('precios-evento').addEventListener('change', cargarPrecios);
    $('precios-form').addEventListener('submit', guardarPrecio);
    $('precios-cancelar').addEventListener('click', () => {
        limpiar($('precios-mensaje'));
        terminarCambioPrecio();
    });
    $('compras-buscar').addEventListener('submit', buscarCompra);
    $('historial-evento').addEventListener('change', cargarHistorial);

    // Cuando cambian las listas se actualizan los selectores de esta pantalla
    alActualizar(() => {
        llenarSelect($('precios-evento'), opcionesEventos(), 'Elija un evento');
        llenarSelect($('historial-evento'), opcionesEventos(), 'Todos los cambios');
        for (const campo of ENTIDADES[seccion] ? ENTIDADES[seccion].campos : []) {
            const select = $('campo-' + campo.clave);
            if (campo.tipo === 'select' && select) {
                llenarSelect(select, campo.opciones(), 'Elija una opción');
            }
        }
    });
}

// Se llama cada vez que se abre la pestaña Administración
export function mostrarAdmin() {
    mostrarSeccion(seccion);
}

function mostrarSeccion(nueva) {
    if (nueva !== seccion) {
        limpiar($('crud-mensaje'), $('precios-mensaje'), $('compras-mensaje'), $('historial-mensaje'));
    }
    seccion = nueva;
    document.querySelectorAll('#admin-menu button').forEach(b =>
        b.classList.toggle('activo', b.dataset.seccion === seccion));

    const esCrud = seccion in ENTIDADES;
    $('admin-crud').hidden = !esCrud;
    $('admin-precios').hidden = seccion !== 'precios';
    $('admin-compras').hidden = seccion !== 'compras';
    $('admin-historial').hidden = seccion !== 'historial';

    if (esCrud) {
        armarFiltros();
        armarFormulario();
        cargarTabla();
    } else if (seccion === 'precios') {
        cargarPrecios();
    } else if (seccion === 'historial') {
        cargarHistorial();
    }
}

// ---------- CRUD genérico

const articulo = entidad => entidad.femenino ? 'la' : 'el';

function armarFiltros() {
    const entidad = ENTIDADES[seccion];
    const contenedor = $('crud-filtros');
    contenedor.replaceChildren();
    for (const filtro of entidad.filtros || []) {
        const select = crear('select', { id: 'filtro-' + filtro.clave, onchange: cargarTabla });
        llenarSelect(select, filtro.opciones(), 'Todos');
        contenedor.append(crear('label', { clase: 'campo' }, crear('span', { texto: filtro.etiqueta }), select));
    }
}

// Sin registro arma el formulario para crear; con registro lo llena para editar
function armarFormulario(registro = null) {
    const entidad = ENTIDADES[seccion];
    editando = registro ? registro[entidad.id] : null;
    const genero = entidad.femenino ? 'a' : 'o';

    $('crud-titulo-form').textContent = registro
        ? `Editar ${entidad.nombre} #${editando}`
        : `Nuev${genero} ${entidad.nombre}`;
    $('crud-guardar').textContent = registro ? 'Guardar cambios' : 'Crear';
    $('crud-cancelar').hidden = !registro;
    $('crud-titulo-tabla').textContent = entidad.titulo;
    $('crud-fuente').textContent = `Tabla ${entidad.tabla} · ${entidad.url}`;

    const campos = $('crud-campos');
    campos.replaceChildren();
    for (const campo of entidad.campos) {
        let control;
        if (campo.tipo === 'select') {
            control = crear('select', { name: campo.clave, id: 'campo-' + campo.clave, 'data-numero': '' });
            llenarSelect(control, campo.opciones(), 'Elija una opción');
        } else {
            control = crear('input', { name: campo.clave, id: 'campo-' + campo.clave, type: campo.tipo || 'text' });
        }
        if (registro) {
            const valor = registro[campo.clave];
            control.value = valor == null ? '' : (campo.tipo === 'time' ? hora(valor) : valor);
            control.disabled = Boolean(campo.soloAlCrear);
        }
        const etiqueta = crear('label', { clase: 'campo' }, crear('span', { texto: campo.etiqueta }), control);
        if (registro && campo.soloAlCrear) {
            etiqueta.append(crear('small', { texto: 'No se puede cambiar después de crear el registro.' }));
        }
        campos.append(etiqueta);
    }
}

async function cargarTabla() {
    const entidad = ENTIDADES[seccion];
    const parametros = new URLSearchParams();
    for (const filtro of entidad.filtros || []) {
        const valor = $('filtro-' + filtro.clave).value;
        if (valor) {
            parametros.set(filtro.clave, valor);
        }
    }
    const consulta = parametros.toString();
    const url = consulta ? `${entidad.url}?${consulta}` : entidad.url;
    try {
        const filas = await api.get(url);
        pintarTabla($('crud-tabla'), entidad.columnas, filas, {
            vacio: `No hay ${entidad.titulo.toLowerCase()} registrad${entidad.femenino ? 'as' : 'os'}.`,
            acciones: fila => [
                boton('Editar', () => {
                    limpiar($('crud-mensaje'));
                    armarFormulario(fila);
                    $('crud-form').scrollIntoView({ block: 'nearest' });
                }),
                boton('Eliminar', () => eliminar(fila), 'boton chico peligro'),
            ],
            marcada: fila => editando !== null && fila[entidad.id] === editando,
        });
    } catch (e) {
        mostrarError($('crud-mensaje'), e.message);
    }
}

async function guardar(evento) {
    evento.preventDefault();
    const entidad = ENTIDADES[seccion];
    const registro = leerFormulario($('crud-form'));
    try {
        if (editando === null) {
            const creado = await api.post(entidad.url, registro);
            mostrarExito($('crud-mensaje'), `Se creó ${articulo(entidad)} ${entidad.nombre} #${creado[entidad.id]}.`);
        } else {
            await api.put(`${entidad.url}/${editando}`, registro);
            mostrarExito($('crud-mensaje'),
                `Se guardaron los cambios ${entidad.femenino ? 'de la' : 'del'} ${entidad.nombre} #${editando}.`);
        }
        armarFormulario();
        await recargarDatos();
        await cargarTabla();
    } catch (e) {
        mostrarError($('crud-mensaje'), e.message);
    }
}

async function eliminar(fila) {
    const entidad = ENTIDADES[seccion];
    const id = fila[entidad.id];
    if (!confirm(`¿Eliminar ${articulo(entidad)} ${entidad.nombre} «${entidad.describir(fila)}»?`)) {
        return;
    }
    try {
        await api.eliminar(`${entidad.url}/${id}`);
        mostrarExito($('crud-mensaje'), `Se eliminó ${articulo(entidad)} ${entidad.nombre} «${entidad.describir(fila)}».`);
        if (editando === id) {
            armarFormulario();
        }
        await recargarDatos();
        await cargarTabla();
    } catch (e) {
        mostrarError($('crud-mensaje'), e.message);
    }
}

// ---------- Precios de un evento (F3: cada cambio queda en el historial)

function eventoDePrecios() {
    return datos.eventos.find(e => e.idEvento === Number($('precios-evento').value));
}

async function cargarPrecios() {
    const evento = eventoDePrecios();
    terminarCambioPrecio();
    limpiar($('precios-tabla'));
    $('precios-info').textContent = '';
    $('precios-form').hidden = !evento;
    if (!evento) {
        $('precios-tabla').append(crear('p', { clase: 'vacio', texto: 'Elija un evento para ver sus tarifas.' }));
        return;
    }
    $('precios-info').textContent = `${nombreEscenario(evento.idEscenario)} · ${fecha(evento.fecha)}, `
        + `${hora(evento.horaInicio)} a ${hora(evento.horaFin)}`;

    // Solo se ofrecen las localidades del escenario del evento
    const localidades = datos.localidades.filter(l => l.idEscenario === evento.idEscenario);
    llenarSelect($('precios-localidad'), localidades.map(l => [l.idLocalidad, l.nombre]), 'Elija una localidad');

    try {
        const tarifas = await api.get(`/api/eventos/${evento.idEvento}/precios`);
        pintarTabla($('precios-tabla'), [
            { titulo: 'Localidad', valor: t => t.localidad },
            { titulo: 'Precio', valor: t => pesos(t.precio), numero: true },
        ], tarifas, {
            vacio: 'Este evento todavía no tiene precios: no aparece en el catálogo.',
            acciones: t => [
                boton('Cambiar precio', () => empezarCambioPrecio(t)),
                boton('Quitar', () => quitarPrecio(t), 'boton chico peligro'),
            ],
            marcada: t => t.idLocalidad === localidadEditada,
        });
    } catch (e) {
        mostrarError($('precios-mensaje'), e.message);
    }
}

function empezarCambioPrecio(tarifa) {
    localidadEditada = tarifa.idLocalidad;
    limpiar($('precios-mensaje'));
    $('precios-titulo-form').textContent = `Cambiar precio de ${tarifa.localidad}`;
    $('precios-localidad').value = tarifa.idLocalidad;
    $('precios-localidad').disabled = true;
    $('precios-valor').value = tarifa.precio;
    $('precios-cancelar').hidden = false;
    $('precios-valor').focus();
}

function terminarCambioPrecio() {
    localidadEditada = null;
    $('precios-titulo-form').textContent = 'Asignar precio a una localidad';
    $('precios-localidad').disabled = false;
    $('precios-localidad').value = '';
    $('precios-valor').value = '';
    $('precios-cancelar').hidden = true;
}

async function guardarPrecio(evento) {
    evento.preventDefault();
    const idEvento = Number($('precios-evento').value);
    const datosPrecio = leerFormulario($('precios-form'));
    try {
        if (localidadEditada === null) {
            const tarifa = await api.post(`/api/eventos/${idEvento}/precios`, datosPrecio);
            mostrarExito($('precios-mensaje'), `Precio asignado a ${tarifa.localidad}: ${pesos(tarifa.precio)}.`);
        } else {
            const tarifa = await api.put(`/api/eventos/${idEvento}/precios/${localidadEditada}`,
                { precio: datosPrecio.precio });
            mostrarExito($('precios-mensaje'), `Nuevo precio de ${tarifa.localidad}: ${pesos(tarifa.precio)}. `
                + 'El cambio quedó registrado en el historial de precios.');
        }
        await cargarPrecios();
    } catch (e) {
        mostrarError($('precios-mensaje'), e.message);
    }
}

async function quitarPrecio(tarifa) {
    if (!confirm(`¿Quitar el precio de ${tarifa.localidad}? La localidad dejará de venderse en este evento.`)) {
        return;
    }
    try {
        await api.eliminar(`/api/eventos/${tarifa.idEvento}/precios/${tarifa.idLocalidad}`);
        mostrarExito($('precios-mensaje'), `Se quitó el precio de ${tarifa.localidad}.`);
        await cargarPrecios();
    } catch (e) {
        mostrarError($('precios-mensaje'), e.message);
    }
}

// ---------- Anulación de compras (F2)

async function buscarCompra(evento) {
    evento.preventDefault();
    limpiar($('compras-mensaje'), $('compras-detalle'));
    const idCompra = $('compras-id').value.trim();
    if (!idCompra) {
        mostrarError($('compras-mensaje'), 'Escriba el número de la compra.');
        return;
    }
    try {
        pintarCompra(await api.get(`/api/compras/${idCompra}`));
    } catch (e) {
        mostrarError($('compras-mensaje'), e.message);
    }
}

function pintarCompra(compra) {
    const detalle = $('compras-detalle');
    limpiar(detalle);
    const vigentes = compra.boletas.filter(b => b.estado === 'Vendida').length;
    pintarDetalle(detalle, [
        ['Compra', '#' + compra.idCompra],
        ['Cliente', compra.cliente],
        ['Fecha', fechaHora(compra.fechaCompra)],
        ['Método de pago', compra.metodoPago],
        ['Total pagado', pesos(compra.total)],
        ['Boletas vigentes', `${vigentes} de ${compra.boletas.length}`],
    ]);
    const tabla = crear('div');
    pintarTabla(tabla, [
        { titulo: 'Boleta', valor: b => b.idBoleta, numero: true },
        { titulo: 'Evento', valor: b => b.evento },
        { titulo: 'Fecha del evento', valor: b => fecha(b.fechaEvento) },
        { titulo: 'Localidad', valor: b => b.localidad },
        { titulo: 'Precio pagado', valor: b => pesos(b.precioPagado), numero: true },
        { titulo: 'Estado', valor: b => etiquetaEstado(b.estado) },
    ], compra.boletas, { vacio: 'La compra no tiene boletas.' });
    detalle.append(tabla);

    const anular = boton(vigentes > 0 ? 'Anular compra' : 'La compra ya está anulada',
        () => anularCompra(compra), 'boton peligro');
    anular.disabled = vigentes === 0;
    detalle.append(crear('div', { clase: 'acciones' }, anular));
}

async function anularCompra(compra) {
    if (!confirm(`¿Anular la compra #${compra.idCompra} de ${compra.cliente}? Sus boletas pasarán a Cancelada.`)) {
        return;
    }
    try {
        const anulada = await api.patch(`/api/compras/${compra.idCompra}/anular`);
        pintarCompra(anulada);
        mostrarExito($('compras-mensaje'),
            `Compra #${anulada.idCompra} anulada: sus boletas quedaron canceladas y los cupos libres.`);
    } catch (e) {
        mostrarError($('compras-mensaje'), e.message);
    }
}

// ---------- Historial de precios y aforos (RNF-06)

async function cargarHistorial() {
    const idEvento = $('historial-evento').value;
    limpiar($('historial-mensaje'));
    try {
        const cambios = await api.get(idEvento ? `/api/historial-precios?idEvento=${idEvento}` : '/api/historial-precios');
        // Un cambio de PRECIO se muestra en pesos; uno de AFORO_MAXIMO en número de cupos
        const valor = (cambio, monto) => cambio.campoModificado === 'PRECIO' ? pesos(monto) : numero(monto) + ' cupos';
        pintarTabla($('historial-tabla'), [
            { titulo: 'Fecha', valor: c => fechaHora(c.fechaCambio) },
            { titulo: 'Campo', valor: c => c.campoModificado === 'PRECIO' ? 'Precio' : 'Aforo máximo' },
            { titulo: 'Evento', valor: c => c.evento || '(aforo de la localidad)' },
            { titulo: 'Localidad', valor: c => c.localidad },
            { titulo: 'Valor anterior', valor: c => valor(c, c.valorAnterior), numero: true },
            { titulo: 'Valor nuevo', valor: c => valor(c, c.valorNuevo), numero: true },
            { titulo: 'Usuario', valor: c => c.usuario },
        ], cambios, { vacio: 'No hay cambios registrados.' });
    } catch (e) {
        mostrarError($('historial-mensaje'), e.message);
    }
}
