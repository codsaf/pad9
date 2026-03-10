package com.pad9.util;

import java.awt.*;
import java.util.Set;

/**
 * Utility class for selecting the best available monospace font
 * from a predefined fallback chain.
 */
public final class FontUtil {

    private static final String[] FALLBACK_FONTS = {
            "JetBrains Mono",
            "Sarasa Mono SC",
            "Consolas",
            "Menlo",
            "DejaVu Sans Mono"
    };

    private FontUtil() {}

    /**
     * Returns the best available monospace font from the fallback chain.
     * Checks the system's installed fonts against the preferred list and
     * returns the first match. Falls back to the generic monospaced font
     * if none of the preferred fonts are available.
     *
     * @param size the font size in points
     * @return a monospace Font instance at the requested size
     */
    public static Font getEditorFont(int size) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] available = ge.getAvailableFontFamilyNames();
        Set<String> fontSet = Set.of(available);

        for (String name : FALLBACK_FONTS) {
            if (fontSet.contains(name)) {
                return new Font(name, Font.PLAIN, size);
            }
        }
        return new Font(Font.MONOSPACED, Font.PLAIN, size);
    }
}
