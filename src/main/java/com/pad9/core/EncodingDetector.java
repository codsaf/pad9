package com.pad9.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

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
        try (FileChannel ch = FileChannel.open(path, StandardOpenOption.READ)) {
            long size = ch.size();
            if (size == 0) return StandardCharsets.UTF_8;

            int readSize = (int) Math.min(size, PROBE_SIZE);
            ByteBuffer buf = ByteBuffer.allocate(readSize);
            ch.read(buf);
            buf.flip();
            byte[] bytes = new byte[buf.remaining()];
            buf.get(bytes);

            Charset bomCharset = detectBOM(bytes);
            if (bomCharset != null) return bomCharset;
            if (isValidUTF8(bytes)) return StandardCharsets.UTF_8;
            if (looksLikeGBK(bytes)) return Charset.forName("GBK");
            if (looksLikeShiftJIS(bytes)) return Charset.forName("Shift_JIS");
            if (looksLikeEUCKR(bytes)) return Charset.forName("EUC-KR");
            return StandardCharsets.ISO_8859_1;
        }
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
            if (i + remaining >= bytes.length) return true;
            for (int j = 1; j <= remaining; j++) {
                if ((bytes[i + j] & 0xC0) != 0x80) return false;
            }
            i += remaining + 1;
        }
        return true;
    }

    private static boolean looksLikeGBK(byte[] bytes) {
        int pairs = 0, invalid = 0;
        for (int i = 0; i < bytes.length - 1; i++) {
            int b1 = bytes[i] & 0xFF, b2 = bytes[i + 1] & 0xFF;
            if (b1 >= 0x81 && b1 <= 0xFE) {
                if ((b2 >= 0x40 && b2 <= 0x7E) || (b2 >= 0x80 && b2 <= 0xFE)) { pairs++; i++; }
                else invalid++;
            }
        }
        return pairs > 0 && invalid <= pairs / 10;
    }

    private static boolean looksLikeShiftJIS(byte[] bytes) {
        int pairs = 0, invalid = 0;
        for (int i = 0; i < bytes.length - 1; i++) {
            int b1 = bytes[i] & 0xFF, b2 = bytes[i + 1] & 0xFF;
            if ((b1 >= 0x81 && b1 <= 0x9F) || (b1 >= 0xE0 && b1 <= 0xEF)) {
                if ((b2 >= 0x40 && b2 <= 0x7E) || (b2 >= 0x80 && b2 <= 0xFC)) { pairs++; i++; }
                else invalid++;
            } else if (b1 >= 0xA1 && b1 <= 0xDF) pairs++;
        }
        return pairs > 0 && invalid <= pairs / 10;
    }

    private static boolean looksLikeEUCKR(byte[] bytes) {
        int pairs = 0, invalid = 0;
        for (int i = 0; i < bytes.length - 1; i++) {
            int b1 = bytes[i] & 0xFF, b2 = bytes[i + 1] & 0xFF;
            if (b1 >= 0xA1 && b1 <= 0xFE) {
                if (b2 >= 0xA1 && b2 <= 0xFE) { pairs++; i++; }
                else invalid++;
            }
        }
        return pairs > 0 && invalid <= pairs / 10;
    }
}
