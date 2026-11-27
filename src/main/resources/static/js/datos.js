import { api } from './api.js';
import { fecha } from './util.js';

// Listas que usan los selectores de Administración. Se cargan al iniciar sesión
// como administrador y se vuelven a pedir después de cada cambio.
export const datos = { escenarios: [], tipos: [], eventos: [], clientes: [], localidades: [] };

const avisos = [];

// Registra una función que se ejecuta cada vez que se recargan las listas
export function alActualizar(funcion) {
    avisos.push(funcion);
}

export async function recargarDatos() {
    const [escenarios, tipos, eventos, clientes, localidades] = await Promise.all([
        api.get('/api/escenarios'),
        api.get('/api/tipos-evento'),
        api.get('/api/eventos'),
        api.get('/api/clientes'),
        api.get('/api/localidades'),
    ]);
    Object.assign(datos, { escenarios, tipos, eventos, clientes, localidades });
    avisos.forEach(funcion => funcion());
}

export function nombreEscenario(id) {
    const escenario = datos.escenarios.find(e => e.idEscenario === id);
    return escenario ? escenario.nombre : id;
}

export function nombreTipo(id) {
    const tipo = datos.tipos.find(t => t.idTipoEvento === id);
    return tipo ? tipo.nombre : id;
}

export const opcionesEscenarios = () => datos.escenarios.map(e => [e.idEscenario, e.nombre]);

export const opcionesTipos = () => datos.tipos.map(t => [t.idTipoEvento, t.nombre]);

export const opcionesEventos = () => datos.eventos.map(e => [e.idEvento, `${e.nombre} · ${fecha(e.fecha)}`]);
