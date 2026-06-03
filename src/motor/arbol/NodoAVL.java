package motor.arbol;

// Nodo del árbol AVL. Guarda clave, valor, hijos y altura del subárbol.
public class NodoAVL<K extends Comparable<K>, V> {
    K clave;               // clave por la que se ordena el arbol
    V valor;               // dato asociado a la clave
    NodoAVL<K, V> izq;     // hijo izquierdo
    NodoAVL<K, V> der;     // hijo derecho
    int altura;            // altura del subarbol con raiz en este nodo

    public NodoAVL(K clave, V valor) {
        this.clave  = clave;
        this.valor  = valor;
        this.altura = 1;   // un nodo recien creado es hoja, altura 1
    }
}
