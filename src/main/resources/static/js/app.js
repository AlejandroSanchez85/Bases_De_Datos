import { recargarDatos } from './datos.js';
import { cargarCatalogo, iniciarCompra, refrescarBoletas } from './compra.js';
import { iniciarAdmin, mostrarAdmin } from './admin.js';
import { iniciarReportes, mostrarReportes } from './reportes.js';
import { $, limpiar, mostrarError } from './util.js';

// Punto de entrada de la página: menú principal y carga inicial de datos

// Al abrir una vista se vuelven a pedir sus datos, para ver siempre lo que hay en la base
const ABRIR = {
    catalogo: cargarCatalogo,
    boletas: refrescarBoletas,
    admin: mostrarAdmin,
    reportes: mostrarReportes,
};

function cambiarVista(vista) {
    document.querySelectorAll('.menu button').forEach(b => b.classList.toggle('activo', b.dataset.vista === vista));
    document.querySelectorAll('main > .vista').forEach(s => {
        s.hidden = s.id !== 'vista-' + vista;
    });
    ABRIR[vista]();
}

async function iniciar() {
    iniciarCompra();
    iniciarAdmin();
    iniciarReportes();
    document.querySelectorAll('.menu button').forEach(b =>
        b.addEventListener('click', () => cambiarVista(b.dataset.vista)));

    try {
        await recargarDatos();
        limpiar($('mensaje-global'));
    } catch (e) {
        mostrarError($('mensaje-global'), e.message);
    }
    cambiarVista('catalogo');
}

iniciar();
