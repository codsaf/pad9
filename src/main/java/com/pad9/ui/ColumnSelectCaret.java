package com.pad9.ui;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.ConfigurableCaret;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * A caret that supports Alt+drag rectangular/column selection
 * in addition to normal text selection.
 */
public class ColumnSelectCaret extends ConfigurableCaret {

    private boolean columnMode;
    private int anchorLine;
    private int anchorCol;
    private final List<Object> highlightTags = new ArrayList<>();
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

            clearHighlights();

            Highlighter h = ta.getHighlighter();
            var painter = new DefaultHighlighter.DefaultHighlightPainter(ta.getSelectionColor());
            StringBuilder sb = new StringBuilder();

            for (int line = startLine; line <= endLine; line++) {
                int lineStart = ta.getLineStartOffset(line);
                int lineEnd = ta.getLineEndOffset(line);
                int lineLen = Math.max(0, lineEnd - lineStart - 1);

                int from = lineStart + Math.min(startCol, lineLen);
                int to = lineStart + Math.min(endCol, lineLen);

                if (sb.length() > 0) sb.append('\n');
                if (from < to) {
                    highlightTags.add(h.addHighlight(from, to, painter));
                    sb.append(ta.getText(from, to - from));
                }
            }
            rectangularText = sb.toString();
        } catch (BadLocationException ex) {
            // ignore
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
