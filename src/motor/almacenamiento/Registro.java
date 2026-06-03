package motor.almacenamiento;

import java.util.*;

// Representa una fila o registro. Internamente es un mapa que asocia
// el nombre de cada campo (en minusculas) con su valor.
// Los nombres de campo se normalizan a minusculas para busqueda case-insensitive.
// Soporta serializacion a JSON para persistencia.
public class Registro {

    private final Map<String, Object> campos;

    public Registro() {
        this.campos = new LinkedHashMap<>();
    }

    public void set(String campo, Object valor) {
        campos.put(campo.toLowerCase(), valor);
    }

    // obtiene el valor de un campo, null si no existe
    public Object get(String campo) {
        return campos.get(campo.toLowerCase());
    }

    public Map<String, Object> getCampos() {
        return Collections.unmodifiableMap(campos);
    }

    // actualiza varios campos a la vez sobrescribiendo los existentes
    public void actualizar(Map<String, Object> nuevos) {
        for (Map.Entry<String, Object> e : nuevos.entrySet())
            campos.put(e.getKey().toLowerCase(), e.getValue());
    }

    // Serializacion JSON manual (sin librerias externas)

    // convierte el registro a JSON: {"campo1": valor1, "campo2": valor2, ...}
    public String toJson() {
        StringBuilder sb = new StringBuilder("{");
        boolean primero = true;
        for (Map.Entry<String, Object> e : campos.entrySet()) {
            if (!primero) sb.append(",");
            primero = false;
            sb.append("\"").append(e.getKey()).append("\":");
            Object v = e.getValue();
            if (v == null)
                sb.append("null");
            else if (v instanceof String)
                sb.append("\"").append(((String) v).replace("\"", "\\\"")).append("\""); // escapar comillas
            else
                sb.append(v);                                        // numero o booleano se escribe directo
        }
        return sb.append("}").toString();
    }

    // reconstruye un registro desde JSON: {"a": 1, "b": "hola"}
    public static Registro fromJson(String json) {
        Registro r = new Registro();
        String s = json.trim();
        if (s.startsWith("{")) s = s.substring(1);                   // quitar {
        if (s.endsWith("}"))   s = s.substring(0, s.length() - 1);  // quitar }
        if (s.trim().isEmpty()) return r;

        for (String token : splitJson(s)) {                          // dividir en pares clave:valor
            token = token.trim();
            if (token.isEmpty()) continue;
            int sep = token.indexOf("\":");                          // buscar separador ": entre comillas
            if (sep == -1) continue;
            String clave = token.substring(1, sep).trim();           // quitar la primera comilla
            String valor = token.substring(sep + 2).trim();          // lo que sigue despues de ":
            r.set(clave, parseValor(valor));
        }
        return r;
    }

    // divide un string JSON en tokens por comas, respetando anidamiento de {} y []
    private static List<String> splitJson(String s) {
        List<String> partes = new ArrayList<>();
        int depth = 0;                                               // nivel de anidamiento
        boolean inStr = false;                                       // dentro de string?
        int inicio = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' && (i == 0 || s.charAt(i - 1) != '\\')) inStr = !inStr; // alternar con comillas no escapadas
            if (!inStr) {
                if (c == '{' || c == '[') depth++;                   // entrar a objeto/array anidado
                if (c == '}' || c == ']') depth--;
                if (c == ',' && depth == 0) {                        // coma en nivel raiz: separa pares
                    partes.add(s.substring(inicio, i));
                    inicio = i + 1;
                }
            }
        }
        partes.add(s.substring(inicio));                             // ultimo token
        return partes;
    }

    // convierte un string que representa un valor JSON a su tipo Java
    private static Object parseValor(String v) {
        v = v.trim();
        if (v.equals("null"))  return null;
        if (v.equals("true"))  return Boolean.TRUE;
        if (v.equals("false")) return Boolean.FALSE;
        if (v.startsWith("\"") && v.endsWith("\""))
            return v.substring(1, v.length() - 1).replace("\\\"", "\""); // string: quitar comillas
        try { return Integer.parseInt(v); }  catch (NumberFormatException ignored) {}
        try { return Double.parseDouble(v); } catch (NumberFormatException ignored) {}
        return v;
    }

    // Evaluacion de condiciones WHERE

    // verifica si el campo cumple la condicion: campo operador valorBuscado
    public boolean cumple(String campo, String operador, Object valorBuscado) {
        Object actual = get(campo);
        if (actual == null) return false;                            // campo no existe, no cumple

        // si ambos son numeros, comparar numericamente
        if (actual instanceof Number && valorBuscado instanceof Number) {
            double a = ((Number) actual).doubleValue();
            double b = ((Number) valorBuscado).doubleValue();
            switch (operador) {
                case "=": case "==": return a == b;
                case "!=": case "<>": return a != b;
                case "<":  return a <  b;
                case "<=": return a <= b;
                case ">":  return a >  b;
                case ">=": return a >= b;
            }
        }

        // si no son numeros (o son mixtos), comparar como texto
        String sa = actual.toString();
        String sb = valorBuscado == null ? "null" : valorBuscado.toString();
        int cmp = sa.compareTo(sb);                                  // compareTo para orden lexicografico
        switch (operador) {
            case "=": case "==": return cmp == 0;
            case "!=": case "<>": return cmp != 0;
            case "<":  return cmp <  0;
            case "<=": return cmp <= 0;
            case ">":  return cmp >  0;
            case ">=": return cmp >= 0;
            default:   return false;
        }
    }

    // verifica si el campo esta en el rango [desde, hasta]
    public boolean cumpleRango(String campo, Object desde, Object hasta) {
        Object actual = get(campo);
        if (actual == null) return false;

        // comparacion numerica si todos son numeros
        if (actual instanceof Number && desde instanceof Number && hasta instanceof Number) {
            double a = ((Number) actual).doubleValue();
            double d = ((Number) desde).doubleValue();
            double h = ((Number) hasta).doubleValue();
            return a >= d && a <= h;
        }

        // comparacion lexicografica como fallback
        String sa = actual.toString();
        String sd = desde == null ? "" : desde.toString();
        String sh = hasta == null ? "" : hasta.toString();
        return sa.compareTo(sd) >= 0 && sa.compareTo(sh) <= 0;
    }

    @Override
    public String toString() { return toJson(); }
}
