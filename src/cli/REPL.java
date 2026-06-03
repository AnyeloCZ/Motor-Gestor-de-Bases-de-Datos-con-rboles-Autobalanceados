package cli;

import java.io.*;
import java.util.*;
import motor.almacenamiento.Registro;
import motor.parser.IMotor;
import motor.parser.Motor;
import motor.parser.ResultadoComando;

// Interfaz de linea de comandos (REPL: Read-Eval-Print Loop).
// Soporta dos modos:
//   - Interactivo: lee comandos del teclado hasta EXIT/QUIT.
//   - Script: ejecuta un archivo .sql con comandos separados por punto y coma.
//
// Uso:
//   java -cp bin cli.REPL                            # modo interactivo
//   java -cp bin cli.REPL --script data/script.sql   # ejecutar script
//   java -cp bin cli.REPL --data ruta/datos          # cambiar directorio de datos
public class REPL {

    private final IMotor motor;                                     // el motor que ejecuta los comandos
    private static final String PROMPT  = "motorbd> ";
    private static final String VERSION = "2.0";

    public REPL(String dirDatos) {
        this.motor = new Motor(dirDatos);                          // inicializar motor con ruta de datos
    }

    // Modo interactivo

    public void iniciarInteractivo() {
        imprimirBanner();                                          // mostrar logo y comandos disponibles
        Scanner sc = new Scanner(System.in);
        StringBuilder buffer = new StringBuilder();                // acumulador para comandos multilinea

        while (true) {
            System.out.print(buffer.length() == 0 ? PROMPT : "     -> "); // prompt normal o continuacion
            if (!sc.hasNextLine()) break;                          // EOF: salir
            String linea = sc.nextLine();

            buffer.append(" ").append(linea.trim());               // acumular en buffer

            // si la linea no termina con ; y no es EXIT/QUIT, seguir acumulando (multilinea)
            if (!linea.trim().endsWith(";")) {
                String up = linea.trim().toUpperCase();
                if (!up.equals("EXIT") && !up.equals("QUIT")) continue;
            }

            // comando completo: limpiar punto y coma, ejecutar e imprimir resultado
            String cmd = buffer.toString().trim();
            if (cmd.endsWith(";")) cmd = cmd.substring(0, cmd.length() - 1).trim(); // quitar ;
            buffer.setLength(0);                                   // reiniciar buffer

            if (cmd.isEmpty()) continue;                           // ignorar linea vacia

            ResultadoComando res = motor.ejecutar(cmd);            // delegar al motor
            imprimirResultado(res);
            if (res.isSalir()) break;                              // EXIT o QUIT
        }
        sc.close();
    }

    // Ejecucion de scripts SQL

    public void ejecutarScript(String rutaArchivo) {
        System.out.println("Cargando script: " + rutaArchivo + "\n");
        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            StringBuilder buffer = new StringBuilder();
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty() || linea.startsWith("--")) continue;  // ignorar comentarios y vacios
                buffer.append(" ").append(linea);
                if (linea.endsWith(";")) {                               // comando completo
                    String cmd = buffer.toString().trim();
                    if (cmd.endsWith(";")) cmd = cmd.substring(0, cmd.length() - 1).trim();
                    buffer.setLength(0);
                    System.out.println(PROMPT + cmd);                   // mostrar comando ejecutado
                    ResultadoComando res = motor.ejecutar(cmd);
                    imprimirResultado(res);
                    System.out.println();
                    if (res.isSalir()) break;
                }
            }
        } catch (IOException e) {
            System.err.println("No se pudo leer el script: " + e.getMessage());
        }
        System.out.println("Script terminado.");
    }

    // Impresion de resultados

    private void imprimirResultado(ResultadoComando res) {
        switch (res.getTipo()) {
            case OK:
                if (res.getMensaje() != null && !res.getMensaje().isEmpty())
                    System.out.println(res.getMensaje());              // mensaje simple
                break;
            case FILAS:
                imprimirTabla(res.getFilas());                         // SELECT: imprimir tabla
                System.out.println(res.getAfectados() + " resultado(s)");
                break;
            case ERROR:
                System.out.println("ERROR: " + res.getMensaje());      // error en rojo conceptual
                break;
            case SALIR:
                System.out.println(res.getMensaje());                  // "Hasta luego."
                break;
        }
    }

    // imprime una lista de registros como tabla formateada con columnas auto-ajustadas
    private void imprimirTabla(List<Registro> filas) {
        if (filas == null || filas.isEmpty()) {
            System.out.println("(sin resultados)");
            return;
        }

        // paso 1: recolectar todas las columnas que aparecen en los registros
        LinkedHashSet<String> cols = new LinkedHashSet<>();
        for (Registro r : filas) cols.addAll(r.getCampos().keySet());

        // paso 2: calcular ancho de cada columna = max(nombre columna, contenido)
        Map<String, Integer> ancho = new LinkedHashMap<>();
        for (String k : cols) ancho.put(k, k.length());               // ancho minimo = nombre de columna
        for (Registro r : filas) {
            for (String k : cols) {
                Object v = r.get(k);
                int len = v == null ? 4 : v.toString().length();      // el texto "null" ocupa 4 caracteres
                if (len > ancho.get(k)) ancho.put(k, len);            // actualizar maximo
            }
        }

        // paso 3: imprimir cabecera
        StringBuilder cab = new StringBuilder();
        StringBuilder sep = new StringBuilder();
        for (String k : cols) {
            int w = ancho.get(k);
            cab.append(String.format("%-" + w + "s  ", k));           // nombre de columna
            sep.append(String.format("%-" + w + "s  ", "-".repeat(w))); // linea separadora
        }
        System.out.println(cab.toString().stripTrailing());
        System.out.println(sep.toString().stripTrailing());

        // paso 4: imprimir cada fila
        for (Registro r : filas) {
            StringBuilder fila = new StringBuilder();
            for (String k : cols) {
                Object v = r.get(k);
                fila.append(String.format("%-" + ancho.get(k) + "s  ", v == null ? "null" : v));
            }
            System.out.println(fila.toString().stripTrailing());
        }
    }

    // Banner de bienvenida

    private void imprimirBanner() {
        System.out.println("  Motor BD v" + VERSION + "  -  Árbol AVL Autobalanceado\n");
        System.out.println("  Ciencias de la Computación I  Grupo 020-83");
        System.out.println("  Casas . Yanez . Herrera  -  2026-I\n");
        System.out.println("Comandos: CREATE SPACE, INSERT, SELECT, UPDATE, DELETE, TREE, SAVE, EXIT");
        System.out.println();
        ResultadoComando r = motor.ejecutar("SHOW SPACES");           // mostrar espacios al iniciar
        if (r.getMensaje() != null) System.out.println(r.getMensaje());
        System.out.println();
    }

    // Punto de entrada del programa

    public static void main(String[] args) {
        String dirDatos = "data";                                     // directorio por defecto
        String script   = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--data")   && i + 1 < args.length) dirDatos = args[i + 1];
            if (args[i].equals("--script") && i + 1 < args.length) script   = args[i + 1];
        }

        REPL repl = new REPL(dirDatos);
        if (script != null) repl.ejecutarScript(script);               // modo script
        else                repl.iniciarInteractivo();                  // modo interactivo
    }
}
