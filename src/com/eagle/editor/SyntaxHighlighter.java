package com.eagle.editor;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Produces StyleSpans for RichTextFX CodeArea based on regex rules per language.
 * Lightweight (no AST parsing) but covers the common syntax categories well
 * enough for a productive editing experience.
 */
public class SyntaxHighlighter {

    // ---------- JavaScript / TypeScript ----------
    private static final String JS_KEYWORDS =
            "\\b(var|let|const|function|return|if|else|for|while|do|switch|case|break|continue|" +
            "class|extends|super|new|this|typeof|instanceof|in|of|try|catch|finally|throw|" +
            "async|await|yield|import|export|default|from|as|null|undefined|true|false|void|delete|" +
            "static|get|set|constructor|package|private|protected|public|interface|enum|implements|" +
            "declare|namespace|module|type|readonly|keyof|never|unknown|any|string|number|boolean|" +
            "symbol|bigint|asserts|is|satisfies|using|await using|abstract|override|out|in|global|" +
            "debugger|with|super|null|undefined|true|false)\\b";

    private static final String JS_BUILTIN =
            "\\b(console|document|window|localStorage|sessionStorage|JSON|Math|Array|Object|String|" +
            "Number|Boolean|Date|RegExp|Map|Set|WeakMap|WeakSet|Promise|Symbol|Reflect|Proxy|" +
            "Error|TypeError|RangeError|SyntaxError|ReferenceError|EvalError|URIError|" +
            "Intl|BigInt|BigInt64Array|BigUint64Array|Float32Array|Float64Array|Int8Array|Int16Array|" +
            "Int32Array|Uint8Array|Uint8ClampedArray|Uint16Array|Uint32Array|" +
            "ArrayBuffer|SharedArrayBuffer|DataView|Atomics|" +
            "WebSocket|Worker|SharedWorker|ServiceWorker|MessageChannel|MessagePort|" +
            "Blob|File|FileReader|FileList|FormData|URL|URLSearchParams|" +
            "TextEncoder|TextDecoder|AbortController|AbortSignal|" +
            "MutationObserver|IntersectionObserver|ResizeObserver|PerformanceObserver|" +
            "CustomEvent|Event|EventTarget|Node|Element|HTMLElement|" +
            "fetch|setTimeout|setInterval|clearTimeout|clearInterval|" +
            "requestAnimationFrame|cancelAnimationFrame|" +
            "structuredClone|atob|btoa|encodeURI|encodeURIComponent|decodeURI|decodeURIComponent|" +
            "parseInt|parseFloat|isNaN|isFinite|eval|uneval|" +
            "Buffer|process|globalThis|Infinity|NaN)\\b";

    private static final Pattern JS_PATTERN = Pattern.compile(
            "(?<COMMENT>//[^\n]*|/\\*(?:.|\\R)*?\\*/)" +
            "|(?<DOCCOMMENT>/\\*\\*[^*]*(?:\\*(?!/)[^*]*)*\\*/)" +
            "|(?<STRING>\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|`(?:\\\\.|[^`\\\\])*`)" +
            "|(?<TEMPLATE>\\$\\{[^}]*})" +
            "|(?<NUMBER>\\b\\d+(\\.\\d+)?(e[+-]?\\d+)?\\b|\\b0[xX][0-9a-fA-F]+\\b|\\b0[bB][01]+\\b|\\b0[oO][0-7]+\\b)" +
            "|(?<KEYWORD>" + JS_KEYWORDS + ")" +
            "|(?<BUILTIN>" + JS_BUILTIN + ")" +
            "|(?<FUNCTIONCALL>\\b[A-Za-z_$][A-Za-z0-9_$]*(?=\\s*\\())" +
            "|(?<BRACE>[{}()\\[\\]])"
    );

    // ---------- HTML ----------
    private static final Pattern HTML_PATTERN = Pattern.compile(
            "(?<COMMENT><!--(?:.|\\R)*?-->)" +
            "|(?<TAG></?\\s*[A-Za-z][A-Za-z0-9-]*)" +
            "|(?<ATTR>\\b[a-zA-Z-]+(?==))" +
            "|(?<ATTRVALUE>\"[^\"]*\"|'[^']*')" +
            "|(?<TAGEND>/?>)" +
            "|(?<DOCTYPE><!DOCTYPE\\s+[^>]+>)" +
            "|(?<ENTITY>&[A-Za-z]+;|&#\\d+;)" +
            "|(?<STRING>\"[^\"]*\"|'[^']*')"
    );

    // ---------- JSX / TSX ----------
    private static final Pattern JSX_PATTERN = Pattern.compile(
            "(?<JSXTAG></?\\s*[A-Z][A-Za-z0-9.]*)" +
            "|(?<JSXATTR>\\b[a-zA-Z-]+(?=\\s*=))" +
            "|(?<JSXTAGEND>/?>)" +
            "|(?<COMMENT>//[^\n]*|/\\*(?:.|\\R)*?\\*/)" +
            "|(?<DOCCOMMENT>/\\*\\*[^*]*(?:\\*(?!/)[^*]*)*\\*/)" +
            "|(?<STRING>\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|`(?:\\\\.|[^`\\\\])*`)" +
            "|(?<TEMPLATE>\\$\\{[^}]*})" +
            "|(?<NUMBER>\\b\\d+(\\.\\d+)?(e[+-]?\\d+)?\\b|\\b0[xX][0-9a-fA-F]+\\b|\\b0[bB][01]+\\b|\\b0[oO][0-7]+\\b)" +
            "|(?<KEYWORD>" + JS_KEYWORDS + ")" +
            "|(?<BUILTIN>" + JS_BUILTIN + ")" +
            "|(?<FUNCTIONCALL>\\b[A-Za-z_$][A-Za-z0-9_$]*(?=\\s*\\())" +
            "|(?<BRACE>[{}()\\[\\]])"
    );

    // ---------- Vue ----------
    private static final Pattern VUE_PATTERN = Pattern.compile(
            "(?<COMMENT><!--(?:.|\\R)*?-->)" +
            "|(?<VUEDIRECTIVE>@[a-zA-Z-]+|:[a-zA-Z-]+|v-(?:bind|model|if|else-if|else|for|show|on|text|html|pre|once|cloak|slot)(?::[a-zA-Z-]+)?(?:\\.(?:stop|prevent|capture|self|once|passive))?)" +
            "|(?<VUEINTERP>\\{\\{[^}]+}})" +
            "|(?<TAG></?\\s*[A-Za-z][A-Za-z0-9-]*)" +
            "|(?<ATTR>\\b[a-zA-Z-]+(?==))" +
            "|(?<ATTRVALUE>\"[^\"]*\"|'[^']*')" +
            "|(?<TAGEND>/?>)" +
            "|(?<DOCTYPE><!DOCTYPE\\s+[^>]+>)" +
            "|(?<ENTITY>&[A-Za-z]+;|&#\\d+;)" +
            "|(?<STRING>\"[^\"]*\"|'[^']*')"
    );

    // ---------- Svelte ----------
    private static final Pattern SVELTE_PATTERN = Pattern.compile(
            "(?<COMMENT><!--(?:.|\\R)*?-->)" +
            "|(?<SVELTEBLOCK>\\{[#:/]\\w+(?:\\s+[^}]*)?}|\\{/\\w+}|\\{:else[^}]*}|\\{:\\w+\\s+[^}]*})" +
            "|(?<SVELTEDIRECTIVE>bind:|on:|use:|transition:|animate:|in:|out:|class:|style:)" +
            "|(?<TAG></?\\s*[A-Za-z][A-Za-z0-9-]*)" +
            "|(?<ATTR>\\b[a-zA-Z-]+(?==))" +
            "|(?<ATTRVALUE>\"[^\"]*\"|'[^']*')" +
            "|(?<TAGEND>/?>)" +
            "|(?<DOCTYPE><!DOCTYPE\\s+[^>]+>)" +
            "|(?<ENTITY>&[A-Za-z]+;|&#\\d+;)" +
            "|(?<STRING>\"[^\"]*\"|'[^']*')"
    );

    // ---------- CSS ----------
    private static final String CSS_AT_RULES =
            "@(media|import|font-face|keyframes|supports|container|layer|property|namespace|" +
            "page|counter-style|scroll-timeline|view-timeline|starting-style)";

    private static final Pattern CSS_PATTERN = Pattern.compile(
            "(?<COMMENT>/\\*(?:.|\\R)*?\\*/)" +
            "|(?<ATRULE>" + CSS_AT_RULES + ")" +
            "|(?<SELECTOR>[.#]?[A-Za-z][A-Za-z0-9_-]*(?=\\s*\\{))" +
            "|(?<PSEUDO>::?[A-Za-z-]+(?=[\\s{,:]))" +
            "|(?<PROPERTY>[a-zA-Z-]+(?=\\s*:))" +
            "|(?<STRING>\"[^\"]*\"|'[^']*')" +
            "|(?<NUMBER>\\b\\d+(\\.\\d+)?(px|em|rem|%|vh|vw|vmin|vmax|s|ms|deg|rad|grad|turn|" +
            "cm|mm|in|pt|pc|ch|ex|fr|lh|rlh|dvb|svb|cvb|dvw|svw|cvw|vb|vi)?\\b)" +
            "|(?<COLOR>#[0-9a-fA-F]{3,8}\\b|rgba?\\([^)]+\\)|hsla?\\([^)]+\\)|color\\([^)]+\\))" +
            "|(?<VARIABLE>--[A-Za-z][A-Za-z0-9-]*)" +
            "|(?<SCSSVAR>\\$[A-Za-z_][A-Za-z0-9_-]*)" +
            "|(?<FUNCTION>\\b[a-zA-Z-]+(?=\\())" +
            "|(?<BRACE>[{}])"
    );

    // ---------- JSON ----------
    private static final Pattern JSON_PATTERN = Pattern.compile(
            "(?<KEY>\"[^\"]*\"(?=\\s*:))" +
            "|(?<STRING>\"[^\"]*\")" +
            "|(?<NUMBER>\\b-?\\d+(\\.\\d+)?\\b)" +
            "|(?<BOOL>\\b(true|false|null)\\b)" +
            "|(?<BRACE>[{}\\[\\]])"
    );

    // ---------- Java ----------
    private static final String JAVA_KEYWORDS =
            "\\b(public|private|protected|class|interface|extends|implements|new|return|if|else|" +
            "for|while|do|switch|case|break|continue|try|catch|finally|throw|throws|import|package|" +
            "static|final|void|int|long|double|float|boolean|char|byte|short|String|this|super|" +
            "null|true|false|enum|abstract|synchronized|volatile|transient|instanceof)\\b";

    private static final Pattern JAVA_PATTERN = Pattern.compile(
            "(?<COMMENT>//[^\n]*|/\\*(?:.|\\R)*?\\*/)" +
            "|(?<STRING>\"(?:\\\\.|[^\"\\\\])*\")" +
            "|(?<NUMBER>\\b\\d+(\\.\\d+)?\\b)" +
            "|(?<KEYWORD>" + JAVA_KEYWORDS + ")" +
            "|(?<ANNOTATION>@[A-Za-z]+)" +
            "|(?<BRACE>[{}()\\[\\]])"
    );

    // ---------- PHP ----------
    private static final String PHP_KEYWORDS =
            "\\b(echo|print|die|exit|return|require|include|if|else|elseif|for|foreach|while|do|" +
            "switch|case|break|continue|function|class|interface|trait|abstract|final|public|private|" +
            "protected|static|const|new|this|self|parent|extends|implements|use|namespace|" +
            "try|catch|finally|throw|instanceof|clone|true|false|null|isset|empty|unset|array|" +
            "global|var|declare|match|enum|readonly|mixed|void|int|float|string|bool)\\b";

    private static final Pattern PHP_PATTERN = Pattern.compile(
            "(?<COMMENT>//[^\n]*|/\\*(?:.|\\R)*?\\*/|#[^\n]*)" +
            "|(?<STRING>\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*')" +
            "|(?<NUMBER>\\b\\d+(\\.\\d+)?\\b)" +
            "|(?<KEYWORD>" + PHP_KEYWORDS + ")" +
            "|(?<VARIABLE>\\$[A-Za-z_][A-Za-z0-9_]*)" +
            "|(?<FUNCTIONCALL>\\b[A-Za-z_][A-Za-z0-9_]*(?=\\s*\\())" +
            "|(?<BRACE>[{}()\\[\\]])"
    );

    // ---------- Python ----------
    private static final String PYTHON_KEYWORDS =
            "\\b(def|class|return|if|elif|else|for|while|break|continue|try|except|finally|" +
            "raise|with|as|import|from|pass|yield|lambda|global|nonlocal|assert|del|" +
            "True|False|None|self|cls|and|or|not|in|is|async|await|match|case)\\b";

    private static final Pattern PYTHON_PATTERN = Pattern.compile(
            "(?<COMMENT>#[^\n]*)" +
            "|(?<STRING>\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|\"\"\"(?:.|\\R)*?\"\"\"|'''(?:.|\\R)*?''')" +
            "|(?<NUMBER>\\b\\d+(\\.\\d+)?\\b)" +
            "|(?<KEYWORD>" + PYTHON_KEYWORDS + ")" +
            "|(?<FUNCTIONCALL>\\b[A-Za-z_][A-Za-z0-9_]*(?=\\s*\\())" +
            "|(?<BRACE>[{}()\\[\\]:])"
    );

    // ---------- SQL ----------
    private static final String SQL_KEYWORDS =
            "\\b(SELECT|FROM|WHERE|INSERT|INTO|VALUES|UPDATE|SET|DELETE|CREATE|TABLE|ALTER|DROP|" +
            "JOIN|LEFT|RIGHT|INNER|OUTER|FULL|CROSS|ON|AND|OR|NOT|IN|BETWEEN|LIKE|IS|NULL|" +
            "EXISTS|HAVING|GROUP|BY|ORDER|ASC|DESC|LIMIT|OFFSET|UNION|ALL|DISTINCT|AS|CASE|" +
            "WHEN|THEN|ELSE|END|COUNT|SUM|AVG|MIN|MAX|INDEX|PRIMARY|FOREIGN|KEY|REFERENCES|" +
            "UNIQUE|CHECK|DEFAULT|VARCHAR|INT|INTEGER|BIGINT|DECIMAL|FLOAT|DOUBLE|BOOLEAN|" +
            "DATE|DATETIME|TIMESTAMP|TEXT|BLOB|ENUM|BEGIN|COMMIT|ROLLBACK|GRANT|REVOKE|" +
            "EXPLAIN|DESCRIBE|SHOW|TRUNCATE)\\b";

    private static final Pattern SQL_PATTERN = Pattern.compile(
            "(?<COMMENT>--[^\n]*|/\\*(?:.|\\R)*?\\*/)" +
            "|(?<STRING>\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*')" +
            "|(?<NUMBER>\\b\\d+(\\.\\d+)?\\b)" +
            "|(?<KEYWORD>" + SQL_KEYWORDS + ")" +
            "|(?<BRACE>[()])"
    );

    // ---------- YAML ----------
    private static final Pattern YAML_PATTERN = Pattern.compile(
            "(?<COMMENT>#[^\n]*)" +
            "|(?<KEY>\\b[A-Za-z_][A-Za-z0-9_-]*(?=:))" +
            "|(?<STRING>\"[^\"]*\"|'[^']*')" +
            "|(?<NUMBER>\\b\\d+(\\.\\d+)?\\b)" +
            "|(?<BOOL>\\b(true|false|yes|no|on|off|null)\\b)"
    );

    // ---------- Shell / Bash ----------
    private static final Pattern SH_PATTERN = Pattern.compile(
            "(?<COMMENT>#[^\n]*)" +
            "|(?<STRING>\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|`[^`]*`)" +
            "|(?<NUMBER>\\b\\d+\\b)" +
            "|(?<KEYWORD>\\b(if|then|else|elif|fi|for|while|do|done|case|esac|in|function|return|" +
            "exit|continue|break|export|local|readonly|unset|echo|printf|read|source)\\b)" +
            "|(?<VARIABLE>\\$[A-Za-z_][A-Za-z0-9_]*|\\$\\{[^}]+\\}|\\$\\d+)" +
            "|(?<BRACE>[{}()])"
    );

    // ---------- C ----------
    private static final String C_KEYWORDS =
            "\\b(auto|break|case|char|const|continue|default|do|double|else|enum|extern|" +
            "float|for|goto|if|int|long|register|return|short|signed|sizeof|static|struct|" +
            "switch|typedef|union|unsigned|void|volatile|while|NULL|printf|scanf|malloc|" +
            "calloc|realloc|free|fopen|fclose|fread|fwrite|fprintf|fscanf|fgets|fputs|" +
            "fseek|ftell|rewind|feof|ferror|strlen|strcpy|strcat|strcmp|strchr|strstr|" +
            "sprintf|sscanf|atoi|atol|atof|rand|srand|exit|abort|assert|enum|const|" +
            "size_t|FILE|int8_t|int16_t|int32_t|int64_t|uint8_t|uint16_t|uint32_t|uint64_t)\\b";

    private static final Pattern C_PATTERN = Pattern.compile(
            "(?<COMMENT>//[^\n]*|/\\*(?:.|\\R)*?\\*/)" +
            "|(?<STRING>\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*')" +
            "|(?<NUMBER>\\b\\d+(\\.\\d+)?(f|l|ll|u)?\\b|\\b0[xX][0-9a-fA-F]+\\b|\\b0[bB][01]+\\b)" +
            "|(?<KEYWORD>" + C_KEYWORDS + ")" +
            "|(?<PREPROC>#\\s*(include|define|ifdef|ifndef|endif|pragma|if|else|elif|error|warning|undef|line)\\b[^\n]*)" +
            "|(?<BRACE>[{}()\\[\\]])"
    );

    // ---------- C++ ----------
    private static final String CPP_KEYWORDS =
            "\\b(alignas|alignof|and|and_eq|asm|auto|bitand|bitor|bool|break|case|catch|" +
            "char|char8_t|char16_t|char32_t|class|compl|concept|const|consteval|constexpr|" +
            "constinit|continue|co_await|co_return|co_yield|decltype|default|delete|do|" +
            "double|dynamic_cast|else|enum|explicit|export|extern|false|float|for|friend|" +
            "goto|if|inline|int|long|mutable|namespace|new|noexcept|nullptr|operator|or|" +
            "or_eq|override|private|protected|public|register|reinterpret_cast|requires|" +
            "return|short|signed|sizeof|static|static_assert|static_cast|struct|switch|" +
            "template|this|thread_local|throw|true|try|typedef|typeid|typename|union|" +
            "unsigned|using|virtual|void|volatile|wchar_t|while|xor|xor_eq|" +
            "cout|cin|cerr|clog|endl|flush|" +
            "string|vector|map|set|pair|list|deque|queue|stack|unordered_map|unordered_set|" +
            "shared_ptr|unique_ptr|weak_ptr|make_shared|make_unique|" +
            "function|bind|" +
            "ifstream|ofstream|fstream|stringstream|" +
            "nullptr_t|size_t|ptrdiff_t|int8_t|int16_t|int32_t|int64_t|uint8_t|uint16_t|uint32_t|uint64_t)\\b";

    private static final Pattern CPP_PATTERN = Pattern.compile(
            "(?<COMMENT>//[^\n]*|/\\*(?:.|\\R)*?\\*/)" +
            "|(?<STRING>\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|R\"[^)]*\\([^)]*\\)\")" +
            "|(?<NUMBER>\\b\\d+(\\.\\d+)?(f|l|ll|u|ul|ull)?\\b|\\b0[xX][0-9a-fA-F]+\\b|\\b0[bB][01]+\\b)" +
            "|(?<KEYWORD>" + CPP_KEYWORDS + ")" +
            "|(?<PREPROC>#\\s*(include|define|ifdef|ifndef|endif|pragma|if|else|elif|error|warning|undef|line|import|module|export)\\b[^\n]*)" +
            "|(?<BRACE>[{}()\\[\\]])"
    );

    // ---------- Kotlin ----------
    private static final String KOTLIN_KEYWORDS =
            "\\b(package|import|class|interface|object|companion|init|constructor|" +
            "fun|val|var|if|else|when|for|while|do|break|continue|return|try|catch|finally|throw|" +
            "public|private|protected|internal|abstract|open|final|override|sealed|data|" +
            "enum|annotation|inner|infix|inline|operator|tailrec|external|suspend|" +
            "true|false|null|this|super|is|!is|as|as\\?|in|!in|" +
            "typealias|reified|crossinline|noinline|" +
            "let|apply|run|also|with|takeIf|takeUnless|" +
            "List|MutableList|ArrayList|Map|MutableMap|HashMap|Set|MutableSet|HashSet|" +
            "String|Int|Long|Double|Float|Boolean|Char|Byte|Short|Unit|Any|Nothing|" +
            "Array|IntArray|LongArray|DoubleArray|BooleanArray|" +
            "println|print|readLine|" +
            "listOf|mapOf|setOf|mutableListOf|mutableMapOf|mutableSetOf|" +
            "emptyList|emptyMap|emptySet|" +
            "sequence|asSequence|filter|map|flatMap|forEach|reduce|fold|" +
            "groupBy|associate|partition|zip|distinct|sortedBy)\\b";

    private static final Pattern KOTLIN_PATTERN = Pattern.compile(
            "(?<COMMENT>//[^\n]*|/\\*(?:.|\\R)*?\\*/)" +
            "|(?<STRING>\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|\"\"\"(?:.|\\R)*?\"\"\")" +
            "|(?<NUMBER>\\b\\d+(\\.\\d+)?(f|L)?\\b|\\b0[xX][0-9a-fA-F]+\\b|\\b0[bB][01]+\\b)" +
            "|(?<KEYWORD>" + KOTLIN_KEYWORDS + ")" +
            "|(?<ANNOTATION>@[A-Za-z]+)" +
            "|(?<BRACE>[{}()\\[\\]])"
    );

    // ---------- Go ----------
    private static final String GO_KEYWORDS =
            "\\b(break|case|chan|const|continue|default|defer|else|fallthrough|for|" +
            "func|go|goto|if|import|interface|map|package|range|return|select|" +
            "struct|switch|type|var|" +
            "true|false|nil|iota|" +
            "int|int8|int16|int32|int64|uint|uint8|uint16|uint32|uint64|" +
            "uintptr|float32|float64|complex64|complex128|bool|string|byte|rune|error|" +
            "make|len|cap|new|append|copy|close|delete|" +
            "panic|recover|print|println|" +
            "string|int|error|bool|" +
            "fmt|http|json|io|os|time|strings|strconv|" +
            "Print|Printf|Println|Sprintf|Fprintf|" +
            "Errorf|New)\\b";

    private static final Pattern GO_PATTERN = Pattern.compile(
            "(?<COMMENT>//[^\n]*|/\\*(?:.|\\R)*?\\*/)" +
            "|(?<STRING>\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|`[^`]*`)" +
            "|(?<NUMBER>\\b\\d+(\\.\\d+)?(f|i)?\\b|\\b0[xX][0-9a-fA-F]+\\b|\\b0[oO][0-7]+\\b|\\b0[bB][01]+\\b)" +
            "|(?<KEYWORD>" + GO_KEYWORDS + ")" +
            "|(?<BRACE>[{}()\\[\\]])"
    );

    // ---------- Rust ----------
    private static final String RUST_KEYWORDS =
            "\\b(as|async|await|break|const|continue|crate|dyn|else|enum|extern|" +
            "false|fn|for|if|impl|in|let|loop|match|mod|move|mut|pub|ref|return|" +
            "self|Self|static|struct|super|trait|true|type|unsafe|use|where|while|" +
            "abstract|become|box|do|final|macro|override|priv|try|typeof|unsized|virtual|yield|" +
            "i8|i16|i32|i64|i128|isize|u8|u16|u32|u64|u128|usize|f32|f64|bool|char|str|" +
            "String|Vec|Option|Result|Box|Rc|Arc|Cell|RefCell|HashMap|HashSet|BTreeMap|BTreeSet|" +
            "Some|None|Ok|Err|" +
            "println|print|eprintln|eprint|format|" +
            "vec|panic|unreachable|unimplemented|assert|assert_eq|assert_ne|" +
            "clone|copy|into|from|as_ref|as_mut|unwrap|expect|map|and_then|is_some|is_none|" +
            "iter|into_iter|iter_mut|collect|filter|for_each|find|fold|" +
            "mod|use|fn|let|mut|const|static|type|impl|trait|struct|enum)\\b";

    private static final Pattern RUST_PATTERN = Pattern.compile(
            "(?<COMMENT>//[^\n]*|/\\*(?:.|\\R)*?\\*/)" +
            "|(?<STRING>\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|r#\"[^\"]*\"#|r\"[^\"]*\")" +
            "|(?<NUMBER>\\b\\d+(\\.\\d+)?(f32|f64|u8|u16|u32|u64|i8|i16|i32|i64|usize|isize)?\\b|\\b0[xX][0-9a-fA-F]+\\b|\\b0[oO][0-7]+\\b|\\b0[bB][01]+\\b)" +
            "|(?<KEYWORD>" + RUST_KEYWORDS + ")" +
            "|(?<LIFETIME>'[a-zA-Z_][a-zA-Z0-9_]*)" +
            "|(?<MACRO>[a-zA-Z_][a-zA-Z0-9_]*!)" +
            "|(?<BRACE>[{}()\\[\\]])"
    );

    // ---------- Markdown ----------
    private static final Pattern MARKDOWN_PATTERN = Pattern.compile(
            "(?<HEADER>^#{1,6}\\s.+$)" +
            "|(?<BOLD>\\*\\*[^*]+\\*\\*|__[^_]+__)" +
            "|(?<ITALIC>\\*[^*]+\\*|_[^_]+_)" +
            "|(?<CODE>`[^`]+`)" +
            "|(?<FENCED>```[\\s\\S]*?```)" +
            "|(?<LINK>\\[[^\\]]+\\]\\([^)]+\\))" +
            "|(?<IMAGE>!\\[[^\\]]+\\]\\([^)]+\\))" +
            "|(?<LIST>^\\s*[-*+]\\s+.*$|^\\s*\\d+\\.\\s+.*$)" +
            "|(?<BLOCKQUOTE>^\\s*>.*$)" +
            "|(?<HR>^\\s*[-*_]{3,}\\s*$)" +
            "|(?<STRIKETHROUGH>~~[^~]+~~)" +
            "|(?<TABLE>\\|[^|]+\\|)"
    );

    // ---------- XML ----------
    private static final Pattern XML_PATTERN = Pattern.compile(
            "(?<COMMENT><!--(?:.|\\R)*?-->)" +
            "|(?<TAG></?\\s*[A-Za-z][A-Za-z0-9-]*)" +
            "|(?<ATTR>\\b[a-zA-Z-]+(?==))" +
            "|(?<ATTRVALUE>\"[^\"]*\"|'[^']*')" +
            "|(?<TAGEND>/?>)" +
            "|(?<DOCTYPE><!DOCTYPE\\s+[^>]+>)" +
            "|(?<ENTITY>&[A-Za-z]+;|&#\\d+;)" +
            "|(?<STRING>\"[^\"]*\"|'[^']*')"
    );

    public static StyleSpans<Collection<String>> computeHighlighting(String text, LanguageType lang) {
        switch (lang) {
            case JAVASCRIPT:
            case TYPESCRIPT:
                return highlightWith(text, JS_PATTERN);
            case JSX:
            case TSX:
                return highlightWith(text, JSX_PATTERN);
            case HTML:
                return highlightWith(text, HTML_PATTERN);
            case VUE:
                return highlightWith(text, VUE_PATTERN);
            case SVELTE:
                return highlightWith(text, SVELTE_PATTERN);
            case ASTRO:
                return highlightWith(text, HTML_PATTERN);
            case XML:
            case SVG:
                return highlightWith(text, XML_PATTERN);
            case CSS:
                return highlightWith(text, CSS_PATTERN);
            case SCSS:
            case LESS:
            case SASS:
                return highlightWith(text, CSS_PATTERN);
            case JSON:
                return highlightWith(text, JSON_PATTERN);
            case JAVA:
                return highlightWith(text, JAVA_PATTERN);
            case PHP:
                return highlightWith(text, PHP_PATTERN);
            case PYTHON:
                return highlightWith(text, PYTHON_PATTERN);
            case SQL:
                return highlightWith(text, SQL_PATTERN);
            case YAML:
                return highlightWith(text, YAML_PATTERN);
            case SH:
            case DOCKERFILE:
                return highlightWith(text, SH_PATTERN);
            case C:
                return highlightWith(text, C_PATTERN);
            case CPP:
                return highlightWith(text, CPP_PATTERN);
            case KOTLIN:
                return highlightWith(text, KOTLIN_PATTERN);
            case GO:
                return highlightWith(text, GO_PATTERN);
            case RUST:
                return highlightWith(text, RUST_PATTERN);
            case MARKDOWN:
                return highlightWith(text, MARKDOWN_PATTERN);
            default:
                return noHighlighting(text);
        }
    }

    private static StyleSpans<Collection<String>> noHighlighting(String text) {
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
        builder.add(Collections.emptyList(), text.length());
        return builder.create();
    }

    private static StyleSpans<Collection<String>> highlightWith(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        int lastEnd = 0;
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();

        while (matcher.find()) {
            String styleClass = firstMatchingGroup(matcher);
            builder.add(Collections.singleton("syn-default"), matcher.start() - lastEnd);
            builder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastEnd = matcher.end();
        }
        builder.add(Collections.singleton("syn-default"), text.length() - lastEnd);
        return builder.create();
    }

    private static String firstMatchingGroup(Matcher matcher) {
        String[] groups = {
                "COMMENT", "DOCCOMMENT", "STRING", "TEMPLATE", "NUMBER", "KEYWORD", "BUILTIN",
                "FUNCTIONCALL", "BRACE",
                "TAG", "ATTR", "ATTRVALUE", "TAGEND", "DOCTYPE", "ENTITY",
                "SELECTOR", "PROPERTY", "COLOR", "ATRULE", "PSEUDO", "FUNCTION", "VARIABLE", "SCSSVAR",
                "KEY", "BOOL", "ANNOTATION", "PREPROC",
                "LIFETIME", "MACRO",
                "HEADER", "BOLD", "ITALIC", "CODE", "FENCED", "LINK", "IMAGE",
                "LIST", "BLOCKQUOTE", "HR", "STRIKETHROUGH", "TABLE",
                "JSXTAG", "JSXATTR", "JSXTAGEND",
                "VUEDIRECTIVE", "VUEINTERP",
                "SVELTEBLOCK", "SVELTEDIRECTIVE",
        };
        for (String g : groups) {
            try {
                if (matcher.group(g) != null) return "syn-" + g.toLowerCase();
            } catch (IllegalArgumentException ignored) { }
        }
        return "syn-default";
    }
}
