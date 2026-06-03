package motor.catalogo;

// Tipos de datos soportados para los campos de un esquema.
// parse() acepta alias comunes en SQL (INT, INTEGER, TEXT, etc).
public enum TipoDato {
    ENTERO, TEXTO, REAL, BOOLEAN;

    // convierte un string a TipoDato aceptando alias comunes
    public static TipoDato parse(String s) {
        switch (s.toUpperCase().trim()) {
            case "ENTERO": case "INT": case "INTEGER": return ENTERO;
            case "TEXTO":  case "TEXT": case "STRING":  return TEXTO;
            case "REAL":   case "FLOAT": case "DOUBLE": return REAL;
            case "BOOLEAN": case "BOOL":                return BOOLEAN;
            default: throw new IllegalArgumentException("Tipo desconocido: " + s);
        }
    }
}
