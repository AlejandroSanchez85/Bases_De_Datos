// Funciones de apoyo para dar formato y armar tablas, formularios y mensajes.
// Los textos que vienen de la API se ponen con textContent, nunca como HTML.

export const $ = id => document.getElementById(id);

const formatoPesos = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
const formatoFecha = new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'short', year: 'numeric' });

export function pesos(valor) {
    return valor == null ? '' : formatoPesos.format(valor);
}

export function numero(valor) {
    return valor == null ? '' : Number(valor).toLocaleString('es-CO');
}

export function porcentaje(valor) {
    return valor == null ? '' : Number(valor).toLocaleString('es-CO', { maximumFractionDigits: 2 }) + ' %';
}

// '2026-12-12' -> '12 dic 2026'. Se arma como fecha local para que la zona horaria no cambie el día.
export function fecha(texto) {
    if (!texto) {
        return '';
    }
    const [anio, mes, dia] = texto.split('-').map(Number);
    return formatoFecha.format(new Date(anio, mes - 1, dia));
}

// '2026-12-12' -> 'sábado, 12 de diciembre de 2026'
const formatoLargo = new Intl.DateTimeFormat('es-CO', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });

export function fechaLarga(texto) {
    const [anio, mes, dia] = texto.split('-').map(Number);
    return formatoLargo.format(new Date(anio, mes - 1, dia));
}

// Cuadro con el día y el mes de una fecha, como en una boleta
export function cuadroDia(texto) {
    const [anio, mes, dia] = texto.split('-').map(Number);
    const nombreMes = new Intl.DateTimeFormat('es-CO', { month: 'short' }).format(new Date(anio, mes - 1, dia));
    return crear('div', { clase: 'dia' }, crear('strong', { texto: String(dia) }),
        crear('span', { texto: nombreMes.replace('.', '') }));
}

// Fecha de hoy (o dentro de unos días) como '2026-11-27', en hora local, para
// compararla con las fechas que devuelve la API
export function hoy(masDias = 0) {
    const d = new Date();
    d.setDate(d.getDate() + masDias);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

// Quita tildes y pasa a minúsculas, para buscar sin importar cómo se escribió
export function normalizar(texto) {
    return String(texto ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
}

// Muestra una sola vista de la página y marca su opción en el menú
export function mostrarVista(vista, opcionMenu = vista) {
    document.querySelectorAll('main > .vista').forEach(s => {
        s.hidden = s.id !== 'vista-' + vista;
    });
    document.querySelectorAll('#menu button').forEach(b => b.classList.toggle('activo', b.dataset.vista === opcionMenu));
    window.scrollTo(0, 0);
}

// '19:00:00' -> '19:00'
export function hora(texto) {
    return texto ? texto.slice(0, 5) : '';
}

// '2026-11-26T23:23:07.12' -> '26 nov 2026, 23:23'
export function fechaHora(texto) {
    return texto ? fecha(texto.slice(0, 10)) + ', ' + texto.slice(11, 16) : '';
}

// Crea un elemento. 'texto' va como textContent y las claves 'on...' como eventos.
export function crear(etiqueta, atributos = {}, ...hijos) {
    const elemento = document.createElement(etiqueta);
    for (const [clave, valor] of Object.entries(atributos)) {
        if (clave === 'texto') {
            elemento.textContent = valor;
        } else if (clave === 'clase') {
            elemento.className = valor;
        } else if (clave.startsWith('on')) {
            elemento.addEventListener(clave.slice(2), valor);
        } else {
            elemento.setAttribute(clave, valor);
        }
    }
    for (const hijo of hijos) {
        if (hijo != null) {
            elemento.append(hijo);
        }
    }
    return elemento;
}

export function boton(texto, alHacerClic, clase = 'boton chico secundario') {
    return crear('button', { type: 'button', clase, texto, onclick: alHacerClic });
}

export function etiquetaEstado(estado) {
    return crear('span', { clase: 'etiqueta ' + estado.toLowerCase(), texto: estado });
}

// Cupos disponibles; si no queda ninguno muestra «Agotada»
export function disponibles(cantidad) {
    return cantidad > 0 ? numero(cantidad) : crear('span', { clase: 'etiqueta agotada', texto: 'Agotada' });
}

export function barraOcupacion(valor) {
    const relleno = crear('span');
    relleno.style.width = Math.min(Number(valor), 100) + '%';
    return crear('div', {},
        document.createTextNode(porcentaje(valor)),
        crear('div', { clase: Number(valor) >= 100 ? 'barra llena' : 'barra' }, relleno));
}

// columnas: [{ titulo, valor: fila => texto o elemento, numero: true para alinear a la derecha }]
// opciones: vacio (texto si no hay filas), acciones (fila => botones), pie (fila <tr> de totales)
export function pintarTabla(contenedor, columnas, filas, opciones = {}) {
    contenedor.replaceChildren();
    if (filas.length === 0) {
        contenedor.append(crear('p', { clase: 'vacio', texto: opciones.vacio || 'No hay registros.' }));
        return;
    }

    const encabezado = crear('tr', {},
        ...columnas.map(c => crear('th', { clase: c.numero ? 'numero' : '', texto: c.titulo })));
    if (opciones.acciones) {
        encabezado.append(crear('th'));
    }

    const cuerpo = crear('tbody');
    for (const fila of filas) {
        const tr = crear('tr');
        for (const columna of columnas) {
            const valor = columna.valor(fila);
            const celda = crear('td', { clase: columna.numero ? 'numero' : '' });
            celda.append(valor instanceof Node ? valor : String(valor ?? ''));
            tr.append(celda);
        }
        if (opciones.acciones) {
            tr.append(crear('td', { clase: 'botones' }, ...opciones.acciones(fila)));
        }
        if (opciones.marcada && opciones.marcada(fila)) {
            tr.classList.add('seleccionada');
        }
        cuerpo.append(tr);
    }

    const tabla = crear('table', {}, crear('thead', {}, encabezado), cuerpo);
    if (opciones.pie) {
        tabla.append(crear('tfoot', {}, opciones.pie));
    }
    contenedor.append(crear('div', { clase: 'contenedor-tabla' }, tabla));
}

// Fila de totales para el pie de una tabla: celdas = [{ texto, numero }, ...]
export function filaTotales(celdas) {
    return crear('tr', {}, ...celdas.map(c => crear('td', { clase: c.numero ? 'numero' : '', texto: c.texto ?? '' })));
}

// Datos sueltos en cuadrícula: pares = [['Etiqueta', 'valor'], ...]
export function pintarDetalle(contenedor, pares) {
    contenedor.append(crear('div', { clase: 'detalle' },
        ...pares.map(([etiqueta, valor]) =>
            crear('div', {}, crear('span', { clase: 'rotulo', texto: etiqueta }),
                valor instanceof Node ? valor : String(valor ?? '')))));
}

export function limpiar(...contenedores) {
    contenedores.forEach(c => c.replaceChildren());
}

export function mostrarExito(contenedor, texto) {
    mostrarAlerta(contenedor, 'exito', null, texto);
}

// El mensaje de error es el que devolvió la API (o PostgreSQL a través de ella)
export function mostrarError(contenedor, texto) {
    mostrarAlerta(contenedor, 'error', 'No se pudo completar la operación', texto);
}

function mostrarAlerta(contenedor, tipo, titulo, texto) {
    const alerta = crear('div', { clase: 'alerta ' + tipo, role: tipo === 'error' ? 'alert' : 'status' });
    if (titulo) {
        alerta.append(crear('strong', { texto: titulo }));
    }
    alerta.append(texto);
    contenedor.replaceChildren(alerta);
    alerta.scrollIntoView({ block: 'nearest' });
}

// opciones = [[valor, texto], ...]; vacio agrega una primera opción sin valor.
// Si la opción que estaba elegida sigue existiendo, se conserva.
export function llenarSelect(select, opciones, vacio) {
    const elegida = select.value;
    select.replaceChildren();
    if (vacio !== undefined) {
        select.append(crear('option', { value: '', texto: vacio }));
    }
    for (const [valor, texto] of opciones) {
        select.append(crear('option', { value: valor, texto }));
    }
    if ([...select.options].some(o => o.value === elegida)) {
        select.value = elegida;
    }
}

// Lee los campos con name de un formulario. Los numéricos se envían como número
// y los vacíos como null, para que sea la API la que diga qué dato falta.
export function leerFormulario(formulario) {
    const datos = {};
    for (const campo of formulario.elements) {
        if (!campo.name) {
            continue;
        }
        const valor = campo.value.trim();
        if (valor === '') {
            datos[campo.name] = null;
        } else if (campo.type === 'number' || campo.dataset.numero !== undefined) {
            datos[campo.name] = Number(valor);
        } else {
            datos[campo.name] = valor;
        }
    }
    return datos;
}
