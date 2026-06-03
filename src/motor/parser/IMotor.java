package motor.parser;

// Punto de entrada unico para ejecutar comandos.
// Implementado por Motor (logica real) y usado por la CLI y los tests.
public interface IMotor {
    ResultadoComando ejecutar(String comando);
}
