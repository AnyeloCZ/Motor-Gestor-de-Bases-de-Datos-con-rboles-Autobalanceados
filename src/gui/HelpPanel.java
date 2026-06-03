package gui;

import javax.swing.*;
import java.awt.*;

// Panel de ayuda con descripcion de cada pestana, ejemplos de comandos
// y referencia rapida de tipos de datos soportados.
class HelpPanel extends JPanel {
    HelpPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JTextArea help = new JTextArea();
        help.setEditable(false);
        help.setLineWrap(true);
        help.setWrapStyleWord(true);
        help.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        help.setBackground(new Color(252, 252, 255));

        help.setText(
                "  MOTOR BD AVL - Ayuda de uso\n\n"
            + "Esta interfaz gráfica es una capa visual sobre el motor\n"
            + "de base de datos. Todos los cambios se persisten en disco\n"
            + "automáticamente al insertar, actualizar o eliminar.\n\n"

            + "PESTAÑAS:\n\n"

            + "  Espacios:\n"
            + "  - Crear espacios con campos definidos (tipo + PK).\n"
            + "  - Eliminar espacios (borra registros y archivos).\n"
            + "  - Lista de espacios activos con estadísticas.\n\n"

            + "  Registros:\n"
            + "  - Insertar: llenar formulario y presionar Insertar.\n"
            + "  - Actualizar: seleccionar fila en tabla, modificar\n"
            + "    valores en formulario y presionar Actualizar.\n"
            + "  - Eliminar: seleccionar fila y presionar Eliminar.\n"
            + "  - La clave primaria NO se puede modificar.\n\n"

            + "  Búsquedas:\n"
            + "  - Buscar por campo con operadores: =, !=, <, <=, >, >=\n"
            + "  - Buscar por PK: atajo para búsqueda exacta por clave.\n"
            + "  - Buscar rango: usa BETWEEN para intervalos.\n\n"

            + "  Árbol AVL:\n"
            + "  - Visualización gráfica del árbol AVL con nodos.\n"
            + "  - Raíz en azul, nodo buscado en amarillo.\n"
            + "  - Recorrido inorden mostrado en panel lateral.\n"
            + "  - Opción compacto para reducir espacio entre nodos.\n\n"

            + "  Consola:\n"
            + "  - Ejecute comandos SQL directamente.\n"
            + "  - Historial con flechas arriba/abajo.\n"
            + "  - Ctrl+Enter para ejecutar.\n"
            + "  - Botones rápidos para comandos frecuentes.\n\n"

            + "TIPOS DE DATOS:\n"
            + "  ENTERO  : números enteros (Integer)\n"
            + "  TEXTO   : cadenas de texto (String)\n"
            + "  REAL    : números con decimales (Double)\n"
            + "  BOOLEAN : verdadero/falso (true/false)\n\n"

            + "EJEMPLOS DE COMANDOS:\n"
            + "  CREATE SPACE estudiantes (id ENTERO PK, nombre TEXTO,\n"
            + "                          promedio REAL, activo BOOLEAN)\n"
            + "  INSERT INTO estudiantes VALUES (1, 'Ana', 4.5, true)\n"
            + "  SELECT * FROM estudiantes\n"
            + "  SELECT * FROM estudiantes WHERE promedio >= 4.0\n"
            + "  SELECT * FROM estudiantes WHERE id BETWEEN 1 AND 10\n"
            + "  UPDATE estudiantes SET promedio=5.0 WHERE id = 1\n"
            + "  DELETE FROM estudiantes WHERE id = 5\n"
            + "  DELETE FROM estudiantes ALL\n"
            + "  TREE estudiantes\n"
            + "  SAVE ALL\n\n"

            + "ATAJOS DE TECLADO:\n"
            + "  Ctrl+S  : Guardar todo\n"
            + "  Ctrl+Q  : Salir\n"
            + "  F5      : Refrescar todos los paneles\n"
            + "  Ctrl+L  : Limpiar consola\n\n"
            + "Ciencias de la Computación I - Grupo 020-83\n"
            + "Casas / Yanez / Herrera  |  2026-I\n"
        );

        help.setCaretPosition(0); // scroll al inicio
        add(new JScrollPane(help), BorderLayout.CENTER);
    }
}
