# BogoTickets

Sistema de venta de boletería para escenarios de Bogotá. Controla el aforo de cada localidad para impedir la sobreventa, registra las compras de los clientes y genera reportes de ocupación y recaudo. Lo usan tres actores: el **cliente**, que consulta el catálogo y compra; el **administrador de escenarios y eventos**, que configura escenarios, localidades, eventos y precios; y el **analista de ventas**, que consulta los reportes.

Proyecto final de Bases de Datos (grupo 020-83): base de datos en PostgreSQL, API REST con Spring Boot y JdbcTemplate, e interfaz web en HTML, CSS y JavaScript que usa la API.

## Integrante

- Dilan Alejandro Sanchez Escobar — 20222020252

## Requisitos previos

| Herramienta | Versión |
|---|---|
| PostgreSQL y pgAdmin 4 | PostgreSQL 14 o superior (probado en 14, 16 y 18) |
| Java (JDK) | 17 o superior (probado con 17, 21 y 25) |
| IDE | IntelliJ IDEA (la edición Community sirve) o VS Code con *Extension Pack for Java* |
| Git | cualquier versión reciente |

No hace falta instalar Maven: el proyecto trae Maven Wrapper (`mvnw` y `mvnw.cmd`). La primera ejecución necesita internet para descargar las dependencias.

## Instalación

### 1. Clonar el repositorio

```
git clone https://github.com/AlejandroSanchez85/Bases_De_Datos.git
cd Bases_De_Datos
```

### 2. Crear la base de datos (pgAdmin)

1. Conectado a la base `postgres`, abrir el Query Tool y ejecutar:
   ```sql
   CREATE DATABASE bogotickets ENCODING 'UTF8';
   ```
2. Abrir el Query Tool sobre `bogotickets` y ejecutar **en orden** los scripts `01` a `05` de la carpeta `sql/` (abrir el archivo y presionar F5). Cada script termina con una consulta de verificación.

| Orden | Script | Qué crea | Resultado esperado al final |
|---|---|---|---|
| 1 | `01_tablas.sql` | 9 tablas, restricciones e índices | 9 tablas con su número de restricciones |
| 2 | `02_funciones_triggers.sql` | triggers de reglas de negocio, función de compra y procedimiento de anulación | 11 triggers |
| 3 | `03_vistas.sql` | 5 vistas | 5 vistas |
| 4 | `04_datos.sql` | datos de prueba | 6 escenarios, 19 localidades, 7 tipos, 15 eventos, 47 tarifas, 12 clientes, 77 compras, 104 boletas, 6 cambios en el historial |
| 5 | `05_usuarios_permisos.sql` | roles, usuarios y permisos | 3 usuarios y 3 roles |

Los otros dos scripts no hacen falta para instalar: `06_consultas_api.sql` reúne las consultas con vistas y subconsultas que ejecuta la API, y `07_pruebas_reglas.sql` trae pruebas que violan cada regla a propósito. En ambos se selecciona una consulta o prueba y se ejecuta sola.

Los scripts se pueden volver a ejecutar para dejar los datos como al principio: `01` reinicia las tablas y `05` vuelve a crear los usuarios. Antes de hacerlo hay que detener la API.

### 3. Configurar la conexión

La conexión está en `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/bogotickets
spring.datasource.username=usuario_app
spring.datasource.password=${DB_PASSWORD:Cambiar_App_2026}
```

- La API entra con `usuario_app`, el usuario que crea el script `05`.
- Si PostgreSQL usa otro puerto (por ejemplo 5433), se cambia en la URL.
- La contraseña es la de ejemplo del script `05`. Si se cambia en la base, se puede escribir aquí o definir la variable de entorno `DB_PASSWORD`.

### 4. Ejecutar la aplicación

**Desde IntelliJ IDEA:** *File → Open* y elegir la carpeta `Bases_De_Datos`. Esperar a que IntelliJ cargue el proyecto Maven (barra de progreso abajo a la derecha), abrir `src/main/java/com/bogotickets/BogoTicketsApplication.java` y pulsar el botón ▶ junto a `main`.

**Desde la terminal**, en la carpeta del proyecto:

```
mvnw.cmd spring-boot:run        (Windows)
./mvnw spring-boot:run          (Linux o macOS)
```

La aplicación queda lista cuando la consola muestra `Started BogoTicketsApplication`. Entonces:

- Interfaz web: http://localhost:8080
- Swagger (probar los endpoints): http://localhost:8080/swagger-ui.html

Si el puerto 8080 está ocupado, se cambia `server.port` en `application.properties`.

## Interfaz web

La interfaz usa solo la API (`fetch` a `/api/...`); nunca se conecta a la base. Cuando la API responde un error, la pantalla muestra el mensaje tal como llega, que en las reglas de negocio es el mensaje del trigger de PostgreSQL.

| Pantalla | Actor | Qué permite |
|---|---|---|
| Catálogo y compra | Cliente | Ver los eventos a la venta, los cupos de cada localidad y comprar boletas (caso de uso principal) |
| Mis boletas | Cliente | Ver el historial de compras de un cliente y consultar una boleta por su código |
| Administración | Administrador | CRUD de escenarios, localidades, tipos de evento, eventos y clientes; precios por localidad; anulación de compras; historial de precios |
| Reportes | Analista de ventas | Ocupación y recaudo, eventos con baja ocupación, tarifas sobre el promedio, clientes sin compras y comportamiento de compra |

### Prueba del caso principal: compra que falla a mitad de camino

1. En **Catálogo y compra**, pulsar *Comprar* en «Concierto de Navidad: Filarmónica de Bogotá».
2. Elegir un cliente, la localidad **Palco Presidencial** (le quedan 2 cupos) y **3** boletas. Pulsar *Confirmar compra*.
3. La pantalla muestra: «Localidad agotada: «Palco Presidencial» de «Concierto de Navidad: Filarmónica de Bogotá» no tiene más cupos (aforo 8)». La tabla de disponibilidad sigue con 6 vendidas.
4. En pgAdmin se comprueba que no quedó nada a medias (siguen 77 compras y 104 boletas):
   ```sql
   SELECT (SELECT COUNT(*) FROM compra) AS compras, (SELECT COUNT(*) FROM boleta) AS boletas;
   ```
5. Con **2** boletas la compra sí se registra.

Las dos primeras boletas alcanzaron a insertarse, pero la tercera hizo fallar el trigger y PostgreSQL deshizo la transacción completa. El número de esa compra rechazada no se reutiliza porque las secuencias no retroceden con el ROLLBACK, por eso la siguiente compra exitosa salta un número. Esta prueba usa el evento del 12 de diciembre de 2026: después de esa fecha el mensaje será que el evento ya se realizó.

## Diagrama entidad-relación

![Diagrama ER](docs/diagrama_er.png)

La documentación completa (descripción, casos de uso, diccionario de datos, restricciones, llaves foráneas y endpoints) está en `docs/Documentacion_BogoTickets.docx`.

## Endpoints

Base: `http://localhost:8080`. Los datos se envían y reciben en JSON. Los 42 endpoints también aparecen en Swagger.

| Método | URL | Descripción |
|---|---|---|
| **Escenarios** | | |
| GET | `/api/escenarios` | Lista todos los escenarios. |
| GET | `/api/escenarios/{id}` | Retorna un escenario por id. |
| POST | `/api/escenarios` | Crea un escenario. |
| PUT | `/api/escenarios/{id}` | Actualiza nombre, dirección y capacidad. |
| DELETE | `/api/escenarios/{id}` | Elimina un escenario sin eventos (borra sus localidades). |
| **Localidades** | | |
| GET | `/api/localidades?idEscenario=` | Lista las localidades; opcional por escenario. |
| GET | `/api/localidades/{id}` | Retorna una localidad por id. |
| POST | `/api/localidades` | Crea una localidad en un escenario. |
| PUT | `/api/localidades/{id}` | Actualiza nombre y aforo (el cambio de aforo se audita). |
| DELETE | `/api/localidades/{id}` | Elimina una localidad sin ventas ni auditoría. |
| **Tipos de evento** | | |
| GET | `/api/tipos-evento` | Lista los tipos de evento. |
| GET | `/api/tipos-evento/{id}` | Retorna un tipo por id. |
| POST | `/api/tipos-evento` | Crea un tipo. |
| PUT | `/api/tipos-evento/{id}` | Actualiza nombre y descripción. |
| DELETE | `/api/tipos-evento/{id}` | Elimina un tipo sin eventos. |
| **Eventos** | | |
| GET | `/api/eventos?idEscenario=&idTipo=` | Lista los eventos; filtros opcionales. |
| GET | `/api/eventos/{id}` | Retorna un evento por id. |
| POST | `/api/eventos` | Programa un evento. |
| PUT | `/api/eventos/{id}` | Actualiza nombre, tipo, fecha y horario. |
| DELETE | `/api/eventos/{id}` | Elimina un evento sin ventas ni auditoría (borra sus tarifas). |
| **Clientes** | | |
| GET | `/api/clientes` | Lista los clientes. |
| GET | `/api/clientes/{id}` | Retorna un cliente por id. |
| POST | `/api/clientes` | Registra un cliente. |
| PUT | `/api/clientes/{id}` | Actualiza nombre, correo y teléfono. |
| DELETE | `/api/clientes/{id}` | Elimina la cuenta de un cliente sin compras. |
| **Precios de un evento** | | |
| GET | `/api/eventos/{id}/precios` | Lista las tarifas del evento. |
| POST | `/api/eventos/{id}/precios` | Asigna precio a una localidad del escenario del evento. |
| PUT | `/api/eventos/{id}/precios/{idLocalidad}` | Cambia el precio; queda en el historial (F3). |
| DELETE | `/api/eventos/{id}/precios/{idLocalidad}` | Quita una tarifa sin boletas vendidas. |
| **Catálogo y compras** | | |
| GET | `/api/catalogo` | Eventos a la venta ordenados por fecha (vista v_catalogo_eventos). |
| GET | `/api/eventos/{id}/disponibilidad` | Cupos y precio por localidad en tiempo real (F4, vista). |
| POST | `/api/compras` | Compra de boletas en una transacción (F1, caso principal). |
| GET | `/api/compras/{id}` | Compra con sus boletas. |
| PATCH | `/api/compras/{id}/anular` | Anula la compra y libera los cupos (F2). |
| GET | `/api/boletas/{id}` | Datos completos de una boleta (vista v_boletas_detalle). |
| GET | `/api/clientes/{id}/boletas` | Historial de compras del cliente (F6, vista). |
| GET | `/api/historial-precios?idEvento=` | Cambios de precio y aforo registrados (F3). |
| **Reportes** | | |
| GET | `/api/reportes/ocupacion?idEvento=` | Ocupación y recaudo por localidad (F5, vista). |
| GET | `/api/reportes/eventos-baja-ocupacion?umbral=50` | Eventos con ocupación menor al umbral (F5, subconsulta en FROM). |
| GET | `/api/reportes/precios-sobre-promedio` | Tarifas mayores al promedio (subconsulta escalar). |
| GET | `/api/reportes/clientes-sin-compras` | Clientes que nunca han comprado (subconsulta NOT EXISTS). |
| GET | `/api/reportes/clientes` | Compras y boletas por cliente sin datos sensibles (vista). |

Ejemplo de compra:

```
POST /api/compras
{"idCliente": 5, "idEvento": 11, "idLocalidad": 13, "cantidad": 2, "metodoPago": "TARJETA"}
```

### Códigos de respuesta

| Código | Cuándo |
|---|---|
| 200 | GET, PUT y PATCH exitosos |
| 201 | POST exitoso; devuelve el registro creado |
| 204 | DELETE exitoso |
| 400 | Falta un dato obligatorio o un dato viola un CHECK (precio negativo, correo con mayúsculas...) |
| 404 | El id no existe |
| 409 | Choca con una regla o con otros datos: localidad agotada, evento realizado, documento repetido, registro con dependientes |
| 503 | PostgreSQL está apagado o la conexión de `application.properties` es incorrecta |

Todos los errores tienen la forma `{"estado": 409, "mensaje": "..."}`. La clase `ManejadorErrores` traduce el SQLSTATE de PostgreSQL al código HTTP: `P0001` (RAISE EXCEPTION de un trigger) da 409; `23505` (UNIQUE) y `23503` o `23001` (FOREIGN KEY) dan 409; `23514` (CHECK) y `23502` (NOT NULL) dan 400; y `P0002` da 404.

## Requerimientos funcionales (R5)

| Funcionalidad | Qué hace | Endpoint | Tablas |
|---|---|---|---|
| **F1. Compra de boletas con control de aforo** (caso principal) | La función `fn_registrar_compra` crea la compra y una boleta por cupo en una sola transacción. El trigger `trg_boleta_validar` revisa cada boleta: que no se supere el aforo (con un bloqueo para compras simultáneas), que el evento no se haya realizado y que la localidad tenga precio; además fija el precio pagado. Si una boleta falla, no se guarda nada. | `POST /api/compras` | Modifica `compra` y `boleta`. Consulta `cliente`, `evento`, `localidad` y `precio_localidad` |
| **F2. Anulación de compras** | El procedimiento `sp_anular_compra` pasa a Cancelada todas las boletas de la compra y libera sus cupos. No se anula una compra de un evento ya realizado ni una ya anulada. Nada se borra. | `PATCH /api/compras/{id}/anular` | Modifica `boleta`. Consulta `compra` y `evento` |
| **F3. Cambio de precio con auditoría** | Cambia el precio de una localidad en un evento. El trigger `trg_auditar_precio` guarda en el historial el valor anterior, el nuevo, el usuario y la fecha. Las boletas ya vendidas conservan el precio que se pagó. | `PUT /api/eventos/{id}/precios/{idLocalidad}` y `GET /api/historial-precios` | Modifica `precio_localidad` e inserta en `historial_precios` |
| **F4. Disponibilidad en tiempo real** | Muestra por localidad el precio, el aforo, las boletas vendidas y las disponibles. Solo cuentan las boletas Vendida: una cancelada libera el cupo. | `GET /api/eventos/{id}/disponibilidad` | Vista `v_ocupacion_localidad` (`precio_localidad`, `evento`, `escenario`, `localidad`, `boleta`) |
| **F5. Reportes de ocupación y recaudo** | Porcentaje de ocupación y dinero recaudado por localidad, y lista de eventos cuya ocupación general está por debajo de un umbral. | `GET /api/reportes/ocupacion` y `GET /api/reportes/eventos-baja-ocupacion?umbral=50` | Vista `v_ocupacion_localidad` |
| **F6. Historial de compras del cliente** | Boletas del cliente con evento, escenario, localidad, precio pagado y estado. | `GET /api/clientes/{id}/boletas` | Vista `v_boletas_detalle` (`boleta`, `compra`, `cliente`, `evento`, `tipo_evento`, `escenario`, `localidad`) |

## Vistas y subconsultas (R3 y R4)

| Objeto | Tipo | Usado en |
|---|---|---|
| `v_ocupacion_localidad` | Vista de reporte | `GET /api/eventos/{id}/disponibilidad`, `GET /api/reportes/ocupacion`, `GET /api/reportes/eventos-baja-ocupacion` |
| `v_catalogo_eventos` | Vista de simplificación | `GET /api/catalogo` |
| `v_boletas_detalle` | Vista de simplificación (une 7 tablas) | `GET /api/boletas/{id}`, `GET /api/clientes/{id}/boletas`, `GET /api/compras/{id}` |
| `v_clientes_publico` | Vista de seguridad (sin documento ni teléfono) | `GET /api/reportes/clientes` |
| `v_boletas_vigentes` | Vista con `WITH CHECK OPTION` | pgAdmin (administrador) |
| Subconsulta escalar en WHERE | Tarifas por encima del promedio | `GET /api/reportes/precios-sobre-promedio` |
| Subconsulta `NOT EXISTS` | Clientes sin compras | `GET /api/reportes/clientes-sin-compras` |
| Subconsulta en FROM | Ocupación general por evento | `GET /api/reportes/eventos-baja-ocupacion` |
| Subconsulta escalar en SELECT | Total pagado de una compra | `GET /api/compras/{id}` |

Las vistas están comentadas en `sql/03_vistas.sql` y las subconsultas en `sql/06_consultas_api.sql`.

## Reglas de negocio en la base de datos

Las reglas que comparan varias filas o tablas se implementan con triggers en `02_funciones_triggers.sql`:

| Regla | Implementación |
|---|---|
| No vender más boletas que el aforo de la localidad, ni siquiera con compras simultáneas (RN-07, RNF-01, RNF-10) | `trg_boleta_validar` con bloqueo por evento y localidad |
| No vender boletas de un evento ya realizado ni antes de su creación (RN-17, RNF-03) | `trg_boleta_validar` |
| Solo se venden localidades con precio en el evento (RN-08) | `trg_boleta_validar` y la FK compuesta de `boleta` |
| El precio pagado queda fijo en la boleta (RN-16) | `trg_boleta_validar` |
| Las localidades no superan la capacidad del escenario (RN-05) | `trg_localidad_validar`, `trg_escenario_capacidad` |
| La localidad de una tarifa pertenece al escenario del evento (RN-15) | `trg_precio_localidad` |
| Dos eventos del mismo escenario no se cruzan en horario (RNF-02) | `trg_evento_horario` |
| La compra no es anterior al registro del cliente (RN-12) | `trg_compra_validar` |
| Todo cambio de precio o aforo queda auditado (RNF-06) | `trg_auditar_precio`, `trg_auditar_aforo` |
| Boletas, compras e historial no se borran (RNF-07) | `trg_boleta_no_borrar`, `trg_compra_no_borrar`, `trg_historial_no_borrar` |

## Usuarios y permisos

| Usuario | Hereda | Uso |
|---|---|---|
| `usuario_app` | `rol_cliente`, `rol_administrador`, `rol_lectura` | Cuenta con la que la API se conecta a la base |
| `admin_escenarios` | `rol_administrador` | Administrador de escenarios y eventos en pgAdmin |
| `usuario_reportes` | `rol_lectura` | Analista de ventas: solo lee vistas |

Ningún usuario tiene `GRANT ALL`, `TRUNCATE` ni permiso para crear tablas. Las contraseñas de `05_usuarios_permisos.sql` son de ejemplo.

## Estructura del repositorio

```
sql/                              scripts de la base de datos (01 a 05 en orden)
docs/                             diagrama ER y documentación en Word
src/main/java/com/bogotickets/
    BogoTicketsApplication.java   clase principal
    controller/                   endpoints REST: reciben la petición y devuelven el código HTTP
    service/                      responden 404 si el registro no existe y coordinan cada operación
    repository/                   SQL con JdbcTemplate
    model/                        records con las columnas de cada tabla
    dto/                          records de peticiones, vistas y reportes
    exception/                    traducción de los errores de PostgreSQL a códigos HTTP
src/main/resources/
    application.properties        conexión a PostgreSQL
    static/                       interfaz web: index.html, css/ y js/
pom.xml                           dependencias (Maven)
mvnw, mvnw.cmd, .mvn/             Maven Wrapper
```

Cada petición pasa por las capas controlador → servicio → repositorio. Las reglas de negocio están en la base de datos: la API no las repite. Solo exige que lleguen los campos obligatorios (`@Valid`) y traduce los errores de PostgreSQL a respuestas HTTP.
