import { api } from './api.js';
import { $, leerFormulario, limpiar, mostrarError, mostrarExito } from './util.js';

// Pantalla de inicio de sesión y de registro de clientes

let alEntrar = null;

export function iniciarIngreso(funcionAlEntrar) {
    alEntrar = funcionAlEntrar;
    document.querySelectorAll('[data-acceso]').forEach(b =>
        b.addEventListener('click', () => mostrarPestana(b.dataset.acceso)));
    $('form-entrar').addEventListener('submit', entrar);
    $('form-registro').addEventListener('submit', registrarse);
    // Cuentas de prueba: llenan el formulario para no tener que escribirlas
    document.querySelectorAll('.cuentas-prueba button').forEach(b => b.addEventListener('click', () => {
        mostrarPestana('entrar');
        $('entrar-correo').value = b.dataset.correo;
        $('entrar-contrasena').value = b.dataset.clave;
        $('entrar-contrasena').focus();
    }));
}

export function mostrarIngreso(mensaje) {
    $('aplicacion').hidden = true;
    $('pantalla-ingreso').hidden = false;
    $('form-entrar').reset();
    $('form-registro').reset();
    limpiar($('entrar-mensaje'), $('registro-mensaje'));
    mostrarPestana('entrar');
    if (mensaje) {
        mostrarExito($('entrar-mensaje'), mensaje);
    }
    $('entrar-correo').focus();
}

function mostrarPestana(pestana) {
    document.querySelectorAll('[data-acceso]').forEach(b => b.classList.toggle('activo', b.dataset.acceso === pestana));
    $('form-entrar').hidden = pestana !== 'entrar';
    $('form-registro').hidden = pestana !== 'registro';
}

async function entrar(evento) {
    evento.preventDefault();
    const correo = $('entrar-correo').value.trim();
    const contrasena = $('entrar-contrasena').value;
    if (!correo || !contrasena) {
        mostrarError($('entrar-mensaje'), 'Escriba su correo y su contraseña.');
        return;
    }
    try {
        await alEntrar(await api.entrar(correo, contrasena));
    } catch (e) {
        mostrarError($('entrar-mensaje'), e.message);
    }
}

// Crea la cuenta en la API y después inicia sesión con ella
async function registrarse(evento) {
    evento.preventDefault();
    const datos = leerFormulario($('form-registro'));
    if (datos.contrasena !== $('registro-repetir').value) {
        mostrarError($('registro-mensaje'), 'Las dos contraseñas no coinciden.');
        return;
    }
    try {
        const cliente = await api.post('/api/auth/registro', datos);
        await alEntrar(await api.entrar(cliente.correo, datos.contrasena));
    } catch (e) {
        mostrarError($('registro-mensaje'), e.message);
    }
}
