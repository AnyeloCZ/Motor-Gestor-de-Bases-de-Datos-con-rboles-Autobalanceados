package motor.arbol;

import java.util.*;

// Árbol AVL genérico (Adelson-Velsky y Landis).
// Se autobalancea para garantizar que la altura siempre sea O(log n).
// La diferencia de altura entre hijos izquierdo y derecho de cualquier nodo
// nunca supera 1 en valor absoluto (factor de balance en [-1, 0, 1]).
//
// Complejidad:
//   insertar, buscar, eliminar, contiene: O(log n)
//   inorden, inordenEntradas, rango:      O(n)
//   getTamano, getAltura, estaVacio:      O(1)
public class ArbolAVL<K extends Comparable<K>, V> implements IIndiceArbol<K, V> {

    private NodoAVL<K, V> raiz;   // raiz del arbol
    private int tamano;           // cantidad de nodos

    // Metodos auxiliares del AVL

    // devuelve la altura del nodo (0 si es null)
    private int altura(NodoAVL<K, V> n) {
        return n == null ? 0 : n.altura;
    }

    // factor de balance: altura izquierda menos altura derecha
    // si el resultado es mayor que 1 hay desbalance a la izquierda
    // si el resultado es menor que -1 hay desbalance a la derecha
    private int fb(NodoAVL<K, V> n) {
        return n == null ? 0 : altura(n.izq) - altura(n.der);
    }

    // recalcula la altura de un nodo como 1 + max(altura hijos)
    private void recalcularAltura(NodoAVL<K, V> n) {
        n.altura = 1 + Math.max(altura(n.izq), altura(n.der));
    }

    // Rotaciones del AVL

    // rotacion simple a la derecha (caso LL).
    // El hijo izquierdo del nodo (x) sube a la raiz del subarbol,
    // el nodo original (y) pasa a ser hijo derecho de x,
    // y el antiguo hijo derecho de x (B) pasa a ser hijo izquierdo de y.
    private NodoAVL<K, V> rotDer(NodoAVL<K, V> y) {
        NodoAVL<K, V> x = y.izq;
        NodoAVL<K, V> t = x.der;
        x.der = y;
        y.izq = t;
        recalcularAltura(y);
        recalcularAltura(x);
        return x;   // x es la nueva raiz del subarbol
    }

    // rotacion simple a la izquierda (caso RR).
    // El hijo derecho del nodo (y) sube a la raiz del subarbol,
    // el nodo original (x) pasa a ser hijo izquierdo de y,
    // y el antiguo hijo izquierdo de y (B) pasa a ser hijo derecho de x.
    private NodoAVL<K, V> rotIzq(NodoAVL<K, V> x) {
        NodoAVL<K, V> y = x.der;
        NodoAVL<K, V> t = y.izq;
        y.izq = x;
        x.der = t;
        recalcularAltura(x);
        recalcularAltura(y);
        return y;   // y es la nueva raiz del subarbol
    }

    // detecta y corrige desbalance en un nodo aplicando la rotacion correspondiente
    private NodoAVL<K, V> balancear(NodoAVL<K, V> n) {
        recalcularAltura(n);
        int b = fb(n);

        // caso LL: desbalance izquierdo, hijo izquierdo con fb mayor o igual a cero.
        // Se resuelve con rotacion simple derecha.
        if (b > 1 && fb(n.izq) >= 0)  return rotDer(n);

        // caso LR: desbalance izquierdo, hijo izquierdo con fb negativo.
        // Se resuelve con rotacion izquierda sobre el hijo y luego derecha sobre el nodo.
        if (b > 1 && fb(n.izq) < 0)   { n.izq = rotIzq(n.izq); return rotDer(n); }

        // caso RR: desbalance derecho, hijo derecho con fb menor o igual a cero.
        // Se resuelve con rotacion simple izquierda.
        if (b < -1 && fb(n.der) <= 0) return rotIzq(n);

        // caso RL: desbalance derecho, hijo derecho con fb positivo.
        // Se resuelve con rotacion derecha sobre el hijo y luego izquierda sobre el nodo.
        if (b < -1 && fb(n.der) > 0)  { n.der = rotDer(n.der); return rotIzq(n); }

        return n; // ya esta balanceado
    }

    // Insercion

    @Override
    public void insertar(K clave, V valor) {
        boolean[] nuevo = {false};                     // bandera para saber si se inserto o actualizo
        raiz = insertar(raiz, clave, valor, nuevo);
        if (nuevo[0]) tamano++;                        // solo incrementar si fue insercion nueva
    }

    private NodoAVL<K, V> insertar(NodoAVL<K, V> n, K clave, V valor, boolean[] nuevo) {
        if (n == null) {
            nuevo[0] = true;                           // se creo un nodo nuevo
            return new NodoAVL<>(clave, valor);
        }
        int c = clave.compareTo(n.clave);              // comparar clave con la del nodo actual
        if      (c < 0) n.izq = insertar(n.izq, clave, valor, nuevo);  // va a la izquierda
        else if (c > 0) n.der = insertar(n.der, clave, valor, nuevo);  // va a la derecha
        else            n.valor = valor;               // clave repetida: actualizar valor
        return balancear(n);                           // rebalancear al subir por la recursion
    }

    // Busqueda

    @Override
    public V buscar(K clave) {
        NodoAVL<K, V> n = buscarNodo(raiz, clave);    // O(log n)
        return n != null ? n.valor : null;
    }

    private NodoAVL<K, V> buscarNodo(NodoAVL<K, V> n, K clave) {
        if (n == null) return null;                    // no encontrado
        int c = clave.compareTo(n.clave);
        if (c < 0) return buscarNodo(n.izq, clave);    // buscar por izquierda
        if (c > 0) return buscarNodo(n.der, clave);    // buscar por derecha
        return n;                                      // encontrado
    }

    @Override
    public boolean contiene(K clave) {
        return buscar(clave) != null;
    }

    // Eliminacion

    @Override
    public boolean eliminar(K clave) {
        if (!contiene(clave)) return false;            // no existe, nada que hacer
        raiz = eliminar(raiz, clave);
        tamano--;
        return true;
    }

    private NodoAVL<K, V> eliminar(NodoAVL<K, V> n, K clave) {
        if (n == null) return null;
        int c = clave.compareTo(n.clave);
        if      (c < 0) n.izq = eliminar(n.izq, clave);  // buscar a eliminar por izquierda
        else if (c > 0) n.der = eliminar(n.der, clave);  // buscar a eliminar por derecha
        else {
            // nodo encontrado. Si tiene 0 o 1 hijo, devolver el hijo que tenga
            if (n.izq == null) return n.der;
            if (n.der == null) return n.izq;

            // dos hijos: reemplazar con el sucesor inorden (el menor del subarbol derecho)
            NodoAVL<K, V> suc = minimo(n.der);
            n.clave = suc.clave;                     // copiar clave del sucesor
            n.valor = suc.valor;                     // copiar valor del sucesor
            n.der   = eliminar(n.der, suc.clave);     // eliminar el sucesor de su posicion original
        }
        return balancear(n);                           // rebalancear
    }

    // encuentra el nodo con la clave minima en un subarbol (el que esta mas a la izquierda)
    private NodoAVL<K, V> minimo(NodoAVL<K, V> n) {
        while (n.izq != null) n = n.izq;
        return n;
    }

    // Recorridos

    @Override
    public List<V> inorden() {
        List<V> lista = new ArrayList<>();
        inorden(raiz, lista);
        return lista;
    }

    private void inorden(NodoAVL<K, V> n, List<V> lista) {
        if (n == null) return;
        inorden(n.izq, lista);
        lista.add(n.valor);
        inorden(n.der, lista);
    }

    // Consulta por rango

    @Override
    public List<V> rango(K desde, K hasta) {
        List<V> res = new ArrayList<>();
        rango(raiz, desde, hasta, res);
        return res;                                    // valores con clave en [desde, hasta]
    }

    private void rango(NodoAVL<K, V> n, K desde, K hasta, List<V> res) {
        if (n == null) return;
        int cd = desde.compareTo(n.clave);             // comparar limite inferior con clave actual
        int ch = hasta.compareTo(n.clave);             // comparar limite superior con clave actual

        if (cd < 0) rango(n.izq, desde, hasta, res);   // si desde < clave, explorar izquierda
        if (cd <= 0 && ch >= 0) res.add(n.valor);      // si clave en rango, agregar
        if (ch > 0) rango(n.der, desde, hasta, res);   // si hasta > clave, explorar derecha
        // O(k + log n) donde k es la cantidad de elementos en el rango
    }

    // Getters

    @Override public int getTamano()     { return tamano; }
    @Override public int getAltura()     { return altura(raiz); }

    // Visualizacion para depuracion

    @Override
    public String visualizar() {
        if (raiz == null) return "(arbol vacio)";
        StringBuilder sb = new StringBuilder();
        visualizar(raiz, sb, "", "");
        return sb.toString();
    }

    // imprime el arbol de forma jerarquica mostrando clave, altura y factor de balance
    private void visualizar(NodoAVL<K, V> n, StringBuilder sb,
                             String prefix, String childPrefix) {
        if (n == null) return;
        sb.append(prefix)
          .append("[").append(n.clave).append("]")
          .append("  h=").append(n.altura)              // altura del subarbol
          .append(" fb=").append(fb(n))                  // factor de balance
          .append("\n");

        if (n.der != null || n.izq != null) {
            visualizar(n.der, sb, childPrefix + "├─R─ ", childPrefix + "│    ");
            visualizar(n.izq, sb, childPrefix + "└─L─ ", childPrefix + "     ");
        }
    }
}
