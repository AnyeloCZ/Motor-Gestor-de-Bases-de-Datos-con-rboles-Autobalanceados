-- Dataset: espacio libre (no relacional)
-- Cada registro puede tener campos diferentes
-- Ejecutar: java -cp build/classes cli.REPL --script data/script_libre.sql

CREATE SPACE documentos;

-- Insertar registros con diferentes campos cada uno
INSERT INTO documentos (id, titulo, autor, paginas) VALUES ('doc1', 'Arboles AVL', 'Adelson-Velsky', 12);
INSERT INTO documentos (id, titulo, editorial, anio) VALUES ('doc2', 'Estructuras de Datos', 'McGraw-Hill', 2020);
INSERT INTO documentos (id, nombre_archivo, tamanio_kb, formato) VALUES ('doc3', 'informe_final.pdf', 2456, 'PDF');
INSERT INTO documentos (id, asunto, remitente, leido) VALUES ('doc4', 'Reunion lunes', 'profesor@ud.edu.co', false);
INSERT INTO documentos (id, descripcion, etiquetas, prioridad) VALUES ('doc5', 'Tarea pendiente', 'urgente,cc1', 'alta');

-- Cada registro tiene campos distintos, demostrando esquema libre
SELECT * FROM documentos;

-- Buscar por clave primaria (obligatorio en todo espacio)
SELECT * FROM documentos WHERE id = 'doc3';

-- Buscar por campo que solo existe en algunos registros
SELECT * FROM documentos WHERE autor = 'Adelson-Velsky';

DESC documentos;
TREE documentos;
SAVE ALL;
