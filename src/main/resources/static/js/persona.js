import { api } from './api.js';
import { crear, llenarSelect } from './util.js';

// Campos de un cliente que comparten el registro, Mi cuenta y el formulario de clientes del
// administrador. País, departamento y ciudad son selectores encadenados y solo se envía la
// ciudad (idCiudad): el departamento y el país salen de ella en la base (ciudad ->
// departamento -> pais), como en las tablas.

export const TIPOS_DOCUMENTO = [
    ['CC', 'Cédula de ciudadanía'],
    ['TI', 'Tarjeta de identidad'],
    ['CE', 'Cédula de extranjería'],
    ['PA', 'Pasaporte'],
    ['PPT', 'Permiso por protección temporal'],
];

export const GENEROS = [['FEMENINO', 'Femenino'], ['MASCULINO', 'Masculino'], ['OTRO', 'Otro']];

let ubicaciones = [];          // filas de v_ubicaciones: ciudad con su departamento, país e indicativo

export async function cargarUbicaciones() {
    if (ubicaciones.length === 0) {
        ubicaciones = await api.get('/api/ubicaciones');
    }
    return ubicaciones;
}

// "Bogotá", "Chía, Cundinamarca" o "Caracas, Venezuela"
export function textoCiudad(idCiudad) {
    const u = ubicaciones.find(x => x.idCiudad === idCiudad);
    if (!u) {
        return '';
    }
    if (u.departamento.startsWith(u.ciudad)) {
        return u.ciudad;
    }
    return u.pais === 'Colombia' ? `${u.ciudad}, ${u.departamento}` : `${u.ciudad}, ${u.pais}`;
}

export const nombreTipoDocumento = tipo => (TIPOS_DOCUMENTO.find(t => t[0] === tipo) || [tipo, tipo])[1];

export const nombreGenero = genero => (GENEROS.find(g => g[0] === genero) || [genero, genero])[1];

// Lista sin repetidos, ordenada por nombre, con Colombia primero
function unicos(filas, id, nombre) {
    const mapa = new Map(filas.map(f => [f[id], f[nombre]]));
    return [...mapa.entries()].sort((a, b) =>
        (a[1] === 'Colombia' ? -1 : b[1] === 'Colombia' ? 1 : a[1].localeCompare(b[1])));
}

// "+57 Colombia", "+1 Canadá, Estados Unidos"...
function opcionesIndicativo() {
    const paises = new Map();
    for (const u of ubicaciones) {
        paises.set(u.indicativo, new Set([...(paises.get(u.indicativo) || []), u.pais]));
    }
    return [...paises.entries()]
        .map(([indicativo, nombres]) => [indicativo, `${indicativo} ${[...nombres].sort().join(', ')}`])
        .sort((a, b) => (a[0] === '+57' ? -1 : b[0] === '+57' ? 1 : a[1].localeCompare(b[1])));
}

function campo(etiqueta, control, clase = 'campo') {
    return crear('label', { clase }, crear('span', { texto: etiqueta }), control);
}

function seccion(titulo, ...campos) {
    return crear('fieldset', { clase: 'seccion-persona' }, crear('legend', { texto: titulo }),
        crear('div', { clase: 'formulario rejilla-persona' }, ...campos));
}

// Arma los campos dentro del contenedor.
// opciones.identidadFija: bloquea el documento y la fecha de nacimiento (al editar)
// opciones.contrasena: 'nueva' (contraseña y repetir) o 'inicial' (la pone el administrador)
// opciones.unaColumna: para paneles angostos
export function armarCamposPersona(contenedor, opciones = {}) {
    const tipo = crear('select', { name: 'tipoDocumento' });
    llenarSelect(tipo, TIPOS_DOCUMENTO.map(([v, t]) => [v, `${v} · ${t}`]));
    const genero = crear('select', { name: 'genero' });
    llenarSelect(genero, GENEROS, 'Elija una opción');
    const indicativo = crear('select', { name: 'indicativo', clase: 'indicativo' });
    llenarSelect(indicativo, opcionesIndicativo());
    indicativo.value = '+57';
    const celular = crear('input', { name: 'celular', type: 'tel', autocomplete: 'tel-national' });

    const pais = crear('select', { 'data-campo': 'pais' });
    const departamento = crear('select', { 'data-campo': 'departamento' });
    const ciudad = crear('select', { name: 'idCiudad', 'data-numero': '' });
    llenarSelect(pais, unicos(ubicaciones, 'idPais', 'pais'));
    pais.addEventListener('change', () => {
        llenarDepartamentos(contenedor);
        // El indicativo sigue al país, aunque se puede cambiar
        const u = ubicaciones.find(x => x.idPais === Number(pais.value));
        if (u) {
            indicativo.value = u.indicativo;
        }
    });
    departamento.addEventListener('change', () => llenarCiudades(contenedor));

    const documento = crear('input', { name: 'numeroDocumento', autocomplete: 'off' });
    const nacimiento = crear('input', { name: 'fechaNacimiento', type: 'date' });
    if (opciones.identidadFija) {
        tipo.disabled = true;
        documento.disabled = true;
        nacimiento.disabled = true;
    }

    const secciones = [
        seccion('Identificación',
            campo('Tipo de documento', tipo),
            campo('Número de documento', documento),
            campo('Nombres', crear('input', { name: 'nombres', autocomplete: 'given-name' })),
            campo('Apellidos', crear('input', { name: 'apellidos', autocomplete: 'family-name' })),
            campo('Fecha de nacimiento', nacimiento),
            campo('Género', genero)),
        seccion('Contacto',
            campo('Correo', crear('input', { name: 'correo', type: 'email', autocomplete: 'email' }), 'campo completo'),
            crear('div', { clase: 'campo completo' }, crear('span', { texto: 'Celular' }),
                crear('div', { clase: 'grupo-celular' }, indicativo, celular))),
        seccion('Residencia',
            campo('País', pais),
            campo('Departamento o estado', departamento),
            campo('Ciudad o municipio', ciudad),
            campo('Dirección', crear('input', { name: 'direccion', autocomplete: 'street-address' }))),
    ];
    if (opciones.contrasena === 'nueva') {
        secciones.push(seccion('Contraseña',
            campo('Contraseña', crear('input', { name: 'contrasena', type: 'password', autocomplete: 'new-password' })),
            campo('Repetir contraseña', crear('input', { 'data-campo': 'repetir', type: 'password',
                autocomplete: 'new-password' }))));
    } else if (opciones.contrasena === 'inicial') {
        secciones.push(seccion('Acceso',
            campo('Contraseña inicial', crear('input', { name: 'contrasena', type: 'password',
                autocomplete: 'new-password' }), 'campo completo')));
    }
    contenedor.replaceChildren(...secciones);
    contenedor.classList.toggle('una-columna-persona', Boolean(opciones.unaColumna));

    // Un formulario nuevo empieza en Bogotá, de donde son la mayoría de los clientes
    const bogota = ubicaciones.find(u => u.ciudad === 'Bogotá');
    if (bogota) {
        elegirCiudad(contenedor, bogota.idCiudad);
    }
}

function llenarDepartamentos(contenedor) {
    const pais = Number(contenedor.querySelector('[data-campo=pais]').value);
    llenarSelect(contenedor.querySelector('[data-campo=departamento]'),
        unicos(ubicaciones.filter(u => u.idPais === pais), 'idDepartamento', 'departamento'), 'Elija una opción');
    llenarCiudades(contenedor);
}

function llenarCiudades(contenedor) {
    const departamento = Number(contenedor.querySelector('[data-campo=departamento]').value);
    llenarSelect(contenedor.querySelector('[name=idCiudad]'),
        unicos(ubicaciones.filter(u => u.idDepartamento === departamento), 'idCiudad', 'ciudad'), 'Elija una opción');
}

// Pone los tres selectores en la ciudad indicada
function elegirCiudad(contenedor, idCiudad) {
    const u = ubicaciones.find(x => x.idCiudad === idCiudad);
    if (!u) {
        return;
    }
    contenedor.querySelector('[data-campo=pais]').value = u.idPais;
    llenarDepartamentos(contenedor);
    contenedor.querySelector('[data-campo=departamento]').value = u.idDepartamento;
    llenarCiudades(contenedor);
    contenedor.querySelector('[name=idCiudad]').value = u.idCiudad;
}

// Llena los campos con los datos de un cliente que devolvió la API
export function llenarCamposPersona(contenedor, persona) {
    for (const clave of ['tipoDocumento', 'numeroDocumento', 'nombres', 'apellidos', 'fechaNacimiento', 'genero',
                         'correo', 'indicativo', 'celular', 'direccion']) {
        const control = contenedor.querySelector(`[name=${clave}]`);
        if (control) {
            control.value = persona[clave] ?? '';
        }
    }
    elegirCiudad(contenedor, persona.idCiudad);
}

// Valor del campo «Repetir contraseña» (no se envía a la API)
export function contrasenaRepetida(contenedor) {
    const campoRepetir = contenedor.querySelector('[data-campo=repetir]');
    return campoRepetir ? campoRepetir.value : null;
}
