package motor.almacenamiento;

import motor.arbol.ArbolAVL;
import motor.arbol.IIndiceArbol;
import motor.catalogo.Esquema;

import java.util.*;

// Espacio de almacenamiento que guarda registros indexados por clave primaria
// en un Árbol AVL. Las claves se normalizan a minúsculas y sin comillas
// para búsqueda uniforme.
//
// Operaciones sobre clave primaria: O(log n) usando el AVL.
// Operaciones sobre otros campos: O(n) por escaneo lineal.
public class Espacio implements IEspacio {

    private final Esquema esquema;                                      // definicion de campos y clave
    private final IIndiceArbol<String, Registro> indice;                // AVL que mapea claves String a objetos Registro

    public Espacio(Esquema esquema) {
        this.esquema = esquema;
        this.indice  = new ArbolAVL<>();
    }

    // Insercion

    @Override
    public void insertar(Registro r) {
        esquema.validar(r.getCampos());                                // verifica que tenga clave primaria
        String clave = obtenerClave(r);                                // extraer y normalizar la clave
        if (indice.contiene(clave))                                    // no permitir duplicados
            throw new IllegalStateException("Clave duplicada: " + clave);
        indice.insertar(clave, r);                                     // O(log n) en el AVL
    }

    // Busqueda

    @Override
    public Registro buscarPorClave(String clave) {
        return indice.buscar(norm(clave));                             // O(log n), normalizar antes de buscar
    }

    @Override
    public List<Registro> todos() {
        return indice.inorden();                                       // recorrido inorden = ordenado por clave
    }

    // busca registros que cumplan campo op valor.
    // Si la condicion es igualdad sobre la clave primaria se usa el AVL
    // con complejidad O(log n). En cualquier otro caso se recorre todo O(n).
    @Override
    public List<Registro> buscarPorCondicion(String campo, String op, String valorStr) {
        List<Registro> resultado = new ArrayList<>();

        // atajo: igualdad sobre clave primaria usa el AVL directamente
        if (campo.equals(esquema.getCampoClave()) && (op.equals("=") || op.equals("=="))) {
            Registro r = buscarPorClave(valorStr);
            if (r != null) resultado.add(r);
            return resultado;
        }

        // escaneo lineal sobre todos los registros
        Object valorObj = parsearValorCondicion(valorStr);
        for (Registro r : indice.inorden()) {
            if (r.cumple(campo, op, valorObj)) resultado.add(r);
        }
        return resultado;
    }

    // busca registros en el rango [desde, hasta].
    // Si el campo es la clave primaria se usa el metodo rango del AVL
    // con complejidad O(log n + k). Si es otro campo se hace escaneo lineal O(n).
    @Override
    public List<Registro> buscarRango(String campo, String desde, String hasta) {
        if (campo.equals(esquema.getCampoClave())) {
            return indice.rango(norm(desde), norm(hasta));               // AVL directo O(log n + k)
        }

        Object desdeObj = parsearValorCondicion(desde);
        Object hastaObj = parsearValorCondicion(hasta);
        List<Registro> resultado = new ArrayList<>();
        for (Registro r : indice.inorden()) {
            if (r.cumpleRango(campo, desdeObj, hastaObj)) resultado.add(r);
        }
        return resultado;
    }

    // Actualizacion

    @Override
    public int actualizar(String campoCond, String op, String valorCond,
                          Map<String, Object> nuevos) {
        if (nuevos.containsKey(esquema.getCampoClave())) {
            throw new IllegalArgumentException("No se puede actualizar la clave primaria");
        }
        List<Registro> afectados = buscarPorCondicion(campoCond, op, valorCond);
        for (Registro r : afectados) r.actualizar(nuevos);
        return afectados.size();
    }

    // Eliminacion

    @Override
    public boolean eliminarPorClave(String clave) {
        return indice.eliminar(norm(clave));                                     // O(log n)
    }

    @Override
    public int eliminarPorCondicion(String campo, String op, String valorStr) {
        List<Registro> afectados = buscarPorCondicion(campo, op, valorStr);
        int count = 0;
        for (Registro r : afectados) {
            if (indice.eliminar(obtenerClave(r))) count++;                       // eliminar del AVL
        }
        return count;
    }

    // Getters

    @Override public Esquema getEsquema()        { return esquema; }
    @Override public int getTamano()             { return indice.getTamano(); }
    @Override public int getAltura()             { return indice.getAltura(); }
    @Override public String visualizarArbol()    { return indice.visualizar(); }

    // Auxiliares

    // extrae la clave primaria del registro y la normaliza
    private String obtenerClave(Registro r) {
        Object v = r.get(esquema.getCampoClave());
        if (v == null)
            throw new IllegalArgumentException("Falta la clave primaria en el registro");
        return norm(v.toString());
    }

    // normaliza un string de clave: minusculas, sin comillas alrededor
    private String norm(String s) {
        return s.trim().toLowerCase().replaceAll("^['\"]|['\"]$", "");
    }

    // parsea un valor usado en condiciones WHERE, infiriendo el tipo
    private Object parsearValorCondicion(String v) {
        v = v.trim().replaceAll("^['\"]|['\"]$", "");                 // quitar comillas
        try { return Integer.parseInt(v); }   catch (NumberFormatException ignored) {}
        try { return Double.parseDouble(v); } catch (NumberFormatException ignored) {}
        if (v.equalsIgnoreCase("true"))  return Boolean.TRUE;
        if (v.equalsIgnoreCase("false")) return Boolean.FALSE;
        return v;                                                      // texto
    }
}
