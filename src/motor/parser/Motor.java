package motor.parser;

import motor.almacenamiento.*;
import motor.catalogo.*;
import motor.persistencia.GestorPersistencia;
import motor.persistencia.IPersistencia;

import java.util.*;

// Motor principal: recibe comandos en texto, los parsea y coordina las capas
// de almacenamiento (GestorEspacios), catálogo de esquemas y persistencia.
//
// Comandos soportados:
//   CREATE SPACE nombre [(campo tipo [PK], ...)]    -- crear tabla/espacio
//   DROP SPACE nombre                               -- eliminar espacio
//   SHOW SPACES                                     -- listar espacios activos
//   DESC nombre                                     -- describir esquema
//   INSERT INTO nombre VALUES (v1,...)               -- insertar registro
//   INSERT INTO nombre (c1,...) VALUES (v1,...)      -- insertar con campos explicitos
//   SELECT * FROM nombre [WHERE campo op valor]      -- buscar registros
//   SELECT * FROM nombre WHERE campo BETWEEN v1 AND v2 -- busqueda por rango
//   UPDATE nombre SET c=v [,c=v] WHERE campo op valor -- actualizar
//   DELETE FROM nombre WHERE campo op valor           -- eliminar por condicion
//   DELETE FROM nombre ALL                            -- eliminar todo
//   TREE nombre                                       -- visualizar arbol AVL
//   SAVE nombre | SAVE ALL                            -- guardar a disco
//   EXIT | QUIT                                       -- salir
public class Motor implements IMotor {

    private final Catalogo catalogo;                    // registro de esquemas activos
    private final GestorEspacios gestorEspacios;        // controla espacios en memoria
    private final IPersistencia persistencia;           // guarda/carga desde disco

    // constructor con persistencia por defecto (escribe en dirDatos/)
    public Motor(String dirDatos) {
        this.catalogo       = new Catalogo();
        this.gestorEspacios = new GestorEspacios(catalogo);
        this.persistencia   = new GestorPersistencia(dirDatos);
        persistencia.cargarTodo(gestorEspacios, catalogo);   // restaurar datos del disco
    }

    // constructor usado en tests para inyectar una persistencia mock
    public Motor(String dirDatos, IPersistencia persistencia) {
        this.catalogo       = new Catalogo();
        this.gestorEspacios = new GestorEspacios(catalogo);
        this.persistencia   = persistencia;
        persistencia.cargarTodo(gestorEspacios, catalogo);
    }

    // Enrutador principal

    @Override
    public ResultadoComando ejecutar(String comando) {
        if (comando == null) return ResultadoComando.error("Comando vacio.");
        comando = comando.trim();
        if (comando.isEmpty() || comando.startsWith("--")) return ResultadoComando.ok("");   // comentario SQL

        String upper = comando.toUpperCase();               // normalizar a mayusculas para comparar
        try {
            if (upper.startsWith("CREATE SPACE"))  return crearEspacio(comando);
            if (upper.startsWith("DROP SPACE"))    return eliminarEspacio(comando);
            if (upper.startsWith("SHOW SPACES"))   return listarEspacios();
            if (upper.startsWith("DESC "))         return describir(comando);
            if (upper.startsWith("INSERT INTO"))   return insertar(comando);
            if (upper.startsWith("SELECT"))        return seleccionar(comando);
            if (upper.startsWith("UPDATE"))        return actualizar(comando);
            if (upper.startsWith("DELETE FROM"))   return eliminarRegistros(comando);
            if (upper.startsWith("TREE"))          return verArbol(comando);
            if (upper.startsWith("SAVE ALL"))      return guardarTodo();
            if (upper.startsWith("SAVE "))         return guardar(comando);
            if (upper.equals("EXIT") || upper.equals("QUIT")) return ResultadoComando.salir();
            return ResultadoComando.error("Comando no reconocido: " + comando);
        } catch (Exception e) {
            return ResultadoComando.error(e.getMessage());    // cualquier error lo atrapamos y devolvemos
        }
    }

    // DDL: crear espacio

    private ResultadoComando crearEspacio(String cmd) {
        String resto = cmd.substring(12).trim();                    // quitar "CREATE SPACE"
        int paren = resto.indexOf('(');                             // buscar definicion de campos
        String nombre;
        Esquema esquema;

        if (paren == -1) {
            nombre  = resto.trim();
            esquema = new Esquema(nombre);                          // espacio libre, sin campos fijos
        } else {
            nombre = resto.substring(0, paren).trim();              // nombre antes del parentesis
            String defCampos = resto.substring(paren + 1, resto.lastIndexOf(')')).trim();  // lo que esta dentro de ()
            esquema = new Esquema(nombre, parsearCampos(defCampos)); // espacio con esquema fijo
        }

        gestorEspacios.crear(esquema);                               // crear en memoria
        persistencia.guardar(gestorEspacios.obtener(nombre));        // persistir inmediatamente
        return ResultadoComando.ok("Espacio '" + nombre + "' creado. Clave: " + esquema.getCampoClave());
    }

    // parsea "id ENTERO PK, nombre TEXTO, promedio REAL" a lista de objetos Campo
    private List<Campo> parsearCampos(String def) {
        List<Campo> lista = new ArrayList<>();
        for (String p : def.split(",")) {                            // separar por comas
            String[] t = p.trim().split("\\s+");                     // dividir por espacios
            if (t.length < 2) continue;                              // necesita al menos nombre y tipo
            boolean pk = t.length >= 3 && t[2].equalsIgnoreCase("PK");
            lista.add(new Campo(t[0], TipoDato.parse(t[1]), pk));
        }
        // si ningun campo se marco como PK, el primero toma ese rol
        boolean tienePK = lista.stream().anyMatch(Campo::isClavePrimaria);
        if (!tienePK && !lista.isEmpty()) {
            Campo c = lista.get(0);
            lista.set(0, new Campo(c.getNombre(), c.getTipo(), true));
        }
        return lista;
    }

    // DDL: eliminar espacio

    private ResultadoComando eliminarEspacio(String cmd) {
        String nombre = cmd.substring(10).trim();                    // quitar "DROP SPACE"
        if (!gestorEspacios.existe(nombre))
            return ResultadoComando.error("El espacio '" + nombre + "' no existe.");
        gestorEspacios.eliminar(nombre);                             // eliminar de memoria
        persistencia.eliminar(nombre.toLowerCase());                 // eliminar archivos del disco
        return ResultadoComando.ok("Espacio '" + nombre + "' eliminado.");
    }

    // SHOW SPACES

    private ResultadoComando listarEspacios() {
        Collection<IEspacio> lista = gestorEspacios.todos();         // todos los espacios activos
        if (lista.isEmpty()) return ResultadoComando.ok("(Sin espacios creados)");

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-20s %-8s %-10s %-10s%n", "ESPACIO", "TIPO", "REGISTROS", "ALTURA AVL"));
        sb.append("-".repeat(52)).append("\n");
        for (IEspacio e : lista) {
            sb.append(String.format("%-20s %-8s %-10d %-10d%n",
                e.getEsquema().getNombre(),
                e.getEsquema().isLibre() ? "libre" : "fijo",         // tipo de esquema
                e.getTamano(),                                        // cuantos registros tiene
                e.getAltura()));                                      // altura del arbol AVL
        }
        return ResultadoComando.ok(sb.toString().trim());
    }

    // DESC

    private ResultadoComando describir(String cmd) {
        IEspacio esp = requerir(cmd.substring(5).trim());             // nombre despues de "DESC "
        Esquema esq  = esp.getEsquema();
        StringBuilder sb = new StringBuilder();
        sb.append("Espacio:    ").append(esq.getNombre()).append("\n");
        sb.append("Tipo:       ").append(esq.isLibre() ? "libre (no relacional)" : "fijo (relacional)").append("\n");
        sb.append("Clave PK:   ").append(esq.getCampoClave()).append("\n");
        sb.append("Registros:  ").append(esp.getTamano()).append("\n");
        sb.append("Altura AVL: ").append(esp.getAltura()).append("\n");

        if (!esq.isLibre()) {                                         // si es esquema fijo, mostrar campos
            sb.append("\nCampos:\n");
            sb.append(String.format("  %-15s %-10s %-5s%n", "NOMBRE", "TIPO", "PK"));
            sb.append("  ").append("-".repeat(32)).append("\n");
            for (Campo c : esq.getCampos())
                sb.append(String.format("  %-15s %-10s %-5s%n",
                    c.getNombre(), c.getTipo(), c.isClavePrimaria() ? "SI" : ""));
        }
        return ResultadoComando.ok(sb.toString().trim());
    }

    // INSERT

    private ResultadoComando insertar(String cmd) {
        String resto = cmd.substring(11).trim();                     // quitar "INSERT INTO"
        int valIdx = resto.toUpperCase().indexOf("VALUES");
        if (valIdx == -1) return ResultadoComando.error("Falta VALUES en INSERT.");

        String cabecera = resto.substring(0, valIdx).trim();         // "nombre" o "nombre (c1, c2)"
        String valsPart = resto.substring(valIdx + 6).trim();        // "(v1, v2, ...)"

        String nombre;
        List<String> camposOrden = null;
        int paren = cabecera.indexOf('(');
        if (paren == -1) {
            nombre = cabecera.trim();                                // sin lista de campos
        } else {
            nombre = cabecera.substring(0, paren).trim();            // nombre antes de (
            camposOrden = splitCsv(cabecera.substring(paren + 1, cabecera.lastIndexOf(')')).trim());
        }

        IEspacio esp = requerir(nombre);
        Esquema  esq = esp.getEsquema();

        if (!valsPart.startsWith("(") || !valsPart.contains(")"))
            return ResultadoComando.error("Formato VALUES incorrecto. Esperado: (v1, v2, ...)");

        List<String> valores = splitCsv(valsPart.substring(1, valsPart.lastIndexOf(')')).trim());

        Registro r = new Registro();
        if (camposOrden != null) {
            // INSERT INTO t (c1, c2) VALUES (v1, v2)
            for (int i = 0; i < camposOrden.size() && i < valores.size(); i++) {
                String campo = camposOrden.get(i).trim();
                Object val = esq.isLibre()
                    ? parseLibre(valores.get(i).trim())              // espacio libre: inferir tipo
                    : parseConEsquema(esq, campo, valores.get(i).trim()); // espacio fijo: usar tipo del campo
                r.set(campo, val);
            }
        } else {
            // INSERT INTO t VALUES (v1, v2, ...) -- solo para esquemas fijos
            List<Campo> campos = esq.getCampos();
            if (esq.isLibre())
                return ResultadoComando.error(
                    "Espacio libre requiere lista de campos: INSERT INTO " + nombre + " (c1,...) VALUES (...)" );
            if (campos.size() != valores.size())
                return ResultadoComando.error(
                    "Se esperaban " + campos.size() + " valores, llegaron " + valores.size() + ".");
            for (int i = 0; i < campos.size(); i++)
                r.set(campos.get(i).getNombre(), campos.get(i).parsearValor(valores.get(i).trim()));
        }

        esp.insertar(r);                                             // insertar en el AVL, O(log n)
        persistencia.guardar(esp);                                   // persistir cambios
        return ResultadoComando.ok("1 registro insertado en '" + nombre + "'.");
    }

    // SELECT

    private ResultadoComando seleccionar(String cmd) {
        String upper = cmd.toUpperCase();
        int fromIdx = upper.indexOf("FROM");                         // obligatorio en SELECT
        if (fromIdx == -1) return ResultadoComando.error("Falta FROM en SELECT.");

        String restoFrom = cmd.substring(fromIdx + 4).trim();        // texto despues de FROM
        int whereIdx = restoFrom.toUpperCase().indexOf(" WHERE ");
        String nombre;
        String wherePart = null;

        if (whereIdx == -1) {
            nombre = restoFrom.trim();                               // sin WHERE: traer todo
        } else {
            nombre    = restoFrom.substring(0, whereIdx).trim();     // nombre antes de WHERE
            wherePart = restoFrom.substring(whereIdx + 7).trim();    // condicion despues de WHERE
        }

        IEspacio esp = requerir(nombre);
        List<Registro> resultado;

        if (wherePart == null) {
            resultado = esp.todos();                                 // SELECT sin WHERE trae todos los registros
        } else {
            int betweenIdx = wherePart.toUpperCase().indexOf(" BETWEEN ");
            if (betweenIdx != -1) {
                // SELECT * FROM t WHERE campo BETWEEN v1 AND v2
                String campo  = wherePart.substring(0, betweenIdx).trim().toLowerCase();
                String[] partes = wherePart.substring(betweenIdx + 9).split("(?i)\\s+AND\\s+");
                if (partes.length != 2)
                    return ResultadoComando.error("BETWEEN requiere: campo BETWEEN v1 AND v2");
                resultado = esp.buscarRango(campo, limpiar(partes[0]), limpiar(partes[1]));
            } else {
                // SELECT * FROM t WHERE campo op valor
                String[] cond = parsearCondicion(wherePart);          // [campo, op, valor]
                if (cond == null)
                    return ResultadoComando.error("Condicion WHERE invalida: " + wherePart);
                resultado = esp.buscarPorCondicion(cond[0], cond[1], cond[2]);
            }
        }

        return ResultadoComando.filas(resultado);
    }

    // UPDATE

    private ResultadoComando actualizar(String cmd) {
        String upper    = cmd.toUpperCase();
        int setIdx      = upper.indexOf(" SET ");
        int whereIdx    = upper.indexOf(" WHERE ");
        if (setIdx   == -1) return ResultadoComando.error("Falta SET en UPDATE.");
        if (whereIdx == -1) return ResultadoComando.error("Falta WHERE en UPDATE.");

        String nombre    = cmd.substring(6, setIdx).trim();           // "t" en "UPDATE t SET ..."
        String setPart   = cmd.substring(setIdx + 5, whereIdx).trim();// "c=v, c2=v2"
        String wherePart = cmd.substring(whereIdx + 7).trim();        // "campo op valor"

        IEspacio esp  = requerir(nombre);
        String[] cond = parsearCondicion(wherePart);
        if (cond == null) return ResultadoComando.error("Condicion WHERE invalida.");

        Map<String, Object> nuevos = parsearSet(setPart, esp);        // parsear campo=valor,...
        int n = esp.actualizar(cond[0], cond[1], cond[2], nuevos);
        if (n > 0) persistencia.guardar(esp);                        // solo guardar si se modifico algo
        return ResultadoComando.afectados(n, n + " registro(s) actualizado(s) en '" + nombre + "'.");
    }

    // DELETE

    private ResultadoComando eliminarRegistros(String cmd) {
        String upper    = cmd.toUpperCase();
        int fromIdx     = upper.indexOf("FROM");
        String restoFrom = cmd.substring(fromIdx + 4).trim();         // texto despues de FROM
        int whereIdx    = restoFrom.toUpperCase().indexOf(" WHERE ");

        String nombre;
        String wherePart;

        if (whereIdx == -1) {
            String[] partes = restoFrom.split("\\s+", 2);
            nombre    = partes[0];
            wherePart = partes.length > 1 ? partes[1].trim() : "";
        } else {
            nombre    = restoFrom.substring(0, whereIdx).trim();
            wherePart = restoFrom.substring(whereIdx + 7).trim();
        }

        IEspacio esp = requerir(nombre);

        if (wherePart.isEmpty() || wherePart.equalsIgnoreCase("ALL")) {
            // DELETE FROM t ALL: elimina todos los registros del espacio
            int total = esp.getTamano();
            for (Registro r : esp.todos()) {                         // recorrer todos en orden
                Object k = r.get(esp.getEsquema().getCampoClave()); // sacar clave primaria
                if (k != null) esp.eliminarPorClave(k.toString());   // eliminar del AVL
            }
            persistencia.guardar(esp);
            return ResultadoComando.afectados(total, total + " registro(s) eliminado(s) de '" + nombre + "'.");
        }

        // DELETE FROM t WHERE campo op valor
        String[] cond = parsearCondicion(wherePart);
        if (cond == null) return ResultadoComando.error("Condicion WHERE invalida.");
        int n = esp.eliminarPorCondicion(cond[0], cond[1], cond[2]);
        if (n > 0) persistencia.guardar(esp);
        return ResultadoComando.afectados(n, n + " registro(s) eliminado(s) de '" + nombre + "'.");
    }

    // TREE (visualizar arbol)

    private ResultadoComando verArbol(String cmd) {
        IEspacio esp = requerir(cmd.substring(4).trim());             // nombre despues de "TREE"
        return ResultadoComando.ok("Arbol AVL de '" + esp.getEsquema().getNombre() + "':\n"
                + esp.visualizarArbol());
    }

    // SAVE

    private ResultadoComando guardar(String cmd) {
        IEspacio esp = requerir(cmd.substring(4).trim());             // "SAVE nombre"
        persistencia.guardar(esp);
        return ResultadoComando.ok("'" + esp.getEsquema().getNombre() + "' guardado en disco.");
    }

    private ResultadoComando guardarTodo() {
        int n = 0;
        for (IEspacio e : gestorEspacios.todos()) { persistencia.guardar(e); n++; }
        return ResultadoComando.ok(n + " espacio(s) guardado(s).");
    }

    // Metodos auxiliares

    // busca un espacio por nombre, lanza excepcion si no existe
    private IEspacio requerir(String nombre) {
        IEspacio e = gestorEspacios.obtener(nombre.trim());
        if (e == null)
            throw new IllegalArgumentException("El espacio '" + nombre.trim() + "' no existe.");
        return e;
    }

    // parsea "campo op valor" devolviendo [campo, operador, valor]
    // prueba los operadores de dos caracteres primero para no romper ">=" con ">"
    private String[] parsearCondicion(String where) {
        for (String op : new String[]{"<=", ">=", "!=", "<>", "=", "<", ">"}) {
            int idx = where.indexOf(op);
            if (idx > 0) {
                String campo = where.substring(0, idx).trim().toLowerCase();
                String valor = where.substring(idx + op.length()).trim();
                return new String[]{campo, op, limpiar(valor)};      // limpiar quita comillas
            }
        }
        return null;
    }

    // parsea "c=v, c2=v2" a mapa {c: v, c2: v2}
    private Map<String, Object> parsearSet(String setPart, IEspacio esp) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (String asig : setPart.split(",")) {
            int eq = asig.indexOf('=');
            if (eq == -1) continue;
            String campo = asig.substring(0, eq).trim().toLowerCase();
            String valor = limpiar(asig.substring(eq + 1).trim());
            Esquema esq  = esp.getEsquema();
            if (!esq.isLibre()) {
                Campo c = esq.getCampo(campo);                       // para esquema fijo, parsear con tipo
                if (c != null) { map.put(campo, c.parsearValor(valor)); continue; }
            }
            map.put(campo, parseLibre(valor));                       // espacio libre: inferir tipo
        }
        return map;
    }

    // divide una lista separada por comas respetando comillas.
    // Por ejemplo, "Ana, 'Gomez, Juan'" se divide en dos elementos:
    // "Ana" y "'Gomez, Juan'" (la coma dentro de comillas no cuenta).
    private List<String> splitCsv(String s) {
        List<String> lista = new ArrayList<>();
        boolean inStr = false;                                       // dentro de comillas?
        int inicio = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'' || c == '"') inStr = !inStr;               // alternar bandera con comillas
            if (c == ',' && !inStr) {                                // coma fuera de comillas = separador
                lista.add(s.substring(inicio, i).trim());
                inicio = i + 1;
            }
        }
        lista.add(s.substring(inicio).trim());                       // ultimo elemento
        return lista;
    }

    // intenta convertir un string a entero, real, booleano o lo deja como texto
    private Object parseLibre(String v) {
        v = limpiar(v);
        try { return Integer.parseInt(v); }   catch (NumberFormatException ignored) {}
        try { return Double.parseDouble(v); } catch (NumberFormatException ignored) {}
        if (v.equalsIgnoreCase("true"))  return Boolean.TRUE;
        if (v.equalsIgnoreCase("false")) return Boolean.FALSE;
        return v;                                                    // si no es numero ni booleano, es texto
    }

    // parsea un valor usando el tipo declarado en el esquema del campo
    private Object parseConEsquema(Esquema esq, String campo, String valor) {
        Campo c = esq.getCampo(campo);
        return c != null ? c.parsearValor(valor) : parseLibre(valor);
    }

    // quita comillas simples o dobles que rodean un string
    private String limpiar(String s) {
        return s.trim().replaceAll("^['\"]|['\"]$", "");
    }
}
