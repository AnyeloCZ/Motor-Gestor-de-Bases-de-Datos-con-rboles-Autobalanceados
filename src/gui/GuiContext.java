package gui;

import motor.almacenamiento.Registro;
import motor.parser.Motor;
import motor.parser.ResultadoComando;

import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Contexto compartido entre todos los paneles de la GUI.
// Contiene la instancia del motor y metodos auxiliares.
class GuiContext {
    final Motor motor;
    private final List<Runnable> refreshListeners = new ArrayList<>();

    GuiContext(String dataDir) {
        this.motor = new Motor(dataDir);
    }

    ResultadoComando execute(String command) {
        return motor.ejecutar(command);
    }

    void addRefreshListener(Runnable listener) {
        refreshListeners.add(listener);
    }

    void notifyDataChanged() {
        for (Runnable listener : refreshListeners) {
            listener.run();
        }
    }

    // extrae nombres de espacios de SHOW SPACES
    List<String> listSpaces() {
        ResultadoComando result = execute("SHOW SPACES");
        List<String> spaces = new ArrayList<>();
        String msg = result.getMensaje();
        if (result.isError() || msg == null) return spaces;
        for (String line : msg.split("\\R")) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("ESPACIO") || t.startsWith("-") || t.startsWith("(")) continue;
            String[] parts = t.split("\\s+");
            if (parts.length > 0) spaces.add(parts[0]);
        }
        return spaces;
    }

    // extrae lista de campos de la salida de DESC
    List<FieldInfo> describeFields(String space) {
        ResultadoComando result = execute("DESC " + space);
        List<FieldInfo> fields = new ArrayList<>();
        if (result.isError() || result.getMensaje() == null) return fields;
        boolean inFields = false;
        for (String line : result.getMensaje().split("\\R")) {
            String t = line.trim();
            if (t.equals("Campos:")) { inFields = true; continue; }
            if (!inFields || t.isEmpty() || t.startsWith("NOMBRE") || t.startsWith("-")) continue;
            String[] parts = t.split("\\s+");
            if (parts.length >= 2) {
                fields.add(new FieldInfo(parts[0], parts[1], parts.length >= 3 && parts[2].equalsIgnoreCase("SI")));
            }
        }
        return fields;
    }

    String primaryKey(String space) {
        for (FieldInfo field : describeFields(space)) {
            if (field.primaryKey) return field.name;
        }
        return "";
    }

    // convierte lista de registros a modelo para JTable
    DefaultTableModel tableModel(List<Registro> rows) {
        Set<String> columns = new LinkedHashSet<>();
        if (rows != null) {
            for (Registro row : rows) columns.addAll(row.getCampos().keySet());
        }
        DefaultTableModel model = new DefaultTableModel() {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        for (String column : columns) model.addColumn(column);
        if (rows != null) {
            for (Registro row : rows) {
                Object[] values = new Object[columns.size()];
                int i = 0;
                for (String column : columns) values[i++] = row.get(column);
                model.addRow(values);
            }
        }
        return model;
    }

    String literal(String raw, String type) {
        String value = raw == null ? "" : raw.trim();
        if (type.equalsIgnoreCase("TEXTO")) {
            return "'" + value.replace("'", "''") + "'";
        }
        return value;
    }

    String literalFromField(String raw, FieldInfo field) {
        return literal(raw, field == null ? "TEXTO" : field.type);
    }

    FieldInfo findField(List<FieldInfo> fields, String name) {
        for (FieldInfo field : fields) {
            if (field.name.equalsIgnoreCase(name)) return field;
        }
        return null;
    }

    String joinValues(Map<String, String> values, List<FieldInfo> fields) {
        List<String> parts = new ArrayList<>();
        for (FieldInfo field : fields) {
            parts.add(literal(values.get(field.name), field.type));
        }
        return String.join(", ", parts);
    }

    // estadisticas rapidas: [total espacios, total registros]
    int[] stats() {
        ResultadoComando result = execute("SHOW SPACES");
        int espacios = 0, registros = 0;
        if (!result.isError() && result.getMensaje() != null) {
            for (String line : result.getMensaje().split("\\R")) {
                String t = line.trim();
                if (t.isEmpty() || t.startsWith("ESPACIO") || t.startsWith("-") || t.startsWith("(")) continue;
                espacios++;
                String[] parts = t.split("\\s+");
                if (parts.length >= 3) {
                    try { registros += Integer.parseInt(parts[2]); } catch (NumberFormatException ignored) {}
                }
            }
        }
        return new int[]{espacios, registros};
    }
}
