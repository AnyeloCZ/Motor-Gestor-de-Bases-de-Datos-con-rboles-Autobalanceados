-- Dataset pequeño: 5 estudiantes
-- Ejecutar: java -cp build/classes cli.REPL --script data/script_pequeno.sql

CREATE SPACE estudiantes (id ENTERO PK, nombre TEXTO, promedio REAL, semestre ENTERO, activo BOOLEAN);

INSERT INTO estudiantes VALUES (1, 'Ana Gomez', 4.5, 6, true);
INSERT INTO estudiantes VALUES (2, 'Carlos Ruiz', 3.8, 4, true);
INSERT INTO estudiantes VALUES (3, 'Laura Torres', 4.1, 8, false);
INSERT INTO estudiantes VALUES (4, 'Miguel Herrera', 3.5, 2, true);
INSERT INTO estudiantes VALUES (5, 'Sofia Castro', 4.9, 10, true);

SELECT * FROM estudiantes;
SELECT * FROM estudiantes WHERE promedio >= 4.0;
SELECT * FROM estudiantes WHERE semestre BETWEEN 4 AND 8;

TREE estudiantes;
DESC estudiantes;
SAVE ALL;
