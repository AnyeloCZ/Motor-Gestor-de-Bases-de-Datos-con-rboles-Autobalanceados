package motor.almacenamiento;

import motor.catalogo.Catalogo;
import motor.catalogo.Esquema;

import java.util.*;

// Administra los espacios de almacenamiento activos en memoria.
// Coordina la creación, eliminación y consulta de espacios.
// También mantiene sincronizado el catálogo de esquemas.
public class GestorEspacios {

    private final Map<String, IEspacio> espacios = new LinkedHashMap<>(); // asocia el nombre del espacio con su objeto
    private final Catalogo catalogo;                                      // referencia al catalogo central

    public GestorEspacios(Catalogo catalogo) {
        this.catalogo = catalogo;
    }

    // crea un nuevo espacio en memoria y lo registra en el catalogo
    public IEspacio crear(Esquema esquema) {
        String nombre = esquema.getNombre();
        if (espacios.containsKey(nombre))                                // no permitir duplicados
            throw new IllegalStateException("El espacio '" + nombre + "' ya existe.");
        IEspacio e = new Espacio(esquema);                               // espacio con AVL interno
        espacios.put(nombre, e);
        catalogo.registrar(esquema);                                     // sincronizar catalogo
        return e;
    }

    // elimina un espacio de memoria y del catalogo, devuelve false si no existe
    public boolean eliminar(String nombre) {
        nombre = nombre.toLowerCase();
        if (!espacios.containsKey(nombre)) return false;
        espacios.remove(nombre);
        catalogo.eliminar(nombre);
        return true;
    }

    // obtiene un espacio por nombre, null si no existe
    public IEspacio obtener(String nombre) {
        return espacios.get(nombre.toLowerCase());
    }

    // verifica si un espacio existe
    public boolean existe(String nombre) {
        return espacios.containsKey(nombre.toLowerCase());
    }

    // devuelve coleccion inmutable de todos los espacios activos
    public Collection<IEspacio> todos() {
        return Collections.unmodifiableCollection(espacios.values());
    }

    // usado por la capa de persistencia para cargar espacios restaurados del disco
    public void cargar(IEspacio e) {
        espacios.put(e.getEsquema().getNombre(), e);
    }
}
