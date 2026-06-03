package motor.catalogo;

// Representa un campo de un esquema: nombre, tipo de dato y si es clave primaria.
// Los nombres se almacenan en minusculas para busqueda uniforme.
public class Campo {

    private final String nombre;
    private final TipoDato tipo;
    private final boolean clavePrimaria;

    public Campo(String nombre, TipoDato tipo, boolean clavePrimaria) {
        this.nombre        = nombre.trim().toLowerCase();              // normalizar a minusculas
        this.tipo          = tipo;
        this.clavePrimaria = clavePrimaria;
    }

    public String getNombre()        { return nombre; }
    public TipoDato getTipo()        { return tipo; }
    public boolean isClavePrimaria() { return clavePrimaria; }

    // convierte un string al tipo Java que corresponde segun el tipo del campo.
    // Por ejemplo, "123" con tipo ENTERO devuelve el Integer 123.
    public Object parsearValor(String valor) {
        if (valor == null || valor.equalsIgnoreCase("null")) return null;
        String v = valor.trim().replaceAll("^['\"]|['\"]$", "");      // quitar comillas
        try {
            switch (tipo) {
                case ENTERO:  return Integer.parseInt(v);
                case REAL:    return Double.parseDouble(v);
                case BOOLEAN: return Boolean.parseBoolean(v);
                default:      return v;                                // TEXTO: devolver sin cambios
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                "'" + v + "' no es valido para tipo " + tipo + " en campo " + nombre);
        }
    }

    @Override
    public String toString() {
        return nombre + ":" + tipo + (clavePrimaria ? " PK" : "");     // ej: "id:ENTERO PK"
    }
}
