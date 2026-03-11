package com.pad9.util;

import java.awt.*;
import java.util.Set;

/**
 * Utility class for selecting the best available monospace font
 * from a predefined fallback chain, with CJK character support.
 */
public final class FontUtil {

    private static final String[] CJK_FONTS = { "Sarasa Mono SC", "Sarasa Mono CL" };
    private static final String[] LATIN_FONTS = { "JetBrains Mono", "Consolas" };
    private static final char CJK_TEST_CHAR = '\u4e2d';
    private static volatile String cachedFontName;

    private FontUtil() {}

    /**
     * Returns the best available monospace font from the fallback chain.
     * All candidate fonts must pass a CJK display check to ensure Chinese
     * character support. Falls back to Java's Monospaced composite font
     * which always supports CJK via logical font mapping.
     *
     * @param size the font size in points
     * @return a monospace Font instance at the requested size
     */
    public static Font getEditorFont(int size) {
        if (cachedFontName != null) {
            return new Font(cachedFontName, Font.PLAIN, size);
        }

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Set<String> fontSet = Set.of(ge.getAvailableFontFamilyNames());

        for (String name : CJK_FONTS) {
            if (fontSet.contains(name) && new Font(name, Font.PLAIN, size).canDisplay(CJK_TEST_CHAR)) {
                cachedFontName = name;
                return new Font(name, Font.PLAIN, size);
            }
        }

        for (String name : LATIN_FONTS) {
            if (fontSet.contains(name) && new Font(name, Font.PLAIN, size).canDisplay(CJK_TEST_CHAR)) {
                cachedFontName = name;
                return new Font(name, Font.PLAIN, size);
            }
        }

        cachedFontName = Font.MONOSPACED;
        return new Font(Font.MONOSPACED, Font.PLAIN, size);
    }
}
