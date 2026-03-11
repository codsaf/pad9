package com.pad9.util;

import java.awt.*;
import java.util.Set;

/**
 * Utility class for selecting the best available monospace font
 * from a predefined fallback chain, with CJK character support.
 */
public final class FontUtil {

    private static final String[] PREFERRED_FONTS = {
            "Sarasa Mono SC",       // CJK + Latin, excellent metrics
            "JetBrains Mono",       // Latin, good CJK fallback in some builds
            "Sarasa Mono CL",       // CJK + Latin variant
    };

    private static final char CJK_TEST_CHAR = '\u4e2d'; // '中'

    private FontUtil() {}

    /**
     * Returns the best available monospace font from the fallback chain.
     * Prioritizes fonts with CJK support. Falls back to Java's logical
     * Monospaced composite font which has built-in CJK coverage with
     * correct line metrics.
     *
     * @param size the font size in points
     * @return a monospace Font instance at the requested size
     */
    public static Font getEditorFont(int size) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Set<String> fontSet = Set.of(ge.getAvailableFontFamilyNames());

        for (String name : PREFERRED_FONTS) {
            if (fontSet.contains(name)) {
                Font font = new Font(name, Font.PLAIN, size);
                if (font.canDisplay(CJK_TEST_CHAR)) {
                    return font;
                }
            }
        }

        // Java logical Monospaced: composite font with CJK fallback
        // and properly balanced line metrics
        return new Font(Font.MONOSPACED, Font.PLAIN, size);
    }
}
