package com.pad9.ui;

import com.pad9.core.FileManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

/**
 * The main application window for Pad9.
 * Contains the editor pane, menu bar with file operations,
 * and coordinates file I/O using virtual threads.
 */
public class MainFrame extends JFrame {

    private final EditorPane editorPane;
    private final JFileChooser fileChooser = new JFileChooser();

    /**
     * Creates the main application window with menu bar and editor.
     */
    public MainFrame() {
        super("Pad9");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        editorPane = new EditorPane();
        add(editorPane, BorderLayout.CENTER);

        setJMenuBar(createMenuBar());
    }

    private JMenuBar createMenuBar() {
        int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        JMenuItem newItem = new JMenuItem("New");
        newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, shortcutMask));
        newItem.addActionListener(e -> newFile());

        JMenuItem openItem = new JMenuItem("Open...");
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, shortcutMask));
        openItem.addActionListener(e -> openFile());

        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutMask));
        saveItem.addActionListener(e -> saveFile());

        JMenuItem saveAsItem = new JMenuItem("Save As...");
        saveAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                shortcutMask | KeyEvent.SHIFT_DOWN_MASK));
        saveAsItem.addActionListener(e -> saveFileAs());

        fileMenu.add(newItem);
        fileMenu.addSeparator();
        fileMenu.add(openItem);
        fileMenu.addSeparator();
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);

        menuBar.add(fileMenu);
        return menuBar;
    }

    private void newFile() {
        editorPane.getTextArea().setText("");
        editorPane.setFile(null);
        editorPane.clearModified();
        setTitle("Pad9 — Untitled");
    }

    private void openFile() {
        if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        openFile(fileChooser.getSelectedFile());
    }

    /**
     * Opens the specified file in the editor using a virtual thread for I/O.
     *
     * @param file the file to open
     */
    public void openFile(File file) {
        Thread.startVirtualThread(() -> {
            try {
                FileManager.ReadResult result = FileManager.read(file.toPath());
                SwingUtilities.invokeLater(() -> {
                    editorPane.getTextArea().setText(result.content());
                    editorPane.getTextArea().setCaretPosition(0);
                    editorPane.setFile(file);
                    editorPane.setCharset(result.charset());
                    editorPane.clearModified();
                    setTitle("Pad9 — " + file.getName());
                });
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this,
                            "Failed to open file: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE));
            }
        });
    }

    private void saveFile() {
        File file = editorPane.getFile();
        if (file == null) { saveFileAs(); return; }
        doSave(file);
    }

    private void saveFileAs() {
        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = fileChooser.getSelectedFile();
        editorPane.setFile(file);
        doSave(file);
    }

    private void doSave(File file) {
        Thread.startVirtualThread(() -> {
            try {
                FileManager.write(file.toPath(),
                        editorPane.getTextArea().getText(),
                        editorPane.getCharset());
                SwingUtilities.invokeLater(() -> {
                    editorPane.clearModified();
                    setTitle("Pad9 — " + file.getName());
                });
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this,
                            "Failed to save file: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE));
            }
        });
    }

    /** Returns the current editor pane. */
    public EditorPane getCurrentEditor() {
        return editorPane;
    }
}
