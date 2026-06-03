package gui;

import motor.parser.ResultadoComando;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

// Panel para crear y eliminar espacios.
// Valida que haya exactamente una PK, sin campos repetidos ni vacios.
// Permite agregar/quitar campos dinamicamente en una tabla editable.
class SpacePanel extends JPanel {
    private final GuiContext context;
    private final JTextField nameField = new JTextField(18);
    private final JTextField dropField = new JTextField(18);
    private final JComboBox<String> dropCombo = new JComboBox<>();
    private final JTextArea spacesArea = new JTextArea();
    private final DefaultTableModel fieldsModel = new DefaultTableModel(
            new Object[]{"Nombre", "Tipo", "PK"}, 0) {
        @Override public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 2 ? Boolean.class : String.class;
        }
    };

    SpacePanel(GuiContext context) {
        this.context = context;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Panel de creacion
        JTable fieldsTable = new JTable(fieldsModel);
        fieldsTable.setRowHeight(22);
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"ENTERO", "TEXTO", "REAL", "BOOLEAN"});
        fieldsTable.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(typeCombo));
        // filas por defecto
        fieldsModel.addRow(new Object[]{"id", "ENTERO", true});
        fieldsModel.addRow(new Object[]{"nombre", "TEXTO", false});

        JPanel createPanel = new JPanel(new BorderLayout(8, 8));
        createPanel.setBorder(BorderFactory.createTitledBorder("Crear espacio"));

        JPanel topCreate = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        topCreate.add(new JLabel("Nombre:"));
        topCreate.add(nameField);

        JButton addField    = new JButton("+ Campo");
        JButton removeField = new JButton("- Campo");
        JButton clearFields = new JButton("Limpiar campos");
        JButton createBtn   = new JButton("Crear espacio");

        // estilo boton crear (color verde)
        createBtn.setBackground(new Color(60, 140, 60));
        createBtn.setForeground(Color.WHITE);
        createBtn.setFocusPainted(false);
        createBtn.setFont(createBtn.getFont().deriveFont(Font.BOLD));

        topCreate.add(addField);
        topCreate.add(removeField);
        topCreate.add(clearFields);
        topCreate.add(createBtn);
        createPanel.add(topCreate, BorderLayout.NORTH);

        JScrollPane tableScroll = new JScrollPane(fieldsTable);
        tableScroll.setPreferredSize(new Dimension(400, 120));
        createPanel.add(tableScroll, BorderLayout.CENTER);

        // Panel de administracion
        JPanel managePanel = new JPanel(new GridBagLayout());
        managePanel.setBorder(BorderFactory.createTitledBorder("Administrar espacios"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; managePanel.add(new JLabel("Espacio:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2; managePanel.add(dropCombo, gbc);
        gbc.gridwidth = 1;
        gbc.gridx = 3; managePanel.add(new JLabel("o nombre:"), gbc);
        gbc.gridx = 4; managePanel.add(dropField, gbc);

        JButton dropBtn   = new JButton("Eliminar espacio");
        JButton refreshBtn = new JButton("Refrescar");
        dropBtn.setForeground(new Color(180, 50, 50));

        gbc.gridx = 5; managePanel.add(dropBtn, gbc);
        gbc.gridx = 6; managePanel.add(refreshBtn, gbc);

        spacesArea.setEditable(false);
        spacesArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        spacesArea.setRows(8);
        spacesArea.setBackground(new Color(248, 248, 252));
        JScrollPane spacesScroll = new JScrollPane(spacesArea);

        GridBagConstraints areaGbc = new GridBagConstraints();
        areaGbc.gridx = 0; areaGbc.gridy = 1; areaGbc.gridwidth = 7;
        areaGbc.weightx = 1; areaGbc.weighty = 1;
        areaGbc.fill = GridBagConstraints.BOTH;
        areaGbc.insets = new Insets(6, 4, 4, 4);
        managePanel.add(spacesScroll, areaGbc);

        // dividir pantalla verticalmente
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, createPanel, managePanel);
        split.setResizeWeight(0.45);
        split.setDividerLocation(200);
        add(split, BorderLayout.CENTER);

        // listeners
        addField.addActionListener(e -> fieldsModel.addRow(new Object[]{"", "TEXTO", false}));
        removeField.addActionListener(e -> {
            int row = fieldsTable.getSelectedRow();
            if (row >= 0) fieldsModel.removeRow(row);
        });
        clearFields.addActionListener(e -> {
            fieldsModel.setRowCount(0);
            fieldsModel.addRow(new Object[]{"id", "ENTERO", true});
            fieldsModel.addRow(new Object[]{"nombre", "TEXTO", false});
            nameField.setText("");
        });
        createBtn.addActionListener(e -> createSpace());
        dropBtn.addActionListener(e -> dropSpace());
        refreshBtn.addActionListener(e -> refreshSpaces());
        context.addRefreshListener(this::refreshSpaces);
        refreshSpaces();
    }

    private void createSpace() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showError("Ingrese el nombre del espacio.");
            return;
        }

        Set<String> names = new HashSet<>();
        int pkCount = 0;
        StringBuilder fields = new StringBuilder();

        for (int i = 0; i < fieldsModel.getRowCount(); i++) {
            String field = value(i, 0).toLowerCase();
            String type  = value(i, 1).toUpperCase();
            boolean pk   = Boolean.TRUE.equals(fieldsModel.getValueAt(i, 2));

            if (field.isEmpty())         { showError("Hay campos sin nombre."); return; }
            if (!type.matches("ENTERO|TEXTO|REAL|BOOLEAN")) {
                showError("Tipo no valido en '" + field + "'. Use ENTERO, TEXTO, REAL o BOOLEAN."); return;
            }
            if (!names.add(field))       { showError("Hay campos repetidos: " + field); return; }
            if (pk) pkCount++;

            if (fields.length() > 0) fields.append(", ");
            fields.append(field).append(" ").append(type);
            if (pk) fields.append(" PK");
        }

        if (pkCount == 0) {
            showError("Debe marcar al menos un campo como clave primaria (PK).");
            return;
        }

        ResultadoComando result = context.execute(
                "CREATE SPACE " + name + " (" + fields + ")");
        showResult(result);
        if (!result.isError()) {
            nameField.setText("");
            context.notifyDataChanged();
        }
    }

    private String value(int row, int col) {
        Object v = fieldsModel.getValueAt(row, col);
        return v == null ? "" : v.toString().trim();
    }

    private void dropSpace() {
        String selected = (String) dropCombo.getSelectedItem();
        String name = dropField.getText().trim();
        if (name.isEmpty() && selected != null) name = selected;
        if (name.isEmpty()) {
            showError("Seleccione o escriba el espacio a eliminar.");
            return;
        }

        int ok = JOptionPane.showConfirmDialog(this,
                "Eliminar el espacio '" + name + "'?\nSe borraran todos sus registros y archivos.",
                "Confirmar eliminacion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;

        ResultadoComando result = context.execute("DROP SPACE " + name);
        showResult(result);
        if (!result.isError()) {
            dropField.setText("");
            context.notifyDataChanged();
        }
    }

    private void refreshSpaces() {
        ResultadoComando result = context.execute("SHOW SPACES");
        spacesArea.setText(result.getMensaje() != null ? result.getMensaje() : "");
        dropCombo.setModel(new DefaultComboBoxModel<>(context.listSpaces().toArray(new String[0])));
    }

    private void showResult(ResultadoComando r) {
        if (r.isError()) showError(r.getMensaje());
        else JOptionPane.showMessageDialog(this, r.getMensaje(), "OK", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
