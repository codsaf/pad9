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
    private static Font cachedFont;

    private FontUtil() {}

    /**
     * Returns the best available monospace font from the fallback chain.
     * Prioritizes CJK-capable fonts, then Latin monospace fonts, then
     * Java's Monospaced composite font. Result is cached.
     *
     * @param size the font size in points
     * @return a monospace Font instance at the requested size
     */
    public static Font getEditorFont(int size) {
        if (cachedFont != null) {
            return cachedFont.getSize() == size ? cachedFont : cachedFont.deriveFont((float) size);
        }

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Set<String> fontSet = Set.of(ge.getAvailableFontFamilyNames());

        for (String name : CJK_FONTS) {
            if (fontSet.contains(name)) {
                Font font = new Font(name, Font.PLAIN, size);
                if (font.canDisplay(CJK_TEST_CHAR)) {
                    cachedFont = font;
                    return font;
                }
            }
        }

        for (String name : LATIN_FONTS) {
            if (fontSet.contains(name)) {
                cachedFont = new Font(name, Font.PLAIN, size);
                return cachedFont;
            }
        }

        cachedFont = new Font(Font.MONOSPACED, Font.PLAIN, size);
        return cachedFont;
    }
}
