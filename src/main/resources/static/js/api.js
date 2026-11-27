// Llamadas a la API REST. La página nunca toca la base de datos: todo pasa por aquí.
// Si la API responde con error, se lanza un Error con el mensaje que ella envió
// ({"estado": 409, "mensaje": "..."}) para mostrárselo al usuario tal cual.

async function pedir(metodo, ruta, cuerpo) {
    const opciones = { method: metodo, headers: {} };
    if (cuerpo !== undefined) {
        opciones.headers['Content-Type'] = 'application/json';
        opciones.body = JSON.stringify(cuerpo);
    }

    let respuesta;
    try {
        respuesta = await fetch(ruta, opciones);
    } catch (e) {
        throw new Error('No hay conexión con la API. Revise que la aplicación esté corriendo.');
    }

    if (respuesta.status === 204) {
        return null;
    }
    const datos = await respuesta.json().catch(() => null);
    if (!respuesta.ok) {
        throw new Error(datos && datos.mensaje ? datos.mensaje : 'La API respondió con el código ' + respuesta.status);
    }
    return datos;
}

export const api = {
    get: ruta => pedir('GET', ruta),
    post: (ruta, cuerpo) => pedir('POST', ruta, cuerpo),
    put: (ruta, cuerpo) => pedir('PUT', ruta, cuerpo),
    patch: ruta => pedir('PATCH', ruta),
    eliminar: ruta => pedir('DELETE', ruta),
};
