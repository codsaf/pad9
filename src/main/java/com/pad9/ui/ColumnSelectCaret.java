package com.pad9.ui;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.ConfigurableCaret;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * A caret that supports Alt+drag rectangular/column selection
 * and multi-line column editing (insert, backspace, delete on all selected lines).
 * All column cursors are painted uniformly with synchronized blinking.
 */
public class ColumnSelectCaret extends ConfigurableCaret {

    private static final int CURSOR_WIDTH = 2;

    private boolean columnMode;        // true during Alt+drag
    private boolean activeSelection;   // true when column selection is active (after drag release)
    private boolean columnEditing;     // true during batch edit operations (guards DocumentListener)

    private int anchorLine, anchorCol;
    private int lastStartLine = -1, lastEndLine, lastStartCol, lastEndCol;
    private int cursorCol;

    private final List<Object> highlightTags = new ArrayList<>();
    private Highlighter.HighlightPainter selectionPainter;
    private Color cachedSelectionColor;
    private String rectangularText;

    // --- Caret painting ---

    @Override
    public void paint(Graphics g) {
        // Outside column mode: default caret behavior
        if (!activeSelection || lastStartLine < 0) {
            super.paint(g);
            return;
        }

        // Column mode: paint ALL cursors ourselves at cursorCol for consistent width & blinking
        if (!isVisible()) return;

        RSyntaxTextArea ta = (RSyntaxTextArea) getComponent();
        if (ta == null) return;

        g.setColor(ta.getCaretColor());
        try {
            for (int line = lastStartLine; line <= lastEndLine; line++) {
                int lineStart = ta.getLineStartOffset(line);
                int lineLen = ta.getLineEndOffset(line) - lineStart - 1;
                int pos = lineStart + Math.min(cursorCol, lineLen);
                Rectangle2D r = ta.modelToView2D(pos);
                if (r != null) {
                    g.fillRect((int) r.getX(), (int) r.getY(), CURSOR_WIDTH, (int) r.getHeight());
                }
            }
        } catch (BadLocationException ignored) {}
    }

    @Override
    public void setVisible(boolean v) {
        super.setVisible(v);
        // Trigger full repaint so all column cursors blink in sync
        if (activeSelection && lastStartLine >= 0) {
            var c = getComponent();
            if (c != null) c.repaint();
        }
    }

    // --- Mouse event overrides ---

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.isAltDown() && SwingUtilities.isLeftMouseButton(e)) {
            enterColumnDrag(e);
        } else {
            exitColumnMode();
            columnMode = false;
            super.mousePressed(e);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        // Support pressing Alt mid-drag to switch into column mode
        if (!columnMode && e.isAltDown() && SwingUtilities.isLeftMouseButton(e)) {
            RSyntaxTextArea ta = (RSyntaxTextArea) getComponent();
            try {
                int markPos = getMark();
                anchorLine = ta.getLineOfOffset(markPos);
                anchorCol = markPos - ta.getLineStartOffset(anchorLine);
                columnMode = true;
            } catch (BadLocationException ex) {
                super.mouseDragged(e);
                return;
            }
        }

        if (!columnMode) {
            super.mouseDragged(e);
            return;
        }

        RSyntaxTextArea ta = (RSyntaxTextArea) getComponent();
        int offset = ta.viewToModel2D(e.getPoint());
        try {
            int curLine = ta.getLineOfOffset(offset);
            int curCol = offset - ta.getLineStartOffset(curLine);

            int startLine = Math.min(anchorLine, curLine);
            int endLine = Math.max(anchorLine, curLine);
            int startCol = Math.min(anchorCol, curCol);
            int endCol = Math.max(anchorCol, curCol);

            if (startLine == lastStartLine && endLine == lastEndLine
                    && startCol == lastStartCol && endCol == lastEndCol) {
                e.consume();
                return;
            }
            lastStartLine = startLine; lastEndLine = endLine;
            lastStartCol = startCol; lastEndCol = endCol;

            updateHighlights(ta);
        } catch (BadLocationException ex) {
            clearHighlights();
            rectangularText = null;
        }
        e.consume();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (columnMode) {
            columnMode = false;
            if (lastStartLine >= 0) {
                activeSelection = true;
                // Cursor at the non-anchor end of the selection (where the mouse ended)
                cursorCol = (anchorCol == lastStartCol) ? lastEndCol : lastStartCol;
                syncMainCaret();
                // Repaint to show blinking cursors
                var c = getComponent();
                if (c != null) c.repaint();
            }
            e.consume();
            return;
        }
        super.mouseReleased(e);
    }

    private void enterColumnDrag(MouseEvent e) {
        columnMode = true;
        exitColumnMode();

        RSyntaxTextArea ta = (RSyntaxTextArea) getComponent();
        int offset = ta.viewToModel2D(e.getPoint());
        try {
            anchorLine = ta.getLineOfOffset(offset);
            anchorCol = offset - ta.getLineStartOffset(anchorLine);
        } catch (BadLocationException ex) {
            columnMode = false;
            super.mousePressed(e);
            return;
        }
        super.setDot(offset);
        e.consume();
    }

    // --- Main caret sync ---

    /** Positions the main caret at cursorCol on the first selected line. */
    private void syncMainCaret() {
        RSyntaxTextArea ta = (RSyntaxTextArea) getComponent();
        if (ta == null || lastStartLine < 0) return;
        try {
            int lineStart = ta.getLineStartOffset(lastStartLine);
            int lineLen = ta.getLineEndOffset(lastStartLine) - lineStart - 1;
            int pos = lineStart + Math.min(cursorCol, lineLen);
            super.setDot(pos);
        } catch (BadLocationException ignored) {}
    }

    // --- Query methods ---

    /** Returns true if there is an active rectangular selection with text. */
    public boolean hasRectangularSelection() {
        return rectangularText != null && !rectangularText.isEmpty();
    }

    /** Returns the text of the rectangular selection. */
    public String getRectangularText() {
        return rectangularText;
    }

    /** Returns true if column selection is active (for editing). */
    public boolean isColumnSelectionActive() {
        return activeSelection && lastStartLine >= 0;
    }

    /** Returns true if a column edit operation is in progress. */
    public boolean isColumnEditing() {
        return columnEditing;
    }

    // --- Column editing operations ---

    /**
     * Inserts a character at the cursor column on every selected line.
     * If a rectangular selection exists, deletes it first.
     * Short lines are padded with spaces to reach the cursor column.
     */
    public void columnInsert(char ch) {
        if (!isColumnSelectionActive()) return;
        columnEditing = true;
        RSyntaxTextArea ta = (RSyntaxTextArea) getComponent();
        ta.beginAtomicEdit();
        try {
            if (lastStartCol != lastEndCol) {
                deleteSelectedRect(ta);
                cursorCol = lastStartCol;
                lastEndCol = lastStartCol;
            }

            String s = String.valueOf(ch);
            for (int line = lastEndLine; line >= lastStartLine; line--) {
                int lineStart = ta.getLineStartOffset(line);
                int lineLen = ta.getLineEndOffset(line) - lineStart - 1;
                if (cursorCol > lineLen) {
                    String padding = " ".repeat(cursorCol - lineLen);
                    ta.getDocument().insertString(lineStart + lineLen, padding + s, null);
                } else {
                    ta.getDocument().insertString(lineStart + cursorCol, s, null);
                }
            }
            cursorCol++;
            lastStartCol = lastEndCol = cursorCol;
            syncMainCaret();
            updateHighlights(ta);
        } catch (BadLocationException ignored) {
        } finally {
            ta.endAtomicEdit();
            columnEditing = false;
        }
    }

    /**
     * Deletes the character before the cursor column on every selected line,
     * or deletes the rectangular selection if one exists.
     */
    public void columnBackspace() {
        if (!isColumnSelectionActive()) return;
        columnEditing = true;
        RSyntaxTextArea ta = (RSyntaxTextArea) getComponent();
        ta.beginAtomicEdit();
        try {
            if (lastStartCol != lastEndCol) {
                deleteSelectedRect(ta);
                cursorCol = lastStartCol;
                lastEndCol = lastStartCol;
            } else {
                if (cursorCol <= 0) return;
                for (int line = lastEndLine; line >= lastStartLine; line--) {
                    int lineStart = ta.getLineStartOffset(line);
                    int lineLen = ta.getLineEndOffset(line) - lineStart - 1;
                    if (cursorCol - 1 < lineLen) {
                        ta.getDocument().remove(lineStart + cursorCol - 1, 1);
                    }
                }
                cursorCol--;
                lastStartCol = lastEndCol = cursorCol;
            }
            syncMainCaret();
            updateHighlights(ta);
        } catch (BadLocationException ignored) {
        } finally {
            ta.endAtomicEdit();
            columnEditing = false;
        }
    }

    /**
     * Deletes the character at the cursor column on every selected line,
     * or deletes the rectangular selection if one exists.
     */
    public void columnDelete() {
        if (!isColumnSelectionActive()) return;
        columnEditing = true;
        RSyntaxTextArea ta = (RSyntaxTextArea) getComponent();
        ta.beginAtomicEdit();
        try {
            if (lastStartCol != lastEndCol) {
                deleteSelectedRect(ta);
                cursorCol = lastStartCol;
                lastEndCol = lastStartCol;
            } else {
                for (int line = lastEndLine; line >= lastStartLine; line--) {
                    int lineStart = ta.getLineStartOffset(line);
                    int lineLen = ta.getLineEndOffset(line) - lineStart - 1;
                    if (cursorCol < lineLen) {
                        ta.getDocument().remove(lineStart + cursorCol, 1);
                    }
                }
            }
            syncMainCaret();
            updateHighlights(ta);
        } catch (BadLocationException ignored) {
        } finally {
            ta.endAtomicEdit();
            columnEditing = false;
        }
    }

    private void deleteSelectedRect(RSyntaxTextArea ta) throws BadLocationException {
        for (int line = lastEndLine; line >= lastStartLine; line--) {
            int lineStart = ta.getLineStartOffset(line);
            int lineLen = ta.getLineEndOffset(line) - lineStart - 1;
            int from = Math.min(lastStartCol, lineLen);
            int to = Math.min(lastEndCol, lineLen);
            if (to > from) {
                ta.getDocument().remove(lineStart + from, to - from);
            }
        }
    }

    // --- Column cursor movement (Left/Right) ---

    /** Moves all column cursors one position to the left. */
    public void columnMoveLeft() {
        if (!isColumnSelectionActive() || cursorCol <= 0) return;
        cursorCol--;
        lastStartCol = lastEndCol = cursorCol;
        syncMainCaret();
        updateHighlights((RSyntaxTextArea) getComponent());
    }

    /** Moves all column cursors one position to the right. */
    public void columnMoveRight() {
        if (!isColumnSelectionActive()) return;
        RSyntaxTextArea ta = (RSyntaxTextArea) getComponent();
        try {
            int maxLen = 0;
            for (int line = lastStartLine; line <= lastEndLine; line++) {
                int lineStart = ta.getLineStartOffset(line);
                maxLen = Math.max(maxLen, ta.getLineEndOffset(line) - lineStart - 1);
            }
            if (cursorCol >= maxLen) return;
        } catch (BadLocationException ignored) { return; }
        cursorCol++;
        lastStartCol = lastEndCol = cursorCol;
        syncMainCaret();
        updateHighlights(ta);
    }

    // --- Column selection extension (Alt+Up/Down) ---

    /** Extends the column selection one line upward, or creates one from the caret position. */
    public void extendColumnUp() {
        RSyntaxTextArea ta = (RSyntaxTextArea) getComponent();
        if (!isColumnSelectionActive()) {
            try {
                int offset = ta.getCaretPosition();
                int line = ta.getLineOfOffset(offset);
                int col = offset - ta.getLineStartOffset(line);
                if (line == 0) return;
                lastStartLine = line - 1;
                lastEndLine = line;
                lastStartCol = lastEndCol = col;
                cursorCol = col;
                activeSelection = true;
            } catch (BadLocationException ex) {
                return;
            }
        } else {
            if (lastStartLine > 0) lastStartLine--;
        }
        syncMainCaret();
        updateHighlights(ta);
    }

    /** Extends the column selection one line downward, or creates one from the caret position. */
    public void extendColumnDown() {
        RSyntaxTextArea ta = (RSyntaxTextArea) getComponent();
        if (!isColumnSelectionActive()) {
            try {
                int offset = ta.getCaretPosition();
                int line = ta.getLineOfOffset(offset);
                int col = offset - ta.getLineStartOffset(line);
                if (line >= ta.getLineCount() - 1) return;
                lastStartLine = line;
                lastEndLine = line + 1;
                lastStartCol = lastEndCol = col;
                cursorCol = col;
                activeSelection = true;
            } catch (BadLocationException ex) {
                return;
            }
        } else {
            if (lastEndLine < ta.getLineCount() - 1) lastEndLine++;
        }
        syncMainCaret();
        updateHighlights(ta);
    }

    // --- Exit / clear ---

    /** Exits column selection mode, clearing all highlights and state. */
    public void exitColumnMode() {
        activeSelection = false;
        clearHighlights();
        rectangularText = null;
        lastStartLine = -1;
        var c = getComponent();
        if (c != null) c.repaint();
    }

    /** Clears the rectangular selection (alias for exitColumnMode). */
    public void clearColumnSelection() {
        exitColumnMode();
    }

    // --- Highlight management (rectangular selection only) ---

    private void updateHighlights(RSyntaxTextArea ta) {
        clearHighlights();
        if (lastStartLine < 0) return;

        // Cursor-only mode: paint() handles cursor drawing
        if (lastStartCol == lastEndCol) {
            rectangularText = null;
            ta.repaint();
            return;
        }

        try {
            Color selColor = ta.getSelectionColor();
            if (selectionPainter == null || !selColor.equals(cachedSelectionColor)) {
                selectionPainter = new DefaultHighlighter.DefaultHighlightPainter(selColor);
                cachedSelectionColor = selColor;
            }

            Highlighter h = ta.getHighlighter();
            StringBuilder sb = new StringBuilder();

            for (int line = lastStartLine; line <= lastEndLine; line++) {
                int lineStart = ta.getLineStartOffset(line);
                int lineLen = ta.getLineEndOffset(line) - lineStart - 1;
                int from = lineStart + Math.min(lastStartCol, lineLen);
                int to = lineStart + Math.min(lastEndCol, lineLen);

                if (sb.length() > 0) sb.append('\n');
                if (from < to) {
                    highlightTags.add(h.addHighlight(from, to, selectionPainter));
                    sb.append(ta.getText(from, to - from));
                }
            }
            rectangularText = sb.length() > 0 ? sb.toString() : null;
        } catch (BadLocationException ex) {
            clearHighlights();
            rectangularText = null;
        }
    }

    private void clearHighlights() {
        var c = getComponent();
        if (c == null) return;
        Highlighter h = c.getHighlighter();
        for (Object tag : highlightTags) {
            h.removeHighlight(tag);
        }
        highlightTags.clear();
    }
}
