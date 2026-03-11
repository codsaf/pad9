package com.pad9.util;

import java.awt.*;
import java.util.Set;

/**
 * Utility class for selecting the best available monospace font
 * from a predefined fallback chain, with CJK character support.
 */
public final class FontUtil {

    // CJK-capable fonts checked first, then Latin-only fallbacks
    private static final String[] CJK_FONTS = {
            "Sarasa Mono SC", "Sarasa Mono CL",
    };
    private static final String[] LATIN_FONTS = {
            "JetBrains Mono", "Consolas",
    };

    private static final char CJK_TEST_CHAR = '\u4e2d';
    private static volatile String cachedFontName;

    private FontUtil() {}

    /**
     * Returns the best available monospace font from the fallback chain.
     * Prioritizes CJK-capable fonts, then Latin monospace fonts, then
     * Java's Monospaced composite font.
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

        // Prefer fonts with native CJK support
        for (String name : CJK_FONTS) {
            if (fontSet.contains(name)) {
                Font font = new Font(name, Font.PLAIN, size);
                if (font.canDisplay(CJK_TEST_CHAR)) {
                    cachedFontName = name;
                    return font;
                }
            }
        }

        // Fall back to good Latin monospace fonts
        for (String name : LATIN_FONTS) {
            if (fontSet.contains(name)) {
                cachedFontName = name;
                return new Font(name, Font.PLAIN, size);
            }
        }

        cachedFontName = Font.MONOSPACED;
        return new Font(Font.MONOSPACED, Font.PLAIN, size);
    }
}
