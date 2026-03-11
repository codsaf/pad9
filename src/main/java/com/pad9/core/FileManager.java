package com.pad9.core;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Handles file reading and writing with automatic encoding detection.
 * All methods are static utilities; this class cannot be instantiated.
 */
public final class FileManager {

    private FileManager() {}

    /**
     * Reads a file's content as a string, auto-detecting encoding.
     * Strips the BOM character if present at the start of the content.
     *
     * @param path the file to read
     * @return a ReadResult containing the content and detected charset
     * @throws IOException if the file cannot be read
     */
    public static ReadResult read(Path path) throws IOException {
        Charset charset = EncodingDetector.detect(path);
        String content = Files.readString(path, charset);
        if (!content.isEmpty() && content.charAt(0) == '\uFEFF') {
            content = content.substring(1);
        }
        return new ReadResult(content, charset);
    }

    /**
     * Writes text to a file using the specified charset.
     *
     * @param path    the file to write to
     * @param content the text content to write
     * @param charset the charset to encode with
     * @throws IOException if the file cannot be written
     */
    public static void write(Path path, String content, Charset charset) throws IOException {
        Files.writeString(path, content, charset);
    }

    /** Result of reading a file, containing the decoded text and the detected charset. */
    public record ReadResult(String content, Charset charset) {}
}
