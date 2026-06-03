package motor.arbol;

import java.util.List;

// Operaciones básicas de un índice basado en árbol binario de búsqueda
// autobalanceado. Complejidad de inserción, búsqueda y eliminación: O(log n).
// Recorridos inorden y por rango: O(n) en el peor caso.
public interface IIndiceArbol<K extends Comparable<K>, V> {

    void insertar(K clave, V valor);                   // inserta o actualiza por clave
    V buscar(K clave);                                  // busca por clave, null si no esta
    boolean eliminar(K clave);                          // elimina por clave, false si no existe
    boolean contiene(K clave);                          // true si la clave esta en el arbol

    List<V> inorden();                                  // devuelve todos los valores ordenados por clave (menor a mayor)
    List<V> rango(K desde, K hasta);                    // valores con clave entre [desde, hasta]

    int getTamano();                                    // cantidad de nodos en el arbol
    int getAltura();                                    // altura del arbol (vacio = 0)
    String visualizar();                                // representacion en texto del arbol para depurar
}
