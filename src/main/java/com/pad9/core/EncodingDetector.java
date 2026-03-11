package com.pad9.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Detects file encoding by checking BOM, validating UTF-8,
 * then trying GBK/Shift-JIS/EUC-KR heuristics.
 * Defaults to UTF-8 for valid ASCII-only content,
 * falls back to ISO-8859-1 when no encoding matches.
 */
public final class EncodingDetector {

    private static final int PROBE_SIZE = 8192;

    private EncodingDetector() {}

    /**
     * Detects the charset of the given file by checking BOM and byte patterns.
     * Returns UTF-8 as default if detection is inconclusive.
     *
     * @param path the file to probe
     * @return the detected charset
     * @throws IOException if the file cannot be read
     */
    public static Charset detect(Path path) throws IOException {
        byte[] bytes;
        try (InputStream in = Files.newInputStream(path)) {
            bytes = in.readNBytes(PROBE_SIZE);
        }
        if (bytes.length == 0) return StandardCharsets.UTF_8;

        Charset bomCharset = detectBOM(bytes);
        if (bomCharset != null) return bomCharset;
        if (isValidUTF8(bytes)) return StandardCharsets.UTF_8;
        if (looksLikeGBK(bytes)) return Charset.forName("GBK");
        if (looksLikeShiftJIS(bytes)) return Charset.forName("Shift_JIS");
        if (looksLikeEUCKR(bytes)) return Charset.forName("EUC-KR");
        return StandardCharsets.ISO_8859_1;
    }

    private static Charset detectBOM(byte[] bytes) {
        if (bytes.length >= 3 &&
                bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
            return StandardCharsets.UTF_8;
        }
        if (bytes.length >= 2) {
            if (bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF) return StandardCharsets.UTF_16BE;
            if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE) return StandardCharsets.UTF_16LE;
        }
        return null;
    }

    private static boolean isValidUTF8(byte[] bytes) {
        int i = 0;
        while (i < bytes.length) {
            int b = bytes[i] & 0xFF;
            int remaining;
            if (b <= 0x7F) { i++; continue; }
            else if (b >= 0xC2 && b <= 0xDF) remaining = 1;
            else if (b >= 0xE0 && b <= 0xEF) remaining = 2;
            else if (b >= 0xF0 && b <= 0xF4) remaining = 3;
            else return false;
            // Incomplete sequence at end of probe buffer — assume valid (truncated)
            if (i + remaining >= bytes.length) return true;
            for (int j = 1; j <= remaining; j++) {
                if ((bytes[i + j] & 0xC0) != 0x80) return false;
            }
            i += remaining + 1;
        }
        return true;
    }

    /**
     * Generic double-byte encoding heuristic. Counts valid lead+trail byte
     * pairs vs. invalid sequences. Returns true if enough valid pairs found.
     */
    private static boolean looksLikeDoubleByte(byte[] bytes, BytePairChecker checker) {
        int pairs = 0, invalid = 0;
        for (int i = 0; i < bytes.length - 1; i++) {
            int b1 = bytes[i] & 0xFF, b2 = bytes[i + 1] & 0xFF;
            int result = checker.check(b1, b2);
            if (result > 0) { pairs += result; i++; }
            else if (result < 0) invalid++;
        }
        return pairs > 0 && invalid <= pairs / 10;
    }

    @FunctionalInterface
    private interface BytePairChecker {
        /** Returns >0 for valid pair (skip trail byte), <0 for invalid lead, 0 for non-lead byte. */
        int check(int b1, int b2);
    }

    private static boolean looksLikeGBK(byte[] bytes) {
        return looksLikeDoubleByte(bytes, (b1, b2) -> {
            if (b1 >= 0x81 && b1 <= 0xFE) {
                return ((b2 >= 0x40 && b2 <= 0x7E) || (b2 >= 0x80 && b2 <= 0xFE)) ? 1 : -1;
            }
            return 0;
        });
    }

    private static boolean looksLikeShiftJIS(byte[] bytes) {
        return looksLikeDoubleByte(bytes, (b1, b2) -> {
            if ((b1 >= 0x81 && b1 <= 0x9F) || (b1 >= 0xE0 && b1 <= 0xEF)) {
                return ((b2 >= 0x40 && b2 <= 0x7E) || (b2 >= 0x80 && b2 <= 0xFC)) ? 1 : -1;
            }
            if (b1 >= 0xA1 && b1 <= 0xDF) return 1; // half-width katakana
            return 0;
        });
    }

    private static boolean looksLikeEUCKR(byte[] bytes) {
        return looksLikeDoubleByte(bytes, (b1, b2) -> {
            if (b1 >= 0xA1 && b1 <= 0xFE) {
                return (b2 >= 0xA1 && b2 <= 0xFE) ? 1 : -1;
            }
            return 0;
        });
    }
}
