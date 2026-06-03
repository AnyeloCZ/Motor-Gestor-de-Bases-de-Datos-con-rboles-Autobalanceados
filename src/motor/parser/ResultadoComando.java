package motor.parser;

import motor.almacenamiento.Registro;
import java.util.List;

// Envuelve el resultado de ejecutar un comando.
// Puede ser: mensaje simple (OK), filas de datos, error, o salir del programa.
// La CLI/GUI solo lee esta clase, nunca toca el modelo directamente.
public class ResultadoComando {

    public enum Tipo { OK, FILAS, ERROR, SALIR }

    private final Tipo tipo;
    private final String mensaje;
    private final List<Registro> filas;
    private final int afectados;           // cantidad de registros afectados (insert/update/delete)

    private ResultadoComando(Tipo tipo, String mensaje, List<Registro> filas, int afectados) {
        this.tipo      = tipo;
        this.mensaje   = mensaje;
        this.filas     = filas;
        this.afectados = afectados;
    }

    // fabricas estaticas para crear resultados rapidamente

    public static ResultadoComando ok(String msg) {
        return new ResultadoComando(Tipo.OK, msg, null, 0);
    }

    public static ResultadoComando filas(List<Registro> f) {
        return new ResultadoComando(Tipo.FILAS, null, f, f.size());
    }

    public static ResultadoComando error(String msg) {
        return new ResultadoComando(Tipo.ERROR, msg, null, 0);
    }

    public static ResultadoComando afectados(int n, String msg) {
        return new ResultadoComando(Tipo.OK, msg, null, n);
    }

    public static ResultadoComando salir() {
        return new ResultadoComando(Tipo.SALIR, "Hasta luego.", null, 0);
    }

    // getters

    public Tipo getTipo()            { return tipo; }
    public String getMensaje()       { return mensaje; }
    public List<Registro> getFilas() { return filas; }
    public int getAfectados()        { return afectados; }
    public boolean isError()         { return tipo == Tipo.ERROR; }
    public boolean isSalir()         { return tipo == Tipo.SALIR; }
}
