package com.pad9.ui;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * A status bar panel displayed at the bottom of the main window.
 * Shows cursor position (line/column), file encoding, language type, and file size.
 */
public class StatusBar extends JPanel {

    private final JLabel positionLabel = new JLabel("Ln 1, Col 1");
    private final JLabel encodingLabel = new JLabel("UTF-8");
    private final JLabel languageLabel = new JLabel("Plain Text");
    private final JLabel fileSizeLabel = new JLabel("");

    /**
     * Creates the status bar panel.
     */
    public StatusBar() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setOpaque(false);
        leftPanel.add(positionLabel);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightPanel.setOpaque(false);
        rightPanel.add(encodingLabel);
        rightPanel.add(languageLabel);
        rightPanel.add(fileSizeLabel);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.EAST);
    }

    /**
     * Updates the cursor position display.
     *
     * @param line   the 1-based line number
     * @param column the 1-based column number
     */
    public void updatePosition(int line, int column) {
        positionLabel.setText("Ln " + line + ", Col " + column);
    }

    /**
     * Updates the encoding display.
     *
     * @param encoding the charset display name (e.g. "UTF-8")
     */
    public void updateEncoding(String encoding) {
        encodingLabel.setText(encoding);
    }

    /**
     * Updates the language display.
     *
     * @param language the language name (e.g. "Java", "Plain Text")
     */
    public void updateLanguage(String language) {
        languageLabel.setText(language);
    }

    /**
     * Updates the file size display. Shows nothing for unsaved files.
     *
     * @param file the file to read size from, or null for untitled editors
     */
    public void updateFileSize(File file) {
        if (file == null || !file.exists()) {
            fileSizeLabel.setText("");
            return;
        }
        long bytes = file.length();
        if (bytes < 1024) {
            fileSizeLabel.setText(bytes + " B");
        } else if (bytes < 1024 * 1024) {
            fileSizeLabel.setText(String.format("%.1f KB", bytes / 1024.0));
        } else {
            fileSizeLabel.setText(String.format("%.1f MB", bytes / (1024.0 * 1024)));
        }
    }
}
