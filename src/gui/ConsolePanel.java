package gui;

import motor.almacenamiento.Registro;
import motor.parser.ResultadoComando;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

// Panel de consola SQL. Soporta entrada de comandos, historial basico,
// boton para limpiar y atajo Ctrl+Enter para ejecutar.
class ConsolePanel extends JPanel {
    private final GuiContext context;
    private final JTextField commandField = new JTextField(64);
    private final JTextArea output = new JTextArea();
    private final java.util.List<String> historial = new java.util.ArrayList<>();
    private int historialIdx = 0;

    ConsolePanel(GuiContext context) {
        this.context = context;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // barra de comandos con estilo
        JPanel topBar = new JPanel(new BorderLayout(6, 0));
        topBar.setBorder(BorderFactory.createTitledBorder("Comando SQL"));

        JPanel inputPanel = new JPanel(new BorderLayout(4, 0));
        JLabel prompt = new JLabel("motorbd>");
        prompt.setFont(new Font("Monospaced", Font.BOLD, 13));
        prompt.setForeground(new Color(0, 100, 0));
        inputPanel.add(prompt, BorderLayout.WEST);

        commandField.setFont(new Font("Monospaced", Font.PLAIN, 13));
        commandField.setBackground(new Color(255, 255, 245));
        inputPanel.add(commandField, BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        JButton btnEjecutar = new JButton("Ejecutar (Ctrl+Enter)");
        btnEjecutar.setMnemonic(KeyEvent.VK_E);
        JButton btnLimpiar = new JButton("Limpiar consola");
        JButton btnAyuda = new JButton("?");
        btnAyuda.setToolTipText("Mostrar comandos disponibles");

        btnEjecutar.setBackground(new Color(70, 130, 180));
        btnEjecutar.setForeground(Color.WHITE);
        btnEjecutar.setFocusPainted(false);

        botones.add(btnEjecutar);
        botones.add(btnLimpiar);
        botones.add(btnAyuda);
        inputPanel.add(botones, BorderLayout.EAST);
        topBar.add(inputPanel, BorderLayout.CENTER);

        // comandos rapidos
        JPanel quickCmds = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        String[] quick = {"SHOW SPACES", "SAVE ALL", "TREE", "SELECT * FROM", "DESC"};
        for (String cmd : quick) {
            JButton btn = new JButton(cmd);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            btn.setMargin(new Insets(2, 6, 2, 6));
            btn.addActionListener(e -> {
                commandField.setText(cmd + " ");
                commandField.requestFocus();
            });
            quickCmds.add(btn);
        }
        topBar.add(quickCmds, BorderLayout.SOUTH);

        add(topBar, BorderLayout.NORTH);

        // area de salida
        output.setEditable(false);
        output.setFont(new Font("Monospaced", Font.PLAIN, 12));
        output.setBackground(new Color(30, 30, 35));
        output.setForeground(new Color(200, 220, 200));
        output.setCaretColor(new Color(200, 220, 200));
        JScrollPane scroll = new JScrollPane(output);
        scroll.setBorder(BorderFactory.createLoweredSoftBevelBorder());
        add(scroll, BorderLayout.CENTER);

        // barra inferior con atajos
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomBar.add(new JLabel("Ctrl+Enter: ejecutar  |  Flechas: historial  |  Ctrl+L: limpiar"));
        add(bottomBar, BorderLayout.SOUTH);

        // listeners
        btnEjecutar.addActionListener(e -> ejecutarComando());
        btnLimpiar.addActionListener(e -> limpiarConsola());
        btnAyuda.addActionListener(e -> mostrarAyuda());

        commandField.addActionListener(e -> ejecutarComando());

        // atajo Ctrl+Enter y Ctrl+L
        commandField.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "ejecutar");
        commandField.getActionMap().put("ejecutar", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { ejecutarComando(); }
        });

        // navegacion por historial con flechas
        commandField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP && !historial.isEmpty()) {
                    historialIdx = Math.max(0, historialIdx - 1);
                    commandField.setText(historial.get(historialIdx));
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN && !historial.isEmpty()) {
                    historialIdx = Math.min(historial.size() - 1, historialIdx + 1);
                    commandField.setText(historial.get(historialIdx));
                    e.consume();
                }
            }
        });

        output.append("  Motor BD AVL - Consola SQL\n");
        output.append("  Escriba un comando y presione Enter o Ctrl+Enter para ejecutar.\n");
        output.append("  Use los botones superiores para comandos rápidos.\n\n");
    }

    private void ejecutarComando() {
        String command = commandField.getText().trim();
        if (command.endsWith(";")) command = command.substring(0, command.length() - 1).trim();
        if (command.isEmpty()) return;

        // guardar en historial
        if (historial.isEmpty() || !historial.get(historial.size() - 1).equals(command)) {
            historial.add(command);
        }
        historialIdx = historial.size();

        // mostrar comando ejecutado
        output.append("\n" + estiloPrompt() + command + "\n");

        ResultadoComando result = context.execute(command);
        output.append(formatearResultado(result));
        output.append("\n");

        // scroll al final
        output.setCaretPosition(output.getDocument().getLength());
        commandField.setText("");

        if (!result.isError()) context.notifyDataChanged();
    }

    private String estiloPrompt() {
        String hora = String.format("%02d:%02d",
                java.time.LocalTime.now().getHour(),
                java.time.LocalTime.now().getMinute());
        return hora + " motorbd> ";
    }

    void limpiarConsola() {
        output.setText("");
        output.append("  Consola limpiada.\n\n");
    }

    private void mostrarAyuda() {
        output.append("\nCOMANDOS DISPONIBLES:\n\n"
                + "CREATE SPACE nombre [(campo tipo [PK], ...)]\n"
                + "DROP SPACE nombre\n"
                + "SHOW SPACES\n"
                + "DESC nombre\n"
                + "INSERT INTO nombre VALUES (v1, v2, ...)\n"
                + "INSERT INTO nombre (c1,...) VALUES (v1,...)\n"
                + "SELECT * FROM nombre [WHERE campo op valor]\n"
                + "SELECT * FROM nombre WHERE campo BETWEEN v1 AND v2\n"
                + "UPDATE nombre SET c=v WHERE campo op valor\n"
                + "DELETE FROM nombre WHERE campo op valor\n"
                + "DELETE FROM nombre ALL\n"
                + "TREE nombre\n"
                + "SAVE nombre | SAVE ALL\n"
                + "EXIT | QUIT (solo en CLI)\n\n");
    }

    private String formatearResultado(ResultadoComando result) {
        if (result.isError()) {
            return "▶ ERROR: " + result.getMensaje();
        }
        if (result.getFilas() != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Resultados (").append(result.getAfectados()).append("):\n");

            java.util.LinkedHashSet<String> cols = new java.util.LinkedHashSet<>();
            for (Registro row : result.getFilas()) cols.addAll(row.getCampos().keySet());

            java.util.Map<String, Integer> anchos = new java.util.LinkedHashMap<>();
            for (String c : cols) anchos.put(c, c.length());
            for (Registro row : result.getFilas()) {
                for (String c : cols) {
                    Object v = row.get(c);
                    int len = v == null ? 4 : v.toString().length();
                    if (len > anchos.get(c)) anchos.put(c, len);
                }
            }

            for (String c : cols) {
                sb.append(String.format("%-" + (anchos.get(c) + 2) + "s", c));
            }
            sb.append("\n");
            for (String c : cols) {
                sb.append("-".repeat(anchos.get(c) + 2));
            }
            sb.append("\n");

            for (Registro row : result.getFilas()) {
                for (String c : cols) {
                    Object v = row.get(c);
                    sb.append(String.format("%-" + (anchos.get(c) + 2) + "s", v == null ? "null" : v));
                }
                sb.append("\n");
            }
            return sb.toString();
        }
        return "▶ " + (result.getMensaje() != null ? result.getMensaje() : "OK");
    }
}
