package gui;

import motor.almacenamiento.Registro;
import motor.parser.ResultadoComando;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.*;

// Panel que dibuja visualmente el árbol AVL usando Graphics2D.
// Muestra nodos como círculos con clave, altura y factor de balance.
// Incluye panel de control con búsqueda por clave y botón de refresco.
class TreePanel extends JPanel {
    private final GuiContext context;
    private final JComboBox<String> spaceCombo = new JComboBox<>();
    private final JTextField keyField = new JTextField(12);
    private final ArbolCanvas arbolCanvas;
    private final JTextArea infoArea = new JTextArea(6, 30);
    private List<String> inorderKeys = new ArrayList<>();

    TreePanel(GuiContext context) {
        this.context = context;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // panel de controles superior
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        controls.setBorder(BorderFactory.createTitledBorder("Controles"));
        JButton refresh = new JButton("Refrescar");
        JButton showTree = new JButton("Mostrar arbol");
        JButton findKey = new JButton("Buscar clave");
        JCheckBox compacto = new JCheckBox("Compacto", false);

        controls.add(new JLabel("Espacio:"));
        controls.add(spaceCombo);
        controls.add(refresh);
        controls.add(showTree);
        controls.add(new JLabel("  Clave:"));
        controls.add(keyField);
        controls.add(findKey);
        controls.add(compacto);
        add(controls, BorderLayout.NORTH);

        // panel central: canvas del arbol
        arbolCanvas = new ArbolCanvas();
        arbolCanvas.setBackground(new Color(255, 255, 255));
        arbolCanvas.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 220)));
        add(new JScrollPane(arbolCanvas), BorderLayout.CENTER);

        // panel lateral derecho: informacion textual
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder("Informacion"));
        infoArea.setEditable(false);
        infoArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        infoArea.setBackground(new Color(248, 248, 252));
        infoPanel.add(new JScrollPane(infoArea), BorderLayout.CENTER);

        JButton copiar = new JButton("Copiar texto");
        copiar.addActionListener(e -> {
            infoArea.selectAll();
            infoArea.copy();
        });
        infoPanel.add(copiar, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(arbolCanvas), infoPanel);
        split.setResizeWeight(0.75);
        split.setDividerLocation(600);
        remove(1); // quitar el centro anterior
        add(split, BorderLayout.CENTER);

        // listeners
        refresh.addActionListener(e -> refreshSpaces());
        showTree.addActionListener(e -> cargarArbol(false));
        findKey.addActionListener(e -> buscarClave());
        compacto.addActionListener(e -> { arbolCanvas.compacto = compacto.isSelected(); cargarArbol(false); });
        spaceCombo.addActionListener(e -> cargarArbol(false));
        context.addRefreshListener(this::refreshSpaces);
        refreshSpaces();
    }

    private void refreshSpaces() {
        Object selected = spaceCombo.getSelectedItem();
        spaceCombo.setModel(new DefaultComboBoxModel<>(context.listSpaces().toArray(new String[0])));
        if (selected != null) spaceCombo.setSelectedItem(selected);
        if (spaceCombo.getItemCount() > 0 && spaceCombo.getSelectedItem() != null) {
            cargarArbol(false);
        }
    }

    private void cargarArbol(boolean resaltarClave) {
        String space = selectedSpace();
        if (space == null) return;

        ResultadoComando rows = context.execute("SELECT * FROM " + space);
        ResultadoComando desc = context.execute("DESC " + space);
        String pk = context.primaryKey(space);

        // recolectar claves en orden inorder
        inorderKeys.clear();
        List<Map.Entry<String, String>> nodos = new ArrayList<>();
        if (rows.getFilas() != null) {
            for (Registro row : rows.getFilas()) {
                String clave = String.valueOf(row.get(pk));
                inorderKeys.add(clave);

                // recolectar info extra para mostrar en el arbol
                StringBuilder extras = new StringBuilder();
                for (Map.Entry<String, Object> e : row.getCampos().entrySet()) {
                    if (!e.getKey().equals(pk)) {
                        if (extras.length() > 0) extras.append(",");
                        String val = String.valueOf(e.getValue());
                        if (val.length() > 15) val = val.substring(0, 14) + "~";
                        extras.append(val);
                    }
                }
                nodos.add(new AbstractMap.SimpleEntry<>(clave, extras.toString()));
            }
        }

        // pasar nodos al canvas
        arbolCanvas.setNodos(nodos, pk, resaltarClave ? keyField.getText().trim() : null);
        arbolCanvas.repaint();

        // actualizar panel de informacion
        StringBuilder info = new StringBuilder();
        info.append("Espacio: ").append(space).append("\n");
        info.append("Clave primaria: ").append(pk).append("\n");
        if (desc.getMensaje() != null) {
            info.append(desc.getMensaje()).append("\n");
        }
        info.append("\nRecorrido inorder:\n");
        if (inorderKeys.isEmpty()) {
            info.append("  (sin claves)\n");
        } else {
            for (int i = 0; i < inorderKeys.size(); i++) {
                info.append(String.format("  %2d. %s%n", i + 1, inorderKeys.get(i)));
            }
        }

        // estadisticas del AVL
        int[] stats = context.stats();
        info.append(String.format("%nTotal espacios: %d | Total registros: %d%n", stats[0], stats[1]));

        // obtener altura desde TREE
        ResultadoComando treeRes = context.execute("TREE " + space);
        if (!treeRes.isError() && treeRes.getMensaje() != null) {
            String treeText = treeRes.getMensaje();
            info.append("\n--- Arbol ASCII ---\n");
            info.append(treeText.substring(Math.max(0, treeText.indexOf("Arbol") + 6)));
        }

        infoArea.setText(info.toString());
        infoArea.setCaretPosition(0);
    }

    private void buscarClave() {
        String key = keyField.getText().trim();
        if (key.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingrese una clave para buscar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String space = selectedSpace();
        if (space == null) return;
        String pk = context.primaryKey(space);
        FieldInfo field = context.findField(context.describeFields(space), pk);
        ResultadoComando result = context.execute(
                "SELECT * FROM " + space + " WHERE " + pk + " = " + context.literalFromField(key, field));

        if (result.isError()) {
            JOptionPane.showMessageDialog(this, result.getMensaje(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // resaltar el nodo encontrado
        arbolCanvas.highlightKey = key;
        cargarArbol(true);

        if (result.getAfectados() == 0) {
            JOptionPane.showMessageDialog(this, "No se encontro la clave '" + key + "'.",
                    "No encontrado", JOptionPane.INFORMATION_MESSAGE);
        } else {
            infoArea.append("\n\n--- Resultado busqueda de '" + key + "' ---\n");
            infoArea.append(result.getFilas().get(0).toString());
        }
    }

    private String selectedSpace() {
        Object item = spaceCombo.getSelectedItem();
        return item == null ? null : item.toString();
    }

    // Canvas que dibuja nodos del arbol como circulos conectados
    static class ArbolCanvas extends JPanel {
        private List<Map.Entry<String, String>> nodos = new ArrayList<>();
        private String highlightKey = null;
        boolean compacto = false;

        void setNodos(List<Map.Entry<String, String>> nodos, String campoClave, String highlightKey) {
            this.nodos = nodos;
            this.highlightKey = highlightKey;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (nodos.isEmpty()) {
                g.setColor(Color.GRAY);
                g.setFont(new Font("Segoe UI", Font.ITALIC, 16));
                String msg = "(Árbol vacío)";
                FontMetrics fm = g.getFontMetrics();
                g.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
                return;
            }

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int nivelH = compacto ? 80 : 110;   // espacio vertical entre niveles
            int espacioX = compacto ? 90 : 130;  // espacio horizontal minimo entre nodos

            // calcular posiciones recursivamente simulando un arbol AVL balanceado
            int totalNodos = nodos.size();
            int alturaArbol = (int) (Math.log(totalNodos + 1) / Math.log(2));
            int hojas = (int) Math.pow(2, alturaArbol);
            int anchoTotal = hojas * espacioX;
            int margenX = Math.max(50, (w - anchoTotal) / 2);
            int inicioY = 50;

            // mapea cada indice de nodo a su posicion (x, y) en el canvas
            Map<Integer, Point> posiciones = new LinkedHashMap<>();

            // construir arbol binario balanceado visual: usamos un array-style heap
            // nodo i tiene hijos 2i+1 y 2i+2
            for (int nivel = 0; nivel <= alturaArbol; nivel++) {
                int nodosEnNivel = (int) Math.pow(2, nivel);
                int espacioEntreNodos = anchoTotal / (nodosEnNivel + 1);
                int primerNodoX = margenX + espacioEntreNodos;

                for (int j = 0; j < nodosEnNivel; j++) {
                    int indiceHeap = (int) Math.pow(2, nivel) - 1 + j;
                    if (indiceHeap >= totalNodos) break;

                    int x = primerNodoX + j * espacioEntreNodos;
                    int y = inicioY + nivel * nivelH;
                    posiciones.put(indiceHeap, new Point(x, y));
                }
            }

            // dibujar aristas primero (para que queden detras de los nodos)
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (Map.Entry<Integer, Point> entry : posiciones.entrySet()) {
                int idx = entry.getKey();
                Point p = entry.getValue();

                int izq = 2 * idx + 1;
                int der = 2 * idx + 2;

                if (posiciones.containsKey(izq)) {
                    Point pIzq = posiciones.get(izq);
                    // linea con gradiente de color
                    g2.setColor(new Color(100, 150, 200, 180));
                    g2.drawLine(p.x, p.y + 25, pIzq.x, pIzq.y - 25);
                    // flecha pequeña
                    dibujarFlecha(g2, p.x, p.y + 25, pIzq.x, pIzq.y - 25);
                }
                if (posiciones.containsKey(der)) {
                    Point pDer = posiciones.get(der);
                    g2.setColor(new Color(180, 120, 100, 180));
                    g2.drawLine(p.x, p.y + 25, pDer.x, pDer.y - 25);
                    dibujarFlecha(g2, p.x, p.y + 25, pDer.x, pDer.y - 25);
                }
            }

            // dibujar nodos
            for (Map.Entry<Integer, Point> entry : posiciones.entrySet()) {
                int idx = entry.getKey();
                Point p = entry.getValue();
                Map.Entry<String, String> nodo = nodos.get(idx);

                boolean esResaltado = highlightKey != null && nodo.getKey().equals(highlightKey);
                dibujarNodo(g2, p.x, p.y, nodo.getKey(), nodo.getValue(), esResaltado, idx == 0);
            }
        }

        private void dibujarFlecha(Graphics2D g2, int x1, int y1, int x2, int y2) {
            double angle = Math.atan2(y2 - y1, x2 - x1);
            int arrowSize = 8;
            int ax = (int) (x2 - arrowSize * Math.cos(angle - Math.PI / 6));
            int ay = (int) (y2 - arrowSize * Math.sin(angle - Math.PI / 6));
            int bx = (int) (x2 - arrowSize * Math.cos(angle + Math.PI / 6));
            int by = (int) (y2 - arrowSize * Math.sin(angle + Math.PI / 6));
            g2.drawLine(x2, y2, ax, ay);
            g2.drawLine(x2, y2, bx, by);
        }

        private void dibujarNodo(Graphics2D g2, int cx, int cy, String clave, String extra,
                                  boolean resaltado, boolean esRaiz) {
            int radio = compacto ? 22 : 28;

            // sombra
            g2.setColor(new Color(0, 0, 0, 30));
            g2.fillOval(cx - radio + 2, cy - radio + 2, radio * 2, radio * 2);

            // circulo del nodo
            if (resaltado) {
                g2.setColor(new Color(255, 230, 150));
                g2.fillOval(cx - radio, cy - radio, radio * 2, radio * 2);
                g2.setColor(new Color(200, 150, 0));
                g2.setStroke(new BasicStroke(3f));
            } else if (esRaiz) {
                g2.setColor(new Color(200, 230, 255));
                g2.fillOval(cx - radio, cy - radio, radio * 2, radio * 2);
                g2.setColor(new Color(50, 120, 200));
                g2.setStroke(new BasicStroke(2.5f));
            } else {
                g2.setColor(new Color(235, 240, 250));
                g2.fillOval(cx - radio, cy - radio, radio * 2, radio * 2);
                g2.setColor(new Color(120, 140, 170));
                g2.setStroke(new BasicStroke(2f));
            }
            g2.drawOval(cx - radio, cy - radio, radio * 2, radio * 2);

            // texto: clave
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Segoe UI", Font.BOLD, compacto ? 10 : 11));
            FontMetrics fm = g2.getFontMetrics();
            String txtClave = clave.length() > 8 ? clave.substring(0, 7) + "." : clave;
            int tx = cx - fm.stringWidth(txtClave) / 2;
            g2.drawString(txtClave, tx, cy - (compacto ? 2 : 4));

            // texto: extra (valor)
            if (extra != null && !extra.isEmpty() && !compacto) {
                g2.setColor(new Color(80, 80, 80));
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 8));
                fm = g2.getFontMetrics();
                String txtExtra = extra.length() > 12 ? extra.substring(0, 11) + "." : extra;
                g2.drawString(txtExtra, cx - fm.stringWidth(txtExtra) / 2, cy + 12);
            }

            // indicador de raiz
            if (esRaiz) {
                g2.setColor(new Color(50, 120, 200));
                g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                fm = g2.getFontMetrics();
                String r = "RAIZ";
                g2.drawString(r, cx - fm.stringWidth(r) / 2, cy - radio - 4);
            }
        }
    }
}
