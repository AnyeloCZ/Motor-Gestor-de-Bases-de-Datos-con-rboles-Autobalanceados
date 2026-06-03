package test;

import motor.parser.Motor;
import motor.parser.ResultadoComando;

import java.io.File;

// Benchmark empirico que mide el tiempo real de operaciones del motor
// con tamanos crecientes de datos (10, 100, 500, 1000, 5000 registros).
//
// Demuestra que:
// - La busqueda por clave primaria crece logaritmicamente O(log n)
// - La insercion masiva tiene costo O(n log n)
// - La altura del AVL se mantiene acotada por 1.44 * log2(n)
//
// Ejecutar: java -cp build/classes test.TestBenchmark
public class TestBenchmark {

    public static void main(String[] args) {
        System.out.println("BENCHMARK EMPIRICO - MOTOR BD AVL\n");
        System.out.println("Verifica que los tiempos crezcan segun la complejidad teorica.\n");

        String dir = "data_bench";
        int[] tamanos = {10, 100, 500, 1000, 5000};

        // encabezado de la tabla de resultados
        System.out.printf("%-8s %-10s %-10s %-10s %-10s %-10s %-15s%n",
                "n", "insert(s)", "buscar(s)", "inorden(s)", "altura", "log2(n)", "1.44*log2(n)");
        for (int n : tamanos) {
            new File(dir + "/esquemas").mkdirs();
            new File(dir + "/espacios").mkdirs();

            Motor m = new Motor(dir);

            // crear espacio de prueba
            m.ejecutar("CREATE SPACE bench (id ENTERO PK, nombre TEXTO, valor REAL, flag BOOLEAN, extra TEXTO)");

            // medir insercion de n registros
            long t0 = System.nanoTime();
            for (int i = 1; i <= n; i++) {
                m.ejecutar("INSERT INTO bench VALUES (" + i + ", 'registro_" + i + "', " + (i * 1.5) + ", " + (i % 2 == 0) + ", 'extra_" + i + "')");
            }
            long t1 = System.nanoTime();
            double tiempoInsercion = (t1 - t0) / 1_000_000_000.0;

            // medir busqueda por clave primaria (promedio de 100 busquedas)
            long t2 = System.nanoTime();
            for (int i = 0; i < 100; i++) {
                int id = 1 + (i % n);
                m.ejecutar("SELECT * FROM bench WHERE id = " + id);
            }
            long t3 = System.nanoTime();
            double tiempoBusqueda = (t3 - t2) / 1_000_000_000.0 / 100.0;

            // medir recorrido inorden
            long t4 = System.nanoTime();
            m.ejecutar("SELECT * FROM bench");
            long t5 = System.nanoTime();
            double tiempoInorden = (t5 - t4) / 1_000_000_000.0;

            // obtener altura del AVL
            ResultadoComando desc = m.ejecutar("DESC bench");
            int altura = extraerAltura(desc.getMensaje());

            double log2n = Math.log(n) / Math.log(2);
            double cotaTeorica = 1.44 * log2n + 1;

            System.out.printf("%-8d %-10.6f %-10.6f %-10.6f %-10d %-10.1f %-15.1f%n",
                    n, tiempoInsercion, tiempoBusqueda, tiempoInorden, altura, log2n, cotaTeorica);

            // limpiar para la siguiente iteracion
            borrarDir(new File(dir));
            new File(dir + "/esquemas").mkdirs();
            new File(dir + "/espacios").mkdirs();
        }

        System.out.println("\nInterpretacion:");
        System.out.println("  - El tiempo de busqueda por PK debe mantenerse casi constante");
        System.out.println("    aunque n crezca 500x (de 10 a 5000). Esto demuestra O(log n).");
        System.out.println("  - La altura del AVL nunca debe superar 1.44 * log2(n) + 1.");
        System.out.println("  - El tiempo de insercion crece levemente (O(log n) por operacion).");
        System.out.println("  - El tiempo de inorden crece proporcional a n (O(n)).");
        System.out.println();

        // limpiar
        borrarDir(new File(dir));
        System.out.println("Benchmark completado.");
    }

    private static int extraerAltura(String descMsg) {
        if (descMsg == null) return -1;
        for (String linea : descMsg.split("\\R")) {
            if (linea.contains("Altura AVL:")) {
                try {
                    return Integer.parseInt(linea.replaceAll("[^0-9]", "").trim());
                } catch (NumberFormatException ignored) {}
            }
        }
        return -1;
    }

    private static void borrarDir(File dir) {
        if (dir.isDirectory())
            for (File f : dir.listFiles()) borrarDir(f);
        dir.delete();
    }
}
