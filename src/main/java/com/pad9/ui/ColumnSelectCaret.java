package com.pad9.ui;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.ConfigurableCaret;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * A caret that supports Alt+drag rectangular/column selection
 * in addition to normal text selection.
 */
public class ColumnSelectCaret extends ConfigurableCaret {

    private boolean columnMode;
    private int anchorLine, anchorCol;
    private int lastStartLine = -1, lastEndLine, lastStartCol, lastEndCol;
    private final List<Object> highlightTags = new ArrayList<>();
    private Highlighter.HighlightPainter cachedPainter;
    private Color cachedSelectionColor;
    private String rectangularText;

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.isAltDown() && SwingUtilities.isLeftMouseButton(e)) {
            columnMode = true;
            clearColumnSelection();

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
            setDot(offset);
            e.consume();
        } else {
            clearColumnSelection();
            columnMode = false;
            super.mousePressed(e);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
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

            // Skip if selection bounds unchanged
            if (startLine == lastStartLine && endLine == lastEndLine
                    && startCol == lastStartCol && endCol == lastEndCol) {
                e.consume();
                return;
            }
            lastStartLine = startLine; lastEndLine = endLine;
            lastStartCol = startCol; lastEndCol = endCol;

            clearHighlights();

            Color selColor = ta.getSelectionColor();
            if (cachedPainter == null || !selColor.equals(cachedSelectionColor)) {
                cachedPainter = new DefaultHighlighter.DefaultHighlightPainter(selColor);
                cachedSelectionColor = selColor;
            }

            Highlighter h = ta.getHighlighter();
            StringBuilder sb = new StringBuilder();

            for (int line = startLine; line <= endLine; line++) {
                int lineStart = ta.getLineStartOffset(line);
                int lineEnd = ta.getLineEndOffset(line);
                int lineLen = Math.max(0, lineEnd - lineStart - 1);

                int from = lineStart + Math.min(startCol, lineLen);
                int to = lineStart + Math.min(endCol, lineLen);

                if (sb.length() > 0) sb.append('\n');
                if (from < to) {
                    highlightTags.add(h.addHighlight(from, to, cachedPainter));
                    sb.append(ta.getText(from, to - from));
                }
            }
            rectangularText = sb.toString();
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
            e.consume();
            return;
        }
        super.mouseReleased(e);
    }

    /**
     * Returns true if there is an active rectangular selection.
     */
    public boolean hasRectangularSelection() {
        return rectangularText != null && !rectangularText.isEmpty();
    }

    /**
     * Returns the text of the rectangular selection.
     */
    public String getRectangularText() {
        return rectangularText;
    }

    /**
     * Clears the rectangular selection and its highlights.
     */
    public void clearColumnSelection() {
        clearHighlights();
        rectangularText = null;
        lastStartLine = -1;
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
