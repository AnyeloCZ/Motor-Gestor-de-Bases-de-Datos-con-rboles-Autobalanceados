package motor.almacenamiento;

import java.util.List;
import java.util.Map;
import motor.catalogo.Esquema;

// Operaciones que todo espacio de almacenamiento debe ofrecer.
// La unica implementacion concreta es Espacio, que usa ArbolAVL internamente.
//
// Complejidad de operaciones sobre clave primaria: O(log n).
// Operaciones por condicion sobre campos no indexados: O(n) (escaneo lineal).
public interface IEspacio {

    void insertar(Registro r);                                       // O(log n), lanza excepcion si clave duplicada
    Registro buscarPorClave(String clave);                           // O(log n)
    List<Registro> todos();                                          // O(n), devuelve registros en orden de clave

    // O(log n) si la condicion es sobre clave primaria con "=", si no O(n)
    List<Registro> buscarPorCondicion(String campo, String op, String valor);

    // O(log n + k) si sobre clave primaria, O(n) si no
    List<Registro> buscarRango(String campo, String desde, String hasta);

    // actualiza campos de registros que cumplan condicion, devuelve cuantos se afectaron
    int actualizar(String campoCond, String op, String valorCond, Map<String, Object> nuevos);

    // elimina por clave exacta, O(log n)
    boolean eliminarPorClave(String clave);

    // elimina todos los registros que cumplan la condicion, devuelve la cantidad
    int eliminarPorCondicion(String campo, String op, String valor);

    Esquema getEsquema();                                            // definicion del espacio
    int getTamano();                                                 // cantidad de registros
    int getAltura();                                                 // altura del arbol AVL (para verificar balance)
    String visualizarArbol();                                        // representacion del arbol en texto
}
