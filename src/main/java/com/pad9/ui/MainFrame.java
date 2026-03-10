package com.pad9.ui;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    private final EditorPane editorPane;

    /**
     * Creates the main application window.
     */
    public MainFrame() {
        super("Pad9");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        editorPane = new EditorPane();
        add(editorPane, BorderLayout.CENTER);
    }

    /** Returns the current editor pane. */
    public EditorPane getCurrentEditor() {
        return editorPane;
    }
}
