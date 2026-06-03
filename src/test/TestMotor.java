package test;

import motor.parser.Motor;
import motor.parser.ResultadoComando;

import java.io.File;

// Suite de pruebas manual (sin JUnit) para verificar todas las funcionalidades del motor.
// Cubre: creacion de esquemas, insercion, busqueda por clave/condicion/rango,
// actualizacion, eliminacion, persistencia en disco y recuperacion tras reinicio.
//
// Para ejecutar: java -cp bin test.TestMotor
//
// Las pruebas se ejecutan en un directorio temporal data_test/ que se limpia al final.
public class TestMotor {

    private static int ok   = 0;       // contador de pruebas exitosas
    private static int fail = 0;       // contador de pruebas fallidas

    public static void main(String[] args) {
        System.out.println("SUITE DE PRUEBAS - MOTOR BD AVL\n");

        // crear directorio temporal para pruebas (aislado de los datos reales)
        String dir = "data_test";
        new File(dir + "/esquemas").mkdirs();
        new File(dir + "/espacios").mkdirs();

        Motor m = new Motor(dir);

        // 1. Gestion de esquemas
        titulo("1. Gestion de esquemas");
        ok(m, "CREATE SPACE estudiantes (id ENTERO PK, nombre TEXTO, promedio REAL, activo BOOLEAN)", "creado");
        ok(m, "CREATE SPACE notas",                                       "creado");   // espacio libre
        ok(m, "SHOW SPACES",                                              "estudiantes");
        error(m, "CREATE SPACE estudiantes (id ENTERO PK)",               "ya existe");

        // 2. INSERT
        titulo("2. INSERT");
        ok(m, "INSERT INTO estudiantes VALUES (1, 'Ana Gomez', 4.5, true)",   "1 registro");
        ok(m, "INSERT INTO estudiantes VALUES (2, 'Carlos Ruiz', 3.8, true)", "1 registro");
        ok(m, "INSERT INTO estudiantes VALUES (3, 'Laura Torres', 4.1, false)","1 registro");
        ok(m, "INSERT INTO estudiantes VALUES (4, 'Miguel Herrera', 3.5, true)","1 registro");
        ok(m, "INSERT INTO estudiantes VALUES (5, 'Sofia Castro', 4.9, true)", "1 registro");
        error(m, "INSERT INTO estudiantes VALUES (1, 'Dup', 1.0, false)",      "duplicada"); // clave repetida
        // insertar en espacio libre
        ok(m, "INSERT INTO notas (id, materia, nota) VALUES ('n001', 'Calculo', 4.2)",   "1 registro");
        ok(m, "INSERT INTO notas (id, materia, nota) VALUES ('n002', 'Algoritmos', 3.9)","1 registro");

        // 3. SELECT basico
        titulo("3. SELECT");
        filas(m, "SELECT * FROM estudiantes",                                 5); // todos
        filas(m, "SELECT * FROM estudiantes WHERE id = 3",                    1); // por clave
        filas(m, "SELECT * FROM estudiantes WHERE promedio >= 4.0",            3); // por condicion numerica
        filas(m, "SELECT * FROM estudiantes WHERE activo = true",              4); // por booleano
        filas(m, "SELECT * FROM estudiantes WHERE nombre = 'Sofia Castro'",    1); // por texto exacto
        filas(m, "SELECT * FROM notas WHERE materia = 'Calculo'",              1); // espacio libre

        // 4. SELECT BETWEEN (rango)
        titulo("4. SELECT BETWEEN");
        filas(m, "SELECT * FROM estudiantes WHERE id BETWEEN 2 AND 4",          3); // rango sobre PK
        filas(m, "SELECT * FROM estudiantes WHERE id BETWEEN 1 AND 5",          5); // rango completo
        filas(m, "SELECT * FROM estudiantes WHERE id BETWEEN 6 AND 9",          0); // rango vacio
        filas(m, "SELECT * FROM estudiantes WHERE nombre BETWEEN 'Ana Gomez' AND 'Miguel Herrera'", 4);

        // 5. UPDATE
        titulo("5. UPDATE");
        afectados(m, "UPDATE estudiantes SET promedio=4.7 WHERE id = 2",           1);
        afectados(m, "UPDATE estudiantes SET activo=false WHERE promedio < 4.0",    1);
        error(m, "UPDATE estudiantes SET id = 10 WHERE nombre = 'Ana Gomez'",       "clave primaria"); // no se puede cambiar PK
        // verificar que el cambio realmente se aplico
        ResultadoComando r = m.ejecutar("SELECT * FROM estudiantes WHERE id = 2");
        assertTrue("Promedio actualizado a 4.7",
            r.getFilas().get(0).get("promedio").toString().equals("4.7"));

        // 6. DELETE
        titulo("6. DELETE");
        afectados(m, "DELETE FROM estudiantes WHERE id = 5",                  1); // eliminar por clave
        filas(m, "SELECT * FROM estudiantes",                                 4); // quedan 4
        afectados(m, "DELETE FROM notas WHERE materia = 'Calculo'",           1);
        filas(m, "SELECT * FROM notas",                                       1); // queda 1

        // 7. Ver arbol AVL
        titulo("7. Arbol AVL");
        ResultadoComando tree = m.ejecutar("TREE estudiantes");
        assertFalse("Arbol no esta vacio", tree.getMensaje().contains("vacio")); // debe mostrar estructura

        // 8. DESC
        titulo("8. DESC");
        ok(m, "DESC estudiantes", "estudiantes");                               // debe mostrar esquema

        // 9. Persistencia y recuperacion
        titulo("9. Persistencia");
        ok(m, "SAVE ALL", "guardado");                                         // guardar todo a disco
        // simular reinicio: crear nuevo motor que cargue del mismo directorio
        Motor m2 = new Motor(dir);
        filas(m2, "SELECT * FROM estudiantes", 4);                             // deben estar los 4 registros
        filas(m2, "SELECT * FROM notas",         1);
        System.out.println("  >> Datos recuperados correctamente tras reinicio.");

        // 10. DROP SPACE
        titulo("10. DROP SPACE");
        ok(m, "DROP SPACE notas", "eliminado");                                // eliminar espacio
        error(m, "SELECT * FROM notas", "no existe");                          // ya no debe existir

        // 11. DELETE ALL
        titulo("11. DELETE ALL");
        afectados(m, "DELETE FROM estudiantes ALL", 4);                        // borrar todo
        filas(m, "SELECT * FROM estudiantes", 0);                              // debe quedar vacio

        // limpiar carpeta temporal de pruebas
        borrarDir(new File(dir));

        // resumen final
        System.out.println("\n" + "=".repeat(45));
        System.out.printf("  %d prueba(s) OK    %d FALLIDA(S)%n", ok, fail);
        System.out.println("=".repeat(45));
        System.exit(fail > 0 ? 1 : 0);                                         // codigo de salida: 0 si todo OK
    }

    // Metodos auxiliares para las pruebas

    private static void titulo(String t) {
        System.out.println("\n" + t);
    }

    // ejecuta un comando y verifica que sea OK y contenga cierto texto
    private static void ok(Motor m, String cmd, String contiene) {
        ResultadoComando r = m.ejecutar(cmd);
        String msg = r.getMensaje() != null ? r.getMensaje().toLowerCase() : "";
        reportar(cmd, !r.isError() && msg.contains(contiene.toLowerCase()),
            r.isError() ? r.getMensaje() : null);
    }

    // ejecuta un comando y verifica que sea ERROR y contenga cierto texto
    private static void error(Motor m, String cmd, String contiene) {
        ResultadoComando r = m.ejecutar(cmd);
        String msg = r.getMensaje() != null ? r.getMensaje().toLowerCase() : "";
        reportar("(error esperado) " + cmd,
            r.isError() && msg.contains(contiene.toLowerCase()),
            !r.isError() ? "Se esperaba error, no lo hubo" : null);
    }

    // ejecuta un SELECT y verifica que devuelva la cantidad esperada de filas
    private static void filas(Motor m, String cmd, int esperadas) {
        ResultadoComando r = m.ejecutar(cmd);
        int obtenidas = r.getFilas() != null ? r.getFilas().size() : 0;
        boolean paso = !r.isError() && obtenidas == esperadas;
        reportar(cmd + " [filas=" + esperadas + "]", paso,
            paso ? null : "Obtenidas: " + obtenidas + (r.isError() ? " | " + r.getMensaje() : ""));
    }

    // ejecuta un UPDATE/DELETE y verifica que afecte la cantidad esperada de registros
    private static void afectados(Motor m, String cmd, int esperados) {
        ResultadoComando r = m.ejecutar(cmd);
        boolean paso = !r.isError() && r.getAfectados() == esperados;
        reportar(cmd + " [afectados=" + esperados + "]", paso,
            paso ? null : "Afectados: " + r.getAfectados());
    }

    private static void assertTrue(String desc, boolean cond) {
        reportar(desc, cond, cond ? null : "Condicion falsa");
    }

    private static void assertFalse(String desc, boolean cond) {
        reportar(desc, !cond, cond ? "Se esperaba false, fue true" : null);
    }

    // imprime [ OK ] o [FAIL] y actualiza contadores
    private static void reportar(String desc, boolean paso, String detalle) {
        System.out.printf("  %s %s%n", paso ? "[ OK ]" : "[FAIL]",
            desc.length() > 70 ? desc.substring(0, 70) + "..." : desc);
        if (!paso && detalle != null) System.out.println("         >> " + detalle);
        if (paso) ok++; else fail++;
    }

    // borra directorio recursivamente (para limpiar datos de prueba)
    private static void borrarDir(File dir) {
        if (dir.isDirectory())
            for (File f : dir.listFiles()) borrarDir(f);
        dir.delete();
    }
}
