package com.eagle.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeLinter {

    public static class Problem {
        public final int line;
        public final int start;
        public final int end;
        public final String message;
        public final Severity severity;

        public enum Severity { ERROR, WARNING, INFO }

        public Problem(int line, int start, int end, String message, Severity severity) {
            this.line = line;
            this.start = start;
            this.end = end;
            this.message = message;
            this.severity = severity;
        }
    }

    public static List<Problem> lint(String text, LanguageType lang) {
        List<Problem> problems = new ArrayList<>();
        if (text == null || text.isEmpty()) return problems;
        switch (lang) {
            case HTML:
            case VUE:
            case SVELTE:
            case ASTRO: lintHtml(text, problems); break;
            case CSS:
            case SCSS: lintCss(text, problems); break;
            case JAVASCRIPT:
            case TYPESCRIPT:
            case JSX:
            case TSX: lintJs(text, problems); break;
            case JAVA: lintJava(text, problems); break;
            case PYTHON: lintPython(text, problems); break;
            case PHP: lintPhp(text, problems); break;
            case JSON: lintJson(text, problems); break;
            case YAML: lintYaml(text, problems); break;
            case SQL: lintSql(text, problems); break;
            case C: lintC(text, problems); break;
            case CPP: lintCpp(text, problems); break;
            case KOTLIN: lintKotlin(text, problems); break;
            case GO: lintGo(text, problems); break;
            case RUST: lintRust(text, problems); break;
        }
        return problems;
    }

    // ---- HTML ----
    private static final Pattern HTML_TAG = Pattern.compile("</?([a-zA-Z][a-zA-Z0-9]*)\\b[^>]*>");
    private static final Pattern HTML_SELF_CLOSING = Pattern.compile("<(area|base|br|col|embed|hr|img|input|link|meta|param|source|track|wbr)\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
    private static final Pattern HTML_DOCTYPE = Pattern.compile("<!DOCTYPE\\s+html>", Pattern.CASE_INSENSITIVE);

    private static void lintHtml(String text, List<Problem> problems) {
        String clean = text.replaceAll(HTML_COMMENT.pattern(), "");
        clean = clean.replaceAll("<script[^>]*>[\\s\\S]*?</script>", "");
        clean = clean.replaceAll("<style[^>]*>[\\s\\S]*?</style>", "");

        if (!clean.contains("<!DOCTYPE html>") && !clean.contains("<!DOCTYPE HTML>")) {
            problems.add(new Problem(1, 0, 0, "Missing DOCTYPE declaration", Problem.Severity.WARNING));
        }

        List<String> openTags = new ArrayList<>();
        List<Integer> openLines = new ArrayList<>();
        String[] lines = clean.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            Matcher m = HTML_TAG.matcher(lines[i]);
            while (m.find()) {
                String tag = m.group(1).toLowerCase();
                if (m.group().startsWith("</")) {
                    if (openTags.isEmpty()) {
                        problems.add(new Problem(i + 1, m.start(), m.end(), "Unexpected closing tag </" + tag + ">", Problem.Severity.ERROR));
                    } else {
                        String last = openTags.remove(openTags.size() - 1);
                        int lastLine = openLines.remove(openLines.size() - 1);
                        if (!last.equals(tag)) {
                            problems.add(new Problem(i + 1, m.start(), m.end(), "Mismatched tag: </" + tag + "> expected </" + last + ">", Problem.Severity.ERROR));
                            problems.add(new Problem(lastLine, 0, 0, "Tag <" + last + "> opened here", Problem.Severity.INFO));
                        }
                    }
                } else {
                    boolean selfClosing = HTML_SELF_CLOSING.matcher(m.group()).matches() || m.group().endsWith("/>");
                    if (!selfClosing) {
                        openTags.add(tag);
                        openLines.add(i + 1);
                    }
                }
            }
        }
        while (!openTags.isEmpty()) {
            String tag = openTags.remove(0);
            int ln = openLines.remove(0);
            problems.add(new Problem(ln, 0, 0, "Unclosed tag <" + tag + ">", Problem.Severity.ERROR));
        }
    }

    // ---- CSS ----
    private static final Pattern CSS_BRACE_OPEN = Pattern.compile("\\{");
    private static final Pattern CSS_BRACE_CLOSE = Pattern.compile("\\}");
    private static final Pattern CSS_STRING = Pattern.compile("\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'");
    private static final Pattern CSS_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    private static void lintCss(String text, List<Problem> problems) {
        String clean = CSS_COMMENT.matcher(text).replaceAll("");
        clean = CSS_STRING.matcher(clean).replaceAll("\"\"");

        int braces = 0;
        String[] lines = clean.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.contains("{") && !line.trim().matches("^[.#@a-zA-Z][^{]*\\{$") && !line.trim().matches("^@.*\\{$")) {
                problems.add(new Problem(i + 1, line.indexOf('{'), line.indexOf('{') + 1,
                    "Expected a selector before '{'", Problem.Severity.ERROR));
            }
            for (char c : line.toCharArray()) {
                if (c == '{') braces++;
                if (c == '}') braces--;
            }
        }
        if (braces > 0) {
            problems.add(new Problem(countLines(text), 0, 0, braces + " unclosed brace(s)", Problem.Severity.ERROR));
        } else if (braces < 0) {
            problems.add(new Problem(1, 0, 0, (-braces) + " extra closing brace(s)", Problem.Severity.ERROR));
        }

        Pattern propPattern = Pattern.compile("([a-zA-Z-]+)\\s*:\\s*([^;{]+);?");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.endsWith(":") || line.matches(".*:\\s*$")) {
                problems.add(new Problem(i + 1, 0, 0, "Missing value after ':'", Problem.Severity.ERROR));
            }
            if (line.contains(":") && !line.contains(";") && !line.endsWith("{") && !line.endsWith("}") && !line.startsWith("/*") && !line.startsWith("@") && !line.matches(".*\\{[^}]*$")) {
                if (!line.trim().endsWith(";") && line.contains(":")) {
                    boolean inBlock = false;
                    for (int j = i; j >= 0; j--) {
                        if (lines[j].contains("{")) inBlock = true;
                        if (lines[j].contains("}")) break;
                    }
                    if (inBlock && !line.trim().startsWith("/*")) {
                        problems.add(new Problem(i + 1, 0, 0, "Missing semicolon", Problem.Severity.WARNING));
                    }
                }
            }
        }
    }

    // ---- JS ----
    private static final Pattern JS_COMMENT_SINGLE = Pattern.compile("//[^\n]*");
    private static final Pattern JS_COMMENT_MULTI = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern JS_STRING_SINGLE = Pattern.compile("'(?:\\\\.|[^'\\\\])*'");
    private static final Pattern JS_STRING_DOUBLE = Pattern.compile("\"(?:\\\\.|[^\"\\\\])*\"");
    private static final Pattern JS_TEMPLATE = Pattern.compile("`(?:\\\\.|[^`\\\\])*`");
    private static final Pattern JS_RESERVED = Pattern.compile("\\b(abstract|boolean|byte|char|double|final|float|goto|implements|int|interface|long|native|package|private|protected|public|short|static|synchronized|throws|transient|volatile)\\b");
    private static final Pattern JS_DEBUGGER = Pattern.compile("\\bdebugger\\b");
    private static final Pattern JS_EVAL = Pattern.compile("\\beval\\s*\\(");
    private static final Pattern JS_EQ_EQ = Pattern.compile("[^!=]!=(?!=)[^=]");
    private static final Pattern JS_EQ_EQ_EQ = Pattern.compile("[^!=]==(?!=)");
    private static final Pattern JS_CONSOLES = Pattern.compile("\\bconsole\\.(log|warn|error|debug|info|trace)\\s*\\(");

    private static void lintJs(String text, List<Problem> problems) {
        String clean = JS_COMMENT_SINGLE.matcher(text).replaceAll("");
        clean = JS_COMMENT_MULTI.matcher(clean).replaceAll("");
        clean = JS_STRING_SINGLE.matcher(clean).replaceAll("''");
        clean = JS_STRING_DOUBLE.matcher(clean).replaceAll("\"\"");
        clean = JS_TEMPLATE.matcher(clean).replaceAll("``");

        // Brace/paren/bracket balance
        int parens = 0, brackets = 0, braces = 0;
        String[] lines = clean.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            for (char c : lines[i].toCharArray()) {
                switch (c) {
                    case '(': parens++; break;
                    case ')': parens--; break;
                    case '[': brackets++; break;
                    case ']': brackets--; break;
                    case '{': braces++; break;
                    case '}': braces--; break;
                }
            }
        }
        if (parens > 0) problems.add(new Problem(countLines(text), 0, 0, parens + " unclosed parenthesis", Problem.Severity.ERROR));
        if (parens < 0) problems.add(new Problem(1, 0, 0, (-parens) + " extra closing parenthesis", Problem.Severity.ERROR));
        if (brackets > 0) problems.add(new Problem(countLines(text), 0, 0, brackets + " unclosed bracket(s)", Problem.Severity.ERROR));
        if (brackets < 0) problems.add(new Problem(1, 0, 0, (-brackets) + " extra closing bracket(s)", Problem.Severity.ERROR));
        if (braces > 0) problems.add(new Problem(countLines(text), 0, 0, braces + " unclosed brace(s)", Problem.Severity.ERROR));
        if (braces < 0) problems.add(new Problem(1, 0, 0, (-braces) + " extra closing brace(s)", Problem.Severity.ERROR));

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();

            // Variable declared but not assigned
            if (trimmed.startsWith("const ") || trimmed.startsWith("let ") || trimmed.startsWith("var ")) {
                if (!line.contains("=") && !line.endsWith(";") && !line.endsWith("{") && !line.endsWith(",") && !line.endsWith("(")) {
                    problems.add(new Problem(i + 1, 0, 0, "Variable declared but not assigned", Problem.Severity.WARNING));
                }
            }

            // Incomplete assignment
            if (trimmed.equals("=") || trimmed.matches("^[a-zA-Z_$][a-zA-Z0-9_$]*\\s*=\\s*$")) {
                problems.add(new Problem(i + 1, 0, 0, "Incomplete assignment", Problem.Severity.ERROR));
            }

            // == instead of === (potential bug)
            java.util.regex.Matcher eqEq = JS_EQ_EQ.matcher(line);
            while (eqEq.find()) {
                int pos = eqEq.start() + 1;
                problems.add(new Problem(i + 1, pos, pos + 2, "Expected '===' instead of '=='", Problem.Severity.WARNING));
            }

            // console.log left in code
            java.util.regex.Matcher consoleM = JS_CONSOLES.matcher(line);
            while (consoleM.find()) {
                problems.add(new Problem(i + 1, consoleM.start(), consoleM.end(), "Possible debug console leftover", Problem.Severity.INFO));
            }

            // debugger statement
            java.util.regex.Matcher debugM = JS_DEBUGGER.matcher(line);
            while (debugM.find()) {
                problems.add(new Problem(i + 1, debugM.start(), debugM.end(), "debugger statement left in code", Problem.Severity.INFO));
            }

            // eval usage
            java.util.regex.Matcher evalM = JS_EVAL.matcher(line);
            while (evalM.find()) {
                problems.add(new Problem(i + 1, evalM.start(), evalM.end(), "eval can be unsafe", Problem.Severity.WARNING));
            }

            // Reserved word usage as identifier
            java.util.regex.Matcher resM = JS_RESERVED.matcher(line);
            while (resM.find()) {
                problems.add(new Problem(i + 1, resM.start(), resM.end(), "'" + resM.group() + "' is a reserved word", Problem.Severity.WARNING));
            }
        }
    }

    // ---- Java ----
    private static final Pattern JAVA_CLASS = Pattern.compile("\\bclass\\s+\\w+");
    private static final Pattern JAVA_MAIN = Pattern.compile("public\\s+static\\s+void\\s+main\\s*\\(\\s*(String\\s*\\[\\s*\\]\\s*\\w*|String\\.\\.\\.\\s*\\w*|\\w*\\s*\\[\\s*\\]\\s*String)\\s*\\)");
    private static final Pattern JAVA_SYSOUT = Pattern.compile("System\\.out\\.(print|println|printf)\\s*\\(");
    private static final Pattern JAVA_EMPTY_CATCH = Pattern.compile("catch\\s*\\([^)]*\\)\\s*\\{\\s*\\}");
    private static final Pattern JAVA_GENERIC_CATCH = Pattern.compile("catch\\s*\\(\\s*Exception\\s+\\w+");
    private static final Pattern JAVA_OVERRIDE = Pattern.compile("(public|protected)\\s+(\\w+\\s+)*\\w+\\s*\\([^)]*\\)\\s*\\{");

    private static void lintJava(String text, List<Problem> problems) {
        String[] lines = text.split("\n", -1);
        String clean = text.replaceAll("\"(?:\\\\.|[^\"\\\\])*\"", "\"\"");

        boolean hasClass = JAVA_CLASS.matcher(clean).find();
        boolean hasMain = JAVA_MAIN.matcher(clean).find();
        if (hasClass && !hasMain) {
            problems.add(new Problem(1, 0, 0, "Missing 'public static void main(String[] args)' entry point", Problem.Severity.ERROR));
        }

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            Matcher m = JAVA_SYSOUT.matcher(line);
            while (m.find()) {
                problems.add(new Problem(i + 1, m.start(), m.end(), "Use Logger instead of System.out.println", Problem.Severity.WARNING));
            }

            m = JAVA_EMPTY_CATCH.matcher(line);
            while (m.find()) {
                problems.add(new Problem(i + 1, m.start(), m.end(), "Empty catch block", Problem.Severity.ERROR));
            }

            m = JAVA_GENERIC_CATCH.matcher(line);
            while (m.find()) {
                problems.add(new Problem(i + 1, m.start(), m.end(), "Catch specific exception instead of generic Exception", Problem.Severity.WARNING));
            }

            if (i > 0) {
                String prev = lines[i - 1].trim();
                String curr = line.trim();
                m = JAVA_OVERRIDE.matcher(curr);
                if (m.find() && !prev.equals("@Override")) {
                    problems.add(new Problem(i + 1, m.start(), m.end(), "Method may be missing @Override annotation", Problem.Severity.WARNING));
                }
            }
        }
    }

    // ---- Python ----
    private static final Pattern PYTHON_CLASS = Pattern.compile("^\\s*class\\s+\\w+\\s*:");
    private static final Pattern PYTHON_INIT = Pattern.compile("^\\s*def\\s+__init__\\s*\\(");
    private static final Pattern PYTHON_DEF = Pattern.compile("def\\s+\\w+\\s*\\(([^)]*)\\)");
    private static final Pattern PYTHON_BARE_EXCEPT = Pattern.compile("except\\s*:");
    private static final Pattern PYTHON_EQ_NONE = Pattern.compile("==\\s*None");

    private static void lintPython(String text, List<Problem> problems) {
        String[] lines = text.split("\n", -1);
        boolean hasClass = false;
        boolean hasInit = false;
        boolean indentMixed = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            String indent = line.substring(0, line.length() - trimmed.length());

            if (!indentMixed && indent.contains("\t") && indent.contains(" ")) {
                problems.add(new Problem(i + 1, 0, 0, "Mixing tabs and spaces for indentation", Problem.Severity.ERROR));
                indentMixed = true;
            }

            Matcher m = PYTHON_BARE_EXCEPT.matcher(line);
            while (m.find()) {
                problems.add(new Problem(i + 1, m.start(), m.end(), "Bare 'except:', specify exception type", Problem.Severity.ERROR));
            }

            m = PYTHON_EQ_NONE.matcher(line);
            while (m.find()) {
                problems.add(new Problem(i + 1, m.start(), m.end(), "Use 'is None' instead of '== None'", Problem.Severity.WARNING));
            }

            m = PYTHON_DEF.matcher(line);
            while (m.find()) {
                String params = m.group(1).trim();
                if (!params.isEmpty() && !params.contains("self")) {
                    problems.add(new Problem(i + 1, m.start(), m.end(), "Method may be missing 'self' parameter", Problem.Severity.WARNING));
                }
            }

            if (PYTHON_CLASS.matcher(line).find()) {
                hasClass = true;
            }
            if (PYTHON_INIT.matcher(line).find()) {
                hasInit = true;
            }
        }

        if (hasClass && !hasInit) {
            problems.add(new Problem(1, 0, 0, "Class missing __init__ method", Problem.Severity.WARNING));
        }
    }

    // ---- PHP ----
    private static final Pattern PHP_OPEN_TAG = Pattern.compile("<\\?(php|=)");
    private static final Pattern PHP_SHORT_TAG = Pattern.compile("<\\?(?!php|=|xml)");
    private static final Pattern PHP_MYSQL = Pattern.compile("\\bmysql_\\w+\\s*\\(");
    private static final Pattern PHP_EREG = Pattern.compile("\\b(ereg|eregi)\\s*\\(");
    private static final Pattern PHP_ECHO_VAR = Pattern.compile("\\becho\\s+\\$");
    private static final Pattern PHP_HTMLSPECIALCHARS = Pattern.compile("htmlspecialchars\\s*\\(");
    private static final Pattern PHP_VAR_CHECK = Pattern.compile("(?<!\\$)\\b([a-zA-Z_]\\w*)\\s*=");

    private static void lintPhp(String text, List<Problem> problems) {
        String[] lines = text.split("\n", -1);
        boolean hasOpenTag = false;
        boolean inPhp = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            if (PHP_OPEN_TAG.matcher(line).find()) {
                inPhp = true;
                hasOpenTag = true;
            }

            Matcher shortM = PHP_SHORT_TAG.matcher(line);
            while (shortM.find()) {
                problems.add(new Problem(i + 1, shortM.start(), shortM.end(), "Use <?php instead of short open tag", Problem.Severity.ERROR));
            }

            Matcher mysqlM = PHP_MYSQL.matcher(line);
            while (mysqlM.find()) {
                problems.add(new Problem(i + 1, mysqlM.start(), mysqlM.end(), "mysql_* functions deprecated, use mysqli or PDO", Problem.Severity.ERROR));
            }

            Matcher eregM = PHP_EREG.matcher(line);
            while (eregM.find()) {
                problems.add(new Problem(i + 1, eregM.start(), eregM.end(), "ereg/eregi deprecated, use preg_match", Problem.Severity.ERROR));
            }

            Matcher echoM = PHP_ECHO_VAR.matcher(line);
            if (echoM.find()) {
                if (!PHP_HTMLSPECIALCHARS.matcher(line).find()) {
                    problems.add(new Problem(i + 1, echoM.start(), echoM.end(), "Echo variable without htmlspecialchars", Problem.Severity.WARNING));
                }
            }

            if (inPhp) {
                Matcher varM = PHP_VAR_CHECK.matcher(line);
                while (varM.find()) {
                    String vn = varM.group(1);
                    if (!vn.matches("function|if|else|foreach|for|while|switch|case|return|new|echo|print|true|false|null|array|isset|empty|die|exit|include|require|class|public|private|protected|static|var|const|throw|try|catch|finally|self|parent|global|clone|yield|match|fn|int|float|string|bool|void|mixed|callable|never|eval|list|unset|define|defined")) {
                        problems.add(new Problem(i + 1, varM.start(), varM.end(), "PHP variables must be prefixed with '$'", Problem.Severity.ERROR));
                    }
                }
            }

            if (line.contains("?>")) {
                inPhp = false;
            }
        }

        if (!hasOpenTag) {
            problems.add(new Problem(1, 0, 0, "Missing <?php opening tag", Problem.Severity.ERROR));
        }
    }

    // ---- JSON ----
    private static final Pattern JSON_SINGLE_QUOTE = Pattern.compile("'(?:\\\\.|[^'\\\\])*'");

    private static void lintJson(String text, List<Problem> problems) {
        String[] lines = text.split("\n", -1);
        String noStrings = text.replaceAll("\"(?:\\\\.|[^\"\\\\])*\"", "\"\"");
        String[] cleanLines = noStrings.split("\n", -1);
        Pattern unquotedKey = Pattern.compile("(?<!\")([a-zA-Z_]\\w*)\\s*:");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();

            if (trimmed.matches(".*,\\s*[}\\]].*")) {
                int idx = line.indexOf(',');
                problems.add(new Problem(i + 1, idx, idx + 1, "Trailing comma before closing bracket", Problem.Severity.ERROR));
            }

            Matcher sq = JSON_SINGLE_QUOTE.matcher(line);
            while (sq.find()) {
                problems.add(new Problem(i + 1, sq.start(), sq.end(), "Use double quotes instead of single quotes", Problem.Severity.ERROR));
            }

            if (trimmed.matches("^[}\\]].*[\\[{]")) {
                problems.add(new Problem(i + 1, 0, 0, "Possible missing comma between entries", Problem.Severity.WARNING));
            }

            Matcher uk = unquotedKey.matcher(cleanLines[i]);
            while (uk.find()) {
                String before = uk.start() > 0 ? String.valueOf(cleanLines[i].charAt(uk.start() - 1)) : "";
                if (!before.equals("\"")) {
                    problems.add(new Problem(i + 1, uk.start(), uk.start() + uk.group(1).length(), "JSON keys must be double-quoted", Problem.Severity.ERROR));
                }
            }
        }
    }

    // ---- YAML ----
    private static final Pattern YAML_KEY_VALUE = Pattern.compile("^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*:");
    private static final Pattern YAML_NO_SPACE = Pattern.compile(":[a-zA-Z_]");

    private static void lintYaml(String text, List<Problem> problems) {
        String[] lines = text.split("\n", -1);
        boolean indentMixed = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();

            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

            if (line.contains("\t")) {
                int tabIdx = line.indexOf('\t');
                problems.add(new Problem(i + 1, tabIdx, tabIdx + 1, "Tabs are not allowed in YAML", Problem.Severity.ERROR));
            }

            String ws = line.substring(0, line.length() - trimmed.length());
            if (!indentMixed && ws.contains("\t") && ws.contains(" ")) {
                problems.add(new Problem(i + 1, 0, 0, "Mixed tabs and spaces in indentation", Problem.Severity.ERROR));
                indentMixed = true;
            }

            Matcher ns = YAML_NO_SPACE.matcher(trimmed);
            if (ns.find()) {
                int col = line.indexOf(':') + ns.start();
                problems.add(new Problem(i + 1, col, col + 1, "Missing space after ':'", Problem.Severity.WARNING));
            }

            Matcher keyM = YAML_KEY_VALUE.matcher(line);
            if (keyM.find()) {
                String key = keyM.group(1);
                for (int j = 0; j < i; j++) {
                    Matcher prevM = YAML_KEY_VALUE.matcher(lines[j]);
                    if (prevM.find() && prevM.group(1).equals(key)) {
                        problems.add(new Problem(i + 1, keyM.start(1), keyM.end(1), "Duplicate key '" + key + "'", Problem.Severity.ERROR));
                        break;
                    }
                }
            }
        }
    }

    // ---- SQL ----
    private static final Pattern SQL_SELECT_STAR = Pattern.compile("\\bSELECT\\s+\\*", Pattern.CASE_INSENSITIVE);
    private static final Pattern SQL_UPDATE = Pattern.compile("\\bUPDATE\\s+\\w+", Pattern.CASE_INSENSITIVE);
    private static final Pattern SQL_DELETE = Pattern.compile("\\bDELETE\\s+FROM", Pattern.CASE_INSENSITIVE);
    private static final Pattern SQL_WHERE = Pattern.compile("\\bWHERE\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SQL_INSERT_WITH_COLS = Pattern.compile("\\bINSERT\\s+INTO\\s+\\w+\\s*\\(", Pattern.CASE_INSENSITIVE);
    private static final Pattern SQL_INSERT = Pattern.compile("\\bINSERT\\s+INTO\\s+\\w+", Pattern.CASE_INSENSITIVE);
    private static final Pattern SQL_JOIN = Pattern.compile("\\bJOIN\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SQL_ON = Pattern.compile("\\bON\\b", Pattern.CASE_INSENSITIVE);

    private static void lintSql(String text, List<Problem> problems) {
        String[] lines = text.split("\n", -1);
        String upper = text.toUpperCase();
        boolean hasWhere = SQL_WHERE.matcher(upper).find();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String lu = line.toUpperCase();

            Matcher sm = SQL_SELECT_STAR.matcher(lu);
            while (sm.find()) {
                problems.add(new Problem(i + 1, sm.start(), sm.end(), "SELECT * should list specific columns", Problem.Severity.WARNING));
            }

            if (SQL_UPDATE.matcher(lu).find() && !hasWhere) {
                problems.add(new Problem(i + 1, 0, 0, "UPDATE without WHERE clause", Problem.Severity.ERROR));
            }

            if (SQL_DELETE.matcher(lu).find() && !hasWhere) {
                problems.add(new Problem(i + 1, 0, 0, "DELETE without WHERE clause", Problem.Severity.ERROR));
            }

            if (SQL_INSERT.matcher(lu).find()) {
                if (!SQL_INSERT_WITH_COLS.matcher(lu).find()) {
                    problems.add(new Problem(i + 1, 0, 0, "INSERT should specify column list", Problem.Severity.WARNING));
                }
            }

            if (SQL_JOIN.matcher(lu).find()) {
                if (!SQL_ON.matcher(lu).find()) {
                    problems.add(new Problem(i + 1, 0, 0, "JOIN should have an ON clause", Problem.Severity.WARNING));
                }
            }
        }

        String trimmed = text.trim();
        if (!trimmed.endsWith(";")) {
            problems.add(new Problem(countLines(text), 0, 0, "SQL statement should end with semicolon", Problem.Severity.WARNING));
        }
    }

    // ---- C ----
    private static final Pattern C_COMMENT_SINGLE = Pattern.compile("//[^\n]*");
    private static final Pattern C_COMMENT_MULTI = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern C_STRING = Pattern.compile("\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'");

    private static void lintC(String text, List<Problem> problems) {
        String clean = C_COMMENT_SINGLE.matcher(text).replaceAll("");
        clean = C_COMMENT_MULTI.matcher(clean).replaceAll("");
        clean = C_STRING.matcher(clean).replaceAll("\"\"");

        int braces = 0, parens = 0;
        String[] lines = clean.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            for (char c : line.toCharArray()) {
                if (c == '{') braces++;
                else if (c == '}') braces--;
                else if (c == '(') parens++;
                else if (c == ')') parens--;
            }
            if (line.contains("goto ")) {
                problems.add(new Problem(i + 1, 0, 0, "Avoid using goto", Problem.Severity.WARNING));
            }
        }
        if (braces > 0) problems.add(new Problem(countLines(text), 0, 0, braces + " unclosed brace(s)", Problem.Severity.ERROR));
        if (braces < 0) problems.add(new Problem(1, 0, 0, (-braces) + " extra closing brace(s)", Problem.Severity.ERROR));
        if (parens > 0) problems.add(new Problem(countLines(text), 0, 0, parens + " unclosed parenthesis", Problem.Severity.ERROR));
        if (parens < 0) problems.add(new Problem(1, 0, 0, (-parens) + " extra closing parenthesis", Problem.Severity.ERROR));
    }

    // ---- C++ ----
    private static void lintCpp(String text, List<Problem> problems) {
        lintC(text, problems); // Same as C base checks
        String clean = C_COMMENT_SINGLE.matcher(text).replaceAll("");
        clean = C_COMMENT_MULTI.matcher(clean).replaceAll("");
        clean = C_STRING.matcher(clean).replaceAll("\"\"");
        String[] lines = clean.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.contains("class ") && !line.contains(";") && !line.contains("{")) {
                problems.add(new Problem(i + 1, 0, 0, "Class declaration missing '{'", Problem.Severity.ERROR));
            }
            if (line.contains("virtual") && !line.contains("~") && !line.contains("=") && !line.contains("{")) {
                if (line.contains("(") && line.contains(")")) {
                    problems.add(new Problem(i + 1, 0, 0, "Virtual method should have override or = 0", Problem.Severity.WARNING));
                }
            }
        }
    }

    // ---- Kotlin ----
    private static final Pattern KOTLIN_STRING = Pattern.compile("\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|\"\"\"(?:.|\\R)*?\"\"\"");

    private static void lintKotlin(String text, List<Problem> problems) {
        String clean = C_COMMENT_SINGLE.matcher(text).replaceAll("");
        clean = C_COMMENT_MULTI.matcher(clean).replaceAll("");
        clean = KOTLIN_STRING.matcher(clean).replaceAll("\"\"");
        String[] lines = clean.split("\n", -1);
        int braces = 0, parens = 0;
        boolean hasMain = false;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            if (trimmed.startsWith("fun main(")) hasMain = true;
            for (char c : line.toCharArray()) {
                if (c == '{') braces++;
                else if (c == '}') braces--;
                else if (c == '(') parens++;
                else if (c == ')') parens--;
            }
            if (trimmed.matches("var\\s+\\w+\\s*=\\s*null")) {
                problems.add(new Problem(i + 1, 0, 0, "Use 'val' instead of 'var' for non-mutated reference", Problem.Severity.WARNING));
            }
            if (trimmed.contains("!!")) {
                problems.add(new Problem(i + 1, 0, 0, "Avoid !! operator, use safe call '?.' or Elvis '?:'", Problem.Severity.WARNING));
            }
        }
        if (braces > 0) problems.add(new Problem(countLines(text), 0, 0, braces + " unclosed brace(s)", Problem.Severity.ERROR));
        if (braces < 0) problems.add(new Problem(1, 0, 0, (-braces) + " extra closing brace(s)", Problem.Severity.ERROR));
        if (parens > 0) problems.add(new Problem(countLines(text), 0, 0, parens + " unclosed parenthesis", Problem.Severity.ERROR));
        if (parens < 0) problems.add(new Problem(1, 0, 0, (-parens) + " extra closing parenthesis", Problem.Severity.ERROR));
    }

    // ---- Go ----
    private static void lintGo(String text, List<Problem> problems) {
        String clean = C_COMMENT_SINGLE.matcher(text).replaceAll("");
        clean = C_COMMENT_MULTI.matcher(clean).replaceAll("");
        clean = C_STRING.matcher(clean).replaceAll("\"\"");
        clean = clean.replaceAll("`[^`]*`", "\"\"");
        String[] lines = clean.split("\n", -1);
        int braces = 0, parens = 0;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            for (char c : line.toCharArray()) {
                if (c == '{') braces++;
                else if (c == '}') braces--;
                else if (c == '(') parens++;
                else if (c == ')') parens--;
            }
            if (trimmed.startsWith("if ") && !trimmed.startsWith("if err") && !trimmed.contains("{")) {
                problems.add(new Problem(i + 1, 0, 0, "Go requires braces on same line for if", Problem.Severity.ERROR));
            }
            if (trimmed.contains("_,") && trimmed.contains("err")) {
                problems.add(new Problem(i + 1, 0, 0, "Error ignored with '_'", Problem.Severity.WARNING));
            }
        }
        if (braces > 0) problems.add(new Problem(countLines(text), 0, 0, braces + " unclosed brace(s)", Problem.Severity.ERROR));
        if (braces < 0) problems.add(new Problem(1, 0, 0, (-braces) + " extra closing brace(s)", Problem.Severity.ERROR));
        if (parens > 0) problems.add(new Problem(countLines(text), 0, 0, parens + " unclosed parenthesis", Problem.Severity.ERROR));
        if (parens < 0) problems.add(new Problem(1, 0, 0, (-parens) + " extra closing parenthesis", Problem.Severity.ERROR));
    }

    // ---- Rust ----
    private static final Pattern RUST_STRING = Pattern.compile("\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|r#\"[^\"]*\"#");

    private static void lintRust(String text, List<Problem> problems) {
        String clean = C_COMMENT_SINGLE.matcher(text).replaceAll("");
        clean = C_COMMENT_MULTI.matcher(clean).replaceAll("");
        clean = RUST_STRING.matcher(clean).replaceAll("\"\"");
        String[] lines = clean.split("\n", -1);
        int braces = 0, parens = 0;
        boolean hasMain = false;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            if (trimmed.startsWith("fn main")) hasMain = true;
            for (char c : line.toCharArray()) {
                if (c == '{') braces++;
                else if (c == '}') braces--;
                else if (c == '(') parens++;
                else if (c == ')') parens--;
            }
            if (trimmed.contains("unwrap(") && !trimmed.contains("expect(")) {
                problems.add(new Problem(i + 1, 0, 0, "Prefer expect() over unwrap() for better error messages", Problem.Severity.WARNING));
            }
            if (trimmed.contains("mut ")) {
                // Just track, not always a problem; but flag as info
                if (trimmed.startsWith("let mut ") && !trimmed.contains("=")) {
                    problems.add(new Problem(i + 1, 0, 0, "Mutable variable without assignment", Problem.Severity.WARNING));
                }
            }
        }
        if (!hasMain) {
            problems.add(new Problem(1, 0, 0, "Binary crate should have a 'fn main()'", Problem.Severity.WARNING));
        }
        if (braces > 0) problems.add(new Problem(countLines(text), 0, 0, braces + " unclosed brace(s)", Problem.Severity.ERROR));
        if (braces < 0) problems.add(new Problem(1, 0, 0, (-braces) + " extra closing brace(s)", Problem.Severity.ERROR));
        if (parens > 0) problems.add(new Problem(countLines(text), 0, 0, parens + " unclosed parenthesis", Problem.Severity.ERROR));
        if (parens < 0) problems.add(new Problem(1, 0, 0, (-parens) + " extra closing parenthesis", Problem.Severity.ERROR));
    }

    private static int countLines(String text) {
        int n = 1;
        for (char c : text.toCharArray()) if (c == '\n') n++;
        return n;
    }
}