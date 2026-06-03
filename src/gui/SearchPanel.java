package gui;

import motor.parser.ResultadoComando;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.*;

// Panel de búsqueda con filtros por campo, operador, valor y rango (BETWEEN).
// Resultados se muestran en una tabla con resaltado de filas.
class SearchPanel extends JPanel {
    private final GuiContext context;
    private final JComboBox<String> spaceCombo = new JComboBox<>();
    private final JComboBox<String> fieldCombo = new JComboBox<>();
    private final JComboBox<String> opCombo = new JComboBox<>(
            new String[]{"=", "!=", "<", "<=", ">", ">="});
    private final JTextField valueField = new JTextField(12);
    private final JTextField fromField = new JTextField(8);
    private final JTextField toField = new JTextField(8);
    private final JTable resultsTable = new JTable();
    private final JLabel statusLabel = new JLabel(" ");
    private List<FieldInfo> fields = Collections.emptyList();

    SearchPanel(GuiContext context) {
        this.context = context;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // panel de filtros
        JPanel filters = new JPanel(new GridBagLayout());
        filters.setBorder(BorderFactory.createTitledBorder("Filtros de búsqueda"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        // fila 1: espacio y refrescar
        gbc.gridx = 0; gbc.gridy = 0; filters.add(new JLabel("Espacio:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2; filters.add(spaceCombo, gbc);
        gbc.gridwidth = 1;
        JButton refreshBtn = new JButton("Refrescar");
        gbc.gridx = 3; filters.add(refreshBtn, gbc);

        // fila 2: campo, operador, valor
        gbc.gridx = 0; gbc.gridy = 1; filters.add(new JLabel("Campo:"), gbc);
        gbc.gridx = 1; filters.add(fieldCombo, gbc);
        gbc.gridx = 2; filters.add(opCombo, gbc);
        gbc.gridx = 3; filters.add(valueField, gbc);

        // fila 3: botones de busqueda
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton searchBtn = new JButton("Buscar");
        JButton byPkBtn   = new JButton("Buscar por PK");
        JButton rangeBtn  = new JButton("Buscar rango");
        JButton clearBtn  = new JButton("Limpiar");

        // estilos
        searchBtn.setBackground(new Color(70, 130, 180));
        searchBtn.setForeground(Color.WHITE);
        searchBtn.setFocusPainted(false);
        clearBtn.setForeground(Color.GRAY);

        btnRow.add(searchBtn);
        btnRow.add(byPkBtn);
        btnRow.add(Box.createHorizontalStrut(15));
        btnRow.add(new JLabel("Desde:"));
        btnRow.add(fromField);
        btnRow.add(new JLabel("Hasta:"));
        btnRow.add(toField);
        btnRow.add(rangeBtn);
        btnRow.add(Box.createHorizontalStrut(10));
        btnRow.add(clearBtn);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 4; gbc.fill = GridBagConstraints.HORIZONTAL;
        filters.add(btnRow, gbc);

        add(filters, BorderLayout.NORTH);

        // tabla de resultados
        resultsTable.setRowHeight(24);
        resultsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        resultsTable.setGridColor(new Color(230, 230, 240));
        resultsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        JScrollPane scroll = new JScrollPane(resultsTable);
        scroll.setBorder(BorderFactory.createTitledBorder("Resultados"));
        add(scroll, BorderLayout.CENTER);

        // barra de estado
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        statusLabel.setForeground(Color.GRAY);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        add(statusLabel, BorderLayout.SOUTH);

        // listeners
        refreshBtn.addActionListener(e -> refreshSpaces());
        spaceCombo.addActionListener(e -> refreshFields());
        searchBtn.addActionListener(e -> runSearch(false));
        byPkBtn.addActionListener(e -> runSearch(true));
        rangeBtn.addActionListener(e -> runRange());
        clearBtn.addActionListener(e -> limpiarResultados());
        context.addRefreshListener(this::refreshSpaces);
        refreshSpaces();
    }

    private void refreshSpaces() {
        Object sel = spaceCombo.getSelectedItem();
        spaceCombo.setModel(new DefaultComboBoxModel<>(context.listSpaces().toArray(new String[0])));
        if (sel != null) spaceCombo.setSelectedItem(sel);
        refreshFields();
    }

    private void refreshFields() {
        String space = selectedSpace();
        fieldCombo.removeAllItems();
        if (space == null) return;
        fields = context.describeFields(space);
        for (FieldInfo f : fields) fieldCombo.addItem(f.name);
    }

    private void runSearch(boolean forcePk) {
        String space = selectedSpace();
        String field = forcePk ? context.primaryKey(space) : selectedField();
        if (space == null || field == null || field.isEmpty()) return;

        FieldInfo info = context.findField(fields, field);
        String cmd = "SELECT * FROM " + space + " WHERE " + field + " "
                + opCombo.getSelectedItem() + " " + context.literalFromField(valueField.getText(), info);
        ejecutarYMostrar(cmd);
    }

    private void runRange() {
        String space = selectedSpace();
        String field = selectedField();
        if (space == null || field == null) return;

        FieldInfo info = context.findField(fields, field);
        String cmd = "SELECT * FROM " + space + " WHERE " + field + " BETWEEN "
                + context.literalFromField(fromField.getText(), info) + " AND "
                + context.literalFromField(toField.getText(), info);
        ejecutarYMostrar(cmd);
    }

    private void ejecutarYMostrar(String command) {
        ResultadoComando result = context.execute(command);
        if (result.isError()) {
            JOptionPane.showMessageDialog(this, result.getMensaje(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        resultsTable.setModel(context.tableModel(result.getFilas()));

        // actualizar barra de estado
        int count = result.getAfectados();
        if (count == 0) {
            statusLabel.setText("Sin resultados.");
            statusLabel.setForeground(new Color(180, 120, 0));
        } else {
            statusLabel.setText(count + " registro(s) encontrado(s).");
            statusLabel.setForeground(new Color(0, 120, 0));
        }
    }

    private void limpiarResultados() {
        resultsTable.setModel(new DefaultTableModel());
        valueField.setText("");
        fromField.setText("");
        toField.setText("");
        statusLabel.setText("Resultados limpiados.");
        statusLabel.setForeground(Color.GRAY);
    }

    private String selectedSpace() {
        Object item = spaceCombo.getSelectedItem();
        return item == null ? null : item.toString();
    }

    private String selectedField() {
        Object item = fieldCombo.getSelectedItem();
        return item == null ? null : item.toString();
    }
}
