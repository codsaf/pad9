package com.pad9.ui;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * A search and replace bar that integrates with RSyntaxTextArea's SearchEngine.
 * Supports case-sensitive search, regular expressions, find next/previous,
 * single replace, and replace all.
 */
public class SearchReplaceBar extends JPanel {

    private final JTextField searchField = new JTextField(30);
    private final JTextField replaceField = new JTextField(30);
    private final JToggleButton caseSensitiveBtn = new JToggleButton("Aa");
    private final JToggleButton regexBtn = new JToggleButton(".*");
    private final JPanel replacePanel;
    private final JLabel resultLabel = new JLabel("");
    private RSyntaxTextArea currentTextArea;

    /**
     * Creates the search/replace bar with find and replace rows.
     * The bar is initially hidden; call {@link #showFind} or {@link #showFindReplace}
     * to display it.
     */
    public SearchReplaceBar() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        setVisible(false);

        // Search row
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        searchPanel.add(new JLabel("Find:"));
        searchPanel.add(searchField);

        JButton prevBtn = new JButton("\u25C0");
        JButton nextBtn = new JButton("\u25B6");
        prevBtn.addActionListener(e -> findNext(false));
        nextBtn.addActionListener(e -> findNext(true));

        searchPanel.add(prevBtn);
        searchPanel.add(nextBtn);
        searchPanel.add(caseSensitiveBtn);
        searchPanel.add(regexBtn);
        searchPanel.add(resultLabel);

        JButton closeBtn = new JButton("\u00D7");
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.addActionListener(e -> dismiss());
        searchPanel.add(closeBtn);

        // Replace row
        replacePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        replacePanel.add(new JLabel("Replace:"));
        replacePanel.add(replaceField);

        JButton replaceBtn = new JButton("Replace");
        JButton replaceAllBtn = new JButton("Replace All");
        replaceBtn.addActionListener(e -> replace());
        replaceAllBtn.addActionListener(e -> replaceAll());

        replacePanel.add(replaceBtn);
        replacePanel.add(replaceAllBtn);

        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.add(searchPanel);
        rows.add(replacePanel);
        add(rows, BorderLayout.CENTER);

        // Enter in search field triggers find next
        searchField.addActionListener(e -> findNext(true));

        // Escape closes the bar (for both search and replace fields)
        InputMap im = searchField.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = searchField.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        am.put("close", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dismiss();
            }
        });

        InputMap rim = replaceField.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap ram = replaceField.getActionMap();
        rim.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        ram.put("close", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dismiss();
            }
        });
    }

    /**
     * Shows the search bar in find-only mode.
     *
     * @param textArea the text area to search within
     */
    public void showFind(RSyntaxTextArea textArea) {
        this.currentTextArea = textArea;
        replacePanel.setVisible(false);
        setVisible(true);
        searchField.requestFocusInWindow();
        searchField.selectAll();
    }

    /**
     * Shows the search bar in find and replace mode.
     *
     * @param textArea the text area to search and replace within
     */
    public void showFindReplace(RSyntaxTextArea textArea) {
        this.currentTextArea = textArea;
        replacePanel.setVisible(true);
        setVisible(true);
        searchField.requestFocusInWindow();
        searchField.selectAll();
    }

    /**
     * Dismisses the search bar and returns focus to the editor.
     */
    public void dismiss() {
        setVisible(false);
        if (currentTextArea != null) {
            currentTextArea.requestFocusInWindow();
        }
    }

    private SearchContext createContext() {
        SearchContext ctx = new SearchContext();
        ctx.setSearchFor(searchField.getText());
        ctx.setMatchCase(caseSensitiveBtn.isSelected());
        ctx.setRegularExpression(regexBtn.isSelected());
        ctx.setWholeWord(false);
        return ctx;
    }

    private void findNext(boolean forward) {
        if (currentTextArea == null) return;
        SearchContext ctx = createContext();
        ctx.setSearchForward(forward);
        SearchResult result = SearchEngine.find(currentTextArea, ctx);
        if (!result.wasFound()) {
            resultLabel.setText("No results");
        } else {
            resultLabel.setText("");
        }
    }

    private void replace() {
        if (currentTextArea == null) return;
        SearchContext ctx = createContext();
        ctx.setReplaceWith(replaceField.getText());
        SearchEngine.replace(currentTextArea, ctx);
    }

    private void replaceAll() {
        if (currentTextArea == null) return;
        SearchContext ctx = createContext();
        ctx.setReplaceWith(replaceField.getText());
        SearchResult result = SearchEngine.replaceAll(currentTextArea, ctx);
        resultLabel.setText(result.getCount() + " replaced");
    }
}
