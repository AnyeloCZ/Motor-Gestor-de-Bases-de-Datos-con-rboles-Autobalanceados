package motor.persistencia;

import motor.almacenamiento.GestorEspacios;
import motor.almacenamiento.IEspacio;
import motor.catalogo.Catalogo;

// Define las operaciones de persistencia: guardar espacio, cargar todo, eliminar espacio del disco.
// La implementacion concreta (GestorPersistencia) usa archivos JSON.
public interface IPersistencia {

    // guarda esquema y registros de un espacio a disco
    void guardar(IEspacio espacio);

    // carga todos los espacios desde disco y los inyecta en gestor y catalogo
    void cargarTodo(GestorEspacios gestor, Catalogo catalogo);

    // elimina los archivos de un espacio del disco
    void eliminar(String nombreEspacio);
}
