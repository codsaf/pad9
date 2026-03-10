package com.pad9.util;

public final class PlatformUtil {

    private static final boolean MAC_OS =
            System.getProperty("os.name").toLowerCase().contains("mac");

    private PlatformUtil() {}

    /**
     * Returns true if running on macOS.
     */
    public static boolean isMacOS() {
        return MAC_OS;
    }

    /**
     * Configures macOS-specific system properties.
     * Must be called before any AWT/Swing initialization.
     */
    public static void configureMacOS() {
        if (!MAC_OS) return;
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("apple.awt.application.name", "Pad9");
    }
}
