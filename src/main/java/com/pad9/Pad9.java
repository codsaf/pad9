package com.pad9;

import com.formdev.flatlaf.FlatDarkLaf;
import com.pad9.ui.MainFrame;
import com.pad9.util.PlatformUtil;

import javax.swing.*;

public class Pad9 {

    public static void main(String[] args) {
        PlatformUtil.configureMacOS();
        FlatDarkLaf.setup();

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
