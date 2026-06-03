package motor.catalogo;

import java.util.*;

// Define la estructura de un espacio de almacenamiento.
// Puede tener esquema fijo (relacional, con campos definidos) o esquema libre
// (no relacional, cada registro puede tener campos arbitrarios).
//
// En ambos casos se define una clave primaria:
//   - En esquema fijo: el primer campo marcado PK, o el primer campo si no hay PK explicito.
//   - En esquema libre: el campo "id".
public class Esquema {

    private final String nombre;          // nombre del espacio (minusculas)
    private final boolean libre;          // true = sin campos fijos, false = esquema definido
    private final List<Campo> campos;     // lista inmutable de campos (vacia si es libre)
    private final String campoClave;      // nombre del campo que actua como clave primaria

    // constructor para esquema fijo (con campos definidos)
    public Esquema(String nombre, List<Campo> campos) {
        this.nombre = nombre.trim().toLowerCase();
        this.libre  = false;
        this.campos = Collections.unmodifiableList(new ArrayList<>(campos)); // copia inmutable

        // buscar cual campo es la clave primaria
        String pk = null;
        for (Campo c : campos) {
            if (c.isClavePrimaria()) { pk = c.getNombre(); break; }
        }
        // si nadie marco PK, el primer campo toma ese rol
        this.campoClave = (pk != null) ? pk : (campos.isEmpty() ? "id" : campos.get(0).getNombre());
    }

    // constructor para esquema libre (sin campos fijos)
    public Esquema(String nombre) {
        this.nombre     = nombre.trim().toLowerCase();
        this.libre      = true;
        this.campos     = Collections.emptyList();                    // sin campos fijos
        this.campoClave = "id";                                       // en esquema libre, PK = "id"
    }

    public String getNombre()        { return nombre; }
    public boolean isLibre()         { return libre; }
    public List<Campo> getCampos()   { return campos; }
    public String getCampoClave()    { return campoClave; }

    // busca un campo por nombre en el esquema, null si no existe
    public Campo getCampo(String nombre) {
        for (Campo c : campos)
            if (c.getNombre().equals(nombre.toLowerCase())) return c;
        return null;
    }

    // verifica que un mapa de campos contenga al menos la clave primaria
    // lanza excepcion si falta, se usa antes de insertar
    public void validar(Map<String, Object> registro) {
        if (!registro.containsKey(campoClave))
            throw new IllegalArgumentException("Falta la clave primaria: " + campoClave);
    }

    @Override
    public String toString() {
        if (libre) return nombre + " [libre]";
        StringBuilder sb = new StringBuilder(nombre + " (");
        for (int i = 0; i < campos.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(campos.get(i));
        }
        return sb.append(")").toString();
    }
}
