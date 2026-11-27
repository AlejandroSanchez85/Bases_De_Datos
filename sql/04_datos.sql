-- =====================================================================
-- BogoTickets · 04 · Datos de prueba
-- Ejecutar después de 03_vistas.sql. Los triggers del script 02 validan
-- cada fila mientras se carga. Valores en pesos colombianos (COP).
-- Registros sin relación a propósito: el escenario 6 no tiene eventos,
-- el tipo 7 (Danza) no tiene eventos, el evento 15 no tiene ventas y
-- los clientes 11 y 12 no han comprado.
-- =====================================================================

INSERT INTO escenario (id_escenario, nombre, direccion, capacidad_maxima) VALUES
(1, 'Estadio El Campín',                        'Carrera 30 # 57-60',  36000),
(2, 'Movistar Arena',                           'Diagonal 61C # 26-36', 14000),
(3, 'Plaza de Toros La Santamaría',             'Carrera 6 # 26-50',    14500),
(4, 'Teatro Mayor Julio Mario Santo Domingo',   'Calle 170 # 67-51',     1700),
(5, 'Teatro Colsubsidio Roberto Arias Pérez',   'Carrera 25 # 26-50',    1200),
(6, 'Teatro Jorge Eliécer Gaitán',              'Carrera 7 # 22-47',     1400);

-- La suma de aforos de cada escenario no supera su capacidad (RN-05)
INSERT INTO localidad (id_localidad, id_escenario, nombre, aforo_maximo) VALUES
( 1, 1, 'Gramilla',            12000),
( 2, 1, 'VIP',                  2000),
( 3, 1, 'Occidental',           8000),
( 4, 1, 'Oriental',             8000),
( 5, 1, 'Norte',                4000),
( 6, 2, 'Cancha General',       5000),
( 7, 2, 'Platea Baja',          3500),
( 8, 2, 'Platea Alta',          4000),
( 9, 2, 'VIP',                  1000),
(10, 3, 'Barrera',              1500),
(11, 3, 'Sombra',               5500),
(12, 3, 'Sol',                  6500),
(13, 4, 'Platea',                700),
(14, 4, 'Balcón',                500),
(15, 4, 'Palco Presidencial',      8),
(16, 5, 'Platea',                550),
(17, 5, 'Balcón',                450),
(18, 6, 'Platea',                600),
(19, 6, 'Balcón',                450);

INSERT INTO tipo_evento (id_tipo_evento, nombre, descripcion) VALUES
(1, 'Concierto', 'Presentaciones musicales en vivo'),
(2, 'Deporte', 'Partidos y competencias deportivas'),
(3, 'Teatro', 'Obras de teatro y artes escénicas'),
(4, 'Música clásica', 'Orquestas, ópera y música de cámara'),
(5, 'Comedia', 'Stand-up y humor en vivo'),
(6, 'Festival', 'Jornadas con varios artistas'),
(7, 'Danza', 'Ballet y danza contemporánea');

-- A finales de noviembre de 2026 están a la venta el evento 4 y los
-- eventos 11 a 15; los demás ya se realizaron
INSERT INTO evento (id_evento, id_escenario, id_tipo_evento, nombre, fecha, hora_inicio, hora_fin, fecha_registro) VALUES
( 1, 1, 2, 'Clásico Capitalino: Millonarios vs. Santa Fe',      '2026-09-20', '15:30', '17:30', '2026-07-15'),
( 2, 1, 6, 'Festival Sonidos del Páramo',                       '2026-11-14', '17:00', '23:30', '2026-08-20'),
( 3, 2, 1, 'Noche de Salsa Bogotá 2026',                        '2026-10-24', '20:00', '23:30', '2026-08-05'),
( 4, 2, 1, 'Concierto Pop Latino: Tour Cielo Abierto',          '2026-12-05', '20:00', '23:00', '2026-09-10'),
( 5, 3, 6, 'Festival de Rock Alternativo Capital',              '2026-10-31', '16:00', '23:00', '2026-07-30'),
( 6, 4, 4, 'Gala Sinfónica: Noche de Beethoven',                '2026-10-18', '19:00', '21:00', '2026-08-12'),
( 7, 5, 3, 'Obra de Teatro: El Último Tranvía',                 '2026-09-12', '19:30', '21:30', '2026-06-20'),
( 8, 5, 5, 'Stand-Up Comedy: Risas en la Capital',              '2026-11-07', '20:00', '21:45', '2026-09-25'),
( 9, 4, 1, 'Recital de Jazz: Bogotá Jazz Night',                '2026-09-26', '20:00', '22:00', '2026-07-28'),
(10, 4, 4, 'Recital de Cámara: Cuarteto Bogotá',                '2026-10-25', '17:00', '18:30', '2026-09-20'),
(11, 4, 4, 'Concierto de Navidad: Filarmónica de Bogotá',       '2026-12-12', '19:00', '21:00', '2026-11-03'),
(12, 1, 2, 'Gran Final del Fútbol Colombiano 2026-II',          '2026-12-16', '18:00', '20:00', '2026-11-20'),
(13, 5, 5, 'Navidad a Carcajadas: Noche de Stand-Up',           '2026-12-19', '20:00', '21:45', '2026-11-10'),
(14, 2, 1, 'Concierto Urbano: Tour Bogotá 2027',                '2027-02-13', '20:00', '23:30', '2026-11-15'),
(15, 4, 4, 'Temporada de Ópera: La Traviata',                   '2027-03-06', '19:30', '22:30', '2026-11-18');

-- Precio de cada localidad en cada evento. En el fútbol no se vende la Gramilla.
INSERT INTO precio_localidad (id_evento, id_localidad, precio) VALUES
(1, 2, 180000), (1, 3, 120000), (1, 4, 90000), (1, 5, 55000),
(2, 1, 320000), (2, 2, 650000), (2, 3, 280000), (2, 4, 220000), (2, 5, 150000),
(3, 6, 240000), (3, 7, 380000), (3, 8, 190000), (3, 9, 520000),
(4, 6, 350000), (4, 7, 520000), (4, 8, 270000), (4, 9, 780000),
(5, 10, 260000), (5, 11, 180000), (5, 12, 120000),
(6, 13, 150000), (6, 14, 95000), (6, 15, 400000),
(7, 16, 85000), (7, 17, 55000),
(8, 16, 90000), (8, 17, 60000),
(9, 13, 120000), (9, 14, 80000), (9, 15, 300000),
(10, 15, 250000),
(11, 13, 140000), (11, 14, 90000), (11, 15, 380000),
(12, 2, 250000), (12, 3, 160000), (12, 4, 120000), (12, 5, 70000),
(13, 16, 95000), (13, 17, 65000),
(14, 6, 280000), (14, 7, 420000), (14, 8, 210000), (14, 9, 650000),
(15, 13, 160000), (15, 14, 100000), (15, 15, 420000);

INSERT INTO cliente (id_cliente, documento, nombre_completo, correo, telefono, fecha_registro) VALUES
( 1, '1020456789', 'Laura Camila Rodríguez Peña',      'laura.rodriguez92@gmail.com',   '3114820931', '2025-11-03'),
( 2, '80123456',   'Carlos Andrés Martínez Gómez',     'carlos.martinez@hotmail.com',   '3207741185', '2025-12-15'),
( 3, '1032789014', 'Valentina Ortiz Salazar',          'valentina.ortiz@outlook.com',   '3003917742', '2026-01-20'),
( 4, '52987654',   'Juliana Patricia Herrera Vargas',  'juliana.herrera@gmail.com',     '3156029387', '2026-02-08'),
( 5, '79345621',   'Andrés Felipe Cárdenas Ruiz',      'andres.cardenas@yahoo.com',     '3182264053', '2026-03-14'),
( 6, '1010234567', 'Daniela Sofía Torres Mejía',       'daniela.torres@gmail.com',      '3125583016', '2026-04-02'),
( 7, '1098765432', 'Santiago Ramírez Londoño',         'santiago.ramirez@hotmail.com',  '3017329846', '2026-05-19'),
( 8, '51456789',   'Mónica Alejandra Castro Beltrán',  'monica.castro@outlook.com',     '3226194470', '2026-06-10'),
( 9, '1014567890', 'Juan Sebastián Pardo Niño',        'juan.pardo@gmail.com',          '3045878129', '2026-07-01'),
(10, '1000123456', 'Camila Andrea Suárez Duarte',      'camila.suarez@gmail.com',       '3138406552', '2026-08-22'),
(11, '80765432',   'Felipe Esteban Guerrero Rojas',    'felipe.guerrero@hotmail.com',   '3194077286', '2026-09-05'),
(12, '1023456789', 'Natalia Isabel Mora Quintero',     'natalia.mora@outlook.com',      '3009652418', '2026-09-28');

-- Cada compra agrupa las boletas que un cliente pagó en una sola transacción
INSERT INTO compra (id_compra, id_cliente, fecha_compra, metodo_pago) VALUES
( 1,  1, '2026-07-05 20:35', 'EFECTIVO'),
( 2,  2, '2026-07-12 13:52', 'TARJETA'),
( 3,  3, '2026-07-20 18:09', 'PSE'),
( 4,  1, '2026-08-01 14:17', 'PSE'),
( 5,  4, '2026-08-02 11:26', 'TARJETA'),
( 6,  2, '2026-08-03 12:51', 'EFECTIVO'),
( 7,  3, '2026-08-05 09:00', 'TARJETA'),
( 8,  3, '2026-08-10 17:08', 'TARJETA'),
( 9,  2, '2026-08-12 19:46', 'TARJETA'),
(10,  4, '2026-08-14 10:25', 'PSE'),
(11,  2, '2026-08-15 11:14', 'TARJETA'),
(12,  6, '2026-08-18 14:17', 'PSE'),
(13,  3, '2026-08-20 09:48', 'TARJETA'),
(14,  5, '2026-08-20 15:42', 'TARJETA'),
(15,  8, '2026-08-25 16:43', 'EFECTIVO'),
(16,  1, '2026-08-28 16:07', 'EFECTIVO'),
(17,  4, '2026-08-30 12:03', 'EFECTIVO'),
(18,  1, '2026-09-01 13:28', 'TARJETA'),
(19,  4, '2026-09-01 14:05', 'PSE'),
(20,  6, '2026-09-01 20:59', 'EFECTIVO'),
(21,  2, '2026-09-02 18:45', 'PSE'),
(22,  7, '2026-09-02 19:34', 'TARJETA'),
(23,  3, '2026-09-02 19:58', 'TARJETA'),
(24,  3, '2026-09-03 11:02', 'TARJETA'),
(25,  4, '2026-09-04 16:19', 'EFECTIVO'),
(26,  5, '2026-09-05 09:36', 'TARJETA'),
(27,  7, '2026-09-05 13:16', 'TARJETA'),
(28,  6, '2026-09-06 14:53', 'PSE'),
(29,  7, '2026-09-07 19:10', 'TARJETA'),
(30,  8, '2026-09-08 12:27', 'EFECTIVO'),
(31,  5, '2026-09-08 19:22', 'TARJETA'),
(32,  9, '2026-09-10 12:51', 'EFECTIVO'),
(33,  9, '2026-09-10 17:44', 'TARJETA'),
(34,  8, '2026-09-10 18:33', 'PSE'),
(35,  9, '2026-09-12 11:50', 'TARJETA'),
(36,  6, '2026-09-12 12:39', 'EFECTIVO'),
(37, 10, '2026-09-14 10:01', 'PSE'),
(38,  3, '2026-09-14 13:04', 'TARJETA'),
(39,  5, '2026-09-15 17:20', 'TARJETA'),
(40,  4, '2026-09-15 17:32', 'TARJETA'),
(41,  5, '2026-09-18 10:49', 'PSE'),
(42,  7, '2026-09-20 11:38', 'TARJETA'),
(43,  6, '2026-09-20 15:06', 'TARJETA'),
(44,  4, '2026-09-20 15:18', 'TARJETA'),
(45,  1, '2026-09-22 10:20', 'TARJETA'),
(46,  6, '2026-09-22 10:37', 'PSE'),
(47,  8, '2026-09-25 17:56', 'TARJETA'),
(48,  2, '2026-09-25 18:05', 'PSE'),
(49,  8, '2026-09-30 16:55', 'EFECTIVO'),
(50,  4, '2026-10-01 12:40', 'TARJETA'),
(51,  7, '2026-10-01 20:23', 'EFECTIVO'),
(52,  7, '2026-10-02 15:54', 'TARJETA'),
(53,  9, '2026-10-03 10:13', 'PSE'),
(54,  9, '2026-10-05 13:40', 'TARJETA'),
(55,  7, '2026-10-05 20:15', 'TARJETA'),
(56,  8, '2026-10-06 20:11', 'EFECTIVO'),
(57,  9, '2026-10-07 09:12', 'TARJETA'),
(58, 10, '2026-10-08 18:57', 'PSE'),
(59, 10, '2026-10-09 15:30', 'TARJETA'),
(60, 10, '2026-10-10 20:47', 'EFECTIVO'),
(61, 10, '2026-10-12 14:29', 'PSE'),
(62,  3, '2026-11-05 09:30', 'TARJETA'),
(63,  1, '2026-11-06 19:10', 'PSE'),
(64,  6, '2026-11-08 11:45', 'TARJETA'),
(65,  6, '2026-11-12 15:00', 'TARJETA'),
(66, 10, '2026-11-12 16:20', 'EFECTIVO'),
(67,  5, '2026-11-15 13:05', 'TARJETA'),
(68, 10, '2026-11-16 20:30', 'TARJETA'),
(69,  8, '2026-11-18 21:00', 'PSE'),
(70,  3, '2026-11-19 12:15', 'PSE'),
(71,  1, '2026-11-20 09:05', 'TARJETA'),
(72,  4, '2026-11-21 08:50', 'TARJETA'),
(73,  2, '2026-11-21 14:30', 'PSE'),
(74,  7, '2026-11-22 17:25', 'TARJETA'),
(75,  9, '2026-11-23 10:10', 'EFECTIVO'),
(76,  5, '2026-11-24 19:40', 'TARJETA'),
(77,  8, '2026-11-25 18:45', 'PSE');

-- precio_pagado es el precio vigente el día de la compra: las boletas
-- compradas antes de un cambio de precio conservan el valor anterior.
INSERT INTO boleta (id_boleta, id_compra, id_evento, id_localidad, precio_pagado, estado) VALUES
(  1,  4,  1,  3, 120000, 'Vendida'),
(  2,  4,  1,  3, 120000, 'Vendida'),
(  3,  6,  1,  4,  90000, 'Vendida'),
(  4,  8,  1,  5,  55000, 'Vendida'),
(  5, 10,  1,  2, 180000, 'Vendida'),
(  6, 14,  1,  4,  90000, 'Vendida'),
(  7, 20,  1,  3, 120000, 'Vendida'),
(  8, 27,  1,  5,  55000, 'Vendida'),
(  9, 34,  1,  4,  90000, 'Cancelada'),
( 10, 35,  1,  3, 120000, 'Vendida'),
( 11, 16,  2,  1, 300000, 'Vendida'),
( 12, 16,  2,  1, 300000, 'Vendida'),
( 13, 16,  2,  1, 300000, 'Vendida'),
( 14, 23,  2,  2, 650000, 'Vendida'),
( 15, 23,  2,  2, 650000, 'Vendida'),
( 16, 40,  2,  3, 280000, 'Vendida'),
( 17, 41,  2,  4, 220000, 'Vendida'),
( 18, 43,  2,  1, 320000, 'Vendida'),
( 19, 51,  2,  1, 320000, 'Vendida'),
( 20, 54,  2,  5, 150000, 'Vendida'),
( 21, 58,  2,  1, 320000, 'Vendida'),
( 22, 11,  3,  6, 240000, 'Vendida'),
( 23, 11,  3,  6, 240000, 'Vendida'),
( 24, 13,  3,  7, 380000, 'Vendida'),
( 25, 19,  3,  6, 240000, 'Vendida'),
( 26, 31,  3,  8, 190000, 'Vendida'),
( 27, 36,  3,  9, 520000, 'Vendida'),
( 28, 47,  3,  6, 240000, 'Vendida'),
( 29, 53,  3,  8, 190000, 'Vendida'),
( 30, 59,  3,  7, 380000, 'Cancelada'),
( 31, 60,  3,  6, 240000, 'Vendida'),
( 32, 38,  4,  6, 330000, 'Vendida'),
( 33, 38,  4,  7, 520000, 'Vendida'),
( 34, 42,  4,  8, 270000, 'Vendida'),
( 35, 49,  4,  7, 520000, 'Vendida'),
( 36, 57,  4,  9, 780000, 'Vendida'),
( 37, 61,  4,  6, 350000, 'Vendida'),
( 38,  9,  5, 12, 100000, 'Vendida'),
( 39, 17,  5, 11, 180000, 'Vendida'),
( 40, 39,  5, 12, 100000, 'Vendida'),
( 41, 46,  5, 12, 120000, 'Vendida'),
( 42, 52,  5, 10, 260000, 'Vendida'),
( 43, 56,  5, 11, 180000, 'Vendida'),
( 44, 18,  6, 15, 400000, 'Vendida'),
( 45, 21,  6, 15, 400000, 'Vendida'),
( 46, 24,  6, 15, 400000, 'Vendida'),
( 47, 25,  6, 15, 400000, 'Vendida'),
( 48, 26,  6, 15, 400000, 'Vendida'),
( 49, 28,  6, 15, 400000, 'Vendida'),
( 50, 29,  6, 15, 400000, 'Vendida'),
( 51, 30,  6, 15, 400000, 'Vendida'),
( 52, 33,  6, 13, 150000, 'Vendida'),
( 53, 37,  6, 14,  95000, 'Vendida'),
( 54, 44,  6, 13, 150000, 'Vendida'),
( 55,  1,  7, 16,  85000, 'Vendida'),
( 56,  2,  7, 17,  55000, 'Vendida'),
( 57,  3,  7, 16,  85000, 'Vendida'),
( 58,  5,  7, 16,  85000, 'Vendida'),
( 59, 15,  7, 17,  55000, 'Vendida'),
( 60,  7,  9, 13, 120000, 'Vendida'),
( 61, 12,  9, 14,  80000, 'Vendida'),
( 62, 22,  9, 13, 120000, 'Vendida'),
( 63, 32,  9, 14,  80000, 'Vendida'),
( 64, 45, 10, 15, 250000, 'Vendida'),
( 65, 45, 10, 15, 250000, 'Vendida'),
( 66, 48, 10, 15, 250000, 'Vendida'),
( 67, 50, 10, 15, 250000, 'Vendida'),
( 68, 55, 10, 15, 250000, 'Vendida'),
( 69, 55, 10, 15, 250000, 'Vendida'),
( 70, 62, 11, 15, 380000, 'Vendida'),
( 71, 62, 11, 15, 380000, 'Vendida'),
( 72, 63, 11, 13, 140000, 'Vendida'),
( 73, 63, 11, 13, 140000, 'Vendida'),
( 74, 63, 11, 13, 140000, 'Vendida'),
( 75, 64, 11, 15, 380000, 'Vendida'),
( 76, 64, 11, 15, 380000, 'Vendida'),
( 77, 65, 13, 16,  95000, 'Vendida'),
( 78, 65, 13, 16,  95000, 'Vendida'),
( 79, 66, 11, 15, 380000, 'Vendida'),
( 80, 66, 11, 15, 380000, 'Vendida'),
( 81, 67, 11, 13, 140000, 'Vendida'),
( 82, 68, 14,  6, 280000, 'Vendida'),
( 83, 68, 14,  6, 280000, 'Vendida'),
( 84, 68, 14,  6, 280000, 'Vendida'),
( 85, 68, 14,  6, 280000, 'Vendida'),
( 86, 69, 11, 14,  90000, 'Vendida'),
( 87, 69, 11, 14,  90000, 'Vendida'),
( 88, 70, 13, 17,  65000, 'Vendida'),
( 89, 71, 14,  9, 650000, 'Vendida'),
( 90, 72, 12,  2, 250000, 'Vendida'),
( 91, 72, 12,  2, 250000, 'Vendida'),
( 92, 73, 12,  3, 160000, 'Vendida'),
( 93, 73, 12,  3, 160000, 'Vendida'),
( 94, 73, 12,  3, 160000, 'Vendida'),
( 95, 74, 12,  4, 120000, 'Vendida'),
( 96, 74, 12,  4, 120000, 'Vendida'),
( 97, 74, 12,  4, 120000, 'Vendida'),
( 98, 74, 12,  4, 120000, 'Vendida'),
( 99, 75, 12,  3, 160000, 'Vendida'),
(100, 75, 12,  3, 160000, 'Vendida'),
(101, 76, 12,  5,  70000, 'Vendida'),
(102, 76, 12,  5,  70000, 'Vendida'),
(103, 77, 14,  8, 210000, 'Vendida'),
(104, 77, 14,  8, 210000, 'Vendida');

-- Cambios previos que dejaron los precios y aforos actuales (RNF-06).
-- Desde ahora estos registros los crean los triggers del script 02.
INSERT INTO historial_precios
    (id_historial, id_localidad, id_evento, campo_modificado, valor_anterior, valor_nuevo, usuario, fecha_cambio) VALUES
(1, 15, NULL, 'AFORO_MAXIMO',      6,      8, 'admin_escenarios', '2026-08-01 09:30:00'),
(2,  9, 3,    'PRECIO',       480000, 520000, 'admin_escenarios', '2026-08-20 10:15:00'),
(3,  2, 2,    'PRECIO',       600000, 650000, 'admin_escenarios', '2026-08-25 11:05:00'),
(4,  1, 2,    'PRECIO',       300000, 320000, 'admin_escenarios', '2026-09-10 09:40:00'),
(5, 12, 5,    'PRECIO',       100000, 120000, 'admin_escenarios', '2026-09-18 14:20:00'),
(6,  6, 4,    'PRECIO',       330000, 350000, 'admin_escenarios', '2026-09-25 16:45:00');

-- Las filas se cargaron con id explícito: se ajustan las secuencias
SELECT setval(pg_get_serial_sequence('escenario', 'id_escenario'), (SELECT MAX(id_escenario) FROM escenario));
SELECT setval(pg_get_serial_sequence('localidad', 'id_localidad'), (SELECT MAX(id_localidad) FROM localidad));
SELECT setval(pg_get_serial_sequence('tipo_evento', 'id_tipo_evento'), (SELECT MAX(id_tipo_evento) FROM tipo_evento));
SELECT setval(pg_get_serial_sequence('evento', 'id_evento'), (SELECT MAX(id_evento) FROM evento));
SELECT setval(pg_get_serial_sequence('cliente', 'id_cliente'), (SELECT MAX(id_cliente) FROM cliente));
SELECT setval(pg_get_serial_sequence('compra', 'id_compra'), (SELECT MAX(id_compra) FROM compra));
SELECT setval(pg_get_serial_sequence('boleta', 'id_boleta'), (SELECT MAX(id_boleta) FROM boleta));
SELECT setval(pg_get_serial_sequence('historial_precios', 'id_historial'), (SELECT MAX(id_historial) FROM historial_precios));

-- Verificación: filas por tabla
SELECT 'escenario' AS tabla, COUNT(*) AS filas FROM escenario
UNION ALL SELECT 'localidad',         COUNT(*) FROM localidad
UNION ALL SELECT 'tipo_evento',       COUNT(*) FROM tipo_evento
UNION ALL SELECT 'evento',            COUNT(*) FROM evento
UNION ALL SELECT 'precio_localidad',  COUNT(*) FROM precio_localidad
UNION ALL SELECT 'cliente',           COUNT(*) FROM cliente
UNION ALL SELECT 'compra',            COUNT(*) FROM compra
UNION ALL SELECT 'boleta',            COUNT(*) FROM boleta
UNION ALL SELECT 'historial_precios', COUNT(*) FROM historial_precios;
