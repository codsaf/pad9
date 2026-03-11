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
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
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
        ColumnSelectCaret columnCaret = new ColumnSelectCaret();

        // Override processKeyEvent at the lowest level to intercept ALL key input
        // during column editing, before InputMap/ActionMap or any other processing.
        textArea = new RSyntaxTextArea() {
            @Override
            protected void processKeyEvent(KeyEvent e) {
                if (getCaret() instanceof ColumnSelectCaret cc) {
                    // Alt+Up/Down: extend column selection (works even without active selection)
                    if (e.getID() == KeyEvent.KEY_PRESSED && e.isAltDown()) {
                        if (e.getKeyCode() == KeyEvent.VK_UP) {
                            cc.extendColumnUp(); e.consume(); return;
                        }
                        if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                            cc.extendColumnDown(); e.consume(); return;
                        }
                    }

                    if (cc.isColumnSelectionActive()) {
                        // KEY_TYPED: insert printable characters, consume all others
                        if (e.getID() == KeyEvent.KEY_TYPED) {
                            char ch = e.getKeyChar();
                            if (ch != KeyEvent.CHAR_UNDEFINED && !Character.isISOControl(ch)) {
                                cc.columnInsert(ch);
                            }
                            e.consume();
                            return;
                        }

                        if (e.getID() == KeyEvent.KEY_PRESSED) {
                            switch (e.getKeyCode()) {
                                case KeyEvent.VK_BACK_SPACE -> cc.columnBackspace();
                                case KeyEvent.VK_DELETE -> cc.columnDelete();
                                case KeyEvent.VK_ESCAPE -> cc.exitColumnMode();
                                case KeyEvent.VK_LEFT -> cc.columnMoveLeft();
                                case KeyEvent.VK_RIGHT -> cc.columnMoveRight();
                                case KeyEvent.VK_ENTER, KeyEvent.VK_TAB, KeyEvent.VK_HOME, KeyEvent.VK_END,
                                     KeyEvent.VK_PAGE_UP, KeyEvent.VK_PAGE_DOWN -> {
                                    cc.exitColumnMode();
                                    super.processKeyEvent(e);
                                    return;
                                }
                                default -> {
                                    // Allow Ctrl/Meta combos (copy, undo, etc.) to pass through
                                    if (e.isControlDown() || e.isMetaDown()) {
                                        super.processKeyEvent(e);
                                        return;
                                    }
                                }
                            }
                            e.consume();
                            return;
                        }

                        // Consume KEY_RELEASED to prevent any lingering handling
                        e.consume();
                        return;
                    }
                }
                super.processKeyEvent(e);
            }
        };
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
        } catch (IOException ignored) {}

        textArea.setFont(FontUtil.getEditorFont(14));
        textArea.setCaret(columnCaret);
        textArea.setCaretStyle(RSyntaxTextArea.INSERT_MODE, CaretStyle.VERTICAL_LINE_STYLE);

        // Override copy to support rectangular selection
        ActionMap am = textArea.getActionMap();
        var defaultCopy = am.get("copy");
        am.put("copy", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (columnCaret.hasRectangularSelection()) {
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                            new StringSelection(columnCaret.getRectangularText()), null);
                } else {
                    defaultCopy.actionPerformed(e);
                }
            }
        });

        scrollPane = new RTextScrollPane(textArea);
        scrollPane.setLineNumbersEnabled(true);
        add(scrollPane, BorderLayout.CENTER);

        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (!columnCaret.isColumnEditing()) columnCaret.clearColumnSelection();
                markModified();
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                if (!columnCaret.isColumnEditing()) columnCaret.clearColumnSelection();
                markModified();
            }
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
