package com.pad9.core;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import java.util.Map;

public final class SyntaxMapper {

    private static final Map<String, String> EXT_MAP = Map.ofEntries(
            Map.entry("java", SyntaxConstants.SYNTAX_STYLE_JAVA),
            Map.entry("js", SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT),
            Map.entry("mjs", SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT),
            Map.entry("ts", SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT),
            Map.entry("py", SyntaxConstants.SYNTAX_STYLE_PYTHON),
            Map.entry("rb", SyntaxConstants.SYNTAX_STYLE_RUBY),
            Map.entry("c", SyntaxConstants.SYNTAX_STYLE_C),
            Map.entry("h", SyntaxConstants.SYNTAX_STYLE_C),
            Map.entry("cpp", SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS),
            Map.entry("hpp", SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS),
            Map.entry("cs", SyntaxConstants.SYNTAX_STYLE_CSHARP),
            Map.entry("go", SyntaxConstants.SYNTAX_STYLE_GO),
            Map.entry("rs", SyntaxConstants.SYNTAX_STYLE_RUST),
            Map.entry("kt", SyntaxConstants.SYNTAX_STYLE_KOTLIN),
            Map.entry("scala", SyntaxConstants.SYNTAX_STYLE_SCALA),
            Map.entry("groovy", SyntaxConstants.SYNTAX_STYLE_GROOVY),
            Map.entry("php", SyntaxConstants.SYNTAX_STYLE_PHP),
            Map.entry("html", SyntaxConstants.SYNTAX_STYLE_HTML),
            Map.entry("htm", SyntaxConstants.SYNTAX_STYLE_HTML),
            Map.entry("css", SyntaxConstants.SYNTAX_STYLE_CSS),
            Map.entry("less", SyntaxConstants.SYNTAX_STYLE_LESS),
            Map.entry("xml", SyntaxConstants.SYNTAX_STYLE_XML),
            Map.entry("json", SyntaxConstants.SYNTAX_STYLE_JSON),
            Map.entry("yaml", SyntaxConstants.SYNTAX_STYLE_YAML),
            Map.entry("yml", SyntaxConstants.SYNTAX_STYLE_YAML),
            Map.entry("md", SyntaxConstants.SYNTAX_STYLE_MARKDOWN),
            Map.entry("markdown", SyntaxConstants.SYNTAX_STYLE_MARKDOWN),
            Map.entry("sql", SyntaxConstants.SYNTAX_STYLE_SQL),
            Map.entry("sh", SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL),
            Map.entry("bash", SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL),
            Map.entry("zsh", SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL),
            Map.entry("bat", SyntaxConstants.SYNTAX_STYLE_WINDOWS_BATCH),
            Map.entry("cmd", SyntaxConstants.SYNTAX_STYLE_WINDOWS_BATCH),
            Map.entry("lua", SyntaxConstants.SYNTAX_STYLE_LUA),
            Map.entry("perl", SyntaxConstants.SYNTAX_STYLE_PERL),
            Map.entry("pl", SyntaxConstants.SYNTAX_STYLE_PERL),
            Map.entry("ini", SyntaxConstants.SYNTAX_STYLE_INI),
            Map.entry("properties", SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE),
            Map.entry("tex", SyntaxConstants.SYNTAX_STYLE_LATEX),
            Map.entry("dockerfile", SyntaxConstants.SYNTAX_STYLE_DOCKERFILE),
            Map.entry("csv", SyntaxConstants.SYNTAX_STYLE_CSV),
            Map.entry("dart", SyntaxConstants.SYNTAX_STYLE_DART),
            Map.entry("jsp", SyntaxConstants.SYNTAX_STYLE_JSP),
            Map.entry("d", SyntaxConstants.SYNTAX_STYLE_D),
            Map.entry("clj", SyntaxConstants.SYNTAX_STYLE_CLOJURE)
    );

    private SyntaxMapper() {}

    /**
     * Returns the RSyntaxTextArea syntax style for the given filename.
     * Returns SYNTAX_STYLE_NONE if the extension is not recognized.
     */
    public static String getSyntaxStyle(String filename) {
        if (filename == null) return SyntaxConstants.SYNTAX_STYLE_NONE;

        String lower = filename.toLowerCase();
        if (lower.equals("makefile") || lower.equals("gnumakefile")) {
            return SyntaxConstants.SYNTAX_STYLE_MAKEFILE;
        }
        if (lower.equals("dockerfile")) {
            return SyntaxConstants.SYNTAX_STYLE_DOCKERFILE;
        }

        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return SyntaxConstants.SYNTAX_STYLE_NONE;
        }
        String ext = filename.substring(dot + 1).toLowerCase();
        return EXT_MAP.getOrDefault(ext, SyntaxConstants.SYNTAX_STYLE_NONE);
    }

    /**
     * Returns a human-readable language name for the given syntax style.
     */
    public static String getLanguageName(String syntaxStyle) {
        if (syntaxStyle == null || syntaxStyle.equals(SyntaxConstants.SYNTAX_STYLE_NONE)) {
            return "Plain Text";
        }
        String name = syntaxStyle;
        int slash = name.lastIndexOf('/');
        if (slash >= 0) name = name.substring(slash + 1);
        if (name.startsWith("x-")) name = name.substring(2);
        if (!name.isEmpty()) name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        return name;
    }
}
