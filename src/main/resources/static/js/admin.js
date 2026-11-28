import { api } from './api.js';
import {
    alActualizar, datos, nombreEscenario, nombreTipo, opcionesEscenarios, opcionesEventos, opcionesTipos,
    recargarDatos,
} from './datos.js';
import { armarCamposPersona, llenarCamposPersona, nombreGenero, textoCiudad } from './persona.js';
import {
    $, boton, crear, edad, etiquetaEstado, fecha, fechaHora, hora, leerFormulario, limpiar, llenarSelect, mostrarError,
    mostrarExito, mostrarVista, normalizar, numero, pesos, pintarDetalle, pintarTabla, textoEdadMinima,
} from './util.js';

// Módulo del administrador. El CRUD de las cinco entidades usa el mismo formulario y la
// misma tabla: cada entidad se describe abajo con su URL, sus campos y sus columnas.
// alEditar: 'bloquear' para los campos que la API no cambia en el PUT. inicial: valor de un
// registro nuevo. Los clientes usan el formulario de persona de persona.js.

const ENTIDADES = {
    eventos: {
        nombre: 'evento', titulo: 'Eventos', tabla: 'evento', url: '/api/eventos', id: 'idEvento',
        filtros: [
            { clave: 'idEscenario', etiqueta: 'Escenario', opciones: opcionesEscenarios },
            { clave: 'idTipo', etiqueta: 'Tipo', opciones: opcionesTipos },
        ],
        campos: [
            { clave: 'idEscenario', etiqueta: 'Escenario', tipo: 'select', opciones: opcionesEscenarios, alEditar: 'bloquear' },
            { clave: 'idTipoEvento', etiqueta: 'Tipo de evento', tipo: 'select', opciones: opcionesTipos },
            { clave: 'nombre', etiqueta: 'Nombre' },
            { clave: 'fecha', etiqueta: 'Fecha', tipo: 'date' },
            { clave: 'horaInicio', etiqueta: 'Hora de inicio', tipo: 'time' },
            { clave: 'horaFin', etiqueta: 'Hora de fin', tipo: 'time' },
            { clave: 'edadMinima', etiqueta: 'Edad mínima (0 = todo público)', tipo: 'number', inicial: '0' },
        ],
        columnas: [
            { titulo: 'Id', valor: e => e.idEvento, numero: true },
            { titulo: 'Evento', valor: e => e.nombre },
            { titulo: 'Tipo', valor: e => nombreTipo(e.idTipoEvento) },
            { titulo: 'Escenario', valor: e => nombreEscenario(e.idEscenario) },
            { titulo: 'Fecha', valor: e => fecha(e.fecha) },
            { titulo: 'Horario', valor: e => `${hora(e.horaInicio)} a ${hora(e.horaFin)}` },
            { titulo: 'Edad', valor: e => textoEdadMinima(e.edadMinima) },
        ],
        describir: e => e.nombre,
    },
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
            { clave: 'idEscenario', etiqueta: 'Escenario', tipo: 'select', opciones: opcionesEscenarios, alEditar: 'bloquear' },
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
    clientes: {
        nombre: 'cliente', titulo: 'Clientes', tabla: 'cliente', url: '/api/clientes', id: 'idCliente',
        formularioPersona: true,
        campos: [],
        columnas: [
            { titulo: 'Id', valor: c => c.idCliente, numero: true },
            { titulo: 'Documento', valor: c => `${c.tipoDocumento} ${c.numeroDocumento}` },
            { titulo: 'Nombre', valor: c => `${c.nombres} ${c.apellidos}` },
            { titulo: 'Edad', valor: c => edad(c.fechaNacimiento), numero: true },
            { titulo: 'Género', valor: c => nombreGenero(c.genero) },
            { titulo: 'Correo', valor: c => c.correo },
            { titulo: 'Celular', valor: c => `${c.indicativo} ${c.celular}` },
            { titulo: 'Residencia', valor: c => textoCiudad(c.idCiudad) },
            { titulo: 'Registro', valor: c => fecha(c.fechaRegistro) },
        ],
        describir: c => `${c.nombres} ${c.apellidos}`,
    },
};

const articulo = entidad => entidad.femenino ? 'la' : 'el';

let seccion = 'eventos';
let editando = null;            // id del registro que se está editando (null = creando)
let filas = [];                 // registros de la entidad que se muestran en la tabla
let localidadEditada = null;    // localidad cuyo precio se está cambiando

export function iniciarAdmin() {
    document.querySelectorAll('#admin-menu button').forEach(b =>
        b.addEventListener('click', () => mostrarSeccion(b.dataset.seccion)));
    $('crud-form').addEventListener('submit', guardar);
    $('crud-cancelar').addEventListener('click', () => {
        limpiar($('crud-mensaje'));
        armarFormulario();
        pintarFilas();
    });
    $('crud-buscar').addEventListener('input', pintarFilas);
    $('precios-evento').addEventListener('change', cargarPrecios);
    $('precios-form').addEventListener('submit', guardarPrecio);
    $('precios-cancelar').addEventListener('click', () => {
        limpiar($('precios-mensaje'));
        terminarCambioPrecio();
        cargarPrecios();
    });
    $('ventas-evento').addEventListener('change', cargarVentas);
    $('ventas-buscar').addEventListener('submit', evento => {
        evento.preventDefault();
        verCompra($('ventas-id').value.trim());
    });
    $('historial-evento').addEventListener('change', cargarHistorial);

    // Cuando cambian las listas se actualizan los selectores de esta pantalla
    alActualizar(() => {
        llenarSelect($('precios-evento'), opcionesEventos(), 'Elija un evento');
        llenarSelect($('ventas-evento'), opcionesEventos(), 'Todas las ventas recientes');
        llenarSelect($('historial-evento'), opcionesEventos(), 'Todos los cambios');
    });
}

// Se llama al iniciar sesión como administrador
export function mostrarAdmin() {
    mostrarVista('admin');
    mostrarSeccion(seccion);
}

function mostrarSeccion(nueva) {
    if (nueva !== seccion) {
        limpiar($('crud-mensaje'), $('precios-mensaje'), $('ventas-mensaje'), $('ventas-detalle'), $('historial-mensaje'));
        $('crud-buscar').value = '';
    }
    seccion = nueva;
    document.querySelectorAll('#admin-menu button').forEach(b =>
        b.classList.toggle('activo', b.dataset.seccion === seccion));

    const esCrud = seccion in ENTIDADES;
    $('admin-crud').hidden = !esCrud;
    $('admin-precios').hidden = seccion !== 'precios';
    $('admin-ventas').hidden = seccion !== 'ventas';
    $('admin-historial').hidden = seccion !== 'historial';

    if (esCrud) {
        armarFiltros();
        armarFormulario();
        cargarTabla();
    } else if (seccion === 'precios') {
        cargarPrecios();
    } else if (seccion === 'ventas') {
        cargarVentas();
    } else if (seccion === 'historial') {
        cargarHistorial();
    }
}

// ---------- CRUD genérico

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
    if (entidad.formularioPersona) {
        // Al editar no se cambian el documento ni la fecha de nacimiento, ni se ve la contraseña
        armarCamposPersona(campos, { identidadFija: Boolean(registro), contrasena: registro ? null : 'inicial',
            unaColumna: true });
        if (registro) {
            llenarCamposPersona(campos, registro);
        }
        return;
    }
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
            control.disabled = campo.alEditar === 'bloquear';
        } else if (campo.inicial !== undefined) {
            control.value = campo.inicial;
        }
        const etiqueta = crear('label', { clase: 'campo' }, crear('span', { texto: campo.etiqueta }), control);
        if (registro && campo.alEditar === 'bloquear') {
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
    try {
        filas = await api.get(consulta ? `${entidad.url}?${consulta}` : entidad.url);
        pintarFilas();
    } catch (e) {
        mostrarError($('crud-mensaje'), e.message);
    }
}

// La búsqueda se hace en la página sobre los registros ya cargados
function pintarFilas() {
    const entidad = ENTIDADES[seccion];
    const texto = normalizar($('crud-buscar').value.trim());
    const visibles = texto === '' ? filas
        : filas.filter(f => entidad.columnas.some(c => normalizar(c.valor(f)).includes(texto)));
    $('crud-cuenta').textContent = visibles.length === filas.length
        ? `${filas.length} registros`
        : `${visibles.length} de ${filas.length} registros`;
    pintarTabla($('crud-tabla'), entidad.columnas, visibles, {
        vacio: `No hay ${entidad.titulo.toLowerCase()} con esos filtros.`,
        acciones: fila => [
            boton('Editar', () => {
                limpiar($('crud-mensaje'));
                armarFormulario(fila);
                pintarFilas();
                $('crud-form').scrollIntoView({ block: 'nearest' });
            }),
            boton('Eliminar', () => eliminar(fila), 'boton chico peligro'),
        ],
        marcada: fila => editando !== null && fila[entidad.id] === editando,
    });
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
    if (localidadEditada === null) {
        llenarSelect($('precios-localidad'), localidades.map(l => [l.idLocalidad, l.nombre]), 'Elija una localidad');
    }

    try {
        const [tarifas, cupos] = await Promise.all([
            api.get(`/api/eventos/${evento.idEvento}/precios`),
            api.get(`/api/eventos/${evento.idEvento}/disponibilidad`),
        ]);
        const cupo = id => cupos.find(c => c.idLocalidad === id) || {};
        pintarTabla($('precios-tabla'), [
            { titulo: 'Localidad', valor: t => t.localidad },
            { titulo: 'Precio', valor: t => pesos(t.precio), numero: true },
            { titulo: 'Aforo', valor: t => numero(cupo(t.idLocalidad).aforoMaximo), numero: true },
            { titulo: 'Vendidas', valor: t => numero(cupo(t.idLocalidad).boletasVendidas), numero: true },
            { titulo: 'Disponibles', valor: t => numero(cupo(t.idLocalidad).boletasDisponibles), numero: true },
        ], tarifas, {
            vacio: 'Este evento todavía no tiene precios: no aparece en la cartelera.',
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
        terminarCambioPrecio();
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

// ---------- Ventas y anulación de compras (F2)

async function cargarVentas() {
    const idEvento = $('ventas-evento').value;
    limpiar($('ventas-mensaje'));
    try {
        const ventas = await api.get(idEvento ? `/api/compras?idEvento=${idEvento}` : '/api/compras');
        pintarTabla($('ventas-tabla'), [
            { titulo: 'Compra', valor: c => '#' + c.idCompra },
            { titulo: 'Fecha', valor: c => fechaHora(c.fechaCompra) },
            { titulo: 'Cliente', valor: c => c.cliente },
            { titulo: 'Evento', valor: c => c.evento },
            { titulo: 'Localidad', valor: c => c.localidades },
            { titulo: 'Boletas', valor: c => `${c.boletasVigentes} de ${c.boletas}`, numero: true },
            { titulo: 'Total', valor: c => pesos(c.total), numero: true },
            { titulo: 'Estado', valor: c => etiquetaEstado(c.estado) },
        ], ventas, {
            vacio: 'No hay ventas para este evento.',
            acciones: c => [boton('Ver', () => verCompra(c.idCompra))],
        });
    } catch (e) {
        mostrarError($('ventas-mensaje'), e.message);
    }
}

async function verCompra(idCompra) {
    limpiar($('ventas-mensaje'), $('ventas-detalle'));
    if (!idCompra) {
        mostrarError($('ventas-mensaje'), 'Escriba el número de la compra.');
        return;
    }
    try {
        pintarCompra(await api.get(`/api/compras/${idCompra}`));
        $('ventas-detalle').scrollIntoView({ block: 'nearest' });
    } catch (e) {
        mostrarError($('ventas-mensaje'), e.message);
    }
}

function pintarCompra({ compra, boletas }) {
    const detalle = $('ventas-detalle');
    limpiar(detalle);
    const caja = crear('div', { clase: 'tarjeta' }, crear('h3', { texto: `Compra #${compra.idCompra}` }));
    pintarDetalle(caja, [
        ['Cliente', compra.cliente],
        ['Fecha de compra', fechaHora(compra.fechaCompra)],
        ['Método de pago', compra.metodoPago],
        ['Evento', compra.evento],
        ['Fecha del evento', `${fecha(compra.fechaEvento)}, ${hora(compra.horaInicio)}`],
        ['Total pagado', pesos(compra.total)],
        ['Boletas vigentes', `${compra.boletasVigentes} de ${compra.boletas}`],
        ['Estado', etiquetaEstado(compra.estado)],
    ]);
    const tabla = crear('div');
    pintarTabla(tabla, [
        { titulo: 'Boleta', valor: b => '#' + b.idBoleta },
        { titulo: 'Localidad', valor: b => b.localidad },
        { titulo: 'Precio pagado', valor: b => pesos(b.precioPagado), numero: true },
        { titulo: 'Estado', valor: b => etiquetaEstado(b.estado) },
    ], boletas);
    caja.append(tabla);

    const anular = boton(compra.boletasVigentes > 0 ? 'Anular compra' : 'La compra ya está anulada',
        () => anularCompra(compra), 'boton peligro');
    anular.disabled = compra.boletasVigentes === 0;
    caja.append(crear('p', { clase: 'ayuda', texto: 'Al anular, las boletas pasan a Cancelada y sus cupos quedan '
        + 'libres. No se borra ningún registro y no se anulan compras de eventos que ya se realizaron.' }),
        crear('div', { clase: 'acciones' }, anular));
    detalle.append(caja);
}

async function anularCompra(compra) {
    if (!confirm(`¿Anular la compra #${compra.idCompra} de ${compra.cliente}? Sus boletas pasarán a Cancelada.`)) {
        return;
    }
    try {
        const anulada = await api.patch(`/api/compras/${compra.idCompra}/anular`);
        pintarCompra(anulada);
        await cargarVentas();
        mostrarExito($('ventas-mensaje'),
            `Compra #${compra.idCompra} anulada: sus boletas quedaron canceladas y los cupos libres.`);
    } catch (e) {
        mostrarError($('ventas-mensaje'), e.message);
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
