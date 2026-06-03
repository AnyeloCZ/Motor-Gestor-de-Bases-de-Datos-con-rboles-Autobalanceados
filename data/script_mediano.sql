-- Dataset mediano: inventario con 20 productos
-- Ejecutar: java -cp build/classes cli.REPL --script data/script_mediano.sql

CREATE SPACE productos (codigo TEXTO PK, nombre TEXTO, precio REAL, stock ENTERO, categoria TEXTO);

INSERT INTO productos (codigo, nombre, precio, stock, categoria) VALUES ('P001', 'Laptop HP', 2500000, 15, 'Tecnologia');
INSERT INTO productos (codigo, nombre, precio, stock, categoria) VALUES ('P002', 'Mouse Logitech', 85000, 50, 'Tecnologia');
INSERT INTO productos (codigo, nombre, precio, stock, categoria) VALUES ('P003', 'Teclado Mecanico', 220000, 30, 'Tecnologia');
INSERT INTO productos (codigo, nombre, precio, stock, categoria) VALUES ('P004', 'Monitor 24in', 950000, 8, 'Tecnologia');
INSERT INTO productos (codigo, nombre, precio, stock, categoria) VALUES ('P005', 'Silla Ergonomica', 780000, 12, 'Mobiliario');
INSERT INTO productos (codigo, nombre, precio, stock, categoria) VALUES ('P006', 'Escritorio', 650000, 6, 'Mobiliario');
INSERT INTO productos (codigo, nombre, precio, stock, categoria) VALUES ('P007', 'Lampara LED', 45000, 100, 'Mobiliario');
INSERT INTO productos (codigo, nombre, precio, stock, categoria) VALUES ('P008', 'Cuaderno 100h', 8500, 200, 'Papeleria');
INSERT INTO productos (codigo, nombre, precio, stock, categoria) VALUES ('P009', 'Lapicero Azul', 1200, 500, 'Papeleria');
INSERT INTO productos (codigo, nombre, precio, stock, categoria) VALUES ('P010', 'Resma Papel', 18000, 80, 'Papeleria');
INSERT INTO productos (codigo, nombre, precio, stock, categoria) VALUES ('P011', 'USB 64GB', 35000, 45, 'Tecnologia');
INSERT INTO productos (codigo, nombre, precio, stock, categoria) VALUES ('P012', 'Disco Externo 1TB', 185000, 20, 'Tecnologia');
INSERT INTO productos (codigo, nombre, precio, stock, categoria) VALUES ('P013', 'Webcam HD', 125000, 18, 'Tecnologia');
INSERT INTO productos (codigo, nombre, precio, stock, categoria) VALUES ('P014', 'Auriculares', 95000, 25, 'Tecnologia');
INSERT INTO productos (codigo, nombre, precio, stock, categoria) VALUES ('P015', 'Calculadora', 32000, 35, 'Papeleria');
INSERT INTO productos (codigo, nombre, precio, stock, categoria) VALUES ('P016', 'Archivador', 28000, 60, 'Papeleria');
INSERT INTO productos (codigo, nombre, precio, stock, categoria) VALUES ('P017', 'Soporte Monitor', 115000, 14, 'Mobiliario');
INSERT INTO productos (codigo, nombre, precio, stock, categoria) VALUES ('P018', 'Hub USB 7p', 55000, 22, 'Tecnologia');
INSERT INTO productos (codigo, nombre, precio, stock, categoria) VALUES ('P019', 'Alfombrilla XL', 42000, 40, 'Mobiliario');
INSERT INTO productos (codigo, nombre, precio, stock, categoria) VALUES ('P020', 'Cable HDMI 2m', 22000, 90, 'Tecnologia');

-- ver todo
SELECT * FROM productos;

-- búsqueda por clave O(log n)
SELECT * FROM productos WHERE codigo = 'P010';

-- búsqueda por condición O(n)
SELECT * FROM productos WHERE categoria = 'Tecnologia';
SELECT * FROM productos WHERE precio >= 100000;
SELECT * FROM productos WHERE stock < 15;

-- rango O(log n + k)
SELECT * FROM productos WHERE codigo BETWEEN 'P005' AND 'P010';

-- actualizar
UPDATE productos SET precio=2350000, stock=10 WHERE codigo = 'P001';
UPDATE productos SET stock=0 WHERE stock < 5;

-- eliminar los agotados
DELETE FROM productos WHERE stock = 0;

-- ver árbol y descripción
TREE productos;
DESC productos;

SAVE ALL;
