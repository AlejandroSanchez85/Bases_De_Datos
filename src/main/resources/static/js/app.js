import { api } from './api.js';
import { iniciarIngreso, mostrarIngreso } from './ingreso.js';
import { abrirVistaCliente, iniciarCliente } from './cliente.js';
import { iniciarAdmin, mostrarAdmin } from './admin.js';
import { iniciarReportes, mostrarReportes } from './reportes.js';
import { recargarDatos } from './datos.js';
import { $, crear, limpiar, mostrarError } from './util.js';

// Punto de entrada de la página: sesión, menú según el rol y cambio de vistas.
// La API también revisa el rol en cada ruta: ocultar un menú no es la única protección.

const MENU_CLIENTE = [['cartelera', 'Cartelera'], ['mis-compras', 'Mis compras'], ['cuenta', 'Mi cuenta']];
const ROLES = {
    CLIENTE: { nombre: 'Cliente', clase: 'rol-cliente' },
    ADMINISTRADOR: { nombre: 'Administrador', clase: 'rol-administrador' },
    ANALISTA: { nombre: 'Analista de ventas', clase: 'rol-analista' },
};

let sesion = null;

async function entrar(datosSesion) {
    sesion = datosSesion;
    $('pantalla-ingreso').hidden = true;
    $('aplicacion').hidden = false;
    limpiar($('mensaje-global'));
    $('usuario-nombre').textContent = sesion.nombre;
    $('usuario-rol').textContent = ROLES[sesion.rol].nombre;
    $('usuario-rol').className = 'etiqueta ' + ROLES[sesion.rol].clase;

    // Solo el cliente tiene menú superior; administrador y analista navegan con pestañas internas
    const menu = $('menu');
    menu.replaceChildren();
    if (sesion.rol === 'CLIENTE') {
        for (const [vista, texto] of MENU_CLIENTE) {
            menu.append(crear('button', { type: 'button', 'data-vista': vista, texto,
                onclick: () => abrirVistaCliente(vista) }));
        }
        abrirVistaCliente('cartelera');
    } else if (sesion.rol === 'ADMINISTRADOR') {
        try {
            await recargarDatos();
        } catch (e) {
            mostrarError($('mensaje-global'), e.message);
        }
        mostrarAdmin();
    } else {
        mostrarReportes();
    }
}

async function salir() {
    try {
        await api.salir();
    } catch (e) {
        // si la sesión ya había vencido no hay nada que cerrar
    }
    sesion = null;
    await api.sesion().catch(() => null);      // trae un token CSRF nuevo para el próximo ingreso
    mostrarIngreso();
}

async function iniciar() {
    iniciarIngreso(entrar);
    iniciarCliente(async mensaje => {          // se llama cuando el cliente elimina su cuenta
        sesion = null;
        await api.sesion().catch(() => null);
        mostrarIngreso(mensaje);
    });
    iniciarAdmin();
    iniciarReportes();
    $('salir').addEventListener('click', salir);
    window.addEventListener('sesion-vencida', () => {
        if (sesion !== null) {
            sesion = null;
            mostrarIngreso('Su sesión venció. Ingrese de nuevo.');
        }
    });

    try {
        await entrar(await api.sesion());        // ya había una sesión abierta
    } catch (e) {
        mostrarIngreso();
    }
}

iniciar();
