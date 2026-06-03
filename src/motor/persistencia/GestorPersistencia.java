package motor.persistencia;

import motor.almacenamiento.Espacio;
import motor.almacenamiento.GestorEspacios;
import motor.almacenamiento.IEspacio;
import motor.almacenamiento.Registro;
import motor.catalogo.Campo;
import motor.catalogo.Catalogo;
import motor.catalogo.Esquema;
import motor.catalogo.TipoDato;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

// Persistencia en disco con formato JSON.
// Cada espacio genera dos archivos: el .schema guarda la definición del
// espacio en una línea JSON, y el .json guarda los registros en formato
// JSON Lines (un registro por línea).
//
// Al iniciar el motor, cargarTodo() lee los .schema, reconstruye los espacios
// y repuebla el arbol AVL insertando los registros uno a uno.
//
// Usa escritura atomica: primero escribe a un archivo temporal (.tmp) y luego
// renombra al destino final, evitando corrupcion si el programa se interrumpe.
public class GestorPersistencia implements IPersistencia {

    private final String dirEsquemas;              // data/esquemas/
    private final String dirEspacios;              // data/espacios/

    public GestorPersistencia(String baseDir) {
        this.dirEsquemas = baseDir + "/esquemas";
        this.dirEspacios = baseDir + "/espacios";
        new File(dirEsquemas).mkdirs();            // crear carpetas si no existen
        new File(dirEspacios).mkdirs();
    }

    // Guardado

    @Override
    public void guardar(IEspacio espacio) {
        guardarEsquema(espacio.getEsquema());      // guardar definicion del espacio
        guardarRegistros(espacio);                 // guardar todos sus registros
    }

    // escribe el esquema como JSON en data/esquemas/<nombre>.schema
    private void guardarEsquema(Esquema e) {
        File target = new File(dirEsquemas, e.getNombre() + ".schema");
        File temp   = new File(dirEsquemas, e.getNombre() + ".schema.tmp");  // archivo temporal para escritura atomica
        try (BufferedWriter w = new BufferedWriter(new FileWriter(temp))) {
            w.write("{\"nombre\":\"" + e.getNombre() + "\"");
            w.write(",\"libre\":" + e.isLibre());
            w.write(",\"clave\":\"" + e.getCampoClave() + "\"");
            w.write(",\"campos\":[");
            List<Campo> campos = e.getCampos();
            for (int i = 0; i < campos.size(); i++) {
                Campo c = campos.get(i);
                if (i > 0) w.write(",");
                w.write("{\"n\":\"" + c.getNombre()
                        + "\",\"t\":\"" + c.getTipo()
                        + "\",\"pk\":" + c.isClavePrimaria() + "}");
            }
            w.write("]}");
            w.newLine();
        } catch (IOException ex) {
            System.err.println("[Persistencia] Error guardando esquema: " + ex.getMessage());
            return;
        }
        // renombrar el archivo temporal al archivo final de forma atomica
        try {
            Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            System.err.println("[Persistencia] No se pudo mover esquema temporal: " + ex.getMessage());
        }
    }

    // escribe cada registro como una linea JSON en data/espacios/<nombre>.json
    private void guardarRegistros(IEspacio espacio) {
        File target = new File(dirEspacios, espacio.getEsquema().getNombre() + ".json");
        File temp   = new File(dirEspacios, espacio.getEsquema().getNombre() + ".json.tmp");
        try (BufferedWriter w = new BufferedWriter(new FileWriter(temp))) {
            for (Registro r : espacio.todos()) {                       // recorrer todos en orden
                w.write(r.toJson());
                w.newLine();
            }
        } catch (IOException ex) {
            System.err.println("[Persistencia] Error guardando registros: " + ex.getMessage());
            return;
        }
        try {
            Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            System.err.println("[Persistencia] No se pudo mover registros temporal: " + ex.getMessage());
        }
    }

    // Carga

    // lee todos los archivos .schema de la carpeta de esquemas y reconstruye los espacios
    @Override
    public void cargarTodo(GestorEspacios gestor, Catalogo catalogo) {
        File dir = new File(dirEsquemas);
        File[] schemas = dir.listFiles((d, n) -> n.endsWith(".schema"));  // solo archivos .schema
        if (schemas == null) return;

        for (File sf : schemas) {
            try {
                Esquema esq = cargarEsquema(sf);                       // parsear JSON del esquema
                if (esq == null) continue;
                Espacio esp = new Espacio(esq);                        // crear espacio vacio con AVL
                cargarRegistros(esp);                                  // insertar registros uno a uno en el AVL
                gestor.cargar(esp);                                    // registrar en gestor
                catalogo.registrar(esq);                               // registrar en catalogo
            } catch (Exception ex) {
                System.err.println("[Persistencia] No se pudo cargar " + sf.getName()
                        + ": " + ex.getMessage());
            }
        }
    }

    // parsea un archivo .schema a un objeto Esquema
    private Esquema cargarEsquema(File f) throws IOException {
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            String linea = r.readLine();                                // el esquema es una sola linea JSON
            if (linea == null || linea.trim().isEmpty()) return null;

            String nombre = extraer(linea, "nombre");                  // extraer campo "nombre"
            boolean libre = "true".equals(extraer(linea, "libre"));    // extraer campo "libre"

            if (libre) return new Esquema(nombre);                     // espacio sin esquema fijo

            // parsear el arreglo "campos": [{"n":"...", "t":"...", "pk":...}, ...]
            List<Campo> campos = new ArrayList<>();
            int idx = linea.indexOf("\"campos\":[");
            if (idx != -1) {
                String rest = linea.substring(idx + 10);               // texto despues de "campos":[
                for (String parte : rest.split("\\},\\{")) {           // separar objetos por },{
                    parte = parte.replaceAll("[\\[\\]{}]", "").trim(); // limpiar corchetes y llaves
                    String n2 = extraerSimple(parte, "n");             // nombre del campo
                    String t  = extraerSimple(parte, "t");             // tipo del campo
                    String pk = extraerSimple(parte, "pk");            // es clave primaria?
                    if (n2 != null && t != null)
                        campos.add(new Campo(n2, TipoDato.valueOf(t), "true".equals(pk)));
                }
            }
            return new Esquema(nombre, campos);
        }
    }

    // lee el archivo .json del espacio e inserta cada registro en el AVL
    private void cargarRegistros(Espacio espacio) {
        File f = new File(dirEspacios, espacio.getEsquema().getNombre() + ".json");
        if (!f.exists()) return;
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            String linea;
            while ((linea = r.readLine()) != null) {                    // leer linea por linea (JSON Lines)
                linea = linea.trim();
                if (linea.isEmpty()) continue;
                try {
                    espacio.insertar(Registro.fromJson(linea));        // parsear JSON e insertar en AVL
                } catch (Exception ex) {
                    System.err.println("[Persistencia] Registro omitido: " + ex.getMessage());
                }
            }
        } catch (IOException ex) {
            System.err.println("[Persistencia] Error leyendo registros: " + ex.getMessage());
        }
    }

    // Eliminacion

    @Override
    public void eliminar(String nombre) {
        new File(dirEsquemas, nombre.toLowerCase() + ".schema").delete(); // borrar .schema
        new File(dirEspacios, nombre.toLowerCase() + ".json").delete();   // borrar .json
    }

    // Parseo manual de JSON (sin librerias externas)

    // extrae el valor de una clave en un JSON simple. Soporta strings y numeros/booleanos.
    // Por ejemplo, dado {"nombre":"pepe"} y la clave "nombre", devuelve "pepe".
    private String extraer(String json, String clave) {
        String key = "\"" + clave + "\":";
        int idx = json.indexOf(key);
        if (idx == -1) return null;
        int start = idx + key.length();
        if (start >= json.length()) return null;
        char c = json.charAt(start);
        if (c == '"') {                                               // el valor es un string entre comillas
            int end = json.indexOf('"', start + 1);
            return end == -1 ? null : json.substring(start + 1, end);
        }
        // el valor es un numero o booleano (sin comillas)
        int end = start;
        while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
        return json.substring(start, end).trim();
    }

    private String extraerSimple(String s, String clave) {
        return extraer(s, clave);
    }
}
