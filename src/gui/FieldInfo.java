package gui;

// Encapsula nombre, tipo y si es clave primaria de un campo.
// Usado por los paneles de la GUI para construir formularios.
class FieldInfo {
    final String name;
    final String type;
    final boolean primaryKey;

    FieldInfo(String name, String type, boolean primaryKey) {
        this.name = name;
        this.type = type;
        this.primaryKey = primaryKey;
    }
}
