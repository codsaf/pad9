package com.pad9.ui;

import com.pad9.core.SyntaxMapper;
import com.pad9.util.FontUtil;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.CaretStyle;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class EditorPane extends JPanel {

    private final RSyntaxTextArea textArea;
    private final RTextScrollPane scrollPane;
    private File file;
    private Charset charset = StandardCharsets.UTF_8;
    private boolean modified = false;

    /**
     * Creates a new editor pane.
     */
    public EditorPane() {
        super(new BorderLayout());
        textArea = new RSyntaxTextArea();
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(true);
        textArea.setFractionalFontMetricsEnabled(false);
        textArea.setTabSize(4);
        textArea.setTabsEmulated(true);

        try {
            Theme theme = Theme.load(
                    getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/dark.xml"));
            theme.apply(textArea);
        } catch (IOException e) {
            // Fallback to default theme if dark theme can't be loaded
        }

        textArea.setFont(FontUtil.getEditorFont(14));
        textArea.setCaretStyle(RSyntaxTextArea.INSERT_MODE, CaretStyle.VERTICAL_LINE_STYLE);

        scrollPane = new RTextScrollPane(textArea);
        scrollPane.setLineNumbersEnabled(true);
        add(scrollPane, BorderLayout.CENTER);

        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { markModified(); }
            @Override
            public void removeUpdate(DocumentEvent e) { markModified(); }
            @Override
            public void changedUpdate(DocumentEvent e) {}
        });
    }

    private void markModified() {
        if (!modified) {
            modified = true;
            firePropertyChange("modified", false, true);
        }
    }

    /** Returns the underlying RSyntaxTextArea. */
    public RSyntaxTextArea getTextArea() { return textArea; }

    /** Returns the file this editor is associated with, or null if untitled. */
    public File getFile() { return file; }

    /** Sets the file and updates syntax highlighting. */
    public void setFile(File file) {
        this.file = file;
        if (file != null) {
            textArea.setSyntaxEditingStyle(SyntaxMapper.getSyntaxStyle(file.getName()));
        }
    }

    /** Returns the charset used for this file. */
    public Charset getCharset() { return charset; }

    /** Sets the charset for this file. */
    public void setCharset(Charset charset) { this.charset = charset; }

    /** Returns true if the content has been modified since last save. */
    public boolean isModified() { return modified; }

    /** Clears the modified flag (call after save). */
    public void clearModified() {
        if (modified) {
            modified = false;
            firePropertyChange("modified", true, false);
        }
    }

    /** Returns the display title for this editor tab. */
    public String getTitle() {
        String name = file != null ? file.getName() : "Untitled";
        return modified ? "\u25cf " + name : name;
    }

    /** Returns the syntax style name for the status bar. */
    public String getLanguageName() {
        return SyntaxMapper.getLanguageName(textArea.getSyntaxEditingStyle());
    }
}
