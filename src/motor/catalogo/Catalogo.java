package motor.catalogo;

import java.util.LinkedHashMap;
import java.util.Map;

// Registro central de todos los esquemas activos en el motor.
// Guarda todos los esquemas activos en un mapa donde la llave es el nombre
// del espacio y el valor es el objeto Esquema. El acceso es O(1).
// Usado por GestorEspacios y por la capa de persistencia.
public class Catalogo {

    private final Map<String, Esquema> espacios = new LinkedHashMap<>(); // mantiene orden de insercion

    // agrega un esquema al catalogo
    public void registrar(Esquema e) {
        espacios.put(e.getNombre(), e);
    }

    // elimina un esquema por nombre
    public void eliminar(String nombre) {
        espacios.remove(nombre.toLowerCase());
    }
}
