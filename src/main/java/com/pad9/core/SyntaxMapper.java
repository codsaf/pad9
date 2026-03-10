package com.pad9.core;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import java.util.HashMap;
import java.util.Map;

public final class SyntaxMapper {

    private static final Map<String, String> EXT_MAP = new HashMap<>();

    static {
        EXT_MAP.put("java", SyntaxConstants.SYNTAX_STYLE_JAVA);
        EXT_MAP.put("js", SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
        EXT_MAP.put("mjs", SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
        EXT_MAP.put("ts", SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT);
        EXT_MAP.put("py", SyntaxConstants.SYNTAX_STYLE_PYTHON);
        EXT_MAP.put("rb", SyntaxConstants.SYNTAX_STYLE_RUBY);
        EXT_MAP.put("c", SyntaxConstants.SYNTAX_STYLE_C);
        EXT_MAP.put("h", SyntaxConstants.SYNTAX_STYLE_C);
        EXT_MAP.put("cpp", SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS);
        EXT_MAP.put("hpp", SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS);
        EXT_MAP.put("cs", SyntaxConstants.SYNTAX_STYLE_CSHARP);
        EXT_MAP.put("go", SyntaxConstants.SYNTAX_STYLE_GO);
        EXT_MAP.put("rs", SyntaxConstants.SYNTAX_STYLE_RUST);
        EXT_MAP.put("kt", SyntaxConstants.SYNTAX_STYLE_KOTLIN);
        EXT_MAP.put("scala", SyntaxConstants.SYNTAX_STYLE_SCALA);
        EXT_MAP.put("groovy", SyntaxConstants.SYNTAX_STYLE_GROOVY);
        EXT_MAP.put("php", SyntaxConstants.SYNTAX_STYLE_PHP);
        EXT_MAP.put("html", SyntaxConstants.SYNTAX_STYLE_HTML);
        EXT_MAP.put("htm", SyntaxConstants.SYNTAX_STYLE_HTML);
        EXT_MAP.put("css", SyntaxConstants.SYNTAX_STYLE_CSS);
        EXT_MAP.put("less", SyntaxConstants.SYNTAX_STYLE_LESS);
        EXT_MAP.put("xml", SyntaxConstants.SYNTAX_STYLE_XML);
        EXT_MAP.put("json", SyntaxConstants.SYNTAX_STYLE_JSON);
        EXT_MAP.put("yaml", SyntaxConstants.SYNTAX_STYLE_YAML);
        EXT_MAP.put("yml", SyntaxConstants.SYNTAX_STYLE_YAML);
        EXT_MAP.put("md", SyntaxConstants.SYNTAX_STYLE_MARKDOWN);
        EXT_MAP.put("markdown", SyntaxConstants.SYNTAX_STYLE_MARKDOWN);
        EXT_MAP.put("sql", SyntaxConstants.SYNTAX_STYLE_SQL);
        EXT_MAP.put("sh", SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL);
        EXT_MAP.put("bash", SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL);
        EXT_MAP.put("zsh", SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL);
        EXT_MAP.put("bat", SyntaxConstants.SYNTAX_STYLE_WINDOWS_BATCH);
        EXT_MAP.put("cmd", SyntaxConstants.SYNTAX_STYLE_WINDOWS_BATCH);
        EXT_MAP.put("lua", SyntaxConstants.SYNTAX_STYLE_LUA);
        EXT_MAP.put("perl", SyntaxConstants.SYNTAX_STYLE_PERL);
        EXT_MAP.put("pl", SyntaxConstants.SYNTAX_STYLE_PERL);
        EXT_MAP.put("ini", SyntaxConstants.SYNTAX_STYLE_INI);
        EXT_MAP.put("properties", SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE);
        EXT_MAP.put("tex", SyntaxConstants.SYNTAX_STYLE_LATEX);
        EXT_MAP.put("dockerfile", SyntaxConstants.SYNTAX_STYLE_DOCKERFILE);
        EXT_MAP.put("csv", SyntaxConstants.SYNTAX_STYLE_CSV);
        EXT_MAP.put("dart", SyntaxConstants.SYNTAX_STYLE_DART);
        EXT_MAP.put("jsp", SyntaxConstants.SYNTAX_STYLE_JSP);
        EXT_MAP.put("d", SyntaxConstants.SYNTAX_STYLE_D);
        EXT_MAP.put("clj", SyntaxConstants.SYNTAX_STYLE_CLOJURE);
    }

    private SyntaxMapper() {}

    /**
     * Returns the RSyntaxTextArea syntax style for the given filename.
     * Returns SYNTAX_STYLE_NONE if the extension is not recognized.
     */
    public static String getSyntaxStyle(String filename) {
        if (filename == null) return SyntaxConstants.SYNTAX_STYLE_NONE;

        // Handle filenames without extensions (e.g., "Makefile", "Dockerfile")
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
        // Extract language name from the style string "text/x-language" or "text/language"
        String name = syntaxStyle;
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        if (name.startsWith("x-")) {
            name = name.substring(2);
        }
        if (!name.isEmpty()) {
            name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
        return name;
    }
}
