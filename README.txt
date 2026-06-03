Motor BD con Árboles AVL Autobalanceados
Proyecto Final - Ciencias de la Computacion I - Grupo 020-83

Motor de base de datos en Java que soporta crear espacios, insertar,
consultar, actualizar y eliminar registros. Usa un arbol AVL propio como
indice primario. Tiene CLI interactiva y GUI con 6 pestanas.

Equipo:
  Anyelo Esteban Casas Zapata    20251020106
  Diego Alejandro Yanez Zabala   20251020103
  Wilfer Arbey Herrera Garzon    20251020071
  Docente: Ing. Simar Enrique Herrera Jimenez


1. REQUISITOS
  - JDK 17 o superior
  - Sin librerías externas (solo Java SE + Swing)
  - Terminal: PowerShell, CMD, Bash


2. ESTRUCTURA

src/
  motor/arbol/            Arbol AVL generico con rotaciones LL/LR/RR/RL
  motor/almacenamiento/   Espacio, Registro, GestorEspacios
  motor/catalogo/         Esquema, Campo, TipoDato, Catalogo
  motor/persistencia/     GestorPersistencia (JSON, escritura atomica)
  motor/parser/           Motor (controlador), ResultadoComando
  cli/                    REPL.java (consola interactiva)
  gui/                    6 pestanas Swing + arbol visual Graphics2D
  test/                   TestMotor (39 pruebas), TestBenchmark

data/
  script_pequeno.sql      Dataset con 5 estudiantes
  script_mediano.sql      Dataset con 20 productos
  script_libre.sql        Dataset no relacional (espacio libre)

docs/
  DOCUMENTACION_TECNICA.txt   Documentacion completa (11 secciones)
  ANALISIS_COMPLEJIDAD.txt    Recurrencias, cotas, Teorema Maestro


3. COMPILAR Y EJECUTAR

Compilar (PowerShell):
  $files = Get-ChildItem -Recurse -Filter *.java | ForEach-Object { $_.FullName }
  javac -encoding UTF-8 -d build/classes -sourcepath src $files

Compilar (Linux/Mac):
  javac -encoding UTF-8 -d build/classes -sourcepath src $(find src -name "*.java")

Consola interactiva:
  java -cp build/classes cli.REPL

Interfaz grafica:
  java -cp build/classes gui.DatabaseManagerGUI

Ejecutar script SQL:
  java -cp build/classes cli.REPL --script data/script_mediano.sql

Ejecutar pruebas:
  java -cp build/classes test.TestMotor

Ejecutar benchmark:
  java -cp build/classes test.TestBenchmark


4. COMANDOS PRINCIPALES

CREATE SPACE estudiantes (id ENTERO PK, nombre TEXTO, promedio REAL, activo BOOLEAN)
CREATE SPACE notas

INSERT INTO estudiantes VALUES (1, 'Ana Gomez', 4.5, true)
INSERT INTO notas (id, materia, nota) VALUES ('n1', 'Calculo', 4.2)

SELECT * FROM estudiantes
SELECT * FROM estudiantes WHERE promedio >= 4.0
SELECT * FROM estudiantes WHERE id BETWEEN 1 AND 10

UPDATE estudiantes SET promedio = 5.0 WHERE id = 1

DELETE FROM estudiantes WHERE id = 3
DELETE FROM estudiantes ALL

TREE estudiantes
DESC estudiantes
SAVE ALL
EXIT


5. PERSISTENCIA

Archivos generados:
  data/esquemas/<nombre>.schema   (JSON con definicion del espacio)
  data/espacios/<nombre>.json     (JSON Lines, un registro por linea)

Escritura atomica: archivo temporal .tmp, luego rename al destino.
Al iniciar el motor se reconstruye el AVL desde los archivos (O(n log n)).


6. ARBOL AVL

Invariante: |altura(izq) - altura(der)| <= 1 en todo nodo.
Factor de balance (fb) en [-1, 0, 1].

Rotaciones:
  LL: fb > 1, fb(izq) >= 0  -> rotacion derecha
  LR: fb > 1, fb(izq) < 0   -> rotacion izq en hijo + rotacion der
  RR: fb < -1, fb(der) <= 0 -> rotacion izquierda
  RL: fb < -1, fb(der) > 0  -> rotacion der en hijo + rotacion izq

Complejidad: O(log n) para buscar, insertar y eliminar.
Altura maxima: 1.44 * log2(n + 2) - 0.328.


7. COMPLEJIDAD

Operacion                     Tiempo
Buscar por clave              O(log n)
Insertar                      O(log n)
Eliminar                      O(log n)
Rango en PK                   O(log n + k)
Inorden                       Theta(n)
Buscar campo no indexado       O(n)
Cargar desde disco             O(n log n)
Guardar a disco                O(n)

Recurrencia busqueda: T(n) = T(n/2) + c => Theta(log n) (Teorema Maestro)
Recurrencia carga: T(n) = sum(log i) = Theta(n log n) (Stirling)


8. GUI (EXTRA)

6 pestanas en Swing:
  Espacios    - crear y eliminar espacios
  Registros   - insertar, ver, actualizar, eliminar
  Busquedas   - buscar por campo, rango BETWEEN
  Arbol AVL   - visualizacion grafica con Graphics2D
  Consola     - terminal integrada con historial
  Ayuda       - comandos y ejemplos

Atajos: Ctrl+S guardar, Ctrl+Q salir, F5 refrescar.
