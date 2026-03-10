package com.pad9.ui;

import com.pad9.core.FileManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

/**
 * The main application window for Pad9.
 * Contains a tabbed editor pane, menu bar with file operations,
 * and coordinates file I/O using virtual threads.
 */
public class MainFrame extends JFrame {

    private final EditorTabPane tabPane;
    private final StatusBar statusBar;
    private final SearchReplaceBar searchReplaceBar;
    private final JFileChooser fileChooser = new JFileChooser();

    /**
     * Creates the main application window with menu bar and tabbed editor.
     */
    public MainFrame() {
        super("Pad9");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        tabPane = new EditorTabPane();
        EditorPane initialEditor = tabPane.addNewTab();
        add(tabPane, BorderLayout.CENTER);

        statusBar = new StatusBar();
        searchReplaceBar = new SearchReplaceBar();
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(searchReplaceBar, BorderLayout.NORTH);
        bottomPanel.add(statusBar, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

        tabPane.addPropertyChangeListener("saveRequested", evt -> {
            if (evt.getNewValue() instanceof EditorPane editor) {
                saveEditor(editor);
            }
        });

        tabPane.addChangeListener(e -> {
            updateTitle();
            EditorPane editor = tabPane.getCurrentEditor();
            if (editor != null) wireStatusBar(editor);
        });

        setJMenuBar(createMenuBar());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleExit();
            }
        });

        wireStatusBar(initialEditor);
    }

    private JMenuBar createMenuBar() {
        int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        JMenuItem newItem = new JMenuItem("New");
        newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, shortcutMask));
        newItem.addActionListener(e -> tabPane.addNewTab());

        JMenuItem openItem = new JMenuItem("Open...");
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, shortcutMask));
        openItem.addActionListener(e -> openFile());

        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutMask));
        saveItem.addActionListener(e -> saveCurrentFile());

        JMenuItem saveAsItem = new JMenuItem("Save As...");
        saveAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                shortcutMask | KeyEvent.SHIFT_DOWN_MASK));
        saveAsItem.addActionListener(e -> saveCurrentFileAs());

        JMenuItem closeTabItem = new JMenuItem("Close Tab");
        closeTabItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, shortcutMask));
        closeTabItem.addActionListener(e -> tabPane.closeCurrentTab());

        fileMenu.add(newItem);
        fileMenu.addSeparator();
        fileMenu.add(openItem);
        fileMenu.addSeparator();
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(closeTabItem);

        fileMenu.addSeparator();
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, shortcutMask));
        exitItem.addActionListener(e -> handleExit());
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);

        JMenu editMenu = new JMenu("Edit");

        JMenuItem undoItem = new JMenuItem("Undo");
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcutMask));
        undoItem.addActionListener(e -> {
            EditorPane ep = tabPane.getCurrentEditor();
            if (ep != null && ep.getTextArea().canUndo()) ep.getTextArea().undoLastAction();
        });

        JMenuItem redoItem = new JMenuItem("Redo");
        redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                shortcutMask | KeyEvent.SHIFT_DOWN_MASK));
        redoItem.addActionListener(e -> {
            EditorPane ep = tabPane.getCurrentEditor();
            if (ep != null && ep.getTextArea().canRedo()) ep.getTextArea().redoLastAction();
        });

        editMenu.add(undoItem);
        editMenu.add(redoItem);
        editMenu.addSeparator();

        JMenuItem findItem = new JMenuItem("Find...");
        findItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, shortcutMask));
        findItem.addActionListener(e -> {
            EditorPane ep = tabPane.getCurrentEditor();
            if (ep != null) searchReplaceBar.showFind(ep.getTextArea());
        });

        JMenuItem replaceItem = new JMenuItem("Replace...");
        replaceItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, shortcutMask));
        replaceItem.addActionListener(e -> {
            EditorPane ep = tabPane.getCurrentEditor();
            if (ep != null) searchReplaceBar.showFindReplace(ep.getTextArea());
        });

        editMenu.add(findItem);
        editMenu.add(replaceItem);
        menuBar.add(editMenu);

        return menuBar;
    }

    private void openFile() {
        if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        openFile(fileChooser.getSelectedFile());
    }

    /**
     * Opens a file in a new tab using a virtual thread for I/O.
     *
     * @param file the file to open
     */
    public void openFile(File file) {
        Thread.startVirtualThread(() -> {
            try {
                FileManager.ReadResult result = FileManager.read(file.toPath());
                SwingUtilities.invokeLater(() -> {
                    EditorPane editor = new EditorPane();
                    editor.getTextArea().setText(result.content());
                    editor.getTextArea().setCaretPosition(0);
                    editor.setFile(file);
                    editor.setCharset(result.charset());
                    editor.clearModified();
                    tabPane.addTab(editor);
                    updateTitle();
                    wireStatusBar(editor);
                });
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this,
                            "Failed to open file: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE));
            }
        });
    }

    private void saveCurrentFile() {
        EditorPane editor = tabPane.getCurrentEditor();
        if (editor != null) saveEditor(editor);
    }

    private void saveCurrentFileAs() {
        EditorPane editor = tabPane.getCurrentEditor();
        if (editor == null) return;
        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        editor.setFile(fileChooser.getSelectedFile());
        saveEditor(editor);
    }

    private void saveEditor(EditorPane editor) {
        File file = editor.getFile();
        if (file == null) {
            if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            file = fileChooser.getSelectedFile();
            editor.setFile(file);
        }
        File targetFile = file;
        Thread.startVirtualThread(() -> {
            try {
                FileManager.write(targetFile.toPath(),
                        editor.getTextArea().getText(),
                        editor.getCharset());
                SwingUtilities.invokeLater(() -> {
                    editor.clearModified();
                    updateTitle();
                });
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this,
                            "Failed to save file: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE));
            }
        });
    }

    private void updateTitle() {
        EditorPane editor = tabPane.getCurrentEditor();
        if (editor != null && editor.getFile() != null) {
            setTitle("Pad9 \u2014 " + editor.getFile().getName());
        } else {
            setTitle("Pad9");
        }
    }

    private void handleExit() {
        for (int i = 0; i < tabPane.getTabCount(); i++) {
            Component comp = tabPane.getComponentAt(i);
            if (comp instanceof EditorPane editor && editor.isModified()) {
                int choice = JOptionPane.showConfirmDialog(this,
                        "There are unsaved changes. Save before exit?",
                        "Unsaved Changes",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (choice == JOptionPane.CANCEL_OPTION) return;
                if (choice == JOptionPane.YES_OPTION) {
                    for (int j = 0; j < tabPane.getTabCount(); j++) {
                        Component c = tabPane.getComponentAt(j);
                        if (c instanceof EditorPane ep && ep.isModified()) {
                            saveEditor(ep);
                        }
                    }
                }
                break;
            }
        }
        dispose();
        System.exit(0);
    }

    private void wireStatusBar(EditorPane editor) {
        editor.getTextArea().addCaretListener(e -> {
            int line = editor.getTextArea().getCaretLineNumber() + 1;
            int col = editor.getTextArea().getCaretOffsetFromLineStart() + 1;
            statusBar.updatePosition(line, col);
        });
        statusBar.updateEncoding(editor.getCharset().displayName());
        statusBar.updateLanguage(editor.getLanguageName());
        statusBar.updateFileSize(editor.getFile());
    }

    /**
     * Returns the tab pane for external access.
     *
     * @return the editor tab pane
     */
    public EditorTabPane getTabPane() {
        return tabPane;
    }
}
