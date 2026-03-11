package com.pad9.ui;

import javax.swing.*;
import java.awt.*;

/**
 * A tabbed pane that manages multiple {@link EditorPane} tabs.
 * Provides custom tab components with close buttons, unsaved change indicators,
 * and save prompts when closing modified tabs.
 */
public class EditorTabPane extends JTabbedPane {

    /**
     * Creates the tabbed editor pane.
     */
    public EditorTabPane() {
        setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        JButton addButton = new JButton("+");
        addButton.setFont(addButton.getFont().deriveFont(Font.BOLD, 16f));
        addButton.setBorderPainted(false);
        addButton.setContentAreaFilled(false);
        addButton.setFocusPainted(false);
        addButton.setMargin(new Insets(0, 6, 0, 6));
        addButton.setToolTipText("New Tab");
        addButton.addActionListener(e -> addNewTab());

        putClientProperty("JTabbedPane.trailingComponent", addButton);
    }

    /**
     * Adds an existing editor as a new tab and selects it.
     *
     * @param editor the editor pane to add
     * @return the added editor pane
     */
    public EditorPane addTab(EditorPane editor) {
        String title = editor.getTitle();
        addTab(title, editor);
        int index = indexOfComponent(editor);
        setTabComponentAt(index, createTabComponent(editor));
        setSelectedComponent(editor);

        editor.addPropertyChangeListener("modified", evt -> updateTabTitle(editor));
        return editor;
    }

    /**
     * Creates a new empty (Untitled) tab.
     *
     * @return the newly created editor pane
     */
    public EditorPane addNewTab() {
        EditorPane editor = new EditorPane();
        return addTab(editor);
    }

    /**
     * Returns the currently selected EditorPane, or null.
     *
     * @return the active editor pane, or null if no tab is selected
     */
    public EditorPane getCurrentEditor() {
        Component comp = getSelectedComponent();
        return comp instanceof EditorPane ep ? ep : null;
    }

    /**
     * Closes the specified tab. Returns true if closed, false if cancelled.
     * If the editor has unsaved changes, prompts the user to save first.
     * When the last tab is closed, a new Untitled tab is automatically created.
     *
     * @param editor the editor pane to close
     * @return true if the tab was closed, false if the user cancelled
     */
    public boolean closeTab(EditorPane editor) {
        if (editor.isModified()) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Save changes to " + editor.getTitle().replace("\u25cf ", "") + "?",
                    "Unsaved Changes",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.CANCEL_OPTION) return false;
            if (choice == JOptionPane.YES_OPTION) {
                firePropertyChange("saveRequested", null, editor);
                if (editor.isModified()) return false;
            }
        }
        remove(editor);
        if (getTabCount() == 0) addNewTab();
        return true;
    }

    /**
     * Closes the currently selected tab.
     *
     * @return true if the tab was closed, false if the user cancelled
     */
    public boolean closeCurrentTab() {
        EditorPane editor = getCurrentEditor();
        return editor != null && closeTab(editor);
    }

    private void updateTabTitle(EditorPane editor) {
        int index = indexOfComponent(editor);
        if (index >= 0) {
            Component tabComp = getTabComponentAt(index);
            if (tabComp instanceof JPanel panel) {
                Component[] children = panel.getComponents();
                if (children.length > 0 && children[0] instanceof JLabel label) {
                    label.setText(editor.getTitle());
                }
            }
            setTitleAt(index, editor.getTitle());
        }
    }

    private JPanel createTabComponent(EditorPane editor) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setOpaque(false);

        JLabel titleLabel = new JLabel(editor.getTitle());
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
        panel.add(titleLabel);

        JButton closeButton = new JButton("\u00d7");
        closeButton.setFont(closeButton.getFont().deriveFont(14f));
        closeButton.setBorderPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setFocusPainted(false);
        closeButton.setMargin(new Insets(0, 2, 0, 2));
        closeButton.addActionListener(e -> closeTab(editor));
        panel.add(closeButton);

        return panel;
    }
}
