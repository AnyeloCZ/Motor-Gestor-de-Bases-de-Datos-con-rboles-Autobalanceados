package gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

// Ventana principal de la interfaz grafica con menu, barra de estado y 6 pestanas.
// Atajos: Ctrl+S = Guardar todo, Ctrl+Q = Salir, F5 = Refrescar.
//
// Para ejecutar: java -cp bin gui.DatabaseManagerGUI [--data ruta]
public class DatabaseManagerGUI extends JFrame {
    private final GuiContext context;
    private final JLabel statusLabel = new JLabel("Listo");
    private final JTabbedPane tabs;
    private SpacePanel spacePanel;
    private RecordPanel recordPanel;
    private SearchPanel searchPanel;
    private TreePanel treePanel;
    private ConsolePanel consolePanel;

    public DatabaseManagerGUI(String dataDir) {
        super("Motor BD AVL - Interfaz Gráfica");
        this.context = new GuiContext(dataDir);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 750);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Barra de menu
        setJMenuBar(crearMenuBar());

        // Pestanas
        tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        spacePanel   = new SpacePanel(context);
        recordPanel  = new RecordPanel(context);
        searchPanel  = new SearchPanel(context);
        treePanel    = new TreePanel(context);
        consolePanel = new ConsolePanel(context);

        tabs.addTab("  Espacios  ",     spacePanel);
        tabs.addTab("  Registros  ",    recordPanel);
        tabs.addTab("  Busquedas  ",    searchPanel);
        tabs.addTab("  Arbol AVL  ",    treePanel);
        tabs.addTab("  Consola  ",      consolePanel);
        tabs.addTab("  Ayuda  ",        new HelpPanel());

        // iconos unicode para las pestanas
        tabs.setIconAt(0, crearIconoTexto("DB "));
        tabs.setIconAt(1, crearIconoTexto("REC"));
        tabs.setIconAt(2, crearIconoTexto("Q  "));
        tabs.setIconAt(3, crearIconoTexto("AVL"));
        tabs.setIconAt(4, crearIconoTexto(">_ "));
        tabs.setIconAt(5, crearIconoTexto("?  "));

        tabs.addChangeListener(e -> actualizarBarraEstado());

        add(tabs, BorderLayout.CENTER);

        // Barra de estado inferior
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                new EmptyBorder(4, 10, 4, 10)));
        statusBar.setBackground(new Color(245, 245, 245));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusBar.add(statusLabel, BorderLayout.WEST);

        JLabel hintLabel = new JLabel("F5: Refrescar  |  Ctrl+S: Guardar  |  Ctrl+Q: Salir");
        hintLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hintLabel.setForeground(Color.GRAY);
        statusBar.add(hintLabel, BorderLayout.EAST);
        add(statusBar, BorderLayout.SOUTH);

        // actualizar barra de estado periodicamente (cada 3 segundos)
        new Timer(3000, e -> actualizarBarraEstado()).start();
        actualizarBarraEstado();

        // handler global de teclas
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED && e.isControlDown()) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_S: guardarTodo(); return true;
                    case KeyEvent.VK_Q: System.exit(0); return true;
                    case KeyEvent.VK_L: consolePanel.limpiarConsola(); return true;
                }
            }
            if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_F5) {
                context.notifyDataChanged();
                actualizarBarraEstado();
                return true;
            }
            return false;
        });
    }

    private JMenuBar crearMenuBar() {
        JMenuBar mb = new JMenuBar();

        // menu Archivo
        JMenu menuArchivo = new JMenu("Archivo");
        menuArchivo.setMnemonic(KeyEvent.VK_A);

        JMenuItem itemGuardar = new JMenuItem("Guardar todo (Ctrl+S)");
        itemGuardar.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        itemGuardar.addActionListener(e -> guardarTodo());
        menuArchivo.add(itemGuardar);

        menuArchivo.addSeparator();

        JMenuItem itemSalir = new JMenuItem("Salir (Ctrl+Q)");
        itemSalir.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK));
        itemSalir.addActionListener(e -> System.exit(0));
        menuArchivo.add(itemSalir);

        mb.add(menuArchivo);

        // menu Ver
        JMenu menuVer = new JMenu("Ver");
        menuVer.setMnemonic(KeyEvent.VK_V);
        JMenuItem itemRefrescar = new JMenuItem("Refrescar todo (F5)");
        itemRefrescar.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        itemRefrescar.addActionListener(e -> {
            context.notifyDataChanged();
            actualizarBarraEstado();
        });
        menuVer.add(itemRefrescar);

        JMenuItem itemMostrarArbol = new JMenuItem("Ir a visualizacion del arbol");
        itemMostrarArbol.addActionListener(e -> tabs.setSelectedIndex(3));
        menuVer.add(itemMostrarArbol);
        mb.add(menuVer);

        // menu Ayuda
        JMenu menuAyuda = new JMenu("Ayuda");
        menuAyuda.setMnemonic(KeyEvent.VK_Y);
        JMenuItem itemAyuda = new JMenuItem("Mostrar ayuda");
        itemAyuda.addActionListener(e -> tabs.setSelectedIndex(5));
        menuAyuda.add(itemAyuda);

        JMenuItem itemAcerca = new JMenuItem("Acerca de...");
        itemAcerca.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Motor BD AVL v2.1\n\n"
                + "Gestor de bases de datos con arboles AVL autobalanceados.\n"
                + "Proyecto Final - Ciencias de la Computacion I\n"
                + "Grupo 020-83  |  2026-I\n\n"
                + "Casas / Yanez / Herrera",
                "Acerca de", JOptionPane.INFORMATION_MESSAGE));
        menuAyuda.add(itemAcerca);
        mb.add(menuAyuda);

        return mb;
    }

    private void guardarTodo() {
        var res = context.execute("SAVE ALL");
        statusLabel.setText(res.getMensaje());
        if (!res.isError()) {
            statusLabel.setForeground(new Color(0, 128, 0));
        } else {
            statusLabel.setForeground(Color.RED);
        }
    }

    private void actualizarBarraEstado() {
        var res = context.execute("SHOW SPACES");
        String texto = res.getMensaje();
        if (texto != null && !texto.startsWith("(")) {
            int espacios = 0;
            int registros = 0;
            for (String line : texto.split("\\R")) {
                String t = line.trim();
                if (t.isEmpty() || t.startsWith("ESPACIO") || t.startsWith("-")) continue;
                espacios++;
                String[] partes = t.split("\\s+");
                if (partes.length >= 3) {
                    try { registros += Integer.parseInt(partes[2]); } catch (NumberFormatException ignored) {}
                }
            }
            statusLabel.setText(String.format("  %d espacio(s)  |  %d registro(s) totales", espacios, registros));
            statusLabel.setForeground(Color.DARK_GRAY);
        } else {
            statusLabel.setText("  Sin espacios creados");
            statusLabel.setForeground(Color.GRAY);
        }
    }

    private Icon crearIconoTexto(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("Monospaced", Font.BOLD, 10));
        lbl.setForeground(new Color(100, 100, 100));
        lbl.setSize(lbl.getPreferredSize());
        return new ImageIcon(crearImagenDeLabel(lbl));
    }

    private java.awt.Image crearImagenDeLabel(JLabel label) {
        BufferedImage img = new BufferedImage(label.getPreferredSize().width, label.getPreferredSize().height,
                BufferedImage.TYPE_INT_ARGB);
        label.paint(img.getGraphics());
        return img;
    }

    public static void main(String[] args) {
        String dataDir = "data";
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals("--data")) dataDir = args[i + 1];
        }
        final String selectedDataDir = dataDir;

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            // personalizar colores de Swing
            UIManager.put("TabbedPane.selected", new Color(240, 245, 255));
            UIManager.put("TabbedPane.background", new Color(230, 230, 240));
            UIManager.put("Panel.background", new Color(252, 252, 255));

        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new DatabaseManagerGUI(selectedDataDir).setVisible(true));
    }
}
