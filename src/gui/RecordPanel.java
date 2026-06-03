package gui;

import motor.parser.ResultadoComando;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.*;

// Panel de gestion de registros (CRUD completo).
// Formulario dinamico a la izquierda, tabla de registros a la derecha.
// Soporta insercion, actualizacion y eliminacion con validacion de PK.
class RecordPanel extends JPanel {
    private final GuiContext context;
    private final JComboBox<String> spaceCombo = new JComboBox<>();
    private final JPanel formPanel = new JPanel(new GridBagLayout());
    private final JTable table = new JTable();
    private final Map<String, JTextField> inputs = new LinkedHashMap<>();
    private List<FieldInfo> fields = Collections.emptyList();

    RecordPanel(GuiContext context) {
        this.context = context;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // barra superior
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        top.setBorder(BorderFactory.createTitledBorder("Espacio"));
        JButton refreshSpaces  = new JButton("Refrescar");
        JButton refreshRecords = new JButton("Ver registros");
        top.add(new JLabel("Espacio:"));
        top.add(spaceCombo);
        top.add(refreshSpaces);
        top.add(refreshRecords);

        JLabel infoLabel = new JLabel("Seleccione un espacio para ver sus registros");
        infoLabel.setForeground(Color.GRAY);
        top.add(infoLabel);

        add(top, BorderLayout.NORTH);

        // panel izquierdo: formulario
        JPanel leftPanel = new JPanel(new BorderLayout(8, 8));
        leftPanel.setBorder(BorderFactory.createTitledBorder("Formulario"));
        leftPanel.setPreferredSize(new Dimension(320, 400));

        formPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        JScrollPane formScroll = new JScrollPane(formPanel);
        formScroll.setBorder(null);
        leftPanel.add(formScroll, BorderLayout.CENTER);

        // botones del formulario
        JPanel btnPanel = new JPanel(new GridLayout(0, 2, 6, 6));
        JButton insertBtn = new JButton("Insertar");
        JButton updateBtn = new JButton("Actualizar selec.");
        JButton deleteBtn = new JButton("Eliminar selec.");
        JButton clearBtn  = new JButton("Limpiar formulario");

        // estilos
        insertBtn.setBackground(new Color(60, 140, 60));
        insertBtn.setForeground(Color.WHITE);
        deleteBtn.setForeground(new Color(180, 50, 50));
        clearBtn.setForeground(new Color(100, 100, 100));

        btnPanel.add(insertBtn);
        btnPanel.add(updateBtn);
        btnPanel.add(deleteBtn);
        btnPanel.add(clearBtn);
        leftPanel.add(btnPanel, BorderLayout.SOUTH);

        // tabla de registros
        table.setRowHeight(24);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setGridColor(new Color(230, 230, 240));
        JScrollPane tableScroll = new JScrollPane(table);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, tableScroll);
        split.setResizeWeight(0.3);
        split.setDividerLocation(320);
        add(split, BorderLayout.CENTER);

        // listeners
        refreshSpaces.addActionListener(e -> refreshSpaces());
        refreshRecords.addActionListener(e -> refreshRecords());
        insertBtn.addActionListener(e -> insertRecord());
        updateBtn.addActionListener(e -> updateSelected());
        deleteBtn.addActionListener(e -> deleteSelected());
        clearBtn.addActionListener(e -> limpiarFormulario());
        spaceCombo.addActionListener(e -> { loadFormAndRecords(); infoLabel.setText(""); });
        table.getSelectionModel().addListSelectionListener(e -> fillFormFromSelection());
        context.addRefreshListener(this::refreshSpaces);
        refreshSpaces();
    }

    private void refreshSpaces() {
        Object sel = spaceCombo.getSelectedItem();
        spaceCombo.setModel(new DefaultComboBoxModel<>(context.listSpaces().toArray(new String[0])));
        if (sel != null) spaceCombo.setSelectedItem(sel);
        loadFormAndRecords();
    }

    private void loadFormAndRecords() {
        String space = selectedSpace();
        formPanel.removeAll();
        inputs.clear();
        if (space == null) {
            formPanel.revalidate(); formPanel.repaint();
            table.setModel(new DefaultTableModel());
            return;
        }
        fields = context.describeFields(space);
        if (fields.isEmpty()) {
            // espacio libre: solo campo id
            fields = Collections.singletonList(new FieldInfo("id", "TEXTO", true));
        }
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 4, 3, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        int row = 0;
        for (FieldInfo field : fields) {
            gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
            String label = field.name + (field.primaryKey ? " *" : "");
            formPanel.add(new JLabel(label), gbc);

            gbc.gridx = 1; gbc.weightx = 1;
            JTextField input = new JTextField(18);
            if (field.primaryKey) {
                input.setBackground(new Color(255, 255, 220)); // fondo amarillo para PK
            }
            // tooltip con tipo
            input.setToolTipText("Tipo: " + field.type + (field.primaryKey ? " (PK)" : ""));
            inputs.put(field.name, input);
            formPanel.add(input, gbc);
            row++;
        }
        // relleno vertical
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.weighty = 1;
        formPanel.add(new JLabel(""), gbc);

        formPanel.revalidate();
        formPanel.repaint();
        refreshRecords();
    }

    private void refreshRecords() {
        String space = selectedSpace();
        if (space == null) return;
        ResultadoComando result = context.execute("SELECT * FROM " + space);
        if (result.isError()) { showError(result.getMensaje()); return; }
        table.setModel(context.tableModel(result.getFilas()));
    }

    private void insertRecord() {
        String space = selectedSpace();
        if (space == null || fields.isEmpty()) return;
        for (FieldInfo field : fields) {
            if (inputs.get(field.name).getText().trim().isEmpty()) {
                showError("Complete el campo: " + field.name);
                inputs.get(field.name).requestFocus();
                return;
            }
        }
        Map<String, String> values = readInputs();
        StringBuilder fieldList = new StringBuilder();
        for (FieldInfo f : fields) {
            if (fieldList.length() > 0) fieldList.append(", ");
            fieldList.append(f.name);
        }
        String cmd = "INSERT INTO " + space + " (" + fieldList + ") VALUES ("
                + context.joinValues(values, fields) + ")";
        ResultadoComando result = context.execute(cmd);
        showResult(result);
        if (!result.isError()) {
            limpiarFormulario();
            refreshRecords();
            context.notifyDataChanged();
        }
    }

    private void updateSelected() {
        String space = selectedSpace();
        int row = table.getSelectedRow();
        if (space == null || row < 0) { showError("Seleccione un registro en la tabla."); return; }

        String pk = context.primaryKey(space);
        FieldInfo pkField = context.findField(fields, pk);
        int pkCol = table.getColumnModel().getColumnIndex(pk);
        Object oldPk = table.getValueAt(row, pkCol);
        String newPk = inputs.get(pk).getText().trim();

        if (!String.valueOf(oldPk).equals(newPk)) {
            showError("No se puede modificar la clave primaria.");
            inputs.get(pk).setText(String.valueOf(oldPk));
            return;
        }
        StringBuilder set = new StringBuilder();
        for (FieldInfo f : fields) {
            if (f.primaryKey) continue;
            if (set.length() > 0) set.append(", ");
            set.append(f.name).append("=").append(context.literal(inputs.get(f.name).getText(), f.type));
        }
        String cmd = "UPDATE " + space + " SET " + set + " WHERE " + pk + " = "
                + context.literalFromField(String.valueOf(oldPk), pkField);
        ResultadoComando result = context.execute(cmd);
        showResult(result);
        if (!result.isError()) {
            refreshRecords();
            context.notifyDataChanged();
        }
    }

    private void deleteSelected() {
        String space = selectedSpace();
        int row = table.getSelectedRow();
        if (space == null || row < 0) { showError("Seleccione un registro en la tabla."); return; }

        String pk = context.primaryKey(space);
        FieldInfo pkField = context.findField(fields, pk);
        int pkCol = table.getColumnModel().getColumnIndex(pk);
        Object key = table.getValueAt(row, pkCol);

        int ok = JOptionPane.showConfirmDialog(this,
                "Eliminar registro con " + pk + " = " + key + "?",
                "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;

        String cmd = "DELETE FROM " + space + " WHERE " + pk + " = "
                + context.literalFromField(String.valueOf(key), pkField);
        ResultadoComando result = context.execute(cmd);
        showResult(result);
        if (!result.isError()) {
            limpiarFormulario();
            refreshRecords();
            context.notifyDataChanged();
        }
    }

    private void fillFormFromSelection() {
        int row = table.getSelectedRow();
        if (row < 0 || table.getColumnCount() == 0) return;
        for (FieldInfo field : fields) {
            JTextField input = inputs.get(field.name);
            if (input == null) continue;
            int col;
            try { col = table.getColumnModel().getColumnIndex(field.name); }
            catch (IllegalArgumentException ex) { continue; }
            Object value = table.getValueAt(row, col);
            input.setText(value == null ? "" : value.toString());
        }
    }

    private void limpiarFormulario() {
        for (JTextField input : inputs.values()) input.setText("");
    }

    private Map<String, String> readInputs() {
        Map<String, String> values = new LinkedHashMap<>();
        for (FieldInfo field : fields) values.put(field.name, inputs.get(field.name).getText());
        return values;
    }

    private String selectedSpace() {
        Object item = spaceCombo.getSelectedItem();
        return item == null ? null : item.toString();
    }

    private void showResult(ResultadoComando r) {
        if (r.isError()) showError(r.getMensaje());
        else JOptionPane.showMessageDialog(this, r.getMensaje(), "OK", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
