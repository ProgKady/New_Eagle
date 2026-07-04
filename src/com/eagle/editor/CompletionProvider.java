package com.eagle.editor;

import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class CompletionProvider {

    public static class Suggestion {
        public final String label;
        public final String insertText;
        public final String category;
        public final String codePreview;
        public final String description;
        public final List<String> params;
        public final String returnType;

        public Suggestion(String label, String insertText, String category) {
            this(label, insertText, category, null, null, null, null);
        }

        public Suggestion(String label, String insertText, String category, String codePreview) {
            this(label, insertText, category, codePreview, null, null, null);
        }

        public Suggestion(String label, String insertText, String category, String codePreview,
                          String description, List<String> params, String returnType) {
            this.label = label;
            this.insertText = insertText;
            this.category = category;
            this.codePreview = codePreview;
            this.description = description;
            this.params = params;
            this.returnType = returnType;
        }
    }

    private static final List<Suggestion> HTML_SUGGESTIONS = new ArrayList<>();
    private static final List<Suggestion> CSS_SUGGESTIONS = new ArrayList<>();
    private static final List<Suggestion> JS_SUGGESTIONS = new ArrayList<>();
    private static final List<Suggestion> JAVA_SUGGESTIONS = new ArrayList<>();
    private static final List<Suggestion> XML_SUGGESTIONS = new ArrayList<>();
    private static final List<Suggestion> JSON_SUGGESTIONS = new ArrayList<>();
    private static final List<Suggestion> PHP_SUGGESTIONS = new ArrayList<>();
    private static final List<Suggestion> PYTHON_SUGGESTIONS = new ArrayList<>();
    private static final List<Suggestion> SQL_SUGGESTIONS = new ArrayList<>();
    private static final List<Suggestion> SCSS_SUGGESTIONS = new ArrayList<>();
    private static final List<Suggestion> YAML_SUGGESTIONS = new ArrayList<>();
    private static final List<Suggestion> SH_SUGGESTIONS = new ArrayList<>();
    private static final List<Suggestion> C_SUGGESTIONS = new ArrayList<>();
    private static final List<Suggestion> CPP_SUGGESTIONS = new ArrayList<>();
    private static final List<Suggestion> MARKDOWN_SUGGESTIONS = new ArrayList<>();
    private static final List<Suggestion> VUE_SUGGESTIONS = new ArrayList<>();
    private static final List<Suggestion> SVELTE_SUGGESTIONS = new ArrayList<>();
    private static final List<Suggestion> ENV_SUGGESTIONS = new ArrayList<>();
    private static final List<Suggestion> GITIGNORE_SUGGESTIONS = new ArrayList<>();
    private static final List<Suggestion> KOTLIN_SUGGESTIONS = new ArrayList<>();
    private static final List<Suggestion> GO_SUGGESTIONS = new ArrayList<>();
    private static final List<Suggestion> RUST_SUGGESTIONS = new ArrayList<>();
    private static final List<Suggestion> ASTRO_SUGGESTIONS = new ArrayList<>();

    private static final File USER_SNIPPETS_FILE = new File(
        System.getProperty("user.home") + "/.webide/snippets/user_snippets.json");

    static {
        loadHtml();
        loadCss();
        loadJavaScript();
        loadDroidScript();
        loadJava();
        loadXml();
        loadJson();
        loadPhp();
        loadPython();
        loadSql();
        loadScss();
        loadYaml();
        loadSh();
        loadC();
        loadCpp();
        loadMarkdown();
        loadKotlin();
        loadGo();
        loadRust();
        loadVue();
        loadSvelte();
        loadEnv();
        loadGitignore();
        loadAstro();
        loadUserSnippets();
    }

    private static Suggestion sug(String label, String insertText, String category, String desc) {
        return new Suggestion(label, insertText, category, null, desc, null, null);
    }

    private static Suggestion sug(String label, String insertText, String category, String desc, List<String> params, String returnType) {
        return new Suggestion(label, insertText, category, null, desc, params, returnType);
    }

    // ========================================================================
    //   HTML
    // ========================================================================
    private static void loadHtml() {
        String[] htmlTags = {
            "html", "head", "title", "meta", "link", "style", "script", "body", "div", "span",
            "h1", "h2", "h3", "h4", "h5", "h6", "p", "a", "img", "figure", "figcaption",
            "ul", "ol", "li", "dl", "dt", "dd", "table", "tr", "td", "th", "thead", "tbody", "tfoot", "colgroup", "col",
            "form", "input", "button", "select", "option", "optgroup", "label", "textarea", "fieldset", "legend",
            "nav", "header", "footer", "section", "article", "aside", "main", "summary", "details",
            "video", "audio", "source", "track", "canvas", "svg", "iframe", "picture",
            "br", "hr", "wbr", "strong", "em", "b", "i", "u", "s", "mark", "small", "sub", "sup",
            "code", "pre", "blockquote", "q", "cite", "abbr", "address", "time", "data",
            "kbd", "samp", "var", "del", "ins", "dfn",
            "progress", "meter", "dialog", "template",
            "ruby", "rt", "rp", "bdi", "bdo",
            "datalist", "output", "map", "area",
            "noscript", "base", "slot",
        };
        for (String tag : htmlTags) {
            HTML_SUGGESTIONS.add(new Suggestion("<" + tag + ">", "<" + tag + ">{CURSOR}</" + tag + ">", "tag", "HTML element tag"));
        }
        HTML_SUGGESTIONS.add(sug("<br>", "<br>", "tag", "HTML void element - line break"));
        HTML_SUGGESTIONS.add(sug("<hr>", "<hr>", "tag", "HTML void element - horizontal rule"));
        HTML_SUGGESTIONS.add(sug("<wbr>", "<wbr>", "tag", "HTML void element - word break opportunity"));
        HTML_SUGGESTIONS.add(sug("<input>", "<input type=\"{CURSOR}\">", "tag", "HTML void element - input field"));
        HTML_SUGGESTIONS.add(sug("<meta>", "<meta {CURSOR}>", "tag", "HTML void element - metadata"));
        HTML_SUGGESTIONS.add(sug("<link>", "<link rel=\"stylesheet\" href=\"{CURSOR}\">", "tag", "HTML void element - external resource link"));
        HTML_SUGGESTIONS.add(sug("<img>", "<img src=\"{CURSOR}\" alt=\"\">", "tag", "HTML void element - image embed"));
        HTML_SUGGESTIONS.add(sug("<source>", "<source src=\"{CURSOR}\" type=\"\">", "tag", "HTML void element - media source"));
        HTML_SUGGESTIONS.add(sug("<track>", "<track src=\"{CURSOR}\" kind=\"subtitles\">", "tag", "HTML void element - text track"));

        String[] htmlAttrs = {
            "class", "id", "style", "title", "lang", "dir", "hidden", "tabindex", "accesskey",
            "data-", "role", "aria-label", "aria-hidden", "aria-expanded", "aria-current", "aria-disabled", "aria-required",
            "src", "href", "alt", "rel", "type", "media",
            "width", "height", "loading", "decoding", "crossorigin",
            "name", "value", "placeholder", "required", "disabled", "readonly", "autofocus", "autocomplete",
            "min", "max", "minlength", "maxlength", "pattern", "step", "multiple", "accept",
            "checked", "selected", "default", "form", "formaction", "formenctype", "formmethod", "formnovalidate", "formtarget",
            "action", "method", "enctype", "novalidate", "target",
            "colspan", "rowspan", "headers", "scope",
            "href", "hreflang", "download", "ping", "rel",
            "srcset", "sizes", "media",
            "controls", "autoplay", "loop", "muted", "poster", "preload",
            "srcdoc", "sandbox", "allow",
            "onclick", "onchange", "oninput", "onsubmit", "onfocus", "onblur", "onmouseover", "onmouseout",
            "onkeydown", "onkeyup", "onload", "onerror", "onscroll", "onresize",
            "spellcheck", "contenteditable", "draggable", "dropzone",
            "itemscope", "itemtype", "itemprop", "itemid", "itemref",
            "slot", "part", "exportparts",
            "inputmode", "enterkeyhint", "autocapitalize", "autocorrect",
            "popover", "popovertarget", "popovertargetaction",
        };
        for (String attr : htmlAttrs) {
            HTML_SUGGESTIONS.add(sug(attr + "=\"\"", attr + "=\"{CURSOR}\"", "attr", "HTML attribute"));
        }

        String[] inputTypes = {
            "text", "password", "email", "number", "tel", "url", "search",
            "checkbox", "radio", "file", "color", "date", "datetime-local",
            "month", "week", "time", "range", "submit", "button", "reset", "hidden",
            "image",
        };
        for (String it : inputTypes) {
            HTML_SUGGESTIONS.add(sug("type=" + it, "type=\"" + it + "\"", "attr", "HTML input type attribute"));
        }

        HTML_SUGGESTIONS.add(sug("! HTML5 skeleton",
                "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>{CURSOR}</title>\n</head>\n<body>\n\n</body>\n</html>", "snippet", "HTML document boilerplate skeleton"));
        HTML_SUGGESTIONS.add(sug("link stylesheet", "<link rel=\"stylesheet\" href=\"{CURSOR}\">", "snippet", "External CSS link tag"));
        HTML_SUGGESTIONS.add(sug("script src", "<script src=\"{CURSOR}\"></script>", "snippet", "External JavaScript script tag"));
        HTML_SUGGESTIONS.add(sug("img with src/alt", "<img src=\"{CURSOR}\" alt=\"\">", "snippet", "Image tag with src and alt attributes"));
        HTML_SUGGESTIONS.add(sug("a with href", "<a href=\"{CURSOR}\">Link</a>", "snippet", "Anchor link tag"));
        HTML_SUGGESTIONS.add(sug("meta viewport", "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">", "snippet", "Viewport meta tag for responsive design"));
        HTML_SUGGESTIONS.add(sug("meta charset", "<meta charset=\"UTF-8\">", "snippet", "Character encoding meta tag"));
        HTML_SUGGESTIONS.add(sug("form with fields", "<form action=\"{CURSOR}\" method=\"POST\">\n    <label>Name</label>\n    <input type=\"text\" name=\"name\">\n    <button type=\"submit\">Submit</button>\n</form>", "snippet", "HTML form with label, input, and button"));
        HTML_SUGGESTIONS.add(sug("table skeleton", "<table>\n    <thead>\n        <tr>\n            <th>{CURSOR}</th>\n        </tr>\n    </thead>\n    <tbody>\n        <tr>\n            <td></td>\n        </tr>\n    </tbody>\n</table>", "snippet", "HTML table structure skeleton"));
        HTML_SUGGESTIONS.add(sug("nav bar", "<nav>\n    <ul>\n        <li><a href=\"{CURSOR}\">Home</a></li>\n        <li><a href=\"#\">About</a></li>\n        <li><a href=\"#\">Contact</a></li>\n    </ul>\n</nav>", "snippet", "Navigation bar with links"));
        HTML_SUGGESTIONS.add(sug("video with source", "<video controls width=\"640\">\n    <source src=\"{CURSOR}\" type=\"video/mp4\">\n</video>", "snippet", "Video element with source tag"));
        HTML_SUGGESTIONS.add(sug("audio", "<audio controls>\n    <source src=\"{CURSOR}\" type=\"audio/mpeg\">\n</audio>", "snippet", "Audio element with source tag"));
        HTML_SUGGESTIONS.add(sug("iframe", "<iframe src=\"{CURSOR}\" width=\"800\" height=\"600\" frameborder=\"0\"></iframe>", "snippet", "Inline frame embed"));
        HTML_SUGGESTIONS.add(sug("details accordion", "<details>\n    <summary>{CURSOR}</summary>\n    <p>Content here</p>\n</details>", "snippet", "Collapsible details accordion"));
        HTML_SUGGESTIONS.add(sug("bootstrap 5 CDN", "<link href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css\" rel=\"stylesheet\">", "snippet", "Bootstrap 5 CSS CDN link"));
        HTML_SUGGESTIONS.add(sug("tailwind CDN", "<script src=\"https://cdn.tailwindcss.com\"></script>", "snippet", "Tailwind CSS CDN script"));
        HTML_SUGGESTIONS.add(sug("fontawesome CDN", "<link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css\">", "snippet", "Font Awesome icons CDN link"));
        HTML_SUGGESTIONS.add(sug("google fonts", "<link href=\"https://fonts.googleapis.com/css2?family={CURSOR}&display=swap\" rel=\"stylesheet\">", "snippet", "Google Fonts import link"));
        HTML_SUGGESTIONS.add(sug("select with options", "<select name=\"{CURSOR}\">\n    <option value=\"\">Select...</option>\n    <option value=\"1\">Option 1</option>\n</select>", "snippet", "Select dropdown with options"));
        HTML_SUGGESTIONS.add(sug("radio group", "<label><input type=\"radio\" name=\"{CURSOR}\" value=\"1\"> Option 1</label>\n<label><input type=\"radio\" name=\"{CURSOR}\" value=\"2\"> Option 2</label>", "snippet", "Radio button group"));
        HTML_SUGGESTIONS.add(sug("checkbox", "<label><input type=\"checkbox\" name=\"{CURSOR}\"> Check me</label>", "snippet", "Checkbox input with label"));
        HTML_SUGGESTIONS.add(sug("fieldset legend", "<fieldset>\n    <legend>{CURSOR}</legend>\n    \n</fieldset>", "snippet", "Fieldset with legend grouping"));
        HTML_SUGGESTIONS.add(sug("card bootstrap", "<div class=\"card\">\n    <div class=\"card-body\">\n        <h5 class=\"card-title\">{CURSOR}</h5>\n        <p class=\"card-text\">Content</p>\n    </div>\n</div>", "snippet", "Bootstrap card component"));
        HTML_SUGGESTIONS.add(sug("container bootstrap", "<div class=\"container\">\n    <div class=\"row\">\n        <div class=\"col\">{CURSOR}</div>\n    </div>\n</div>", "snippet", "Bootstrap grid container layout"));
        HTML_SUGGESTIONS.add(sug("progress bar", "<progress value=\"50\" max=\"100\"></progress>", "snippet", "HTML progress bar element"));
        HTML_SUGGESTIONS.add(sug("dialog modal", "<dialog id=\"{CURSOR}\">\n    <p>Content</p>\n    <button onclick=\"document.getElementById('{CURSOR}').close()\">Close</button>\n</dialog>", "snippet", "HTML dialog modal element"));
        HTML_SUGGESTIONS.add(sug("accordion (pure)", "<details>\n    <summary>{CURSOR}</summary>\n    <p>Hidden content</p>\n</details>", "snippet", "Pure HTML accordion using details/summary"));

        // ---- Alpine.js ----
        String[][] alpineAttrs = {
            {"x-data", "x-data=\"{ {CURSOR}: '' }\"", "attr"},
            {"x-init", "x-init=\"{CURSOR}\"", "attr"},
            {"x-show", "x-show=\"{CURSOR}\"", "attr"},
            {"x-text", "x-text=\"{CURSOR}\"", "attr"},
            {"x-html", "x-html=\"{CURSOR}\"", "attr"},
            {"x-model", "x-model=\"{CURSOR}\"", "attr"},
            {"x-effect", "x-effect=\"{CURSOR}\"", "attr"},
            {"x-if", "x-if=\"{CURSOR}\"", "attr"},
            {"x-for", "x-for=\"item in {CURSOR}\"", "attr"},
            {"x-on:click", "x-on:click=\"{CURSOR}\"", "attr"},
            {"x-on:input", "x-on:input=\"{CURSOR}\"", "attr"},
            {"x-on:submit", "x-on:submit.prevent=\"{CURSOR}\"", "attr"},
            {"x-on:keydown", "x-on:keydown.enter=\"{CURSOR}\"", "attr"},
            {"x-on:change", "x-on:change=\"{CURSOR}\"", "attr"},
            {"x-bind:class", "x-bind:class=\"{CURSOR}\"", "attr"},
            {"x-bind:style", "x-bind:style=\"{CURSOR}\"", "attr"},
            {"x-bind:href", "x-bind:href=\"{CURSOR}\"", "attr"},
            {"x-bind:src", "x-bind:src=\"{CURSOR}\"", "attr"},
            {"x-ref", "x-ref=\"{CURSOR}\"", "attr"},
            {"x-cloak", "x-cloak", "attr"},
            {"x-teleport", "x-teleport=\"{CURSOR}\"", "attr"},
            {"x-transition", "x-transition", "attr"},
            {"x-transition:enter", "x-transition:enter=\"transition ease-out duration-300\"", "attr"},
            {"x-transition:leave", "x-transition:leave=\"transition ease-in duration-200\"", "attr"},
            {"@click", "@click=\"{CURSOR}\"", "attr"},
            {"@keyup", "@keyup.enter=\"{CURSOR}\"", "attr"},
            {"@submit.prevent", "@submit.prevent=\"{CURSOR}\"", "attr"},
        };
        for (String[] al : alpineAttrs) {
            HTML_SUGGESTIONS.add(sug(al[0], al[1], al[2], "Alpine.js directive / attribute"));
        }
        HTML_SUGGESTIONS.add(sug("Alpine dropdown", "<div x-data=\"{ open: false }\">\n    <button @click=\"open = !open\">Toggle</button>\n    <div x-show=\"open\" @click.outside=\"open = false\">\n        {CURSOR}\n    </div>\n</div>", "snippet", "Alpine.js dropdown component"));
        HTML_SUGGESTIONS.add(sug("Alpine modal", "<div x-data=\"{ show: false }\">\n    <button @click=\"show = true\">Open</button>\n    <div x-show=\"show\" class=\"fixed inset-0\" @click.self=\"show = false\">\n        <div @click.stop>\n            <button @click=\"show = false\">&times;</button>\n            {CURSOR}\n        </div>\n    </div>\n</div>", "snippet", "Alpine.js modal component"));
        HTML_SUGGESTIONS.add(sug("Alpine tabs", "<div x-data=\"{ tab: 'first' }\">\n    <button :class=\"{ 'active': tab === 'first' }\" @click=\"tab = 'first'\">First</button>\n    <button :class=\"{ 'active': tab === 'second' }\" @click=\"tab = 'second'\">Second</button>\n    <div x-show=\"tab === 'first'\">{CURSOR}</div>\n    <div x-show=\"tab === 'second'\"></div>\n</div>", "snippet", "Alpine.js tabs component"));
        HTML_SUGGESTIONS.add(sug("Alpine form", "<form x-data=\"{ email: '', password: '' }\" @submit.prevent=\"console.log(email, password)\">\n    <input type=\"email\" x-model=\"email\" placeholder=\"Email\">\n    <input type=\"password\" x-model=\"password\" placeholder=\"Password\">\n    <button type=\"submit\">Submit</button>\n</form>", "snippet", "Alpine.js form with x-model"));
        HTML_SUGGESTIONS.add(sug("Alpine store", "<script>\n    document.addEventListener('alpine:init', () => {\n        Alpine.store('{CURSOR}', {\n            items: [],\n            init() { console.log('store ready'); }\n        });\n    });\n</script>", "snippet", "Alpine.js global store definition"));
        HTML_SUGGESTIONS.add(sug("Alpine data component", "<div x-data=\"{\n    count: 0,\n    increment() { this.count++ },\n    decrement() { this.count-- }\n}\">\n    <button @click=\"decrement\">-</button>\n    <span x-text=\"count\"></span>\n    <button @click=\"increment\">+</button>\n</div>", "snippet", "Alpine.js counter component with methods"));
    }

    // ========================================================================
    //   CSS
    // ========================================================================
    private static void loadCss() {
        String[] cssProps = {
            "color", "background-color", "background", "background-image", "background-size", "background-position", "background-repeat", "background-attachment", "background-clip", "background-origin",
            "font-size", "font-family", "font-weight", "font-style", "font-variant", "font-stretch", "line-height", "font-display", "font-optical-sizing",
            "margin", "margin-top", "margin-bottom", "margin-left", "margin-right",
            "padding", "padding-top", "padding-bottom", "padding-left", "padding-right",
            "border", "border-width", "border-style", "border-color", "border-radius",
            "border-top", "border-bottom", "border-left", "border-right",
            "border-top-left-radius", "border-top-right-radius", "border-bottom-left-radius", "border-bottom-right-radius",
            "border-image", "border-image-source", "border-image-slice", "border-image-width",
            "outline", "outline-width", "outline-style", "outline-color", "outline-offset",
            "width", "height", "max-width", "max-height", "min-width", "min-height",
            "display", "flex", "flex-direction", "flex-wrap", "flex-flow", "flex-grow", "flex-shrink", "flex-basis",
            "justify-content", "align-items", "align-self", "align-content", "gap", "row-gap", "column-gap", "order", "place-items", "place-content", "place-self",
            "grid", "grid-template-columns", "grid-template-rows", "grid-template-areas", "grid-column", "grid-row", "grid-area",
            "grid-column-start", "grid-column-end", "grid-row-start", "grid-row-end",
            "grid-auto-flow", "grid-auto-columns", "grid-auto-rows",
            "position", "top", "left", "right", "bottom", "z-index", "inset",
            "overflow", "overflow-x", "overflow-y", "overflow-wrap", "word-break", "word-wrap",
            "text-align", "text-decoration", "text-transform", "text-indent", "text-shadow", "text-overflow", "text-underline-offset", "text-decoration-thickness",
            "letter-spacing", "word-spacing", "white-space", "vertical-align", "direction", "unicode-bidi",
            "box-shadow", "text-shadow", "filter", "backdrop-filter",
            "transition", "transition-property", "transition-duration", "transition-timing-function", "transition-delay",
            "transform", "transform-origin", "transform-style", "perspective", "perspective-origin", "backface-visibility",
            "translate", "rotate", "scale",
            "animation", "animation-name", "animation-duration", "animation-timing-function", "animation-delay",
            "animation-iteration-count", "animation-direction", "animation-fill-mode", "animation-play-state",
            "opacity", "visibility", "cursor", "pointer-events", "resize", "user-select", "-webkit-appearance", "appearance",
            "content", "counter-increment", "counter-reset", "quotes",
            "list-style", "list-style-type", "list-style-position", "list-style-image",
            "table-layout", "border-collapse", "border-spacing", "empty-cells", "caption-side",
            "clip-path", "shape-outside", "object-fit", "object-position",
            "scroll-behavior", "scrollbar-width", "scrollbar-color", "scroll-margin", "scroll-padding", "scroll-snap-type", "scroll-snap-align",
            "accent-color", "caret-color", "color-scheme",
            "container", "container-type", "container-name",
            "aspect-ratio", "box-sizing",
            "float", "clear",
            "columns", "column-count", "column-width", "column-gap", "column-rule", "column-span", "column-fill",
            "orphans", "widows", "page-break-inside", "page-break-before", "page-break-after",
            "mix-blend-mode", "isolation",
            "will-change", "contain",
            "touch-action", "overscroll-behavior",
            "--*", "var(--*)",
        };
        for (String prop : cssProps) {
            CSS_SUGGESTIONS.add(new Suggestion(prop, prop + ": {CURSOR};", "property", "CSS property for styling elements"));
        }

        String[][] cssValues = {
            {"display: block", "display: block;", "value"},
            {"display: inline", "display: inline;", "value"},
            {"display: inline-block", "display: inline-block;", "value"},
            {"display: flex", "display: flex;", "value"},
            {"display: grid", "display: grid;", "value"},
            {"display: none", "display: none;", "value"},
            {"display: table", "display: table;", "value"},
            {"display: contents", "display: contents;", "value"},
            {"position: relative", "position: relative;", "value"},
            {"position: absolute", "position: absolute;", "value"},
            {"position: fixed", "position: fixed;", "value"},
            {"position: sticky", "position: sticky;", "value"},
            {"flex-direction: row", "flex-direction: row;", "value"},
            {"flex-direction: column", "flex-direction: column;", "value"},
            {"flex-wrap: wrap", "flex-wrap: wrap;", "value"},
            {"justify-content: center", "justify-content: center;", "value"},
            {"justify-content: space-between", "justify-content: space-between;", "value"},
            {"justify-content: space-around", "justify-content: space-around;", "value"},
            {"align-items: center", "align-items: center;", "value"},
            {"align-items: stretch", "align-items: stretch;", "value"},
            {"align-items: flex-start", "align-items: flex-start;", "value"},
            {"text-align: center", "text-align: center;", "value"},
            {"text-align: left", "text-align: left;", "value"},
            {"text-align: right", "text-align: right;", "value"},
            {"text-align: justify", "text-align: justify;", "value"},
            {"overflow: hidden", "overflow: hidden;", "value"},
            {"overflow: auto", "overflow: auto;", "value"},
            {"overflow: scroll", "overflow: scroll;", "value"},
            {"font-weight: bold", "font-weight: bold;", "value"},
            {"font-weight: normal", "font-weight: normal;", "value"},
            {"font-style: italic", "font-style: italic;", "value"},
            {"text-decoration: none", "text-decoration: none;", "value"},
            {"text-decoration: underline", "text-decoration: underline;", "value"},
            {"cursor: pointer", "cursor: pointer;", "value"},
            {"cursor: default", "cursor: default;", "value"},
            {"list-style: none", "list-style: none;", "value"},
            {"box-sizing: border-box", "box-sizing: border-box;", "value"},
            {"clear: both", "clear: both;", "value"},
            {"float: left", "float: left;", "value"},
            {"float: right", "float: right;", "value"},
            {"object-fit: cover", "object-fit: cover;", "value"},
            {"object-fit: contain", "object-fit: contain;", "value"},
        };
        for (String[] v : cssValues) {
            CSS_SUGGESTIONS.add(sug(v[0], v[1], v[2], "CSS value"));
        }

        CSS_SUGGESTIONS.add(sug("@media (max-width)", "@media (max-width: 768px) {\n    {CURSOR}\n}", "snippet", "CSS media query for max-width breakpoint"));
        CSS_SUGGESTIONS.add(sug("@media (min-width)", "@media (min-width: 1024px) {\n    {CURSOR}\n}", "snippet", "CSS media query for min-width breakpoint"));
        CSS_SUGGESTIONS.add(sug("@media (prefers-color-scheme: dark)", "@media (prefers-color-scheme: dark) {\n    {CURSOR}\n}", "snippet", "CSS media query for dark mode preference"));
        CSS_SUGGESTIONS.add(sug("@keyframes", "@keyframes {CURSOR} {\n    0% { }\n    100% { }\n}", "snippet", "CSS keyframe animation definition"));
        CSS_SUGGESTIONS.add(sug("@font-face", "@font-face {\n    font-family: '{CURSOR}';\n    src: url('') format('woff2');\n    font-display: swap;\n}", "snippet", "CSS custom font face declaration"));
        CSS_SUGGESTIONS.add(sug("@import url", "@import url('{CURSOR}');", "snippet", "CSS @import rule for external stylesheets"));
        CSS_SUGGESTIONS.add(sug("@supports", "@supports (display: grid) {\n    {CURSOR}\n}", "snippet", "CSS feature support query"));
        CSS_SUGGESTIONS.add(sug("@container", "@container {CURSOR} (min-width: 400px) {\n    \n}", "snippet", "CSS container query"));
        CSS_SUGGESTIONS.add(sug("@layer", "@layer {CURSOR} {\n    \n}", "snippet", "CSS cascade layer definition"));
        CSS_SUGGESTIONS.add(sug("@property", "@property --{CURSOR} {\n    syntax: '<color>';\n    inherits: false;\n    initial-value: #000;\n}", "snippet", "CSS custom property @property rule"));
        CSS_SUGGESTIONS.add(sug("flex center", "display: flex;\nalign-items: center;\njustify-content: center;", "snippet", "Flexbox centering utility"));
        CSS_SUGGESTIONS.add(sug("flex column", "display: flex;\nflex-direction: column;", "snippet", "Flexbox column direction"));
        CSS_SUGGESTIONS.add(sug("grid 2-col", "display: grid;\ngrid-template-columns: repeat(2, 1fr);\ngap: 1rem;", "snippet", "CSS grid 2-column layout"));
        CSS_SUGGESTIONS.add(sug("grid 3-col", "display: grid;\ngrid-template-columns: repeat(3, 1fr);\ngap: 1rem;", "snippet", "CSS grid 3-column layout"));
        CSS_SUGGESTIONS.add(sug("grid auto-fit", "display: grid;\ngrid-template-columns: repeat(auto-fit, minmax(250px, 1fr));\ngap: 1rem;", "snippet", "CSS responsive auto-fit grid layout"));
        CSS_SUGGESTIONS.add(sug("box-shadow soft", "box-shadow: 0 4px 12px rgba(0,0,0,0.1);", "snippet", "Soft box shadow effect"));
        CSS_SUGGESTIONS.add(sug("box-shadow medium", "box-shadow: 0 8px 30px rgba(0,0,0,0.12);", "snippet", "Medium box shadow effect"));
        CSS_SUGGESTIONS.add(sug("box-shadow large", "box-shadow: 0 20px 60px rgba(0,0,0,0.15);", "snippet", "Large box shadow effect"));
        CSS_SUGGESTIONS.add(sug("text-shadow soft", "text-shadow: 0 2px 4px rgba(0,0,0,0.15);", "snippet", "Soft text shadow effect"));
        CSS_SUGGESTIONS.add(sug("transition fast", "transition: all 0.2s ease;", "snippet", "Fast CSS transition"));
        CSS_SUGGESTIONS.add(sug("transition medium", "transition: all 0.3s ease;", "snippet", "Medium CSS transition"));
        CSS_SUGGESTIONS.add(sug("reset default", "*, *::before, *::after {\n    box-sizing: border-box;\n    margin: 0;\n    padding: 0;\n}", "snippet", "CSS reset default styles"));
        CSS_SUGGESTIONS.add(sug("scrollbar custom",
                "::-webkit-scrollbar { width: 8px; }\n::-webkit-scrollbar-track { background: transparent; }\n::-webkit-scrollbar-thumb { background: #888; border-radius: 4px; }\n::-webkit-scrollbar-thumb:hover { background: #555; }", "snippet", "Custom scrollbar styling"));
        CSS_SUGGESTIONS.add(sug("aspect-ratio 16/9", "aspect-ratio: 16 / 9;\nobject-fit: cover;", "snippet", "16:9 aspect ratio with cover fit"));
        CSS_SUGGESTIONS.add(sug("aspect-ratio 1/1", "aspect-ratio: 1 / 1;\nobject-fit: cover;", "snippet", "1:1 square aspect ratio with cover fit"));
        CSS_SUGGESTIONS.add(sug("aspect-ratio 4/3", "aspect-ratio: 4 / 3;", "snippet", "4:3 aspect ratio"));
        CSS_SUGGESTIONS.add(sug("clip-path circle", "clip-path: circle(50%);", "snippet", "Circular clip path"));
        CSS_SUGGESTIONS.add(sug("gradient linear", "background: linear-gradient(135deg, {CURSOR}, );", "snippet", "Linear gradient background"));
        CSS_SUGGESTIONS.add(sug("gradient radial", "background: radial-gradient(circle, {CURSOR}, );", "snippet", "Radial gradient background"));
        CSS_SUGGESTIONS.add(sug("gradient conic", "background: conic-gradient({CURSOR}, );", "snippet", "Conic gradient background"));
        CSS_SUGGESTIONS.add(sug("glassmorphism", "background: rgba(255, 255, 255, 0.1);\nbackdrop-filter: blur(10px);\n-webkit-backdrop-filter: blur(10px);\nborder: 1px solid rgba(255,255,255,0.2);", "snippet", "Glassmorphism frosted glass effect"));
        CSS_SUGGESTIONS.add(sug("dark theme vars", "--bg: #1a1a2e;\n--text: #eaeaea;\n--accent: #6c5ce7;", "snippet", "Dark theme CSS variables"));
        CSS_SUGGESTIONS.add(sug("light theme vars", "--bg: #ffffff;\n--text: #1a1a2e;\n--accent: #6c5ce7;", "snippet", "Light theme CSS variables"));

        // Pseudo-classes and pseudo-elements
        String[] cssPseudo = {
            ":hover", ":active", ":focus", ":visited", ":link", ":target",
            ":first-child", ":last-child", ":nth-child()", ":nth-of-type()",
            ":first-of-type", ":last-of-type", ":only-child", ":only-of-type",
            ":empty", ":not()", ":has()", ":is()", ":where()",
            ":enabled", ":disabled", ":checked", ":required", ":optional",
            ":valid", ":invalid", ":in-range", ":out-of-range",
            ":read-only", ":read-write", ":placeholder-shown",
            ":focus-within", ":focus-visible",
            ":root", ":scope", ":host",
            "::before", "::after", "::first-letter", "::first-line",
            "::selection", "::placeholder", "::marker", "::backdrop",
            "::cue", "::part()", "::slotted()",
        };
        for (String pseudo : cssPseudo) {
            String insert = pseudo.endsWith("()") ? pseudo.replace("()", "({CURSOR})") : pseudo;
            CSS_SUGGESTIONS.add(sug(pseudo, insert + " {\n    {CURSOR}\n}", "value", "CSS pseudo-class/element selector"));
        }

    }

    // ========================================================================
    //   JAVASCRIPT / TYPESCRIPT
    // ========================================================================
    private static void loadJavaScript() {
        String[] jsKeywords = {
            "var", "let", "const", "function", "return", "if", "else", "for", "while", "do",
            "switch", "case", "break", "continue", "class", "extends", "super", "new", "this", "typeof",
            "try", "catch", "finally", "throw", "async", "await", "import", "export", "default", "from",
            "null", "undefined", "true", "false", "NaN", "Infinity",
            "in", "of", "instanceof", "delete", "void", "yield",
            "debugger", "with",
        };
        for (String kw : jsKeywords) {
            JS_SUGGESTIONS.add(new Suggestion(kw, kw, "keyword", "JavaScript keyword"));
        }

        String[][] jsGlobals = {
            {"console.log", "console.log({CURSOR})", "keyword"},
            {"console.error", "console.error({CURSOR})", "keyword"},
            {"console.warn", "console.warn({CURSOR})", "keyword"},
            {"console.info", "console.info({CURSOR})", "keyword"},
            {"console.table", "console.table({CURSOR})", "keyword"},
            {"console.time", "console.time('{CURSOR}')", "keyword"},
            {"console.timeEnd", "console.timeEnd('{CURSOR}')", "keyword"},
            {"console.group", "console.group('{CURSOR}')", "keyword"},
            {"console.groupEnd", "console.groupEnd()", "keyword"},
            {"document.querySelector", "document.querySelector('{CURSOR}')", "keyword"},
            {"document.querySelectorAll", "document.querySelectorAll('{CURSOR}')", "keyword"},
            {"document.getElementById", "document.getElementById('{CURSOR}')", "keyword"},
            {"document.getElementsByClassName", "document.getElementsByClassName('{CURSOR}')", "keyword"},
            {"document.getElementsByTagName", "document.getElementsByTagName('{CURSOR}')", "keyword"},
            {"document.createElement", "document.createElement('{CURSOR}')", "keyword"},
            {"document.createDocumentFragment", "document.createDocumentFragment()", "keyword"},
            {"document.body", "document.body", "keyword"},
            {"document.head", "document.head", "keyword"},
            {"document.title", "document.title", "keyword"},
            {"document.URL", "document.URL", "keyword"},
            {"document.domain", "document.domain", "keyword"},
            {"document.referrer", "document.referrer", "keyword"},
            {"document.cookie", "document.cookie", "keyword"},
            {"document.forms", "document.forms", "keyword"},
            {"document.images", "document.images", "keyword"},
            {"document.links", "document.links", "keyword"},
            {"window.innerWidth", "window.innerWidth", "keyword"},
            {"window.innerHeight", "window.innerHeight", "keyword"},
            {"window.outerWidth", "window.outerWidth", "keyword"},
            {"window.outerHeight", "window.outerHeight", "keyword"},
            {"window.screenX", "window.screenX", "keyword"},
            {"window.screenY", "window.screenY", "keyword"},
            {"window.scrollX", "window.scrollX", "keyword"},
            {"window.scrollY", "window.scrollY", "keyword"},
            {"window.pageXOffset", "window.pageXOffset", "keyword"},
            {"window.pageYOffset", "window.pageYOffset", "keyword"},
            {"window.location", "window.location", "keyword"},
            {"window.location.href", "window.location.href", "keyword"},
            {"window.location.hostname", "window.location.hostname", "keyword"},
            {"window.location.pathname", "window.location.pathname", "keyword"},
            {"window.location.search", "window.location.search", "keyword"},
            {"window.location.hash", "window.location.hash", "keyword"},
            {"window.location.reload", "window.location.reload()", "keyword"},
            {"window.location.assign", "window.location.assign('{CURSOR}')", "keyword"},
            {"window.location.replace", "window.location.replace('{CURSOR}')", "keyword"},
            {"window.open", "window.open('{CURSOR}')", "keyword"},
            {"window.close", "window.close()", "keyword"},
            {"window.focus", "window.focus()", "keyword"},
            {"window.blur", "window.blur()", "keyword"},
            {"window.print", "window.print()", "keyword"},
            {"window.alert", "alert('{CURSOR}')", "keyword"},
            {"window.confirm", "confirm('{CURSOR}')", "keyword"},
            {"window.prompt", "prompt('{CURSOR}')", "keyword"},
            {"window.addEventListener", "window.addEventListener('{CURSOR}', () => {})", "keyword"},
            {"window.removeEventListener", "window.removeEventListener('{CURSOR}', () => {})", "keyword"},
            {"window.dispatchEvent", "window.dispatchEvent(new Event('{CURSOR}'))", "keyword"},
            {"window.scrollTo", "window.scrollTo({ top: {CURSOR}, behavior: 'smooth' })", "keyword"},
            {"window.scrollBy", "window.scrollBy({ top: {CURSOR}, behavior: 'smooth' })", "keyword"},
            {"window.requestAnimationFrame", "window.requestAnimationFrame(() => {\n    {CURSOR}\n})", "keyword"},
            {"window.setInterval", "setInterval(() => {\n    {CURSOR}\n}, 1000)", "keyword"},
            {"window.setTimeout", "setTimeout(() => {\n    {CURSOR}\n}, 1000)", "keyword"},
            {"window.clearInterval", "clearInterval({CURSOR})", "keyword"},
            {"window.clearTimeout", "clearTimeout({CURSOR})", "keyword"},
            {"window.history.back", "window.history.back()", "keyword"},
            {"window.history.forward", "window.history.forward()", "keyword"},
            {"window.history.go", "window.history.go(-1)", "keyword"},
            {"window.navigator.userAgent", "navigator.userAgent", "keyword"},
            {"window.navigator.platform", "navigator.platform", "keyword"},
            {"window.navigator.language", "navigator.language", "keyword"},
            {"window.navigator.onLine", "navigator.onLine", "keyword"},
            {"window.navigator.clipboard.writeText", "navigator.clipboard.writeText('{CURSOR}')", "keyword"},
            {"window.navigator.clipboard.readText", "navigator.clipboard.readText()", "keyword"},
            {"window.navigator.geolocation.getCurrentPosition", "navigator.geolocation.getCurrentPosition(pos => {\n    console.log(pos.coords);\n})", "keyword"},
            {"localStorage.getItem", "localStorage.getItem('{CURSOR}')", "keyword"},
            {"localStorage.setItem", "localStorage.setItem('{CURSOR}', '')", "keyword"},
            {"localStorage.removeItem", "localStorage.removeItem('{CURSOR}')", "keyword"},
            {"localStorage.clear", "localStorage.clear()", "keyword"},
            {"sessionStorage.getItem", "sessionStorage.getItem('{CURSOR}')", "keyword"},
            {"sessionStorage.setItem", "sessionStorage.setItem('{CURSOR}', '')", "keyword"},
            {"sessionStorage.removeItem", "sessionStorage.removeItem('{CURSOR}')", "keyword"},
            {"JSON.stringify", "JSON.stringify({CURSOR}, null, 2)", "keyword"},
            {"JSON.parse", "JSON.parse({CURSOR})", "keyword"},
            {"Math.random", "Math.random()", "keyword"},
            {"Math.floor", "Math.floor({CURSOR})", "keyword"},
            {"Math.ceil", "Math.ceil({CURSOR})", "keyword"},
            {"Math.round", "Math.round({CURSOR})", "keyword"},
            {"Math.max", "Math.max({CURSOR})", "keyword"},
            {"Math.min", "Math.min({CURSOR})", "keyword"},
            {"Math.abs", "Math.abs({CURSOR})", "keyword"},
            {"Math.pow", "Math.pow({CURSOR})", "keyword"},
            {"Math.sqrt", "Math.sqrt({CURSOR})", "keyword"},
            {"Math.trunc", "Math.trunc({CURSOR})", "keyword"},
            {"Math.sign", "Math.sign({CURSOR})", "keyword"},
            {"Math.PI", "Math.PI", "keyword"},
            {"Array.isArray", "Array.isArray({CURSOR})", "keyword"},
            {"Array.from", "Array.from({CURSOR})", "keyword"},
            {"Array.of", "Array.of({CURSOR})", "keyword"},
            {".push()", ".push({CURSOR})", "keyword"},
            {".pop()", ".pop()", "keyword"},
            {".shift()", ".shift()", "keyword"},
            {".unshift()", ".unshift({CURSOR})", "keyword"},
            {".map()", ".map((item, i) => { {CURSOR} })", "keyword"},
            {".filter()", ".filter(item => {CURSOR})", "keyword"},
            {".reduce()", ".reduce((acc, item) => {CURSOR}, [])", "keyword"},
            {".forEach()", ".forEach((item, i) => { {CURSOR} })", "keyword"},
            {".find()", ".find(item => {CURSOR})", "keyword"},
            {".findIndex()", ".findIndex(item => {CURSOR})", "keyword"},
            {".some()", ".some(item => {CURSOR})", "keyword"},
            {".every()", ".every(item => {CURSOR})", "keyword"},
            {".includes()", ".includes({CURSOR})", "keyword"},
            {".indexOf()", ".indexOf({CURSOR})", "keyword"},
            {".lastIndexOf()", ".lastIndexOf({CURSOR})", "keyword"},
            {".slice()", ".slice({CURSOR})", "keyword"},
            {".splice()", ".splice({CURSOR})", "keyword"},
            {".concat()", ".concat({CURSOR})", "keyword"},
            {".join()", ".join('{CURSOR}')", "keyword"},
            {".reverse()", ".reverse()", "keyword"},
            {".sort()", ".sort((a, b) => {CURSOR})", "keyword"},
            {".flat()", ".flat()", "keyword"},
            {".flatMap()", ".flatMap(item => {CURSOR})", "keyword"},
            {".fill()", ".fill({CURSOR})", "keyword"},
            {".copyWithin()", ".copyWithin({CURSOR})", "keyword"},
            {".toReversed()", ".toReversed()", "keyword"},
            {".toSorted()", ".toSorted((a, b) => {CURSOR})", "keyword"},
            {".toSpliced()", ".toSpliced({CURSOR})", "keyword"},
            {".with()", ".with({CURSOR})", "keyword"},
            {"Object.keys", "Object.keys({CURSOR})", "keyword"},
            {"Object.values", "Object.values({CURSOR})", "keyword"},
            {"Object.entries", "Object.entries({CURSOR})", "keyword"},
            {"Object.assign", "Object.assign({CURSOR})", "keyword"},
            {"Object.freeze", "Object.freeze({CURSOR})", "keyword"},
            {"Object.seal", "Object.seal({CURSOR})", "keyword"},
            {"Object.create", "Object.create({CURSOR})", "keyword"},
            {"Object.defineProperty", "Object.defineProperty({CURSOR})", "keyword"},
            {"Object.fromEntries", "Object.fromEntries({CURSOR})", "keyword"},
            {"Object.groupBy", "Object.groupBy({CURSOR})", "keyword"},
            {"String.prototype.trim", ".trim()", "keyword"},
            {"String.prototype.trimStart", ".trimStart()", "keyword"},
            {"String.prototype.trimEnd", ".trimEnd()", "keyword"},
            {"String.prototype.toLowerCase", ".toLowerCase()", "keyword"},
            {"String.prototype.toUpperCase", ".toUpperCase()", "keyword"},
            {"String.prototype.split", ".split('{CURSOR}')", "keyword"},
            {"String.prototype.replace", ".replace('{CURSOR}', '')", "keyword"},
            {"String.prototype.replaceAll", ".replaceAll('{CURSOR}', '')", "keyword"},
            {"String.prototype.includes", ".includes('{CURSOR}')", "keyword"},
            {"String.prototype.startsWith", ".startsWith('{CURSOR}')", "keyword"},
            {"String.prototype.endsWith", ".endsWith('{CURSOR}')", "keyword"},
            {"String.prototype.substring", ".substring({CURSOR})", "keyword"},
            {"String.prototype.slice", ".slice({CURSOR})", "keyword"},
            {"String.prototype.charAt", ".charAt({CURSOR})", "keyword"},
            {"String.prototype.charCodeAt", ".charCodeAt({CURSOR})", "keyword"},
            {"String.prototype.codePointAt", ".codePointAt({CURSOR})", "keyword"},
            {"String.prototype.concat", ".concat({CURSOR})", "keyword"},
            {"String.prototype.repeat", ".repeat({CURSOR})", "keyword"},
            {"String.prototype.padStart", ".padStart({CURSOR})", "keyword"},
            {"String.prototype.padEnd", ".padEnd({CURSOR})", "keyword"},
            {"String.prototype.match", ".match(/{CURSOR}/)", "keyword"},
            {"String.prototype.matchAll", ".matchAll(/{CURSOR}/g)", "keyword"},
            {"String.prototype.search", ".search(/{CURSOR}/)", "keyword"},
            {"String.prototype.localeCompare", ".localeCompare('{CURSOR}')", "keyword"},
            {"String.prototype.normalize", ".normalize()", "keyword"},
            {"String.raw", "String.raw`{CURSOR}`", "keyword"},
            {"Number.prototype.toFixed", ".toFixed({CURSOR})", "keyword"},
            {"Number.prototype.toPrecision", ".toPrecision({CURSOR})", "keyword"},
            {"Number.prototype.toExponential", ".toExponential({CURSOR})", "keyword"},
            {"Number.isNaN", "Number.isNaN({CURSOR})", "keyword"},
            {"Number.isFinite", "Number.isFinite({CURSOR})", "keyword"},
            {"Number.isInteger", "Number.isInteger({CURSOR})", "keyword"},
            {"Number.parseInt", "parseInt({CURSOR})", "keyword"},
            {"Number.parseFloat", "parseFloat({CURSOR})", "keyword"},
            {"Date.now", "Date.now()", "keyword"},
            {"new Date", "new Date()", "keyword"},
            {"Date.prototype.getFullYear", ".getFullYear()", "keyword"},
            {"Date.prototype.getMonth", ".getMonth()", "keyword"},
            {"Date.prototype.getDate", ".getDate()", "keyword"},
            {"Date.prototype.getDay", ".getDay()", "keyword"},
            {"Date.prototype.getHours", ".getHours()", "keyword"},
            {"Date.prototype.getMinutes", ".getMinutes()", "keyword"},
            {"Date.prototype.getSeconds", ".getSeconds()", "keyword"},
            {"Date.prototype.getMilliseconds", ".getMilliseconds()", "keyword"},
            {"Date.prototype.toISOString", ".toISOString()", "keyword"},
            {"Date.prototype.toLocaleDateString", ".toLocaleDateString()", "keyword"},
            {"Date.prototype.toLocaleTimeString", ".toLocaleTimeString()", "keyword"},
            {"Intl.DateTimeFormat", "new Intl.DateTimeFormat('{CURSOR}')", "keyword"},
            {"Intl.NumberFormat", "new Intl.NumberFormat('{CURSOR}')", "keyword"},
            {"encodeURI", "encodeURI('{CURSOR}')", "keyword"},
            {"encodeURIComponent", "encodeURIComponent('{CURSOR}')", "keyword"},
            {"decodeURI", "decodeURI('{CURSOR}')", "keyword"},
            {"decodeURIComponent", "decodeURIComponent('{CURSOR}')", "keyword"},
            {"fetch", "fetch('{CURSOR}')\n    .then(res => res.json())\n    .then(data => console.log(data))\n    .catch(err => console.error(err))", "keyword"},
            {"fetch POST", "fetch('{CURSOR}', {\n    method: 'POST',\n    headers: { 'Content-Type': 'application/json' },\n    body: JSON.stringify({})\n}).then(res => res.json())", "keyword"},
            {"fetch async", "async function fetchData() {\n    try {\n        const res = await fetch('{CURSOR}');\n        const data = await res.json();\n        console.log(data);\n    } catch (err) {\n        console.error(err);\n    }\n}", "keyword"},
            {"XMLHttpRequest", "const xhr = new XMLHttpRequest();\nxhr.open('GET', '{CURSOR}');\nxhr.onload = () => console.log(xhr.responseText);\nxhr.send();", "keyword"},
            {"addEventListener click", "element.addEventListener('click', () => {\n    {CURSOR}\n})", "keyword"},
            {"addEventListener input", "element.addEventListener('input', (e) => {\n    {CURSOR}\n})", "keyword"},
            {"addEventListener submit", "form.addEventListener('submit', (e) => {\n    e.preventDefault();\n    {CURSOR}\n})", "keyword"},
            {"classList.add", ".classList.add('{CURSOR}')", "keyword"},
            {"classList.remove", ".classList.remove('{CURSOR}')", "keyword"},
            {"classList.toggle", ".classList.toggle('{CURSOR}')", "keyword"},
            {"classList.contains", ".classList.contains('{CURSOR}')", "keyword"},
            {"classList.replace", ".classList.replace('old', '{CURSOR}')", "keyword"},
            {"innerText", ".innerText", "keyword"},
            {"innerHTML", ".innerHTML", "keyword"},
            {"textContent", ".textContent", "keyword"},
            {"outerHTML", ".outerHTML", "keyword"},
            {"value", ".value", "keyword"},
            {"checked", ".checked", "keyword"},
            {"disabled", ".disabled", "keyword"},
            {"style", ".style.{CURSOR}", "keyword"},
            {"dataset", ".dataset.{CURSOR}", "keyword"},
            {"attributes", ".attributes", "keyword"},
            {"parentNode", ".parentNode", "keyword"},
            {"parentElement", ".parentElement", "keyword"},
            {"children", ".children", "keyword"},
            {"childNodes", ".childNodes", "keyword"},
            {"firstChild", ".firstChild", "keyword"},
            {"firstElementChild", ".firstElementChild", "keyword"},
            {"lastChild", ".lastChild", "keyword"},
            {"lastElementChild", ".lastElementChild", "keyword"},
            {"nextSibling", ".nextSibling", "keyword"},
            {"nextElementSibling", ".nextElementSibling", "keyword"},
            {"previousSibling", ".previousSibling", "keyword"},
            {"previousElementSibling", ".previousElementSibling", "keyword"},
            {"Promise", "new Promise((resolve, reject) => {\n    {CURSOR}\n})", "keyword"},
            {"Promise.all", "Promise.all([{CURSOR}]).then(([a, b]) => {\n    \n})", "keyword"},
            {"Promise.allSettled", "Promise.allSettled([{CURSOR}]).then(results => {\n    const succeeded = results.filter(r => r.status === 'fulfilled');\n})", "keyword"},
            {"Promise.race", "Promise.race([{CURSOR}]).then(first => {})", "keyword"},
            {"Promise.any", "Promise.any([{CURSOR}]).then(first => {})", "keyword"},
            {"Promise.withResolvers", "const { promise, resolve, reject } = Promise.withResolvers();", "keyword"},
            {"fetch DELETE", "fetch('{CURSOR}', { method: 'DELETE' })", "keyword"},
            {"fetch PUT", "fetch('{CURSOR}', {\n    method: 'PUT',\n    headers: { 'Content-Type': 'application/json' },\n    body: JSON.stringify({})\n})", "keyword"},
            {"FormData.append", ".append('{CURSOR}', value)", "keyword"},
            {"FormData.get", ".get('{CURSOR}')", "keyword"},
            {"FormData.getAll", ".getAll('{CURSOR}')", "keyword"},
            {"FormData.has", ".has('{CURSOR}')", "keyword"},
            {"FormData.delete", ".delete('{CURSOR}')", "keyword"},
            {"new FormData", "const formData = new FormData();\nformData.append('{CURSOR}', '');", "keyword"},
            {"new URL", "new URL('{CURSOR}', window.location.origin)", "keyword"},
            {"URL.searchParams.get", ".searchParams.get('{CURSOR}')", "keyword"},
            {"URL.searchParams.set", ".searchParams.set('{CURSOR}', '')", "keyword"},
            {"URL.searchParams.append", ".searchParams.append('{CURSOR}', '')", "keyword"},
            {"URL.searchParams.delete", ".searchParams.delete('{CURSOR}')", "keyword"},
            {"URL.searchParams.forEach", ".searchParams.forEach((value, key) => { {CURSOR} })", "keyword"},
            {"URL.searchParams.has", ".searchParams.has('{CURSOR}')", "keyword"},
            {"URL.createObjectURL", "URL.createObjectURL({CURSOR})", "keyword"},
            {"URL.revokeObjectURL", "URL.revokeObjectURL({CURSOR})", "keyword"},
            {"Blob", "new Blob([{CURSOR}], { type: 'text/plain' })", "keyword"},
            {"FileReader.readAsText", "const reader = new FileReader();\nreader.onload = e => console.log(e.target.result);\nreader.readAsText({CURSOR});", "keyword"},
            {"FileReader.readAsDataURL", "const reader = new FileReader();\nreader.onload = e => { {CURSOR} };\nreader.readAsDataURL(file);", "keyword"},
            {"IntersectionObserver", "const observer = new IntersectionObserver((entries) => {\n    entries.forEach(entry => {\n        if (entry.isIntersecting) {\n            {CURSOR}\n        }\n    });\n});\nobserver.observe(element);", "keyword"},
            {"MutationObserver", "const observer = new MutationObserver((mutations) => {\n    mutations.forEach(m => { {CURSOR} });\n});\nobserver.observe(target, { childList: true, subtree: true });", "keyword"},
            {"ResizeObserver", "const observer = new ResizeObserver(entries => {\n    for (const entry of entries) {\n        {CURSOR}\n    }\n});\nobserver.observe(element);", "keyword"},
            {"CustomEvent", "element.dispatchEvent(new CustomEvent('{CURSOR}', { detail: {} }))", "keyword"},
            {"canvas 2d", "const canvas = document.getElementById('{CURSOR}');\nconst ctx = canvas.getContext('2d');", "keyword"},
            {"canvas fillRect", "ctx.fillStyle = '#{CURSOR}';\nctx.fillRect(x, y, w, h);", "keyword"},
            {"canvas arc", "ctx.beginPath();\nctx.arc(x, y, r, 0, Math.PI * 2);\nctx.fill();", "keyword"},
            {"canvas text", "ctx.font = '16px sans-serif';\nctx.fillStyle = '#{CURSOR}';\nctx.fillText('Hello', x, y);", "keyword"},
            {"canvas image", "const img = new Image();\nimg.onload = () => ctx.drawImage(img, 0, 0);\nimg.src = '{CURSOR}';", "keyword"},
            {"requestAnimationFrame loop", "function loop() {\n    {CURSOR}\n    requestAnimationFrame(loop);\n}\nloop();", "keyword"},
            {"AbortController", "const controller = new AbortController();\nconst signal = controller.signal;\n\nfetch('{CURSOR}', { signal })\n    .then(res => res.json())\n    .catch(err => {\n        if (err.name === 'AbortError') {\n            console.log('Fetch aborted');\n        }\n    });\n\n// controller.abort();", "keyword"},
            {"WebSocket", "const ws = new WebSocket('ws://{CURSOR}');\nws.onopen = () => ws.send('Hello');\nws.onmessage = (e) => console.log(e.data);\nws.onclose = () => console.log('Closed');", "keyword"},
            {"import dynamic", "const module = await import('{CURSOR}');", "keyword"},
            {"structuredClone", "const clone = structuredClone({CURSOR});", "keyword"},
            {"EventSource (SSE)", "const es = new EventSource('/{CURSOR}');\nes.onmessage = (e) => {\n    const data = JSON.parse(e.data);\n    console.log(data);\n};", "keyword"},
            {"BroadcastChannel", "const channel = new BroadcastChannel('{CURSOR}');\nchannel.onmessage = (e) => console.log(e.data);\nchannel.postMessage({});", "keyword"},
            {"ServiceWorker register", "if ('serviceWorker' in navigator) {\n    navigator.serviceWorker.register('/{CURSOR}.js')\n        .then(reg => console.log('Registered', reg.scope))\n        .catch(err => console.error(err));\n}", "keyword"},
            {"Notification request", "Notification.requestPermission().then(perm => {\n    if (perm === 'granted') {\n        new Notification('{CURSOR}');\n    }\n});", "keyword"},
            {"Performance.mark", "performance.mark('{CURSOR}');", "keyword"},
            {"Performance.measure", "performance.measure('{CURSOR}');", "keyword"},
            {"structuredClone", "structuredClone({CURSOR})", "keyword"},
            {"atob", "atob('{CURSOR}')", "keyword"},
            {"btoa", "btoa('{CURSOR}')", "keyword"},
            {"crypto.randomUUID", "crypto.randomUUID()", "keyword"},
            {"crypto.subtle.digest", "crypto.subtle.digest('SHA-256', new TextEncoder().encode('{CURSOR}'))", "keyword"},
            {"closest", ".closest('{CURSOR}')", "keyword"},
            {"matches", ".matches('{CURSOR}')", "keyword"},
            {"contains", ".contains({CURSOR})", "keyword"},
            {"compareDocumentPosition", ".compareDocumentPosition({CURSOR})", "keyword"},
            {"insertAdjacentHTML", ".insertAdjacentHTML('{CURSOR}', '')", "keyword"},
            {"insertAdjacentElement", ".insertAdjacentElement('{CURSOR}', element)", "keyword"},
            {"insertAdjacentText", ".insertAdjacentText('{CURSOR}', '')", "keyword"},
            {"scrollIntoView", ".scrollIntoView({ behavior: 'smooth' })", "keyword"},
            {"scrollIntoViewIfNeeded", ".scrollIntoViewIfNeeded()", "keyword"},
            {"focus", ".focus()", "keyword"},
            {"blur", ".blur()", "keyword"},
            {"click", ".click()", "keyword"},
            {"remove", ".remove()", "keyword"},
            {"replaceWith", ".replaceWith({CURSOR})", "keyword"},
            {"before", ".before({CURSOR})", "keyword"},
            {"after", ".after({CURSOR})", "keyword"},
            {"prepend", ".prepend({CURSOR})", "keyword"},
            {"append", ".append({CURSOR})", "keyword"},
            {"cloneNode", ".cloneNode(true)", "keyword"},
            {"isConnected", ".isConnected", "keyword"},
            {"MutationObserver", "new MutationObserver(mutations => {\n    {CURSOR}\n}).observe(target, { childList: true })", "keyword"},
            {"IntersectionObserver", "new IntersectionObserver(entries => {\n    entries.forEach(entry => {\n        if (entry.isIntersecting) { {CURSOR} }\n    });\n}).observe(target)", "keyword"},
            {"ResizeObserver", "new ResizeObserver(entries => {\n    {CURSOR}\n}).observe(target)", "keyword"},
            {"PerformanceObserver", "new PerformanceObserver(list => {\n    {CURSOR}\n}).observe({ entryTypes: ['measure'] })", "keyword"},
            {"CustomEvent", "new CustomEvent('{CURSOR}', { detail: {} })", "keyword"},
            {"new Map", "new Map()", "keyword"},
            {"new Set", "new Set()", "keyword"},
            {"new WeakMap", "new WeakMap()", "keyword"},
            {"new WeakSet", "new WeakSet()", "keyword"},
            {"new Promise", "new Promise((resolve, reject) => {\n    {CURSOR}\n})", "keyword"},
            {"new Proxy", "new Proxy(target, {\n    get(obj, prop) { {CURSOR} }\n})", "keyword"},
            {"Symbol.iterator", "Symbol.iterator", "keyword"},
            {"Symbol.toStringTag", "Symbol.toStringTag", "keyword"},
            {"Reflect.get", "Reflect.get({CURSOR})", "keyword"},
            {"Reflect.set", "Reflect.set({CURSOR})", "keyword"},
            {"Reflect.has", "Reflect.has({CURSOR})", "keyword"},
            {"Reflect.defineProperty", "Reflect.defineProperty({CURSOR})", "keyword"},
            {"Reflect.deleteProperty", "Reflect.deleteProperty({CURSOR})", "keyword"},
            {"Error", "new Error('{CURSOR}')", "keyword"},
            {"TypeError", "new TypeError('{CURSOR}')", "keyword"},
            {"RangeError", "new RangeError('{CURSOR}')", "keyword"},
            {"SyntaxError", "new SyntaxError('{CURSOR}')", "keyword"},
            {"atob", "atob('{CURSOR}')", "keyword"},
            {"btoa", "btoa('{CURSOR}')", "keyword"},
        };
        for (String[] g : jsGlobals) {
            JS_SUGGESTIONS.add(sug(g[0], g[1], g[2], "JavaScript global / built-in API"));
        }

        JS_SUGGESTIONS.add(sug("function declaration", "function {CURSOR}() {\n    \n}", "snippet", "JavaScript function declaration"));
        JS_SUGGESTIONS.add(sug("arrow function", "const {CURSOR} = () => {\n    \n};", "snippet", "ES6 arrow function expression"));
        JS_SUGGESTIONS.add(sug("arrow function shorthand", "const {CURSOR} = () => {CURSOR}", "snippet", "Concise arrow function shorthand"));
        JS_SUGGESTIONS.add(sug("for loop", "for (let i = 0; i < {CURSOR}.length; i++) {\n    \n}", "snippet", "Traditional for loop"));
        JS_SUGGESTIONS.add(sug("for...of", "for (const item of {CURSOR}) {\n    \n}", "snippet", "ES6 for...of loop over iterable"));
        JS_SUGGESTIONS.add(sug("for...in", "for (const key in {CURSOR}) {\n    \n}", "snippet", "for...in loop over enumerable properties"));
        JS_SUGGESTIONS.add(sug("if statement", "if ({CURSOR}) {\n    \n}", "snippet", "Conditional if statement"));
        JS_SUGGESTIONS.add(sug("if/else", "if ({CURSOR}) {\n    \n} else {\n    \n}", "snippet", "If/else conditional statement"));
        JS_SUGGESTIONS.add(sug("switch statement", "switch ({CURSOR}) {\n    case '':\n        break;\n    default:\n        break;\n}", "snippet", "Switch case statement"));
        JS_SUGGESTIONS.add(sug("try/catch", "try {\n    {CURSOR}\n} catch (err) {\n    console.error(err);\n}", "snippet", "Try/catch error handling"));
        JS_SUGGESTIONS.add(sug("try/catch/finally", "try {\n    {CURSOR}\n} catch (err) {\n    console.error(err);\n} finally {\n    \n}", "snippet", "Try/catch/finally error handling"));
        JS_SUGGESTIONS.add(sug("class skeleton", "class {CURSOR} {\n    constructor() {\n        \n    }\n}", "snippet", "ES6 class skeleton"));
        JS_SUGGESTIONS.add(sug("class with methods", "class {CURSOR} {\n    constructor() {\n        \n    }\n    \n    method() {\n        \n    }\n}", "snippet", "ES6 class with methods"));
        JS_SUGGESTIONS.add(sug("class extends", "class {CURSOR} extends Parent {\n    constructor() {\n        super();\n        \n    }\n}", "snippet", "ES6 class inheritance"));
        JS_SUGGESTIONS.add(sug("async function", "async function {CURSOR}() {\n    try {\n        \n    } catch (err) {\n        console.error(err);\n    }\n}", "snippet", "Async function with error handling"));
        JS_SUGGESTIONS.add(sug("Promise", "new Promise((resolve, reject) => {\n    {CURSOR}\n})", "snippet", "Promise constructor pattern"));
        JS_SUGGESTIONS.add(sug("Promise.all", "Promise.all([{CURSOR}]).then(([a, b]) => {\n    \n})", "snippet", "Promise.all concurrency pattern"));
        JS_SUGGESTIONS.add(sug("Promise.race", "Promise.race([{CURSOR}]).then(result => {\n    \n})", "snippet", "Promise.race first-result pattern"));
        JS_SUGGESTIONS.add(sug("async/await fetch", "async function fetchData() {\n    try {\n        const res = await fetch('{CURSOR}');\n        const data = await res.json();\n        console.log(data);\n    } catch (err) {\n        console.error(err);\n    }\n}", "snippet", "Async/await fetch API pattern"));
        JS_SUGGESTIONS.add(sug("import named", "import { {CURSOR} } from './module';", "snippet", "ES6 named import"));
        JS_SUGGESTIONS.add(sug("import default", "import {CURSOR} from './module';", "snippet", "ES6 default import"));
        JS_SUGGESTIONS.add(sug("import * as", "import * as {CURSOR} from './module';", "snippet", "ES6 namespace import"));
        JS_SUGGESTIONS.add(sug("export default", "export default {CURSOR};", "snippet", "ES6 default export"));
        JS_SUGGESTIONS.add(sug("export named", "export function {CURSOR}() {\n    \n}", "snippet", "ES6 named function export"));
        JS_SUGGESTIONS.add(sug("export const", "export const {CURSOR} = () => {\n    \n};", "snippet", "ES6 constant export"));
        JS_SUGGESTIONS.add(sug("module pattern", "const module = (() => {\n    'use strict';\n    \n    return { {CURSOR} };\n})();", "snippet", "IIFE module pattern"));
        JS_SUGGESTIONS.add(sug("destructure object", "const { {CURSOR} } = {};", "snippet", "Object destructuring syntax"));
        JS_SUGGESTIONS.add(sug("destructure array", "const [{CURSOR}] = [];", "snippet", "Array destructuring syntax"));
        JS_SUGGESTIONS.add(sug("spread operator", "...{CURSOR}", "snippet", "Spread operator syntax"));
        JS_SUGGESTIONS.add(sug("rest parameters", "...{CURSOR}", "snippet", "Rest parameters syntax"));
        JS_SUGGESTIONS.add(sug("template literal", "`${ {CURSOR} }`", "snippet", "Template literal string interpolation"));
        JS_SUGGESTIONS.add(sug("ternary", "{CURSOR} ?  : ", "snippet", "Ternary conditional operator"));
        JS_SUGGESTIONS.add(sug("optional chaining", "{CURSOR}?.", "snippet", "Optional chaining operator"));
        JS_SUGGESTIONS.add(sug("nullish coalescing", "{CURSOR} ?? ", "snippet", "Nullish coalescing operator"));
        JS_SUGGESTIONS.add(sug("logical AND assignment", "&&=", "snippet", "Logical AND assignment operator"));
        JS_SUGGESTIONS.add(sug("logical OR assignment", "||=", "snippet", "Logical OR assignment operator"));
        JS_SUGGESTIONS.add(sug("nullish assignment", "??=", "snippet", "Nullish assignment operator"));
        JS_SUGGESTIONS.add(sug("generator function", "function* {CURSOR}() {\n    yield \n}", "snippet", "Generator function declaration"));
        JS_SUGGESTIONS.add(sug("for await...of", "for await (const item of {CURSOR}) {\n    \n}", "snippet", "Async iteration with for await...of"));
        JS_SUGGESTIONS.add(sug("WebSocket", "const ws = new WebSocket('wss://{CURSOR}');\nws.onopen = () => console.log('connected');\nws.onmessage = (e) => console.log(e.data);", "snippet", "WebSocket connection pattern"));
        JS_SUGGESTIONS.add(sug("Service Worker register", "navigator.serviceWorker.register('/sw.js')", "snippet", "Service Worker registration"));
        JS_SUGGESTIONS.add(sug("indexedDB open", "const request = indexedDB.open('{CURSOR}', 1);\nrequest.onupgradeneeded = (e) => {\n    const db = e.target.result;\n    db.createObjectStore('store', { keyPath: 'id' });\n};", "snippet", "IndexedDB database open pattern"));

        // ---- React / JSX / Hooks ----
        String[][] reactHooks = {
            {"useState", "const [{CURSOR}, set{CURSOR}] = useState(initialValue);", "snippet"},
            {"useEffect", "useEffect(() => {\n    {CURSOR}\n    return () => {};\n}, []);", "snippet"},
            {"useRef", "const {CURSOR} = useRef(null);", "snippet"},
            {"useMemo", "const {CURSOR} = useMemo(() => {\n    return value;\n}, []);", "snippet"},
            {"useCallback", "const {CURSOR} = useCallback(() => {\n    \n}, []);", "snippet"},
            {"useContext", "const {CURSOR} = useContext(MyContext);", "snippet"},
            {"useReducer", "const [state, dispatch] = useReducer(reducer, initialState);", "snippet"},
            {"useLayoutEffect", "useLayoutEffect(() => {\n    {CURSOR}\n}, []);", "snippet"},
            {"useImperativeHandle", "useImperativeHandle(ref, () => ({\n    {CURSOR}\n}));", "snippet"},
            {"useDebugValue", "useDebugValue({CURSOR});", "snippet"},
            {"useTransition", "const [isPending, startTransition] = useTransition();", "snippet"},
            {"useDeferredValue", "const deferredValue = useDeferredValue({CURSOR});", "snippet"},
            {"useId", "const id = useId();", "snippet"},
            {"useSyncExternalStore", "const snapshot = useSyncExternalStore(subscribe, getSnapshot);", "snippet"},
            {"createContext", "const MyContext = createContext(defaultValue);", "snippet"},
            {"createRef", "const ref = createRef();", "snippet"},
            {"forwardRef", "const Component = forwardRef((props, ref) => (\n    <div ref={ref}>{CURSOR}</div>\n));", "snippet"},
            {"memo", "const MemoComponent = memo(({ prop }) => {\n    return <div>{CURSOR}</div>;\n});", "snippet"},
            {"lazy", "const LazyComponent = lazy(() => import('./{CURSOR}'));", "snippet"},
            {"Suspense", "<Suspense fallback={<div>Loading...</div>}>\n    <{CURSOR} />\n</Suspense>", "snippet"},
            {"Fragment", "<>{CURSOR}</>", "snippet"},
            {"StrictMode", "<StrictMode>\n    <{CURSOR} />\n</StrictMode>", "snippet"},
        };
        for (String[] rh : reactHooks) {
            JS_SUGGESTIONS.add(sug(rh[0], rh[1], rh[2], "React hook / API"));
        }

        // ---- JSX snippets ----
        JS_SUGGESTIONS.add(sug("JSX component", "function {CURSOR}({ prop }) {\n    return (\n        <div>\n            \n        </div>\n    );\n}", "snippet", "React function component with props"));
        JS_SUGGESTIONS.add(sug("JSX arrow component", "const {CURSOR} = ({ prop }) => {\n    return (\n        <div>\n            \n        </div>\n    );\n};", "snippet", "Arrow function React component"));
        JS_SUGGESTIONS.add(sug("JSX conditional", "{condition && <{CURSOR} />}", "snippet", "JSX conditional rendering (&&)"));
        JS_SUGGESTIONS.add(sug("JSX ternary", "{condition ? <{CURSOR} /> : <Other />}", "snippet", "JSX ternary conditional"));
        JS_SUGGESTIONS.add(sug("JSX map list", "{items.map((item, i) => (\n    <div key={i}>{item}</div>\n))}", "snippet", "JSX map over array to render list"));
        JS_SUGGESTIONS.add(sug("JSX className", "<div className=\"{CURSOR}\">", "snippet", "JSX className attribute"));
        JS_SUGGESTIONS.add(sug("JSX onClick", "<button onClick={() => { {CURSOR} }}>Click</button>", "snippet", "JSX click event handler"));
        JS_SUGGESTIONS.add(sug("JSX onChange", "<input onChange={(e) => { {CURSOR} }} />", "snippet", "JSX input change handler"));
        JS_SUGGESTIONS.add(sug("JSX style object", "<div style={{ color: '{CURSOR}' }}>", "snippet", "JSX inline style object"));
        JS_SUGGESTIONS.add(sug("JSX ref callback", "<div ref={(el) => { {CURSOR} }} />", "snippet", "JSX ref callback pattern"));

        // ---- TypeScript ----
        String[][] tsTypes = {
            {"interface", "interface {CURSOR} {\n    \n}", "snippet"},
            {"type", "type {CURSOR} = {\n    \n};", "snippet"},
            {"enum", "enum {CURSOR} {\n    Value1,\n    Value2,\n}", "snippet"},
            {"extends interface", "interface {CURSOR} extends Base {\n    \n}", "snippet"},
            {"implements interface", "class {CURSOR} implements Interface {\n    \n}", "snippet"},
            {"generic fn", "function {CURSOR}<T>(arg: T): T {\n    return arg;\n}", "snippet"},
            {"Pick", "Pick<{CURSOR}, 'key'>", "snippet"},
            {"Omit", "Omit<{CURSOR}, 'key'>", "snippet"},
            {"Partial", "Partial<{CURSOR}>", "snippet"},
            {"Required", "Required<{CURSOR}>", "snippet"},
            {"Record", "Record<string, {CURSOR}>", "snippet"},
            {"Readonly", "Readonly<{CURSOR}>", "snippet"},
            {"Exclude", "Exclude<{CURSOR}, 'a'>", "snippet"},
            {"Extract", "Extract<{CURSOR}, 'a'>", "snippet"},
            {"NonNullable", "NonNullable<{CURSOR}>", "snippet"},
            {"ReturnType", "ReturnType<typeof {CURSOR}>", "snippet"},
            {"Parameters", "Parameters<typeof {CURSOR}>", "snippet"},
            {"React.FC", "const {CURSOR}: React.FC<Props> = ({ prop }) => {\n    return <div></div>;\n};", "snippet"},
            {"React.ReactNode", "{CURSOR}: React.ReactNode", "snippet"},
            {"React.CSSProperties", "{CURSOR}: React.CSSProperties", "snippet"},
            {"as const", "as const", "snippet"},
            {"satisfies", " satisfies {CURSOR}", "snippet"},
            {"as type", " as {CURSOR}", "snippet"},
        };
        for (String[] ts : tsTypes) {
            JS_SUGGESTIONS.add(sug(ts[0], ts[1], ts[2], "TypeScript type utility"));
        }

        // ---- Next.js ----
        JS_SUGGESTIONS.add(sug("Next.js useRouter", "const router = useRouter();", "snippet", "Next.js useRouter hook"));
        JS_SUGGESTIONS.add(sug("Next.js usePathname", "const pathname = usePathname();", "snippet", "Next.js App Router usePathname hook"));
        JS_SUGGESTIONS.add(sug("Next.js Head", "<Head>\n    <title>{CURSOR}</title>\n</Head>", "snippet", "Next.js Head component"));
        JS_SUGGESTIONS.add(sug("Next.js Link", "<Link href=\"{CURSOR}\">", "snippet", "Next.js Link component"));
        JS_SUGGESTIONS.add(sug("Next.js Image", "<Image src=\"{CURSOR}\" alt=\"\" width={600} height={400} />", "snippet", "Next.js Image component"));
        JS_SUGGESTIONS.add(sug("Next.js getStaticProps", "export async function getStaticProps() {\n    const data = await fetch('{CURSOR}').then(r => r.json());\n    return { props: { data } };\n}", "snippet", "Next.js static props generation"));
        JS_SUGGESTIONS.add(sug("Next.js getServerSideProps", "export async function getServerSideProps(context) {\n    const data = await fetch('{CURSOR}').then(r => r.json());\n    return { props: { data } };\n}", "snippet", "Next.js server-side props"));
        JS_SUGGESTIONS.add(sug("Next.js getStaticPaths", "export async function getStaticPaths() {\n    return { paths: [{ params: { id: '{CURSOR}' } }], fallback: false };\n}", "snippet", "Next.js static paths for SSG"));
        JS_SUGGESTIONS.add(sug("Next.js App Layout", "export default function RootLayout({ children }) {\n    return (\n        <html lang=\"en\">\n            <body>{children}</body>\n        </html>\n    );\n}", "snippet", "Next.js App Router layout"));
        JS_SUGGESTIONS.add(sug("Next.js App Page", "export default function {CURSOR}() {\n    return <div></div>;\n}", "snippet", "Next.js App Router page component"));
        JS_SUGGESTIONS.add(sug("Next.js useSearchParams", "const searchParams = useSearchParams();\nconst value = searchParams.get('{CURSOR}');", "snippet", "Next.js App Router search params"));

        // ---- Solid.js ----
        JS_SUGGESTIONS.add(sug("Solid createSignal", "const [{CURSOR}, set{CURSOR}] = createSignal(initialValue);", "snippet", "Solid.js signal creation"));
        JS_SUGGESTIONS.add(sug("Solid createEffect", "createEffect(() => {\n    {CURSOR}\n});", "snippet", "Solid.js effect tracking"));
        JS_SUGGESTIONS.add(sug("Solid createMemo", "const {CURSOR} = createMemo(() => {\n    return value;\n});", "snippet", "Solid.js memoized value"));
        JS_SUGGESTIONS.add(sug("Solid createResource", "const [{CURSOR}, { mutate, refetch }] = createResource(source, fetcher);", "snippet", "Solid.js async resource"));
        JS_SUGGESTIONS.add(sug("Solid createStore", "const [store, setStore] = createStore({\n    {CURSOR}: value\n});", "snippet", "Solid.js store"));
        JS_SUGGESTIONS.add(sug("Solid For", "<For each={items}>{(item, i) => <div key={i()}>{item}</div>}</For>", "snippet", "Solid.js For component"));
        JS_SUGGESTIONS.add(sug("Solid Show", "<Show when={condition()} fallback={<div>Loading</div>}>\n    <div>{CURSOR}</div>\n</Show>", "snippet", "Solid.js Show component"));
        JS_SUGGESTIONS.add(sug("Solid Switch/Match", "<Switch>\n    <Match when={condition()}>\n        <div>{CURSOR}</div>\n    </Match>\n</Switch>", "snippet", "Solid.js Switch/Match control flow"));
        JS_SUGGESTIONS.add(sug("Solid ErrorBoundary", "<ErrorBoundary fallback={<div>Error</div>}>\n    <{CURSOR} />\n</ErrorBoundary>", "snippet", "Solid.js error boundary"));

        // ---- Lit ----
        JS_SUGGESTIONS.add(sug("Lit html", "html`<div>${CURSOR}</div>`", "snippet", "Lit html template tag"));
        JS_SUGGESTIONS.add(sug("Lit css", "css`\n    :host { display: block; }\n    .{CURSOR} { color: inherit; }\n`", "snippet", "Lit css template tag"));
        JS_SUGGESTIONS.add(sug("Lit LitElement", "class {CURSOR} extends LitElement {\n    render() {\n        return html`<div></div>`;\n    }\n}\ncustomElements.define('my-element', {CURSOR});", "snippet", "Lit LitElement component skeleton"));
        JS_SUGGESTIONS.add(sug("Lit @property", "@property({ type: String }) {CURSOR} = '';", "snippet", "Lit property decorator"));
        JS_SUGGESTIONS.add(sug("Lit @state", "@state() {CURSOR} = false;", "snippet", "Lit state decorator"));
        JS_SUGGESTIONS.add(sug("Lit @customElement", "@customElement('{CURSOR}')\nclass MyElement extends LitElement {}", "snippet", "Lit custom element decorator"));

        // ---- Vue (Composition API / Nuxt) ----
        JS_SUGGESTIONS.add(sug("Vue ref", "const {CURSOR} = ref(initialValue);", "snippet", "Vue Composition API ref"));
        JS_SUGGESTIONS.add(sug("Vue reactive", "const state = reactive({\n    {CURSOR}: value\n});", "snippet", "Vue Composition API reactive"));
        JS_SUGGESTIONS.add(sug("Vue computed", "const {CURSOR} = computed(() => {\n    return state.value;\n});", "snippet", "Vue Composition API computed"));
        JS_SUGGESTIONS.add(sug("Vue watch", "watch({CURSOR}, (newVal, oldVal) => {\n    \n});", "snippet", "Vue Composition API watch"));
        JS_SUGGESTIONS.add(sug("Vue onMounted", "onMounted(() => {\n    {CURSOR}\n});", "snippet", "Vue Composition API onMounted hook"));
        JS_SUGGESTIONS.add(sug("Vue defineComponent", "export default defineComponent({\n    name: '{CURSOR}',\n    setup() {\n        \n    }\n});", "snippet", "Vue Composition API defineComponent"));
        JS_SUGGESTIONS.add(sug("Nuxt3 useState", "const {CURSOR} = useState('{CURSOR}', () => initialValue);", "snippet", "Nuxt 3 useState composable"));
        JS_SUGGESTIONS.add(sug("Nuxt3 useFetch", "const { data, error } = await useFetch('{CURSOR}');", "snippet", "Nuxt 3 useFetch composable"));
        JS_SUGGESTIONS.add(sug("Nuxt3 useHead", "useHead({\n    title: '{CURSOR}',\n    meta: [{ name: 'description', content: '' }]\n});", "snippet", "Nuxt 3 useHead composable"));
        JS_SUGGESTIONS.add(sug("Nuxt3 navigateTo", "await navigateTo('{CURSOR}');", "snippet", "Nuxt 3 navigateTo helper"));
        JS_SUGGESTIONS.add(sug("Nuxt3 definePageMeta", "definePageMeta({\n    layout: '{CURSOR}',\n});", "snippet", "Nuxt 3 definePageMeta"));
        JS_SUGGESTIONS.add(sug("Nuxt3 useRouter", "const router = useRouter();", "snippet", "Nuxt 3 useRouter composable"));

        // ---- Quasar (Vue) ----
        JS_SUGGESTIONS.add(sug("Quasar q-btn", "<q-btn color=\"primary\" label=\"{CURSOR}\" @click=\"handler\" />", "snippet", "Quasar button component"));
        JS_SUGGESTIONS.add(sug("Quasar q-card", "<q-card>\n    <q-card-section>\n        <div class=\"text-h6\">{CURSOR}</div>\n    </q-card-section>\n</q-card>", "snippet", "Quasar card component"));
        JS_SUGGESTIONS.add(sug("Quasar q-input", "<q-input v-model=\"{CURSOR}\" label=\"Label\" outlined />", "snippet", "Quasar input component"));
        JS_SUGGESTIONS.add(sug("Quasar q-select", "<q-select v-model=\"{CURSOR}\" :options=\"options\" label=\"Select\" />", "snippet", "Quasar select component"));
        JS_SUGGESTIONS.add(sug("Quasar q-dialog", "<q-dialog v-model=\"{CURSOR}\">\n    <q-card>\n        <q-card-section>Content</q-card-section>\n    </q-card>\n</q-dialog>", "snippet", "Quasar dialog component"));
        JS_SUGGESTIONS.add(sug("Quasar q-table", "<q-table :rows=\"{CURSOR}\" :columns=\"columns\" row-key=\"id\" />", "snippet", "Quasar table component"));
        JS_SUGGESTIONS.add(sug("Quasar q-list", "<q-list>\n    <q-item clickable v-ripple>\n        <q-item-section>{CURSOR}</q-item-section>\n    </q-item>\n</q-list>", "snippet", "Quasar list component"));

        // ---- Framework7 ----
        JS_SUGGESTIONS.add(sug("Framework7 app", "const app = new Framework7({\n    root: '#app',\n    name: '{CURSOR}',\n    routes: [],\n});", "snippet", "Framework7 app initialization"));
        JS_SUGGESTIONS.add(sug("Framework7 view", "<div class=\"view view-main\">\n    <div class=\"page\">\n        <div class=\"navbar\">\n            <div class=\"navbar-inner\">{CURSOR}</div>\n        </div>\n    </div>\n</div>", "snippet", "Framework7 view structure"));
        JS_SUGGESTIONS.add(sug("Framework7 list", "<div class=\"list\">\n    <ul>\n        <li><a href=\"/{CURSOR}/\" class=\"item-link\">Item</a></li>\n    </ul>\n</div>", "snippet", "Framework7 list component"));
        JS_SUGGESTIONS.add(sug("Framework7 toolbar", "<div class=\"toolbar tabbar\">\n    <div class=\"toolbar-inner\">\n        <a href=\"#\" class=\"tab-link\">{CURSOR}</a>\n    </div>\n</div>", "snippet", "Framework7 toolbar component"));
        JS_SUGGESTIONS.add(sug("Framework7 navbar", "<div class=\"navbar\">\n    <div class=\"navbar-inner\">\n        <div class=\"left\"></div>\n        <div class=\"title\">{CURSOR}</div>\n        <div class=\"right\"></div>\n    </div>\n</div>", "snippet", "Framework7 navbar component"));

        // ---- Onsen UI ----
        JS_SUGGESTIONS.add(sug("Onsen UI page", "<ons-page>\n    <ons-toolbar>\n        <div class=\"center\">{CURSOR}</div>\n    </ons-toolbar>\n    <p>Content</p>\n</ons-page>", "snippet", "Onsen UI page with toolbar"));
        JS_SUGGESTIONS.add(sug("Onsen UI button", "<ons-button modifier=\"large\" onclick=\"\">{CURSOR}</ons-button>", "snippet", "Onsen UI button component"));
        JS_SUGGESTIONS.add(sug("Onsen UI list", "<ons-list>\n    <ons-list-item>{CURSOR}</ons-list-item>\n</ons-list>", "snippet", "Onsen UI list component"));
        JS_SUGGESTIONS.add(sug("Onsen UI input", "<ons-input modifier=\"underbar\" placeholder=\"{CURSOR}\" float></ons-input>", "snippet", "Onsen UI input component"));
        JS_SUGGESTIONS.add(sug("Onsen UI navigator", "<ons-navigator page=\"page.html\"></ons-navigator>", "snippet", "Onsen UI navigator component"));
        JS_SUGGESTIONS.add(sug("Onsen UI switch", "<ons-switch></ons-switch>", "snippet", "Onsen UI switch toggle"));

        // ---- Cordova / PhoneGap API ----
        String[][] cordovaApis = {
            {"cordova device", "document.addEventListener('deviceready', () => {\n    console.log('Device:', device.model);\n    // {CURSOR}\n}, false);", "snippet"},
            {"cordova device.model", "device.model", "keyword"},
            {"cordova device.platform", "device.platform", "keyword"},
            {"cordova device.uuid", "device.uuid", "keyword"},
            {"cordova device.version", "device.version", "keyword"},
            {"cordova device.manufacturer", "device.manufacturer", "keyword"},
            {"cordova device.serial", "device.serial", "keyword"},
            {"cordova camera.getPicture", "navigator.camera.getPicture(\n    (imageData) => {\n        const img = document.getElementById('{CURSOR}');\n        img.src = imageData;\n    },\n    (err) => console.error(err),\n    { quality: 50, destinationType: Camera.DestinationType.DATA_URL }\n);", "snippet"},
            {"cordova camera source PHOTOLIBRARY", "navigator.camera.getPicture(onSuccess, onFail, {\n    sourceType: Camera.PictureSourceType.PHOTOLIBRARY,\n    destinationType: Camera.DestinationType.FILE_URI\n});", "snippet"},
            {"cordova camera source SAVEDPHOTOALBUM", "navigator.camera.getPicture(onSuccess, onFail, {\n    sourceType: Camera.PictureSourceType.SAVEDPHOTOALBUM,\n    destinationType: Camera.DestinationType.FILE_URI\n});", "snippet"},
            {"cordova camera source CAMERA", "navigator.camera.getPicture(onSuccess, onFail, {\n    sourceType: Camera.PictureSourceType.CAMERA,\n    destinationType: Camera.DestinationType.FILE_URI\n});", "snippet"},
            {"cordova geolocation.getCurrentPosition", "navigator.geolocation.getCurrentPosition(\n    (pos) => {\n        console.log('Lat:', pos.coords.latitude, 'Lng:', pos.coords.longitude);\n        {CURSOR}\n    },\n    (err) => console.error(err),\n    { enableHighAccuracy: true }\n);", "snippet"},
            {"cordova geolocation.watchPosition", "const watchId = navigator.geolocation.watchPosition(\n    (pos) => {\n        console.log('Lat:', pos.coords.latitude, 'Lng:', pos.coords.longitude);\n        {CURSOR}\n    },\n    (err) => console.error(err),\n    { enableHighAccuracy: true }\n);", "snippet"},
            {"cordova geolocation.clearWatch", "navigator.geolocation.clearWatch(watchId);", "keyword"},
            {"cordova notification.alert", "navigator.notification.alert(\n    '{CURSOR}',\n    () => {},\n    'Alert',\n    'OK'\n);", "snippet"},
            {"cordova notification.confirm", "navigator.notification.confirm(\n    '{CURSOR}',\n    (buttonIndex) => {\n        if (buttonIndex === 1) {}\n    },\n    'Confirm',\n    ['Yes', 'No']\n);", "snippet"},
            {"cordova notification.prompt", "navigator.notification.prompt(\n    '{CURSOR}',\n    (result) => {\n        console.log('Input:', result.input1);\n    },\n    'Prompt',\n    ['OK', 'Cancel'],\n    'Default'\n);", "snippet"},
            {"cordova notification.beep", "navigator.notification.beep(2);", "keyword"},
            {"cordova notification.vibrate", "navigator.vibrate(1000);", "keyword"},
            {"cordova splashscreen.show", "navigator.splashscreen.show();", "keyword"},
            {"cordova splashscreen.hide", "navigator.splashscreen.hide();", "keyword"},
            {"cordova statusbar.hide", "StatusBar.hide();", "keyword"},
            {"cordova statusbar.show", "StatusBar.show();", "keyword"},
            {"cordova statusbar backgroundColor", "StatusBar.backgroundColorByName('{CURSOR}');", "keyword"},
            {"cordova network.connection.type", "navigator.connection.type", "keyword"},
            {"cordova connection NONE", "Connection.NONE", "keyword"},
            {"cordova connection WIFI", "Connection.WIFI", "keyword"},
            {"cordova connection CELLULAR", "Connection.CELLULAR", "keyword"},
            {"cordova connection ETHERNET", "Connection.ETHERNET", "keyword"},
            {"cordova inappbrowser open", "window.open('{CURSOR}', '_blank', 'location=yes,toolbar=yes');", "snippet"},
            {"cordova inappbrowser event", "const ref = window.open('{CURSOR}', '_blank', 'location=yes');\nref.addEventListener('loadstart', (event) => {\n    console.log('URL:', event.url);\n});", "snippet"},
            {"cordova inappbrowser executeScript", "ref.executeScript({ code: 'document.body.style.backgroundColor = \"red\";' });", "keyword"},
            {"cordova inappbrowser insertCSS", "ref.insertCSS({ code: 'body { background: #000; }' });", "keyword"},
            {"cordova media", "const media = new Media('{CURSOR}', () => {}, (err) => console.error(err));\nmedia.play();", "snippet"},
            {"cordova media.play", "media.play();", "keyword"},
            {"cordova media.pause", "media.pause();", "keyword"},
            {"cordova media.stop", "media.stop();", "keyword"},
            {"cordova media.release", "media.release();", "keyword"},
            {"cordova media.getDuration", "media.getDuration();", "keyword"},
            {"cordova media.getCurrentPosition", "media.getCurrentPosition((pos) => {\n    console.log('Position:', pos);\n});", "snippet"},
            {"cordova file resolveLocalFileSystemURL", "window.resolveLocalFileSystemURL(\n    cordova.file.{CURSOR},\n    (entry) => {},\n    (err) => console.error(err)\n);", "snippet"},
            {"cordova file applicationDirectory", "cordova.file.applicationDirectory", "keyword"},
            {"cordova file applicationStorageDirectory", "cordova.file.applicationStorageDirectory", "keyword"},
            {"cordova file dataDirectory", "cordova.file.dataDirectory", "keyword"},
            {"cordova file cacheDirectory", "cordova.file.cacheDirectory", "keyword"},
            {"cordova file externalApplicationStorageDirectory", "cordova.file.externalApplicationStorageDirectory", "keyword"},
            {"cordova file externalDataDirectory", "cordova.file.externalDataDirectory", "keyword"},
            {"cordova file.externalCacheDirectory", "cordova.file.externalCacheDirectory", "keyword"},
            {"cordova file readAsText", "fileEntry.file((file) => {\n    const reader = new FileReader();\n    reader.onloadend = (e) => {\n        const content = e.target.result;\n        {CURSOR}\n    };\n    reader.readAsText(file);\n});", "snippet"},
            {"cordova file writeTo", "fileEntry.createWriter((writer) => {\n    writer.onwriteend = () => console.log('Written');\n    writer.write('{CURSOR}');\n});", "snippet"},
            {"cordova dialogs.alert", "navigator.notification.alert('{CURSOR}', null, 'Info', 'OK');", "snippet"},
            {"cordova SocialSharing.share", "window.plugins.socialsharing.share(\n    '{CURSOR}',\n    'Subject',\n    null,\n    null\n);", "snippet"},
            {"cordova SocialSharing.shareViaTwitter", "window.plugins.socialsharing.shareViaTwitter('{CURSOR}');", "snippet"},
            {"cordova SocialSharing.shareViaFacebook", "window.plugins.socialsharing.shareViaFacebook('{CURSOR}');", "snippet"},
            {"cordova SQLite openDatabase", "const db = window.sqlitePlugin.openDatabase({\n    name: '{CURSOR}.db',\n    location: 'default'\n});", "snippet"},
            {"cordova SQLite executeSql", "db.executeSql('SELECT * FROM {CURSOR}', [], (rs) => {\n    for (let i = 0; i < rs.rows.length; i++) {\n        console.log(rs.rows.item(i));\n    }\n});", "snippet"},
            {"cordova push init", "const push = PushNotification.init({\n    android: {},\n    ios: { alert: true, badge: true, sound: true }\n});\npush.on('registration', (data) => {\n    console.log('Device reg:', data.registrationId);\n});\npush.on('notification', (data) => {\n    console.log('Notif:', data);\n    {CURSOR}\n});", "snippet"},
            {"cordova admob", "admob.requestAd({ banner: 'ca-app-pub-{CURSOR}' });", "keyword"},
            {"cordova keyboard show", "Keyboard.show();", "keyword"},
            {"cordova keyboard hide", "Keyboard.hide();", "keyword"},
            {"cordova keyboard.shrinkView", "Keyboard.shrinkView(true);", "keyword"},
            {"cordova barcode scanner", "cordova.plugins.barcodeScanner.scan(\n    (result) => {\n        console.log('Barcode:', result.text);\n        {CURSOR}\n    },\n    (err) => console.error(err)\n);", "snippet"},
            {"cordova device orientation", "navigator.compass.getCurrentHeading((heading) => {\n    console.log('Heading:', heading.magneticHeading);\n    {CURSOR}\n});", "snippet"},
            {"cordova accelerometer", "navigator.accelerometer.getCurrentAcceleration((accel) => {\n    console.log('X:', accel.x, 'Y:', accel.y, 'Z:', accel.z);\n    {CURSOR}\n});", "snippet"},
            {"cordova accelerometer watch", "navigator.accelerometer.watchAcceleration((accel) => {\n    console.log('X:', accel.x, 'Y:', accel.y, 'Z:', accel.z);\n    {CURSOR}\n}, null, { frequency: 200 });", "snippet"},
            {"cordova globalization getPreferredLanguage", "navigator.globalization.getPreferredLanguage((lang) => {\n    console.log('Lang:', lang.value);\n    {CURSOR}\n});", "snippet"},
            {"cordova globalization getLocaleName", "navigator.globalization.getLocaleName((locale) => {\n    console.log('Locale:', locale.value);\n    {CURSOR}\n});", "snippet"},
            {"cordova contacts find", "navigator.contacts.find(\n    ['displayName', 'phoneNumbers'],\n    (contacts) => {\n        console.log('Found:', contacts.length);\n        {CURSOR}\n    },\n    (err) => console.error(err)\n);", "snippet"},
            {"cordova vibration vibrate", "navigator.notification.vibrate(2000);", "keyword"},
            {"cordova Toast show", "window.plugins.toast.showWithOptions({\n    message: '{CURSOR}',\n    duration: 'short',\n    position: 'bottom'\n});", "snippet"},
        };
        for (String[] cp : cordovaApis) {
            JS_SUGGESTIONS.add(sug(cp[0], cp[1], cp[2], "Cordova / PhoneGap API"));
        }

        // ---- Capacitor API ----
        String[][] capacitorApis = {
            {"capacitor getPlatform", "import { Capacitor } from '@capacitor/core';\n\nconst platform = Capacitor.getPlatform();\n// 'ios', 'android', 'web'{CURSOR}", "snippet"},
            {"capacitor isNativePlatform", "import { Capacitor } from '@capacitor/core';\n\nif (Capacitor.isNativePlatform()) {\n    {CURSOR}\n}", "snippet"},
            {"capacitor Camera.getPhoto", "import { Camera, CameraResultType } from '@capacitor/camera';\n\nconst photo = await Camera.getPhoto({\n    resultType: CameraResultType.Uri,\n    source: CameraSource.{CURSOR},\n    quality: 90\n});", "snippet"},
            {"capacitor Geolocation.getCurrentPosition", "import { Geolocation } from '@capacitor/geolocation';\n\nconst position = await Geolocation.getCurrentPosition();\nconsole.log('Lat:', position.coords.latitude, 'Lng:', position.coords.longitude);{CURSOR}", "snippet"},
            {"capacitor Geolocation.watchPosition", "import { Geolocation } from '@capacitor/geolocation';\n\nconst watchId = Geolocation.watchPosition({}, (position) => {\n    console.log('Pos:', position.coords.latitude, position.coords.longitude);\n    {CURSOR}\n});", "snippet"},
            {"capacitor Storage.get", "import { Storage } from '@capacitor/storage';\n\nconst { value } = await Storage.get({ key: '{CURSOR}' });", "snippet"},
            {"capacitor Storage.set", "import { Storage } from '@capacitor/storage';\n\nawait Storage.set({ key: '{CURSOR}', value: 'value' });", "snippet"},
            {"capacitor Storage.remove", "import { Storage } from '@capacitor/storage';\n\nawait Storage.remove({ key: '{CURSOR}' });", "snippet"},
            {"capacitor Filesystem.readFile", "import { Filesystem, Directory } from '@capacitor/filesystem';\n\nconst result = await Filesystem.readFile({\n    path: '{CURSOR}',\n    directory: Directory.Data\n});", "snippet"},
            {"capacitor Filesystem.writeFile", "import { Filesystem, Directory } from '@capacitor/filesystem';\n\nawait Filesystem.writeFile({\n    path: '{CURSOR}',\n    data: 'content',\n    directory: Directory.Data\n});", "snippet"},
            {"capacitor Filesystem.deleteFile", "import { Filesystem, Directory } from '@capacitor/filesystem';\n\nawait Filesystem.deleteFile({\n    path: '{CURSOR}',\n    directory: Directory.Data\n});", "snippet"},
            {"capacitor Network.getStatus", "import { Network } from '@capacitor/network';\n\nconst status = await Network.getStatus();\nconsole.log('Connected:', status.connected);{CURSOR}", "snippet"},
            {"capacitor Network.addListener", "import { Network } from '@capacitor/network';\n\nNetwork.addListener('networkStatusChange', (status) => {\n    console.log('Connected:', status.connected);\n    {CURSOR}\n});", "snippet"},
            {"capacitor Share.share", "import { Share } from '@capacitor/share';\n\nawait Share.share({\n    title: '{CURSOR}',\n    text: 'Check this out!',\n    url: 'https://example.com'\n});", "snippet"},
            {"capacitor Haptics.vibrate", "import { Haptics } from '@capacitor/haptics';\n\nawait Haptics.vibrate();", "snippet"},
            {"capacitor Haptics.impact", "import { Haptics, ImpactStyle } from '@capacitor/haptics';\n\nawait Haptics.impact({ style: ImpactStyle.{CURSOR} });", "snippet"},
            {"capacitor StatusBar.setStyle", "import { StatusBar, Style } from '@capacitor/status-bar';\n\nawait StatusBar.setStyle({ style: Style.{CURSOR} });", "snippet"},
            {"capacitor StatusBar.show", "import { StatusBar } from '@capacitor/status-bar';\n\nawait StatusBar.show();", "snippet"},
            {"capacitor StatusBar.hide", "import { StatusBar } from '@capacitor/status-bar';\n\nawait StatusBar.hide();", "snippet"},
            {"capacitor PushNotifications.register", "import { PushNotifications } from '@capacitor/push-notifications';\n\nawait PushNotifications.register();\nPushNotifications.addListener('registration', (token) => {\n    console.log('Token:', token.value);\n    {CURSOR}\n});", "snippet"},
            {"capacitor PushNotifications.getDeliveredNotifications", "import { PushNotifications } from '@capacitor/push-notifications';\n\nconst notifs = await PushNotifications.getDeliveredNotifications();", "snippet"},
            {"capacitor LocalNotifications.schedule", "import { LocalNotifications } from '@capacitor/local-notifications';\n\nawait LocalNotifications.schedule({\n    notifications: [\n        { title: '{CURSOR}', body: 'Body', id: 1 }\n    ]\n});", "snippet"},
            {"capacitor Motion.addListener", "import { Motion } from '@capacitor/motion';\n\nMotion.addListener('accel', (event) => {\n    console.log('Accel:', event.acceleration);\n    {CURSOR}\n});", "snippet"},
            {"capacitor Clipboard.write", "import { Clipboard } from '@capacitor/clipboard';\n\nawait Clipboard.write({ string: '{CURSOR}' });", "snippet"},
            {"capacitor Clipboard.read", "import { Clipboard } from '@capacitor/clipboard';\n\nconst { value } = await Clipboard.read();", "snippet"},
            {"capacitor ScreenOrientation.lock", "import { ScreenOrientation } from '@capacitor/screen-orientation';\n\nawait ScreenOrientation.lock({ orientation: '{CURSOR}' });", "snippet"},
            {"capacitor ScreenOrientation.unlock", "import { ScreenOrientation } from '@capacitor/screen-orientation';\n\nawait ScreenOrientation.unlock();", "snippet"},
            {"capacitor Modals.alert", "import { Modals } from '@capacitor/dialog';\n\nawait Modals.alert({ title: '{CURSOR}', message: 'Message' });", "snippet"},
            {"capacitor Modals.confirm", "import { Modals } from '@capacitor/dialog';\n\nconst { value } = await Modals.confirm({ title: '{CURSOR}', message: 'Confirm?' });", "snippet"},
            {"capacitor Browser.open", "import { Browser } from '@capacitor/browser';\n\nawait Browser.open({ url: '{CURSOR}' });", "snippet"},
            {"capacitor SplashScreen.hide", "import { SplashScreen } from '@capacitor/splash-screen';\n\nawait SplashScreen.hide();", "snippet"},
            {"capacitor App.addListener", "import { App } from '@capacitor/app';\n\nApp.addListener('appStateChange', (state) => {\n    console.log('Active:', state.isActive);\n    {CURSOR}\n});", "snippet"},
            {"capacitor App.exitApp", "import { App } from '@capacitor/app';\n\nApp.exitApp();", "snippet"},
        };
        for (String[] cap : capacitorApis) {
            JS_SUGGESTIONS.add(sug(cap[0], cap[1], cap[2], "Capacitor API"));
        }

        // ---- D3.js ----
        JS_SUGGESTIONS.add(sug("D3.select", "d3.select('{CURSOR}')", "keyword", "D3 select element"));
        JS_SUGGESTIONS.add(sug("D3.selectAll", "d3.selectAll('{CURSOR}')", "keyword", "D3 select all elements"));
        JS_SUGGESTIONS.add(sug("D3.append", "d3.select('{CURSOR}').append('g')", "keyword", "D3 append element"));
        JS_SUGGESTIONS.add(sug("D3.attr", "d3.select('{CURSOR}').attr('fill', 'steelblue')", "keyword", "D3 set attribute"));
        JS_SUGGESTIONS.add(sug("D3.style", "d3.select('{CURSOR}').style('color', 'red')", "keyword", "D3 set style"));
        JS_SUGGESTIONS.add(sug("D3.data join", "d3.select('{CURSOR}')\n    .selectAll('circle')\n    .data(data)\n    .join('circle')\n    .attr('r', d => d.value);", "snippet", "D3 data join pattern"));
        JS_SUGGESTIONS.add(sug("D3.scaleLinear", "const xScale = d3.scaleLinear()\n    .domain([0, d3.max(data, d => d.{CURSOR})])\n    .range([0, width]);", "snippet", "D3 linear scale"));
        JS_SUGGESTIONS.add(sug("D3.scaleBand", "const xScale = d3.scaleBand()\n    .domain(data.map(d => d.{CURSOR}))\n    .range([0, width])\n    .padding(0.1);", "snippet", "D3 band scale"));
        JS_SUGGESTIONS.add(sug("D3.axisBottom", "svg.append('g')\n    .attr('transform', `translate(0, ${height})`)\n    .call(d3.axisBottom(xScale));", "snippet", "D3 bottom axis"));
        JS_SUGGESTIONS.add(sug("D3.axisLeft", "svg.append('g')\n    .call(d3.axisLeft(yScale));", "snippet", "D3 left axis"));
        JS_SUGGESTIONS.add(sug("D3.line", "const line = d3.line()\n    .x(d => x(d.{CURSOR}))\n    .y(d => y(d.value));", "snippet", "D3 line generator"));
        JS_SUGGESTIONS.add(sug("D3.area", "const area = d3.area()\n    .x(d => x(d.{CURSOR}))\n    .y0(y(0))\n    .y1(d => y(d.value));", "snippet", "D3 area generator"));
        JS_SUGGESTIONS.add(sug("D3.force simulation", "const simulation = d3.forceSimulation(data)\n    .force('charge', d3.forceManyBody())\n    .force('center', d3.forceCenter(width / 2, height / 2))\n    .on('tick', () => {\n        {CURSOR}\n    });", "snippet", "D3 force layout simulation"));
        JS_SUGGESTIONS.add(sug("D3.csv", "d3.csv('{CURSOR}').then(data => {\n    console.log(data);\n});", "snippet", "D3 load CSV data"));
        JS_SUGGESTIONS.add(sug("D3.json", "d3.json('{CURSOR}').then(data => {\n    console.log(data);\n});", "snippet", "D3 load JSON data"));

        // ---- jQuery ----
        JS_SUGGESTIONS.add(sug("jQuery ready", "$(document).ready(function() {\n    {CURSOR}\n});", "snippet", "jQuery document ready"));
        JS_SUGGESTIONS.add(sug("jQuery click", "$('{CURSOR}').click(function() {\n    \n});", "snippet", "jQuery click handler"));
        JS_SUGGESTIONS.add(sug("jQuery each", "${CURSOR}.each(function(index, element) {\n    \n});", "snippet", "jQuery each iterator"));
        JS_SUGGESTIONS.add(sug("jQuery ajax", "$.ajax({\n    url: '{CURSOR}',\n    method: 'GET',\n    success: function(data) {},\n    error: function(err) { console.error(err); }\n});", "snippet", "jQuery AJAX request"));
        JS_SUGGESTIONS.add(sug("jQuery on", "$('{CURSOR}').on('click', '.child', function() {\n    \n});", "snippet", "jQuery on event delegation"));
        JS_SUGGESTIONS.add(sug("jQuery animate", "$('{CURSOR}').animate({ opacity: 0.5 }, 500);", "snippet", "jQuery animate CSS"));
        JS_SUGGESTIONS.add(sug("jQuery fadeIn", "${CURSOR}.fadeIn(400);", "keyword", "jQuery fade in"));
        JS_SUGGESTIONS.add(sug("jQuery fadeOut", "${CURSOR}.fadeOut(400);", "keyword", "jQuery fade out"));
        JS_SUGGESTIONS.add(sug("jQuery slideToggle", "${CURSOR}.slideToggle(300);", "keyword", "jQuery slide toggle"));
        JS_SUGGESTIONS.add(sug("jQuery serialize", "${CURSOR}.serialize();", "keyword", "jQuery serialize form"));
        JS_SUGGESTIONS.add(sug("jQuery val", "${CURSOR}.val();", "keyword", "jQuery get value"));
        JS_SUGGESTIONS.add(sug("jQuery text", "${CURSOR}.text('{CURSOR}');", "keyword", "jQuery set text"));
        JS_SUGGESTIONS.add(sug("jQuery html", "${CURSOR}.html('<div></div>');", "keyword", "jQuery set HTML"));
        JS_SUGGESTIONS.add(sug("jQuery addClass", "${CURSOR}.addClass('{CURSOR}');", "keyword", "jQuery add class"));
        JS_SUGGESTIONS.add(sug("jQuery removeClass", "${CURSOR}.removeClass('{CURSOR}');", "keyword", "jQuery remove class"));
        JS_SUGGESTIONS.add(sug("jQuery toggleClass", "${CURSOR}.toggleClass('{CURSOR}');", "keyword", "jQuery toggle class"));
        JS_SUGGESTIONS.add(sug("jQuery hasClass", "${CURSOR}.hasClass('{CURSOR}');", "keyword", "jQuery has class check"));
        JS_SUGGESTIONS.add(sug("jQuery siblings", "${CURSOR}.siblings().hide();", "keyword", "jQuery siblings selector"));
        JS_SUGGESTIONS.add(sug("jQuery parent", "${CURSOR}.parent().{CURSOR}", "keyword", "jQuery parent access"));
        JS_SUGGESTIONS.add(sug("jQuery children", "${CURSOR}.children('.{CURSOR}')", "keyword", "jQuery children selector"));
        JS_SUGGESTIONS.add(sug("jQuery find", "${CURSOR}.find('.{CURSOR}')", "keyword", "jQuery find child elements"));
        JS_SUGGESTIONS.add(sug("jQuery closest", "${CURSOR}.closest('.{CURSOR}')", "keyword", "jQuery closest ancestor"));
        JS_SUGGESTIONS.add(sug("jQuery append", "${CURSOR}.append('<div></div>');", "keyword", "jQuery append content"));
        JS_SUGGESTIONS.add(sug("jQuery prepend", "${CURSOR}.prepend('<div></div>');", "keyword", "jQuery prepend content"));
        JS_SUGGESTIONS.add(sug("jQuery remove", "${CURSOR}.remove();", "keyword", "jQuery remove element"));
        JS_SUGGESTIONS.add(sug("jQuery empty", "${CURSOR}.empty();", "keyword", "jQuery empty element"));
        JS_SUGGESTIONS.add(sug("jQuery css", "${CURSOR}.css('color', 'red');", "keyword", "jQuery get/set CSS"));
        JS_SUGGESTIONS.add(sug("jQuery data", "${CURSOR}.data('{CURSOR}');", "keyword", "jQuery data attribute"));
        JS_SUGGESTIONS.add(sug("jQuery prop", "${CURSOR}.prop('disabled', true);", "keyword", "jQuery property access"));
        JS_SUGGESTIONS.add(sug("jQuery attr", "${CURSOR}.attr('{CURSOR}', 'value');", "keyword", "jQuery attribute access"));
        JS_SUGGESTIONS.add(sug("jQuery width", "${CURSOR}.width();", "keyword", "jQuery element width"));
        JS_SUGGESTIONS.add(sug("jQuery height", "${CURSOR}.height();", "keyword", "jQuery element height"));
        JS_SUGGESTIONS.add(sug("jQuery offset", "${CURSOR}.offset();", "keyword", "jQuery position offset"));
        JS_SUGGESTIONS.add(sug("jQuery scrollTop", "${CURSOR}.scrollTop();", "keyword", "jQuery scroll position"));
        JS_SUGGESTIONS.add(sug("jQuery trigger", "${CURSOR}.trigger('{CURSOR}');", "keyword", "jQuery trigger event"));
        JS_SUGGESTIONS.add(sug("jQuery deferred", "const deferred = $.Deferred();\ndeferred.resolve({CURSOR});", "snippet", "jQuery Deferred pattern"));
        JS_SUGGESTIONS.add(sug("jQuery extend", "const merged = $.extend(true, {}, defaults, options);", "keyword", "jQuery deep extend"));
        JS_SUGGESTIONS.add(sug("jQuery grep", "const filtered = $.grep(array, (item) => {\n    return item.{CURSOR} === true;\n});", "snippet", "jQuery grep filter"));
        JS_SUGGESTIONS.add(sug("jQuery inArray", "$.inArray('{CURSOR}', array);", "keyword", "jQuery in array check"));
        JS_SUGGESTIONS.add(sug("jQuery map", "const mapped = $.map(array, (item, i) => {\n    return item.{CURSOR};\n});", "snippet", "jQuery map array"));

        // ---- Three.js ----
        JS_SUGGESTIONS.add(sug("Three.js scene", "const scene = new THREE.Scene();\nconst camera = new THREE.PerspectiveCamera(75, window.innerWidth / window.innerHeight, 0.1, 1000);\nconst renderer = new THREE.WebGLRenderer({ antialias: true });\nrenderer.setSize(window.innerWidth, window.innerHeight);\ndocument.body.appendChild(renderer.domElement);{CURSOR}", "snippet", "Three.js basic scene setup"));
        JS_SUGGESTIONS.add(sug("Three.js box", "const geometry = new THREE.BoxGeometry(1, 1, 1);\nconst material = new THREE.MeshStandardMaterial({ color: 0x{CURSOR} });\nconst mesh = new THREE.Mesh(geometry, material);\nscene.add(mesh);", "snippet", "Three.js box mesh"));
        JS_SUGGESTIONS.add(sug("Three.js sphere", "const geometry = new THREE.SphereGeometry(1, 32, 32);\nconst material = new THREE.MeshStandardMaterial({ color: 0x{CURSOR} });\nconst sphere = new THREE.Mesh(geometry, material);\nscene.add(sphere);", "snippet", "Three.js sphere mesh"));
        JS_SUGGESTIONS.add(sug("Three.js animation loop", "function animate() {\n    requestAnimationFrame(animate);\n    mesh.rotation.x += 0.01;\n    mesh.rotation.y += 0.01;\n    renderer.render(scene, camera);\n}\nanimate();", "snippet", "Three.js animation loop"));
        JS_SUGGESTIONS.add(sug("Three.js OrbitControls", "const controls = new THREE.OrbitControls(camera, renderer.domElement);\ncontrols.enableDamping = true;\ncontrols.{CURSOR}", "snippet", "Three.js OrbitControls"));
        JS_SUGGESTIONS.add(sug("Three.js GLTFLoader", "const loader = new THREE.GLTFLoader();\nloader.load('{CURSOR}', (gltf) => {\n    scene.add(gltf.scene);\n});", "snippet", "Three.js GLTF model loader"));
        JS_SUGGESTIONS.add(sug("Three.js lights", "const ambientLight = new THREE.AmbientLight(0x404040);\nconst directionalLight = new THREE.DirectionalLight(0xffffff, 1);\ndirectionalLight.position.set(5, 10, 7);\nscene.add(ambientLight);\nscene.add(directionalLight);{CURSOR}", "snippet", "Three.js scene lighting"));

        // ---- Chart.js ----
        JS_SUGGESTIONS.add(sug("Chart.js", "new Chart(document.getElementById('{CURSOR}'), {\n    type: 'bar',\n    data: {\n        labels: ['A', 'B', 'C'],\n        datasets: [{ label: 'Data', data: [10, 20, 30] }]\n    },\n    options: { responsive: true }\n});", "snippet", "Chart.js basic chart"));
        JS_SUGGESTIONS.add(sug("Chart.js line", "new Chart(ctx, {\n    type: 'line',\n    data: {\n        labels: labels,\n        datasets: [{\n            label: '{CURSOR}',\n            data: data,\n            borderColor: 'rgb(75, 192, 192)',\n            tension: 0.1\n        }]\n    }\n});", "snippet", "Chart.js line chart"));
        JS_SUGGESTIONS.add(sug("Chart.js pie", "new Chart(ctx, {\n    type: 'pie',\n    data: {\n        labels: ['Red', 'Blue', 'Yellow'],\n        datasets: [{\n            data: [300, 50, 100],\n            backgroundColor: ['red', 'blue', 'yellow']\n        }]\n    }\n});", "snippet", "Chart.js pie chart"));
        JS_SUGGESTIONS.add(sug("Chart.js doughnut", "new Chart(ctx, {\n    type: 'doughnut',\n    data: {\n        labels: ['{CURSOR}'],\n        datasets: [{ data: [10, 20, 30] }]\n    }\n});", "snippet", "Chart.js doughnut chart"));

        // ---- Lodash / Underscore ----
        JS_SUGGESTIONS.add(sug("lodash debounce", "const debounced = _.debounce(() => {\n    {CURSOR}\n}, 300);", "snippet", "Lodash debounce helper"));
        JS_SUGGESTIONS.add(sug("lodash throttle", "const throttled = _.throttle(() => {\n    {CURSOR}\n}, 200);", "snippet", "Lodash throttle helper"));
        JS_SUGGESTIONS.add(sug("lodash cloneDeep", "const clone = _.cloneDeep({CURSOR});", "keyword", "Lodash deep clone"));
        JS_SUGGESTIONS.add(sug("lodash merge", "const merged = _.merge({}, defaults, {CURSOR});", "keyword", "Lodash merge objects"));
        JS_SUGGESTIONS.add(sug("lodash pick", "const picked = _.pick({CURSOR}, ['key1', 'key2']);", "keyword", "Lodash pick keys"));
        JS_SUGGESTIONS.add(sug("lodash omit", "const omitted = _.omit({CURSOR}, ['key1']);", "keyword", "Lodash omit keys"));
        JS_SUGGESTIONS.add(sug("lodash groupBy", "const grouped = _.groupBy({CURSOR}, 'category');", "keyword", "Lodash group by property"));
        JS_SUGGESTIONS.add(sug("lodash orderBy", "const sorted = _.orderBy({CURSOR}, ['field'], ['asc']);", "keyword", "Lodash order by field"));
        JS_SUGGESTIONS.add(sug("lodash get", "_.get(obj, '{CURSOR}', defaultValue);", "keyword", "Lodash safe get"));
        JS_SUGGESTIONS.add(sug("lodash set", "_.set(obj, '{CURSOR}', value);", "keyword", "Lodash deep set"));
        JS_SUGGESTIONS.add(sug("lodash uniqBy", "_.uniqBy({CURSOR}, 'id');", "keyword", "Lodash unique by property"));
        JS_SUGGESTIONS.add(sug("lodash chunk", "_.chunk({CURSOR}, 5);", "keyword", "Lodash chunk array"));
        JS_SUGGESTIONS.add(sug("lodash isEmpty", "_.isEmpty({CURSOR});", "keyword", "Lodash isEmpty check"));

        // ---- Day.js / date-fns ----
        JS_SUGGESTIONS.add(sug("dayjs format", "dayjs().format('YYYY-MM-DD {CURSOR}');", "keyword", "Day.js format date"));
        JS_SUGGESTIONS.add(sug("dayjs diff", "dayjs('{CURSOR}').diff(dayjs(), 'day');", "keyword", "Day.js date difference"));
        JS_SUGGESTIONS.add(sug("dayjs add", "dayjs().add(7, 'day').format('YYYY-MM-DD');", "keyword", "Day.js add days"));
        JS_SUGGESTIONS.add(sug("date-fns format", "import { format } from 'date-fns';\nformat(new Date(), '{CURSOR}');", "snippet", "date-fns format date"));
        JS_SUGGESTIONS.add(sug("date-fns formatDistance", "import { formatDistance } from 'date-fns';\nformatDistance(new Date('{CURSOR}'), new Date());", "snippet", "date-fns relative distance"));
        JS_SUGGESTIONS.add(sug("date-fns parseISO", "import { parseISO } from 'date-fns';\nparseISO('{CURSOR}');", "snippet", "date-fns parse ISO string"));
        JS_SUGGESTIONS.add(sug("date-fns addDays", "import { addDays } from 'date-fns';\naddDays(new Date(), {CURSOR});", "snippet", "date-fns add days"));

        // ---- Axios / HTTP ----
        JS_SUGGESTIONS.add(sug("axios GET", "import axios from 'axios';\n\nconst { data } = await axios.get('{CURSOR}');", "snippet", "Axios GET request"));
        JS_SUGGESTIONS.add(sug("axios POST", "import axios from 'axios';\n\nconst { data } = await axios.post('{CURSOR}', { body });", "snippet", "Axios POST request"));
        JS_SUGGESTIONS.add(sug("axios interceptors", "axios.interceptors.request.use((config) => {\n    config.headers.Authorization = 'Bearer {CURSOR}';\n    return config;\n});", "snippet", "Axios request interceptor"));
        JS_SUGGESTIONS.add(sug("axios error handling", "try {\n    const { data } = await axios.get('{CURSOR}');\n} catch (error) {\n    if (error.response) {\n        console.log(error.response.status);\n    }\n}", "snippet", "Axios error handling"));

        // ---- More utilities ----
        JS_SUGGESTIONS.add(sug("console.log", "console.log({CURSOR});", "keyword", "Console log output"));
        JS_SUGGESTIONS.add(sug("console.error", "console.error({CURSOR});", "keyword", "Console error output"));
        JS_SUGGESTIONS.add(sug("console.warn", "console.warn({CURSOR});", "keyword", "Console warning output"));
        JS_SUGGESTIONS.add(sug("console.table", "console.table({CURSOR});", "keyword", "Console table output"));
        JS_SUGGESTIONS.add(sug("console.time", "console.time('{CURSOR}');", "keyword", "Console time measurement"));
        JS_SUGGESTIONS.add(sug("console.group", "console.group('{CURSOR}');", "keyword", "Console group collapsible"));
        JS_SUGGESTIONS.add(sug("console.trace", "console.trace();", "keyword", "Console stack trace"));
        JS_SUGGESTIONS.add(sug("performance.now", "performance.now();", "keyword", "Performance timestamp"));
        JS_SUGGESTIONS.add(sug("setTimeout", "setTimeout(() => {\n    {CURSOR}\n}, 1000);", "snippet", "setTimeout delayed execution"));
        JS_SUGGESTIONS.add(sug("setInterval", "setInterval(() => {\n    {CURSOR}\n}, 1000);", "snippet", "setInterval periodic execution"));
        JS_SUGGESTIONS.add(sug("clearTimeout", "clearTimeout({CURSOR});", "keyword", "Clear timeout"));
        JS_SUGGESTIONS.add(sug("requestAnimationFrame", "requestAnimationFrame(() => {\n    {CURSOR}\n});", "snippet", "Animation frame request"));
        JS_SUGGESTIONS.add(sug("JSON.parse", "JSON.parse('{CURSOR}')", "keyword", "Parse JSON string"));
        JS_SUGGESTIONS.add(sug("JSON.stringify", "JSON.stringify({CURSOR}, null, 2)", "keyword", "Stringify JSON"));
        JS_SUGGESTIONS.add(sug("Map basics", "const map = new Map();\nmap.set('{CURSOR}', value);\nmap.get('{CURSOR}');\nmap.has('{CURSOR}');\nmap.delete('{CURSOR}');", "snippet", "ES6 Map usage"));
        JS_SUGGESTIONS.add(sug("Set basics", "const set = new Set();\nset.add({CURSOR});\nset.has({CURSOR});\nset.delete({CURSOR});", "snippet", "ES6 Set usage"));
        JS_SUGGESTIONS.add(sug("Intl.NumberFormat", "new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format({CURSOR});", "snippet", "International number format"));
        JS_SUGGESTIONS.add(sug("Intl.DateTimeFormat", "new Intl.DateTimeFormat('en-US', { dateStyle: 'full' }).format(new Date());", "snippet", "International date format"));
        JS_SUGGESTIONS.add(sug("URL constructor", "const url = new URL('{CURSOR}');\nurl.searchParams.set('key', 'value');", "snippet", "URL API constructor"));
        JS_SUGGESTIONS.add(sug("FormData", "const formData = new FormData();\nformData.append('{CURSOR}', value);", "snippet", "FormData API"));
        JS_SUGGESTIONS.add(sug("Blob", "const blob = new Blob([{CURSOR}], { type: 'text/plain' });", "snippet", "Blob constructor"));
        JS_SUGGESTIONS.add(sug("FileReader", "const reader = new FileReader();\nreader.onload = (e) => {\n    console.log(e.target.result);\n    {CURSOR}\n};\nreader.readAsText(file);", "snippet", "FileReader API"));
        JS_SUGGESTIONS.add(sug("Canvas 2D", "const canvas = document.getElementById('{CURSOR}');\nconst ctx = canvas.getContext('2d');\nctx.fillStyle = 'red';\nctx.fillRect(0, 0, 100, 100);", "snippet", "Canvas 2D drawing"));
        JS_SUGGESTIONS.add(sug("Web Worker", "const worker = new Worker('{CURSOR}.js');\nworker.postMessage({ cmd: 'start' });\nworker.onmessage = (e) => {\n    console.log(e.data);\n};", "snippet", "Web Worker basic usage"));
        JS_SUGGESTIONS.add(sug("Service Worker", "self.addEventListener('install', (event) => {\n    event.waitUntil(self.skipWaiting());\n    {CURSOR}\n});\n\nself.addEventListener('fetch', (event) => {\n    event.respondWith(fetch(event.request));\n});", "snippet", "Service Worker basic structure"));
        JS_SUGGESTIONS.add(sug("localStorage", "localStorage.setItem('{CURSOR}', value);\nconst val = localStorage.getItem('{CURSOR}');\nlocalStorage.removeItem('{CURSOR}');", "snippet", "localStorage API"));
        JS_SUGGESTIONS.add(sug("sessionStorage", "sessionStorage.setItem('{CURSOR}', value);", "keyword", "sessionStorage API"));
        JS_SUGGESTIONS.add(sug("IndexedDB open", "const request = indexedDB.open('{CURSOR}', 1);\nrequest.onupgradeneeded = (event) => {\n    const db = event.target.result;\n    const store = db.createObjectStore('items', { keyPath: 'id' });\n};", "snippet", "IndexedDB database open"));
        JS_SUGGESTIONS.add(sug("IndexedDB add", "const transaction = db.transaction(['{CURSOR}'], 'readwrite');\nconst store = transaction.objectStore('{CURSOR}');\nstore.add({ id: 1, data: 'value' });", "snippet", "IndexedDB add item"));
        JS_SUGGESTIONS.add(sug("matchMedia", "const mq = window.matchMedia('(max-width: 768px)');\nmq.addEventListener('change', (e) => {\n    if (e.matches) { {CURSOR} }\n});", "snippet", "CSS media query match"));
        JS_SUGGESTIONS.add(sug("ResizeObserver", "const observer = new ResizeObserver((entries) => {\n    for (const entry of entries) {\n        console.log(entry.contentRect.width);\n        {CURSOR}\n    }\n});\nobserver.observe(element);", "snippet", "ResizeObserver API"));
        JS_SUGGESTIONS.add(sug("Clipboard API", "await navigator.clipboard.writeText('{CURSOR}');\nconst text = await navigator.clipboard.readText();", "snippet", "Clipboard API read/write"));
        JS_SUGGESTIONS.add(sug("Fullscreen API", "element.requestFullscreen();\nif (document.fullscreenElement) {\n    document.exitFullscreen();\n}", "snippet", "Fullscreen API"));
        JS_SUGGESTIONS.add(sug("Drag and Drop", "element.addEventListener('dragstart', (e) => {\n    e.dataTransfer.setData('text/plain', '{CURSOR}');\n});\ndropZone.addEventListener('dragover', (e) => e.preventDefault());\ndropZone.addEventListener('drop', (e) => {\n    e.preventDefault();\n    const data = e.dataTransfer.getData('text/plain');\n    console.log(data);\n});", "snippet", "HTML5 Drag and Drop"));
        JS_SUGGESTIONS.add(sug("Geolocation API", "navigator.geolocation.getCurrentPosition(\n    (pos) => {\n        const { latitude, longitude } = pos.coords;\n        {CURSOR}\n    },\n    (err) => console.error(err)\n);", "snippet", "Geolocation API"));
        JS_SUGGESTIONS.add(sug("Notification API", "Notification.requestPermission().then((perm) => {\n    if (perm === 'granted') {\n        new Notification('{CURSOR}', { body: 'Message' });\n    }\n});", "snippet", "Web Notification API"));
        JS_SUGGESTIONS.add(sug("Page Visibility", "document.addEventListener('visibilitychange', () => {\n    if (document.hidden) {\n        console.log('Page hidden');\n    } else {\n        console.log('Page visible');\n        {CURSOR}\n    }\n});", "snippet", "Page Visibility API"));
        JS_SUGGESTIONS.add(sug("IntersectionObserver", "const observer = new IntersectionObserver((entries) => {\n    entries.forEach(entry => {\n        if (entry.isIntersecting) {\n            console.log('Visible:', entry.target);\n            {CURSOR}\n        }\n    });\n}, { threshold: 0.5 });\nobserver.observe(element);", "snippet", "IntersectionObserver API"));
        JS_SUGGESTIONS.add(sug("MutationObserver", "const observer = new MutationObserver((mutations) => {\n    mutations.forEach((m) => {\n        console.log('Mutation:', m.type);\n        {CURSOR}\n    });\n});\nobserver.observe(target, { childList: true, subtree: true });", "snippet", "MutationObserver API"));
        JS_SUGGESTIONS.add(sug("Web Speech", "const utter = new SpeechSynthesisUtterance('{CURSOR}');\nspeechSynthesis.speak(utter);", "snippet", "Web Speech API"));
        JS_SUGGESTIONS.add(sug("history.pushState", "history.pushState({ page: 1 }, '', '{CURSOR}');", "keyword", "History pushState"));
        JS_SUGGESTIONS.add(sug("history.replaceState", "history.replaceState({ page: 1 }, '', '{CURSOR}');", "keyword", "History replaceState"));
        JS_SUGGESTIONS.add(sug("window.open", "window.open('{CURSOR}', '_blank', 'width=800,height=600');", "snippet", "Window open popup"));
        JS_SUGGESTIONS.add(sug("window.postMessage", "window.postMessage({ type: '{CURSOR}', data: {} }, '*');", "snippet", "Cross-origin postMessage"));
        JS_SUGGESTIONS.add(sug("window.addEventListener", "window.addEventListener('{CURSOR}', (event) => {\n    console.log(event);\n});", "snippet", "Window event listener"));

        // ---- React Hooks (advanced) ----
        JS_SUGGESTIONS.add(sug("useRef with input", "const inputRef = useRef(null);\n\n// ...\n<input ref={inputRef} />\n\n// Focus input\ninputRef.current.focus();{CURSOR}", "snippet", "React useRef for input focus"));
        JS_SUGGESTIONS.add(sug("useReducer complex", "const initialState = { count: 0, {CURSOR}: '' };\n\nfunction reducer(state, action) {\n    switch (action.type) {\n        case 'INCREMENT':\n            return { ...state, count: state.count + 1 };\n        default:\n            return state;\n    }\n}\n\nconst [state, dispatch] = useReducer(reducer, initialState);", "snippet", "React complex useReducer pattern"));
        JS_SUGGESTIONS.add(sug("custom hook", "function use{CURSOR}(initialValue) {\n    const [value, setValue] = useState(initialValue);\n    \n    useEffect(() => {\n        \n    }, [value]);\n    \n    return [value, setValue];\n}", "snippet", "React custom hook pattern"));
        JS_SUGGESTIONS.add(sug("useLocalStorage hook", "function useLocalStorage(key, initialValue) {\n    const [storedValue, setStoredValue] = useState(() => {\n        try {\n            const item = window.localStorage.getItem(key);\n            return item ? JSON.parse(item) : initialValue;\n        } catch (error) {\n            return initialValue;\n        }\n    });\n    \n    const setValue = (value) => {\n        try {\n            const valueToStore = value instanceof Function ? value(storedValue) : value;\n            setStoredValue(valueToStore);\n            window.localStorage.setItem(key, JSON.stringify(valueToStore));\n        } catch (error) {\n            console.error(error);\n        }\n    };\n    \n    return [storedValue, setValue];\n}{CURSOR}", "snippet", "React useLocalStorage custom hook"));
        JS_SUGGESTIONS.add(sug("useDebounce hook", "function useDebounce(value, delay) {\n    const [debouncedValue, setDebouncedValue] = useState(value);\n    \n    useEffect(() => {\n        const handler = setTimeout(() => {\n            setDebouncedValue(value);\n        }, delay);\n        \n        return () => clearTimeout(handler);\n    }, [value, delay]);\n    \n    return debouncedValue;\n}{CURSOR}", "snippet", "React useDebounce custom hook"));
        JS_SUGGESTIONS.add(sug("useMediaQuery hook", "function useMediaQuery(query) {\n    const [matches, setMatches] = useState(window.matchMedia(query).matches);\n    \n    useEffect(() => {\n        const mq = window.matchMedia(query);\n        const handler = (e) => setMatches(e.matches);\n        mq.addEventListener('change', handler);\n        return () => mq.removeEventListener('change', handler);\n    }, [query]);\n    \n    return matches;\n}{CURSOR}", "snippet", "React useMediaQuery custom hook"));
        JS_SUGGESTIONS.add(sug("useIntersectionObserver hook", "function useIntersectionObserver(ref, options) {\n    const [isIntersecting, setIsIntersecting] = useState(false);\n    \n    useEffect(() => {\n        const observer = new IntersectionObserver(([entry]) => {\n            setIsIntersecting(entry.isIntersecting);\n        }, options);\n        \n        if (ref.current) observer.observe(ref.current);\n        return () => observer.disconnect();\n    }, [ref, options]);\n    \n    return isIntersecting;\n}{CURSOR}", "snippet", "React useIntersectionObserver custom hook"));
        JS_SUGGESTIONS.add(sug("useEffectOnce hook", "function useEffectOnce(callback) {\n    const calledRef = useRef(false);\n    \n    useEffect(() => {\n        if (calledRef.current) return;\n        calledRef.current = true;\n        callback();\n    }, [callback]);\n}{CURSOR}", "snippet", "React useEffectOnce custom hook"));

        // ---- Alpine.js components ----
        JS_SUGGESTIONS.add(sug("Alpine x-data component", "<div x-data=\"{ open: false, count: 0, {CURSOR}: '' }\">\n    <button @click=\"open = !open\">Toggle</button>\n    <div x-show=\"open\">Content</div>\n</div>", "snippet", "Alpine.js component with data"));
        JS_SUGGESTIONS.add(sug("Alpine dropdown", "<div x-data=\"{ open: false }\" @click.away=\"open = false\">\n    <button @click=\"open = !open\">Dropdown</button>\n    <div x-show=\"open\" x-transition>\n        <a href=\"#\" class=\"block px-4 py-2\">{CURSOR}</a>\n    </div>\n</div>", "snippet", "Alpine.js dropdown component"));
        JS_SUGGESTIONS.add(sug("Alpine modal", "<div x-data=\"{ show: false }\">\n    <button @click=\"show = true\">Open Modal</button>\n    <div x-show=\"show\" class=\"fixed inset-0\">\n        <div @click=\"show = false\" class=\"fixed inset-0 bg-black/50\"></div>\n        <div class=\"bg-white p-6 rounded-lg\">\n            <h2>{CURSOR}</h2>\n            <button @click=\"show = false\">Close</button>\n        </div>\n    </div>\n</div>", "snippet", "Alpine.js modal component"));
        JS_SUGGESTIONS.add(sug("Alpine tabs", "<div x-data=\"{ tab: 'first' }\">\n    <div class=\"flex\">\n        <button @click=\"tab = 'first'\" :class=\"{ 'active': tab === 'first' }\">First</button>\n        <button @click=\"tab = 'second'\" :class=\"{ 'active': tab === 'second' }\">Second</button>\n    </div>\n    <div x-show=\"tab === 'first'\">{CURSOR}</div>\n    <div x-show=\"tab === 'second'\">Content 2</div>\n</div>", "snippet", "Alpine.js tabs component"));
        JS_SUGGESTIONS.add(sug("Alpine form", "<form x-data=\"{ email: '', password: '', errors: {} }\" @submit.prevent=\"console.log(email, password)\">\n    <input x-model=\"email\" type=\"email\" placeholder=\"Email\" />\n    <input x-model=\"password\" type=\"password\" placeholder=\"Password\" />\n    <button type=\"submit\">{CURSOR}</button>\n</form>", "snippet", "Alpine.js form component"));
        JS_SUGGESTIONS.add(sug("Alpine store", "document.addEventListener('alpine:init', () => {\n    Alpine.store('{CURSOR}', {\n        items: [],\n        init() {},\n        add(item) {\n            this.items.push(item);\n        }\n    });\n});", "snippet", "Alpine.js global store"));
        JS_SUGGESTIONS.add(sug("Alpine x-init fetch", "<div x-data=\"{ users: [] }\" x-init=\"users = await (await fetch('/{CURSOR}')).json()\">\n    <template x-for=\"user in users\" :key=\"user.id\">\n        <div x-text=\"user.name\"></div>\n    </template>\n</div>", "snippet", "Alpine.js fetch on init"));
        JS_SUGGESTIONS.add(sug("Alpine x-modelable", "<div x-data=\"{ count: 0 }\" x-modelable=\"count\">\n    <button @click=\"count++\">+</button>\n    <span x-text=\"count\"></span>\n</div>", "snippet", "Alpine.js x-modelable directive"));

        // ---- Next.js additions ----
        JS_SUGGESTIONS.add(sug("Next.js API route", "export default function handler(req, res) {\n    if (req.method === 'GET') {\n        res.status(200).json({ message: '{CURSOR}' });\n    } else {\n        res.status(405).end();\n    }\n}", "snippet", "Next.js API route handler"));
        JS_SUGGESTIONS.add(sug("Next.js middleware", "import { NextResponse } from 'next/server';\n\nexport function middleware(request) {\n    const token = request.cookies.get('token');\n    if (!token) {\n        return NextResponse.redirect(new URL('/login', request.url));\n    }\n    return NextResponse.next();\n}{CURSOR}", "snippet", "Next.js middleware"));
        JS_SUGGESTIONS.add(sug("Next.js dynamic route", "export default function Page({ params }) {\n    return <div>ID: {params.{CURSOR}}</div>;\n}", "snippet", "Next.js dynamic route [param]"));
        JS_SUGGESTIONS.add(sug("Next.js loading", "export default function Loading() {\n    return <div>Loading...{CURSOR}</div>;\n}", "snippet", "Next.js loading component"));
        JS_SUGGESTIONS.add(sug("Next.js error", "'use client';\n\nexport default function Error({ error, reset }) {\n    return <div>Error: {error.message}{CURSOR}</div>;\n}", "snippet", "Next.js error component"));
        JS_SUGGESTIONS.add(sug("Next.js generateStaticParams", "export async function generateStaticParams() {\n    const posts = await fetch('{CURSOR}').then(r => r.json());\n    return posts.map((post) => ({ id: post.id }));\n}", "snippet", "Next.js generateStaticParams SSG"));
        JS_SUGGESTIONS.add(sug("Next.js revalidate", "export const revalidate = 60;", "keyword", "Next.js ISR revalidate interval"));

        // ---- Vue Composition API additions ----
        JS_SUGGESTIONS.add(sug("Vue watchEffect", "watchEffect(() => {\n    console.log(state.{CURSOR});\n});", "snippet", "Vue Composition API watchEffect"));
        JS_SUGGESTIONS.add(sug("Vue provide/inject", "// Parent:\nimport { provide, ref } from 'vue';\nconst shared = ref('{CURSOR}');\nprovide('key', shared);\n\n// Child:\nimport { inject } from 'vue';\nconst shared = inject('key');", "snippet", "Vue provide/inject composition API"));
        JS_SUGGESTIONS.add(sug("Vue Teleport", "<Teleport to=\"body\">\n    <div class=\"modal\">\n        <h2>{CURSOR}</h2>\n    </div>\n</Teleport>", "snippet", "Vue Teleport component"));
        JS_SUGGESTIONS.add(sug("Vue KeepAlive", "<KeepAlive include=\"ComponentA\">\n    <component :is=\"currentComponent\" />{CURSOR}\n</KeepAlive>", "snippet", "Vue KeepAlive component"));
        JS_SUGGESTIONS.add(sug("Vue Transition", "<Transition name=\"fade\" mode=\"out-in\">\n    <div :key=\"{CURSOR}\">Content</div>\n</Transition>", "snippet", "Vue Transition component"));
        JS_SUGGESTIONS.add(sug("Vue TransitionGroup", "<TransitionGroup name=\"list\" tag=\"ul\">\n    <li v-for=\"item in items\" :key=\"item.id\">{{ item.{CURSOR} }}</li>\n</TransitionGroup>", "snippet", "Vue TransitionGroup component"));
        JS_SUGGESTIONS.add(sug("Vue Suspense", "<Suspense>\n    <template #default>\n        <AsyncComponent />{CURSOR}\n    </template>\n    <template #fallback>\n        <div>Loading...</div>\n    </template>\n</Suspense>", "snippet", "Vue Suspense component"));
        JS_SUGGESTIONS.add(sug("Pinia store", "import { defineStore } from 'pinia';\n\nexport const use{CURSOR}Store = defineStore('{CURSOR}', () => {\n    const count = ref(0);\n    const doubleCount = computed(() => count.value * 2);\n    \n    function increment() {\n        count.value++;\n    }\n    \n    return { count, doubleCount, increment };\n});", "snippet", "Pinia composition API store"));
        JS_SUGGESTIONS.add(sug("Pinia store options", "import { defineStore } from 'pinia';\n\nexport const use{CURSOR}Store = defineStore('{CURSOR}', {\n    state: () => ({ count: 0 }),\n    getters: { doubleCount: (state) => state.count * 2 },\n    actions: { increment() { this.count++; } }\n});", "snippet", "Pinia options API store"));
        JS_SUGGESTIONS.add(sug("Vue Router composition", "import { useRouter, useRoute } from 'vue-router';\n\nconst router = useRouter();\nconst route = useRoute();\nrouter.push({ name: '{CURSOR}' });", "snippet", "Vue Router composition API"));
    }

    // ========================================================================
    //   DROIDSCRIPT (Android JS) API (loaded from droidscript.dat)
    // ========================================================================
    private static void loadDroidScript() {
        try (InputStream is = CompletionProvider.class.getResourceAsStream("droidscript.dat");
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                int p1 = line.indexOf('|');
                int p2 = line.indexOf('|', p1 + 1);
                if (p1 > 0 && p2 > p1) {
                    String label = line.substring(0, p1);
                    String insert = line.substring(p1 + 1, p2);
                    String desc = line.substring(p2 + 1);
                    JS_SUGGESTIONS.add(new Suggestion(label, insert, "droidscript", desc));
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load droidscript.dat: " + e.getMessage());
        }
    }

    // ========================================================================
    //   JAVA
    // ========================================================================
    private static void loadJava() {
        String[] javaKeywords = {
            "public", "private", "protected", "class", "interface", "enum", "extends", "implements",
            "abstract", "static", "final", "synchronized", "volatile", "transient", "native", "strictfp",
            "new", "return", "if", "else", "for", "while", "do", "switch", "case", "break",
            "continue", "try", "catch", "finally", "throw", "throws", "assert",
            "import", "package", "super", "this",
            "void", "int", "long", "double", "float", "boolean", "char", "byte", "short",
            "String", "Object", "Integer", "Double", "Boolean", "Long", "Float", "Short", "Byte", "Character",
            "List", "ArrayList", "LinkedList",
            "Map", "HashMap", "LinkedHashMap", "TreeMap",
            "Set", "HashSet", "LinkedHashSet", "TreeSet",
            "Optional", "OptionalInt", "OptionalDouble",
            "Stream", "Collectors", "Collector",
            "Consumer", "Supplier", "Function", "Predicate", "BiFunction", "UnaryOperator", "BinaryOperator",
            "Path", "Paths", "Files",
            "System.out.println", "System.out.print", "System.err.println", "System.out.printf",
            "System.currentTimeMillis", "System.nanoTime",
            "java.util.*", "java.io.*", "java.nio.file.*", "java.util.stream.*", "java.util.function.*",
            "javafx.*", "javafx.scene.*", "javafx.fxml.*",
        };
        for (String kw : javaKeywords) {
            JAVA_SUGGESTIONS.add(sug(kw, kw, "keyword", "Java keyword / built-in type"));
        }
        JAVA_SUGGESTIONS.add(sug("public static void main", "public static void main(String[] args) {\n    {CURSOR}\n}", "snippet", "Java main method entry point"));
        JAVA_SUGGESTIONS.add(sug("class skeleton", "public class {CURSOR} {\n    \n}", "snippet", "Java class declaration skeleton"));
        JAVA_SUGGESTIONS.add(sug("enum skeleton", "public enum {CURSOR} {\n    ;\n}", "snippet", "Java enum declaration skeleton"));
        JAVA_SUGGESTIONS.add(sug("interface", "public interface {CURSOR} {\n    \n}", "snippet", "Java interface declaration"));
        JAVA_SUGGESTIONS.add(sug("record", "public record {CURSOR}( ) { }", "snippet", "Java record declaration (Java 14+)"));
        JAVA_SUGGESTIONS.add(sug("for loop", "for (int i = 0; i < {CURSOR}; i++) {\n    \n}", "snippet", "Java for loop"));
        JAVA_SUGGESTIONS.add(sug("enhanced for", "for ({CURSOR} : ) {\n    \n}", "snippet", "Java enhanced for-each loop"));
        JAVA_SUGGESTIONS.add(sug("while loop", "while ({CURSOR}) {\n    \n}", "snippet", "Java while loop"));
        JAVA_SUGGESTIONS.add(sug("try/catch", "try {\n    {CURSOR}\n} catch (Exception e) {\n    e.printStackTrace();\n}", "snippet", "Java try/catch exception handling"));
        JAVA_SUGGESTIONS.add(sug("try-with-resources", "try ({CURSOR}) {\n    \n} catch (Exception e) {\n    e.printStackTrace();\n}", "snippet", "Java try-with-resources pattern"));
        JAVA_SUGGESTIONS.add(sug("if null check", "if ({CURSOR} != null) {\n    \n}", "snippet", "Java null check guard clause"));
        JAVA_SUGGESTIONS.add(sug("Optional orElse", "Optional.ofNullable({CURSOR}).orElse()", "snippet", "Java Optional orElse pattern"));
        JAVA_SUGGESTIONS.add(sug("Optional orElseThrow", "Optional.ofNullable({CURSOR}).orElseThrow(() -> new RuntimeException())", "snippet", "Java Optional orElseThrow pattern"));
        JAVA_SUGGESTIONS.add(sug("stream forEach", "{CURSOR}.stream().forEach(item -> {\n    \n});", "snippet", "Java Stream forEach iteration"));
        JAVA_SUGGESTIONS.add(sug("stream map/collect", "{CURSOR}.stream().map(item -> {\n    return ;\n}).collect(java.util.stream.Collectors.toList());", "snippet", "Java Stream map and collect pipeline"));
        JAVA_SUGGESTIONS.add(sug("stream filter/count", "{CURSOR}.stream().filter(item -> ).count();", "snippet", "Java Stream filter and count"));
        JAVA_SUGGESTIONS.add(sug("stream sorted", "{CURSOR}.stream().sorted((a, b) -> a.compareTo(b)).collect(Collectors.toList());", "snippet", "Java Stream sorted with comparator"));
        JAVA_SUGGESTIONS.add(sug("lambda expression", "({CURSOR}) -> {\n    \n}", "snippet", "Java lambda expression"));
        JAVA_SUGGESTIONS.add(sug("method reference", "{CURSOR}::", "snippet", "Java method reference syntax"));
        JAVA_SUGGESTIONS.add(sug("builder pattern", "new {CURSOR}()\n    .with()\n    .build();", "snippet", "Java builder design pattern"));
        JAVA_SUGGESTIONS.add(sug("Runnable thread", "new Thread(() -> {\n    {CURSOR}\n}).start();", "snippet", "Java thread with lambda"));
        JAVA_SUGGESTIONS.add(sug("Timer", "new Timer().schedule(new TimerTask() {\n    @Override\n    public void run() {\n        {CURSOR}\n    }\n}, 1000);", "snippet", "Java Timer delayed execution"));
        JAVA_SUGGESTIONS.add(sug("FileReader", "try (FileReader fr = new FileReader(\"{CURSOR}\");\n     BufferedReader br = new BufferedReader(fr)) {\n    String line;\n    while ((line = br.readLine()) != null) {\n        System.out.println(line);\n    }\n} catch (IOException e) {\n    e.printStackTrace();\n}", "snippet", "Java BufferedReader file read"));
        JAVA_SUGGESTIONS.add(sug("FileWriter", "try (FileWriter fw = new FileWriter(\"{CURSOR}\")) {\n    fw.write(content);\n} catch (IOException e) {\n    e.printStackTrace();\n}", "snippet", "Java FileWriter file write"));
        JAVA_SUGGESTIONS.add(sug("Properties load", "Properties props = new Properties();\ntry (FileInputStream fis = new FileInputStream(\"{CURSOR}\")) {\n    props.load(fis);\n} catch (IOException e) {\n    e.printStackTrace();\n}", "snippet", "Java Properties file load"));
        JAVA_SUGGESTIONS.add(sug("URL connection", "try {\n    URL url = new URL(\"{CURSOR}\");\n    HttpURLConnection conn = (HttpURLConnection) url.openConnection();\n    conn.setRequestMethod(\"GET\");\n    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {\n        String line; while ((line = br.readLine()) != null) { sb.append(line); }\n    }\n} catch (IOException e) {\n    e.printStackTrace();\n}", "snippet", "Java HTTP URL connection"));
        JAVA_SUGGESTIONS.add(sug("StringBuilder loop", "StringBuilder sb = new StringBuilder();\nfor ({CURSOR} : ) {\n    sb.append(item);\n}", "snippet", "Java StringBuilder append loop"));
        JAVA_SUGGESTIONS.add(sug("switch arrow (14+)", "switch ({CURSOR}) {\n    case value -> System.out.println();\n    default -> System.out.println();\n}", "snippet", "Java switch arrow expression (Java 14+)"));
        JAVA_SUGGESTIONS.add(sug("instanceof pattern (16+)", "if (obj instanceof String {CURSOR} s) {\n    System.out.println(s.length());\n}", "snippet", "Java instanceof pattern matching (Java 16+)"));
        JAVA_SUGGESTIONS.add(sug("sealed class (17+)", "public sealed class {CURSOR} permits SubClass {\n    \n}", "snippet", "Java sealed class declaration (Java 17+)"));
        JAVA_SUGGESTIONS.add(sug("record (16+)", "public record {CURSOR}(Type field) { }", "snippet", "Java record with fields (Java 16+)"));
        JAVA_SUGGESTIONS.add(sug("Stream parallel", "{CURSOR}.stream().parallel().map(item -> {\n    return ;\n}).collect(Collectors.toList());", "snippet", "Java parallel stream pipeline"));
        JAVA_SUGGESTIONS.add(sug("Optional ifPresent", "Optional.ofNullable({CURSOR}).ifPresent(value -> {\n    \n});", "snippet", "Java Optional ifPresent consumer"));
        JAVA_SUGGESTIONS.add(sug("CompletableFuture", "CompletableFuture.supplyAsync(() -> {\n    return {CURSOR};\n}).thenAccept(result -> {\n    System.out.println(result);\n});", "snippet", "Java CompletableFuture async supply"));
        JAVA_SUGGESTIONS.add(sug("DateTimeFormatter", "DateTimeFormatter formatter = DateTimeFormatter.ofPattern(\"yyyy-MM-dd {CURSOR}\");\nString formatted = LocalDate.now().format(formatter);", "snippet", "Java DateTimeFormatter pattern"));
        JAVA_SUGGESTIONS.add(sug("Period between", "Period period = Period.between({CURSOR}, LocalDate.now());\nSystem.out.println(period.getYears() + \" years\");", "snippet", "Java Period between dates"));
        JAVA_SUGGESTIONS.add(sug("Collectors groupingBy", "Map<KeyType, List<ValueType>> grouped = {CURSOR}.stream().collect(Collectors.groupingBy(item -> item.getKey()));", "snippet", "Java Collectors groupingBy"));
        JAVA_SUGGESTIONS.add(sug("Collectors partitioningBy", "Map<Boolean, List<ValueType>> partitioned = {CURSOR}.stream().collect(Collectors.partitioningBy(item -> item.isCondition()));", "snippet", "Java Collectors partitioningBy"));
        JAVA_SUGGESTIONS.add(sug("try/catch multi", "try {\n    {CURSOR}\n} catch (IOException | SQLException e) {\n    e.printStackTrace();\n}", "snippet", "Java multi-catch exception"));
        JAVA_SUGGESTIONS.add(sug("annotation", "public @interface {CURSOR} {\n    String value() default \"\";\n}", "snippet", "Java custom annotation definition"));
    }

    // ========================================================================
    //   XML
    // ========================================================================
    private static void loadXml() {
        String[] xmlTags = {"xml", "root", "item", "element", "value", "name", "description", "config", "property", "bean", "constructor-arg", "property", "list", "map", "set", "entry", "key", "value"};
        for (String t : xmlTags) {
            XML_SUGGESTIONS.add(sug("<" + t + ">", "<" + t + ">{CURSOR}</" + t + ">", "tag", "XML tag"));
        }
        XML_SUGGESTIONS.add(sug("<?xml version?>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "snippet", "XML declaration with version and encoding"));
        XML_SUGGESTIONS.add(sug("<!-- comment -->", "<!-- {CURSOR} -->", "snippet", "XML comment"));
        XML_SUGGESTIONS.add(sug("<![CDATA[", "<![CDATA[{CURSOR}]]>", "snippet", "XML CDATA section"));
        XML_SUGGESTIONS.add(sug("<!DOCTYPE", "<!DOCTYPE {CURSOR}>", "snippet", "XML DOCTYPE declaration"));
        XML_SUGGESTIONS.add(sug("xsl stylesheet", "<?xml-stylesheet type=\"text/xsl\" href=\"{CURSOR}\"?>", "snippet", "XML XSL stylesheet processing instruction"));
    }

    // ========================================================================
    //   JSON
    // ========================================================================
    private static void loadJson() {
        JSON_SUGGESTIONS.add(sug("object {}", "{\n    \"{CURSOR}\": \n}", "keyword", "JSON object literal"));
        JSON_SUGGESTIONS.add(sug("array []", "[\n    {CURSOR}\n]", "keyword", "JSON array literal"));
        JSON_SUGGESTIONS.add(sug("string value", "\"{CURSOR}\"", "keyword", "JSON string value"));
        JSON_SUGGESTIONS.add(sug("number", "{CURSOR}", "keyword", "JSON number value"));
        JSON_SUGGESTIONS.add(sug("boolean", "{CURSOR}", "keyword", "JSON boolean value"));
        JSON_SUGGESTIONS.add(sug("null", "null", "keyword", "JSON null value"));
        JSON_SUGGESTIONS.add(sug("nested object", "{\n    \"{CURSOR}\": {\n        \n    }\n}", "snippet", "JSON nested object structure"));
    }

    // ========================================================================
    //   PHP
    // ========================================================================
    private static void loadPhp() {
        String[] phpKeywords = {
            "<?php", "?>", "echo", "print", "die", "exit", "return", "require", "require_once",
            "include", "include_once", "if", "else", "elseif", "for", "foreach", "while", "do",
            "switch", "case", "break", "continue", "function", "class", "interface", "trait",
            "abstract", "final", "public", "private", "protected", "static", "const",
            "new", "this", "self", "parent", "extends", "implements", "use", "namespace",
            "try", "catch", "finally", "throw", "instanceof", "clone",
            "true", "false", "null", "isset", "empty", "unset",
            "array", "list", "global", "var", "declare", "enddeclare",
            "and", "or", "xor", "as",
            "match", "enum", "readonly", "mixed", "never", "void", "int", "float", "string", "bool", "array|",
        };
        for (String kw : phpKeywords) {
            PHP_SUGGESTIONS.add(sug(kw, kw, "keyword", "PHP keyword / construct"));
        }

        String[][] phpFunctions = {
            {"$_GET", "$_GET['{CURSOR}']", "keyword"},
            {"$_POST", "$_POST['{CURSOR}']", "keyword"},
            {"$_SERVER", "$_SERVER['{CURSOR}']", "keyword"},
            {"$_SESSION", "$_SESSION['{CURSOR}']", "keyword"},
            {"$_COOKIE", "$_COOKIE['{CURSOR}']", "keyword"},
            {"$_FILES", "$_FILES['{CURSOR}']", "keyword"},
            {"$_REQUEST", "$_REQUEST['{CURSOR}']", "keyword"},
            {"$this->", "$this->{CURSOR}", "keyword"},
            {"self::", "self::{CURSOR}", "keyword"},
            {"parent::", "parent::{CURSOR}", "keyword"},
            {"var_dump", "var_dump({CURSOR})", "keyword"},
            {"print_r", "print_r({CURSOR})", "keyword"},
            {"explode", "explode('{CURSOR}', )", "keyword"},
            {"implode", "implode('{CURSOR}', )", "keyword"},
            {"str_replace", "str_replace('{CURSOR}', '', )", "keyword"},
            {"strpos", "strpos(, '{CURSOR}')", "keyword"},
            {"substr", "substr(, {CURSOR})", "keyword"},
            {"strlen", "strlen({CURSOR})", "keyword"},
            {"strtolower", "strtolower({CURSOR})", "keyword"},
            {"strtoupper", "strtoupper({CURSOR})", "keyword"},
            {"trim", "trim({CURSOR})", "keyword"},
            {"json_encode", "json_encode({CURSOR})", "keyword"},
            {"json_decode", "json_decode({CURSOR})", "keyword"},
            {"serialize", "serialize({CURSOR})", "keyword"},
            {"unserialize", "unserialize({CURSOR})", "keyword"},
            {"count", "count({CURSOR})", "keyword"},
            {"array_merge", "array_merge({CURSOR})", "keyword"},
            {"array_map", "array_map('{CURSOR}', )", "keyword"},
            {"array_filter", "array_filter({CURSOR})", "keyword"},
            {"array_keys", "array_keys({CURSOR})", "keyword"},
            {"array_values", "array_values({CURSOR})", "keyword"},
            {"in_array", "in_array({CURSOR}, )", "keyword"},
            {"header", "header('{CURSOR}')", "keyword"},
            {"session_start", "session_start();", "keyword"},
            {"session_destroy", "session_destroy();", "keyword"},
            {"setcookie", "setcookie('{CURSOR}', '', time() + 3600);", "keyword"},
            {"mail", "mail('{CURSOR}', '', )", "keyword"},
            {"date", "date('Y-m-d {CURSOR}')", "keyword"},
            {"time", "time()", "keyword"},
            {"strtotime", "strtotime('{CURSOR}')", "keyword"},
            {"file_get_contents", "file_get_contents('{CURSOR}')", "keyword"},
            {"file_put_contents", "file_put_contents('{CURSOR}', )", "keyword"},
            {"fopen", "fopen('{CURSOR}', 'r')", "keyword"},
            {"fwrite", "fwrite({CURSOR}, )", "keyword"},
            {"fclose", "fclose({CURSOR})", "keyword"},
            {"pdo connect", "new PDO('mysql:host=localhost;dbname={CURSOR}', 'root', '')", "keyword"},
            {"mysqli connect", "new mysqli('localhost', 'root', '', '{CURSOR}')", "keyword"},
            {"htmlspecialchars", "htmlspecialchars({CURSOR}, ENT_QUOTES, 'UTF-8')", "keyword"},
            {"htmlentities", "htmlentities({CURSOR})", "keyword"},
            {"strip_tags", "strip_tags({CURSOR})", "keyword"},
            {"filter_var", "filter_var({CURSOR}, FILTER_VALIDATE_EMAIL)", "keyword"},
            {"password_hash", "password_hash({CURSOR}, PASSWORD_BCRYPT)", "keyword"},
            {"password_verify", "password_verify({CURSOR}, )", "keyword"},
            {"hash", "hash('sha256', {CURSOR})", "keyword"},
            {"uniqid", "uniqid('{CURSOR}', true)", "keyword"},
            {"random_bytes", "random_bytes({CURSOR})", "keyword"},
            {"random_int", "random_int(0, {CURSOR})", "keyword"},
            {"is_null", "is_null({CURSOR})", "keyword"},
            {"is_array", "is_array({CURSOR})", "keyword"},
            {"is_string", "is_string({CURSOR})", "keyword"},
            {"is_numeric", "is_numeric({CURSOR})", "keyword"},
            {"method_exists", "method_exists({CURSOR}, '')", "keyword"},
            {"property_exists", "property_exists({CURSOR}, '')", "keyword"},
            {"class_exists", "class_exists('{CURSOR}')", "keyword"},
            {"interface_exists", "interface_exists('{CURSOR}')", "keyword"},
            {"trait_exists", "trait_exists('{CURSOR}')", "keyword"},
        };
        for (String[] g : phpFunctions) {
            PHP_SUGGESTIONS.add(sug(g[0], g[1], g[2], "PHP function / superglobal"));
        }

        PHP_SUGGESTIONS.add(sug("class skeleton", "class {CURSOR} {\n    public function __construct() {\n        \n    }\n}", "snippet", "PHP class skeleton with constructor"));
        PHP_SUGGESTIONS.add(sug("class with properties", "class {CURSOR} {\n    private $property;\n    \n    public function __construct($property) {\n        $this->property = $property;\n    }\n    \n    public function getProperty() {\n        return $this->property;\n    }\n}", "snippet", "PHP class with properties and getter"));
        PHP_SUGGESTIONS.add(sug("function", "function {CURSOR}($param) {\n    \n}", "snippet", "PHP function declaration"));
        PHP_SUGGESTIONS.add(sug("foreach", "foreach (${CURSOR} as $key => $value) {\n    \n}", "snippet", "PHP foreach loop"));
        PHP_SUGGESTIONS.add(sug("form handler", "if ($_SERVER['REQUEST_METHOD'] === 'POST') {\n    ${CURSOR} = $_POST['field'];\n}", "snippet", "PHP form POST handler pattern"));
        PHP_SUGGESTIONS.add(sug("pdo query", "$stmt = $pdo->prepare('SELECT * FROM {CURSOR} WHERE id = ?');\n$stmt->execute([$id]);\n$result = $stmt->fetchAll();", "snippet", "PHP PDO prepared statement query"));
        PHP_SUGGESTIONS.add(sug("try/catch PDO", "try {\n    {CURSOR}\n} catch (PDOException $e) {\n    echo 'Error: ' . $e->getMessage();\n}", "snippet", "PHP PDO exception handling"));
        PHP_SUGGESTIONS.add(sug("namespace", "namespace App\\{CURSOR};", "snippet", "PHP namespace declaration"));
        PHP_SUGGESTIONS.add(sug("trait", "trait {CURSOR} {\n    \n}", "snippet", "PHP trait declaration"));
        PHP_SUGGESTIONS.add(sug("enum (PHP 8.1)", "enum {CURSOR}: string {\n    case = '';\n}", "snippet", "PHP enum declaration (PHP 8.1+)"));
        PHP_SUGGESTIONS.add(sug("anonymous class", "$object = new class {\n    public function {CURSOR}() {\n        \n    }\n};", "snippet", "PHP anonymous class"));
        PHP_SUGGESTIONS.add(sug("arrow function", "$fn = fn(${CURSOR}) => expr;", "snippet", "PHP arrow function (PHP 7.4+)"));
        PHP_SUGGESTIONS.add(sug("null coalescing", "${CURSOR} = $var ?? 'default';", "snippet", "PHP null coalescing operator"));
        PHP_SUGGESTIONS.add(sug("nullsafe", "${CURSOR} = $user?->getAddress()?->getCity();", "snippet", "PHP nullsafe operator (PHP 8.0+)"));
        PHP_SUGGESTIONS.add(sug("match expression", "$result = match ({CURSOR}) {\n    'a' => 1,\n    'b' => 2,\n    default => 0,\n};", "snippet", "PHP match expression (PHP 8.0+)"));
        PHP_SUGGESTIONS.add(sug("named arguments", "function_name({CURSOR}: value, param2: value2);", "snippet", "PHP named arguments (PHP 8.0+)"));
        PHP_SUGGESTIONS.add(sug("constructor promotion", "class {CURSOR} {\n    public function __construct(\n        private string $name,\n        private int $age = 0\n    ) {}\n}", "snippet", "PHP constructor property promotion (PHP 8.0+)"));
        PHP_SUGGESTIONS.add(sug("str_contains", "if (str_contains({CURSOR}, 'needle')) {\n    echo 'found';\n}", "snippet", "PHP str_contains (PHP 8.0+)"));
        PHP_SUGGESTIONS.add(sug("str_starts_with", "if (str_starts_with({CURSOR}, 'prefix')) {\n    echo 'yes';\n}", "snippet", "PHP str_starts_with (PHP 8.0+)"));
        PHP_SUGGESTIONS.add(sug("fiber", "$fiber = new Fiber(function (): void {\n    ${CURSOR} = Fiber::suspend();\n});", "snippet", "PHP Fiber coroutine (PHP 8.1+)"));
    }

    // ========================================================================
    //   PYTHON
    // ========================================================================
    private static void loadPython() {
        String[] pyKeywords = {
            "def", "class", "return", "if", "elif", "else", "for", "while", "break", "continue",
            "try", "except", "finally", "raise", "with", "as", "import", "from", "as",
            "pass", "yield", "lambda", "global", "nonlocal", "assert", "del",
            "True", "False", "None", "self", "cls",
            "and", "or", "not", "in", "is",
            "async", "await", "match", "case",
            "print", "len", "range", "type", "isinstance", "hasattr", "getattr", "setattr",
            "list", "dict", "set", "tuple", "str", "int", "float", "bool", "bytes", "bytearray",
            "super", "property", "staticmethod", "classmethod",
            "open", "zip", "map", "filter", "sorted", "enumerate", "reversed",
            "min", "max", "sum", "abs", "round", "pow",
            "__init__", "__str__", "__repr__", "__len__", "__getitem__", "__setitem__",
            "__call__", "__enter__", "__exit__", "__iter__", "__next__",
        };
        for (String kw : pyKeywords) {
            PYTHON_SUGGESTIONS.add(sug(kw, kw, "keyword", "Python keyword / built-in"));
        }

        String[][] pyFunctions = {
            {"print()", "print({CURSOR})", "keyword"},
            {"len()", "len({CURSOR})", "keyword"},
            {"range()", "range({CURSOR})", "keyword"},
            {"type()", "type({CURSOR})", "keyword"},
            {"isinstance()", "isinstance({CURSOR}, )", "keyword"},
            {"open()", "open('{CURSOR}', 'r')", "keyword"},
            {"with open", "with open('{CURSOR}', 'r') as f:\n    content = f.read()", "keyword"},
            {"list comprehension", "[x for x in {CURSOR}]", "keyword"},
            {"dict comprehension", "{k: v for k, v in {CURSOR}}", "keyword"},
            {"lambda", "lambda x: {CURSOR}", "keyword"},
            {"map()", "map(lambda x: {CURSOR}, iterable)", "keyword"},
            {"filter()", "filter(lambda x: {CURSOR}, iterable)", "keyword"},
            {"sorted()", "sorted({CURSOR}, key=lambda x: x)", "keyword"},
            {"enumerate()", "enumerate({CURSOR})", "keyword"},
            {"zip()", "zip({CURSOR})", "keyword"},
            {"any()", "any({CURSOR})", "keyword"},
            {"all()", "all({CURSOR})", "keyword"},
            {"sum()", "sum({CURSOR})", "keyword"},
            {"join()", "'{CURSOR}'.join(list)", "keyword"},
            {"split()", ".split('{CURSOR}')", "keyword"},
            {"strip()", ".strip()", "keyword"},
            {"replace()", ".replace('{CURSOR}', '')", "keyword"},
            {"startswith()", ".startswith('{CURSOR}')", "keyword"},
            {"endswith()", ".endswith('{CURSOR}')", "keyword"},
            {"find()", ".find('{CURSOR}')", "keyword"},
            {"append()", ".append({CURSOR})", "keyword"},
            {"extend()", ".extend({CURSOR})", "keyword"},
            {"pop()", ".pop({CURSOR})", "keyword"},
            {"remove()", ".remove({CURSOR})", "keyword"},
            {"insert()", ".insert({CURSOR})", "keyword"},
            {"sort()", ".sort(key=lambda x: {CURSOR})", "keyword"},
            {"reverse()", ".reverse()", "keyword"},
            {"copy()", ".copy()", "keyword"},
            {"clear()", ".clear()", "keyword"},
            {"keys()", ".keys()", "keyword"},
            {"values()", ".values()", "keyword"},
            {"items()", ".items()", "keyword"},
            {"get()", ".get('{CURSOR}')", "keyword"},
            {"pop() dict", ".pop('{CURSOR}')", "keyword"},
            {"update()", ".update({CURSOR})", "keyword"},
            {"setdefault()", ".setdefault('{CURSOR}', )", "keyword"},
            {"os.path.join", "os.path.join('{CURSOR}', )", "keyword"},
            {"os.path.exists", "os.path.exists('{CURSOR}')", "keyword"},
            {"os.listdir", "os.listdir('{CURSOR}')", "keyword"},
            {"os.makedirs", "os.makedirs('{CURSOR}', exist_ok=True)", "keyword"},
            {"os.getcwd", "os.getcwd()", "keyword"},
            {"os.environ", "os.environ.get('{CURSOR}')", "keyword"},
            {"sys.argv", "sys.argv[{CURSOR}]", "keyword"},
            {"sys.exit", "sys.exit({CURSOR})", "keyword"},
            {"re.search", "re.search(r'{CURSOR}', text)", "keyword"},
            {"re.match", "re.match(r'{CURSOR}', text)", "keyword"},
            {"re.findall", "re.findall(r'{CURSOR}', text)", "keyword"},
            {"re.sub", "re.sub(r'{CURSOR}', '', text)", "keyword"},
            {"datetime.now", "datetime.datetime.now()", "keyword"},
            {"datetime.strftime", ".strftime('%Y-%m-%d {CURSOR}')", "keyword"},
            {"random.random", "random.random()", "keyword"},
            {"random.randint", "random.randint(0, {CURSOR})", "keyword"},
            {"random.choice", "random.choice({CURSOR})", "keyword"},
            {"json.dumps", "json.dumps({CURSOR}, indent=2)", "keyword"},
            {"json.loads", "json.loads({CURSOR})", "keyword"},
            {"json.dump", "json.dump({CURSOR}, f, indent=2)", "keyword"},
            {"json.load", "json.load(f)", "keyword"},
            {"math.sqrt", "math.sqrt({CURSOR})", "keyword"},
            {"math.ceil", "math.ceil({CURSOR})", "keyword"},
            {"math.floor", "math.floor({CURSOR})", "keyword"},
            {"math.pi", "math.pi", "keyword"},
            {"collections.Counter", "collections.Counter({CURSOR})", "keyword"},
            {"collections.defaultdict", "collections.defaultdict({CURSOR})", "keyword"},
            {"collections.deque", "collections.deque({CURSOR})", "keyword"},
            {"itertools.chain", "itertools.chain({CURSOR})", "keyword"},
            {"itertools.product", "itertools.product({CURSOR})", "keyword"},
            {"functools.reduce", "functools.reduce(lambda a, b: {CURSOR}, iterable)", "keyword"},
            {"functools.lru_cache", "@functools.lru_cache(maxsize=None)\ndef {CURSOR}():\n    ", "keyword"},
            {"dataclass", "@dataclass\nclass {CURSOR}:\n    ", "keyword"},
        };
        for (String[] g : pyFunctions) {
            PYTHON_SUGGESTIONS.add(sug(g[0], g[1], g[2], "Python function / expression"));
        }

        PYTHON_SUGGESTIONS.add(sug("class skeleton", "class {CURSOR}:\n    def __init__(self):\n        pass", "snippet", "Python class skeleton with constructor"));
        PYTHON_SUGGESTIONS.add(sug("function", "def {CURSOR}(params):\n    pass", "snippet", "Python function declaration"));
        PYTHON_SUGGESTIONS.add(sug("if __name__", "if __name__ == '__main__':\n    {CURSOR}", "snippet", "Python main guard pattern"));
        PYTHON_SUGGESTIONS.add(sug("for loop", "for {CURSOR} in range(10):\n    ", "snippet", "Python for loop"));
        PYTHON_SUGGESTIONS.add(sug("try/except", "try:\n    {CURSOR}\nexcept Exception as e:\n    print(e)", "snippet", "Python try/except exception handling"));
        PYTHON_SUGGESTIONS.add(sug("with open read", "with open('{CURSOR}', 'r') as f:\n    content = f.read()", "snippet", "Python file read with context manager"));
        PYTHON_SUGGESTIONS.add(sug("with open write", "with open('{CURSOR}', 'w') as f:\n    f.write('')", "snippet", "Python file write with context manager"));
        PYTHON_SUGGESTIONS.add(sug("list comprehension", "[{CURSOR} for x in iterable]", "snippet", "Python list comprehension"));
        PYTHON_SUGGESTIONS.add(sug("generator", "def {CURSOR}():\n    yield ", "snippet", "Python generator function"));
        PYTHON_SUGGESTIONS.add(sug("decorator", "def {CURSOR}(func):\n    def wrapper(*args, **kwargs):\n        return func(*args, **kwargs)\n    return wrapper", "snippet", "Python decorator pattern"));
        PYTHON_SUGGESTIONS.add(sug("async def", "async def {CURSOR}():\n    await ", "snippet", "Python async function"));
        PYTHON_SUGGESTIONS.add(sug("FastAPI route", "@app.get('/{CURSOR}')\nasync def route():\n    return {}", "snippet", "FastAPI GET route handler"));
        PYTHON_SUGGESTIONS.add(sug("Django model", "class {CURSOR}(models.Model):\n    name = models.CharField(max_length=100)", "snippet", "Django model class"));
        PYTHON_SUGGESTIONS.add(sug("Flask route", "@app.route('/{CURSOR}')\ndef route():\n    return render_template('')", "snippet", "Flask route handler"));
        PYTHON_SUGGESTIONS.add(sug("type hint", "def {CURSOR}(param: str) -> None:\n    pass", "snippet", "Python type-hinted function"));
        PYTHON_SUGGESTIONS.add(sug("set comprehension", "{x for x in {CURSOR}}", "snippet", "Python set comprehension"));
        PYTHON_SUGGESTIONS.add(sug("error handling", "try:\n    {CURSOR}\nexcept ValueError as e:\n    print(f\"Error: {e}\")\nexcept Exception as e:\n    print(f\"Unexpected: {e}\")", "snippet", "Python multi-except handling"));
        PYTHON_SUGGESTIONS.add(sug("else clause", "for item in {CURSOR}:\n    if condition:\n        break\nelse:\n    print(\"No break occurred\")", "snippet", "Python for-else pattern"));
        PYTHON_SUGGESTIONS.add(sug("contextmanager", "from contextlib import contextmanager\n\n@contextmanager\ndef {CURSOR}():\n    try:\n        yield\n    finally:\n        ", "snippet", "Python context manager decorator"));
        PYTHON_SUGGESTIONS.add(sug("dataclass", "@dataclass\nclass {CURSOR}:\n    name: str\n    value: int = 0", "snippet", "Python dataclass with default"));
        PYTHON_SUGGESTIONS.add(sug("singledispatch", "from functools import singledispatch\n\n@singledispatch\ndef {CURSOR}(arg):\n    raise NotImplementedError\n\n@{CURSOR}.register\ndef _(arg: int):\n    return arg * 2", "snippet", "Python singledispatch generic function"));
        PYTHON_SUGGESTIONS.add(sug("pathlib", "from pathlib import Path\n\npath = Path(\"{CURSOR}\")\nfor file in path.glob(\"*.py\"):\n    print(file.name)", "snippet", "Python pathlib directory iteration"));
        PYTHON_SUGGESTIONS.add(sug("argparse", "import argparse\n\nparser = argparse.ArgumentParser(description='{CURSOR}')\nparser.add_argument('--name', type=str, help='Name')\nargs = parser.parse_args()", "snippet", "Python argparse CLI template"));
        PYTHON_SUGGESTIONS.add(sug("logging", "import logging\n\nlogging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')\nlogger = logging.getLogger(__name__)\nlogger.info('{CURSOR}')", "snippet", "Python logging setup"));
        PYTHON_SUGGESTIONS.add(sug("pytest", "import pytest\n\ndef test_{CURSOR}():\n    assert True", "snippet", "Python pytest test function"));
        PYTHON_SUGGESTIONS.add(sug("threading", "import threading\n\ndef worker():\n    {CURSOR}\n\nthread = threading.Thread(target=worker)\nthread.start()\nthread.join()", "snippet", "Python threading pattern"));
        PYTHON_SUGGESTIONS.add(sug("subprocess", "import subprocess\n\nresult = subprocess.run(['{CURSOR}'], capture_output=True, text=True)\nprint(result.stdout)", "snippet", "Python subprocess run"));
        PYTHON_SUGGESTIONS.add(sug("shutil", "import shutil\n\nshutil.copy2('{CURSOR}', 'destination')", "snippet", "Python shutil file copy"));
        PYTHON_SUGGESTIONS.add(sug("csv reader", "import csv\n\nwith open('{CURSOR}', 'r', newline='') as f:\n    reader = csv.DictReader(f)\n    for row in reader:\n        print(row)", "snippet", "Python CSV DictReader"));
    }

    // ========================================================================
    //   SQL
    // ========================================================================
    private static void loadSql() {
        String[] sqlKeywords = {
            "SELECT", "FROM", "WHERE", "INSERT INTO", "VALUES", "UPDATE", "SET", "DELETE", "CREATE TABLE",
            "ALTER TABLE", "DROP TABLE", "TRUNCATE", "INSERT", "INTO", "JOIN", "LEFT JOIN", "RIGHT JOIN",
            "INNER JOIN", "FULL OUTER JOIN", "CROSS JOIN", "ON", "AND", "OR", "NOT", "IN", "BETWEEN",
            "LIKE", "IS NULL", "IS NOT NULL", "EXISTS", "HAVING", "GROUP BY", "ORDER BY", "ASC", "DESC",
            "LIMIT", "OFFSET", "UNION", "UNION ALL", "DISTINCT", "AS", "CASE", "WHEN", "THEN", "ELSE", "END",
            "COUNT", "SUM", "AVG", "MIN", "MAX", "COALESCE", "NULLIF",
            "INDEX", "CREATE INDEX", "PRIMARY KEY", "FOREIGN KEY", "REFERENCES", "UNIQUE", "CHECK", "DEFAULT",
            "VARCHAR", "INT", "INTEGER", "BIGINT", "SMALLINT", "TINYINT", "DECIMAL", "NUMERIC", "FLOAT", "DOUBLE",
            "BOOLEAN", "DATE", "DATETIME", "TIMESTAMP", "TEXT", "BLOB", "ENUM", "SERIAL",
            "BEGIN", "COMMIT", "ROLLBACK", "SAVEPOINT",
            "GRANT", "REVOKE", "CREATE USER",
            "EXPLAIN", "DESCRIBE", "SHOW",
        };
        for (String kw : sqlKeywords) {
            SQL_SUGGESTIONS.add(sug(kw, kw, "keyword", "SQL keyword / clause"));
        }

        SQL_SUGGESTIONS.add(sug("CREATE TABLE", "CREATE TABLE {CURSOR} (\n    id INT PRIMARY KEY AUTO_INCREMENT,\n    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n);", "snippet", "SQL CREATE TABLE with auto-increment ID"));
        SQL_SUGGESTIONS.add(sug("SELECT *", "SELECT * FROM {CURSOR} WHERE ;", "snippet", "SQL SELECT all columns query"));
        SQL_SUGGESTIONS.add(sug("INSERT INTO", "INSERT INTO {CURSOR} (col1, col2) VALUES (val1, val2);", "snippet", "SQL INSERT statement"));
        SQL_SUGGESTIONS.add(sug("UPDATE", "UPDATE {CURSOR} SET col = value WHERE ;", "snippet", "SQL UPDATE statement"));
        SQL_SUGGESTIONS.add(sug("DELETE", "DELETE FROM {CURSOR} WHERE ;", "snippet", "SQL DELETE statement"));
        SQL_SUGGESTIONS.add(sug("JOIN", "SELECT * FROM {CURSOR} t1\nJOIN t2 ON t1.id = t2.foreign_id\nWHERE ;", "snippet", "SQL JOIN query pattern"));
        SQL_SUGGESTIONS.add(sug("GROUP BY", "SELECT column, COUNT(*) FROM {CURSOR}\nGROUP BY column\nHAVING COUNT(*) > 1;", "snippet", "SQL GROUP BY with HAVING clause"));
        SQL_SUGGESTIONS.add(sug("subquery", "SELECT * FROM {CURSOR}\nWHERE id IN (SELECT foreign_id FROM other_table);", "snippet", "SQL subquery in WHERE clause"));
        SQL_SUGGESTIONS.add(sug("CREATE INDEX", "CREATE INDEX idx_{CURSOR} ON table_name (column);", "snippet", "SQL CREATE INDEX statement"));
        SQL_SUGGESTIONS.add(sug("ALTER TABLE add column", "ALTER TABLE {CURSOR} ADD COLUMN column_name datatype;", "snippet", "SQL ALTER TABLE add column"));
        SQL_SUGGESTIONS.add(sug("ALTER TABLE drop column", "ALTER TABLE {CURSOR} DROP COLUMN column_name;", "snippet", "SQL ALTER TABLE drop column"));
        SQL_SUGGESTIONS.add(sug("transaction", "BEGIN;\n    {CURSOR}\nCOMMIT;", "snippet", "SQL transaction block"));
        SQL_SUGGESTIONS.add(sug("CASE WHEN", "CASE\n    WHEN {CURSOR} THEN 'value1'\n    ELSE 'default'\nEND as alias", "snippet", "SQL CASE WHEN expression"));
        SQL_SUGGESTIONS.add(sug("CTE", "WITH {CURSOR} AS (\n    SELECT * FROM table\n)\nSELECT * FROM cte;", "snippet", "SQL Common Table Expression (WITH)"));
        SQL_SUGGESTIONS.add(sug("window function", "SELECT column, ROW_NUMBER() OVER (PARTITION BY group_col ORDER BY sort_col) AS rn\nFROM {CURSOR};", "snippet", "SQL ROW_NUMBER window function"));
        SQL_SUGGESTIONS.add(sug("RANK", "SELECT column, RANK() OVER (ORDER BY {CURSOR}) AS rank\nFROM table;", "snippet", "SQL RANK window function"));
        SQL_SUGGESTIONS.add(sug("EXISTS", "SELECT * FROM {CURSOR} t1\nWHERE EXISTS (SELECT 1 FROM t2 WHERE t2.id = t1.id);", "snippet", "SQL EXISTS subquery check"));
        SQL_SUGGESTIONS.add(sug("CREATE VIEW", "CREATE VIEW {CURSOR}_view AS\nSELECT * FROM table WHERE condition;", "snippet", "SQL CREATE VIEW statement"));
        SQL_SUGGESTIONS.add(sug("MERGE", "MERGE INTO {CURSOR} AS target\nUSING source_table AS source\nON target.id = source.id\nWHEN MATCHED THEN UPDATE SET target.col = source.col\nWHEN NOT MATCHED THEN INSERT (col) VALUES (source.col);", "snippet", "SQL MERGE (upsert) statement"));
        SQL_SUGGESTIONS.add(sug("INSERT SELECT", "INSERT INTO {CURSOR} (col1, col2)\nSELECT col1, col2 FROM other_table WHERE condition;", "snippet", "SQL INSERT FROM SELECT"));
        SQL_SUGGESTIONS.add(sug("CREATE PROCEDURE", "CREATE PROCEDURE {CURSOR}()\nBEGIN\n    SELECT * FROM table;\nEND;", "snippet", "SQL stored procedure skeleton"));
        SQL_SUGGESTIONS.add(sug("FULL TEXT SEARCH", "SELECT * FROM {CURSOR}\nWHERE MATCH(column) AGAINST('search_term');", "snippet", "SQL FULLTEXT search"));
        SQL_SUGGESTIONS.add(sug("UPSERT", "INSERT INTO {CURSOR} (id, name) VALUES (1, 'name')\nON DUPLICATE KEY UPDATE name = VALUES(name);", "snippet", "SQL INSERT ON DUPLICATE KEY UPDATE (MySQL upsert)"));
    }

    // ========================================================================
    //   SCSS / SASS
    // ========================================================================
    private static void loadScss() {
        String[] scssKeywords = {
            "$variable", "@mixin", "@include", "@function", "@return", "@if", "@else", "@for",
            "@each", "@while", "@extend", "@at-root", "@debug", "@warn", "@error",
            "@use", "@forward", "@import", "@media", "@keyframes",
            "&", "!default", "!optional", "!global",
            "lighten", "darken", "saturate", "desaturate", "mix", "adjust-hue",
            "rgba", "opacity", "complement", "invert",
            "if", "map-get", "map-merge", "map-keys", "map-values", "map-has-key",
            "nth", "join", "append", "length",
        };
        for (String kw : scssKeywords) {
            SCSS_SUGGESTIONS.add(sug(kw, kw, "keyword", "SCSS keyword / function / directive"));
        }

        String[][] scssValues = {
            {"@mixin", "@mixin {CURSOR} {\n    \n}", "keyword"},
            {"@include", "@include {CURSOR};", "keyword"},
            {"@function", "@function {CURSOR}($param) {\n    @return $param;\n}", "keyword"},
            {"@for loop", "@for $i from 1 through {CURSOR} {\n    \n}", "keyword"},
            {"@each loop", "@each $item in {CURSOR} {\n    \n}", "keyword"},
            {"@while loop", "@while {CURSOR} {\n    \n}", "keyword"},
            {"@if/@else", "@if {CURSOR} {\n    \n} @else {\n    \n}", "keyword"},
            {"@extend", "@extend .{CURSOR};", "keyword"},
            {"@use", "@use '{CURSOR}';", "keyword"},
            {"@forward", "@forward '{CURSOR}';", "keyword"},
            {"variable", "${CURSOR}: value;", "keyword"},
            {"map", "${CURSOR}: (\n    key: value,\n);", "keyword"},
            {"nested rule", "& > .{CURSOR} {\n    \n}", "keyword"},
        };
        for (String[] v : scssValues) {
            SCSS_SUGGESTIONS.add(sug(v[0], v[1], v[2], "SCSS at-rule / directive"));
        }

        SCSS_SUGGESTIONS.add(sug("responsive mixin", "@mixin respond-to($breakpoint) {\n    @if $breakpoint == 'sm' {\n        @media (max-width: 640px) { @content; }\n    } @else if $breakpoint == 'md' {\n        @media (max-width: 768px) { @content; }\n    } @else if $breakpoint == 'lg' {\n        @media (max-width: 1024px) { @content; }\n    }\n}", "snippet", "SCSS responsive breakpoint mixin"));
        SCSS_SUGGESTIONS.add(sug("flexbox mixin", "@mixin flex-center {\n    display: flex;\n    align-items: center;\n    justify-content: center;\n}", "snippet", "SCSS flexbox centering mixin"));
    }

    // ========================================================================
    //   YAML
    // ========================================================================
    private static void loadYaml() {
        String[][] yamlItems = {
            {"key: value", "{CURSOR}: ", "keyword"},
            {"list item", "- {CURSOR}", "keyword"},
            {"nested object", "{CURSOR}:\n  key: value", "keyword"},
            {"multiline |", "|{CURSOR}\n  ", "keyword"},
            {"multiline >", ">{CURSOR}\n  ", "keyword"},
            {"boolean true", "true", "keyword"},
            {"boolean false", "false", "keyword"},
            {"null", "null", "keyword"},
            {"number", "{CURSOR}", "keyword"},
            {"string", "'{CURSOR}'", "keyword"},
        };
        for (String[] v : yamlItems) {
            YAML_SUGGESTIONS.add(sug(v[0], v[1], v[2], "YAML construct / value type"));
        }
        YAML_SUGGESTIONS.add(sug("docker compose", "version: '3'\nservices:\n  {CURSOR}:\n    image: \n    ports:\n      - \"3000:3000\"", "snippet", "Docker Compose service definition"));
        YAML_SUGGESTIONS.add(sug("github actions", "name: {CURSOR}\non: [push]\njobs:\n  build:\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/checkout@v3\n      - run: npm test", "snippet", "GitHub Actions workflow"));
        YAML_SUGGESTIONS.add(sug("ansible playbook", "---\n- name: {CURSOR}\n  hosts: all\n  tasks:\n    - name: \n      apt:\n        name: \n        state: present", "snippet", "Ansible playbook task definition"));
    }

    // ========================================================================
    //   SHELL / BASH
    // ========================================================================
    private static void loadSh() {
        String[] shKeywords = {
            "#!/bin/bash", "#!/bin/sh", "#!/usr/bin/env bash",
            "if", "then", "else", "elif", "fi", "for", "while", "do", "done",
            "case", "esac", "in", "function", "return", "exit", "continue", "break",
            "export", "local", "readonly", "unset", "set", "source", ".",
            "echo", "printf", "read", "test", "[", "]]",
            "$0", "$1", "$2", "$@", "$#", "$?", "$$", "$!",
            "${}", "$()", "$(())",
        };
        for (String kw : shKeywords) {
            SH_SUGGESTIONS.add(sug(kw, kw, "keyword", "Shell keyword / syntax"));
        }

        String[][] shCommands = {
            {"ls", "ls -la {CURSOR}", "keyword"},
            {"cd", "cd {CURSOR}", "keyword"},
            {"mkdir", "mkdir -p {CURSOR}", "keyword"},
            {"rm", "rm -rf {CURSOR}", "keyword"},
            {"cp", "cp -r {CURSOR} ", "keyword"},
            {"mv", "mv {CURSOR} ", "keyword"},
            {"cat", "cat {CURSOR}", "keyword"},
            {"grep", "grep '{CURSOR}' ", "keyword"},
            {"sed", "sed -i 's/{CURSOR}//g' ", "keyword"},
            {"awk", "awk '{print {CURSOR}}' ", "keyword"},
            {"find", "find . -name '{CURSOR}'", "keyword"},
            {"chmod", "chmod +x {CURSOR}", "keyword"},
            {"chown", "chown {CURSOR}", "keyword"},
            {"ps", "ps aux | grep {CURSOR}", "keyword"},
            {"kill", "kill -9 {CURSOR}", "keyword"},
            {"tar", "tar -czf {CURSOR}.tar.gz ", "keyword"},
            {"curl", "curl -X GET '{CURSOR}'", "keyword"},
            {"wget", "wget {CURSOR}", "keyword"},
            {"ssh", "ssh user@{CURSOR}", "keyword"},
            {"scp", "scp {CURSOR} user@host:", "keyword"},
            {"git init", "git init", "keyword"},
            {"git clone", "git clone {CURSOR}", "keyword"},
            {"git add", "git add {CURSOR}", "keyword"},
            {"git commit", "git commit -m '{CURSOR}'", "keyword"},
            {"git push", "git push origin {CURSOR}", "keyword"},
            {"git pull", "git pull origin {CURSOR}", "keyword"},
            {"git status", "git status", "keyword"},
            {"git log", "git log --oneline --graph", "keyword"},
            {"git branch", "git branch {CURSOR}", "keyword"},
            {"git checkout", "git checkout {CURSOR}", "keyword"},
            {"git merge", "git merge {CURSOR}", "keyword"},
            {"docker ps", "docker ps -a", "keyword"},
            {"docker images", "docker images", "keyword"},
            {"docker run", "docker run -it --rm {CURSOR}", "keyword"},
            {"docker exec", "docker exec -it {CURSOR} bash", "keyword"},
            {"docker build", "docker build -t {CURSOR} .", "keyword"},
            {"docker compose up", "docker compose up -d", "keyword"},
            {"npm init", "npm init -y", "keyword"},
            {"npm install", "npm install {CURSOR}", "keyword"},
            {"npm start", "npm start", "keyword"},
            {"npm run build", "npm run build", "keyword"},
            {"npx create-react-app", "npx create-react-app {CURSOR}", "keyword"},
            {"node", "node {CURSOR}", "keyword"},
            {"python", "python {CURSOR}", "keyword"},
            {"pip install", "pip install {CURSOR}", "keyword"},
            {"conda install", "conda install {CURSOR}", "keyword"},
            {"cargo new", "cargo new {CURSOR}", "keyword"},
            {"rustc", "rustc {CURSOR}.rs", "keyword"},
        };
        for (String[] g : shCommands) {
            SH_SUGGESTIONS.add(sug(g[0], g[1], g[2], "Shell command / program"));
        }

        SH_SUGGESTIONS.add(sug("for loop", "for item in {CURSOR}; do\n    echo \"$item\"\ndone", "snippet", "Shell for loop"));
        SH_SUGGESTIONS.add(sug("if condition", "if [ {CURSOR} ]; then\n    echo \"\"\nfi", "snippet", "Shell if condition"));
        SH_SUGGESTIONS.add(sug("function", "function {CURSOR}() {\n    local result=\n    echo \"$result\"\n}", "snippet", "Shell function definition"));
        SH_SUGGESTIONS.add(sug("case statement", "case ${CURSOR} in\n    pattern)\n        ;;\n    *)\n        ;;\nesac", "snippet", "Shell case statement"));
        SH_SUGGESTIONS.add(sug("while read line", "while IFS= read -r line; do\n    echo \"$line\"\ndone < \"{CURSOR}\"", "snippet", "Shell while read line loop"));
        SH_SUGGESTIONS.add(sug("argument parsing", "while [[ $# -gt 0 ]]; do\n    case $1 in\n        -f|--flag)\n            shift\n            ;;\n        *)\n            echo \"Unknown: $1\"\n            exit 1\n            ;;\n    esac\ndone", "snippet", "Shell argument parsing pattern"));
        SH_SUGGESTIONS.add(sug("heredoc", "cat << 'EOF' > {CURSOR}\ncontent\nEOF", "snippet", "Shell heredoc redirect"));
        SH_SUGGESTIONS.add(sug("trap cleanup", "trap 'cleanup' EXIT\n\ncleanup() {\n    rm -f ${CURSOR}\n}", "snippet", "Shell trap cleanup pattern"));
    }

    // ========================================================================
    //   C
    // ========================================================================
    private static void loadC() {
        String[] cKeywords = {
            "#include", "#define", "#ifdef", "#ifndef", "#endif", "#pragma",
            "int", "char", "float", "double", "void", "long", "short", "unsigned", "signed",
            "struct", "union", "enum", "typedef", "const", "volatile", "static", "extern", "register",
            "auto", "return", "if", "else", "for", "while", "do", "switch", "case", "break",
            "continue", "goto", "sizeof", "NULL",
            "printf", "scanf", "malloc", "calloc", "realloc", "free", "fopen", "fclose",
            "fread", "fwrite", "fprintf", "fscanf", "fgets", "fputs", "feof",
            "strlen", "strcpy", "strcat", "strcmp", "strchr", "strstr", "sprintf",
            "atoi", "atol", "atof", "itoa",
            "FILE", "size_t",
        };
        for (String kw : cKeywords) {
            C_SUGGESTIONS.add(sug(kw, kw, "keyword", "C keyword / function / built-in"));
        }

        C_SUGGESTIONS.add(sug("main", "int main(int argc, char *argv[]) {\n    {CURSOR}\n    return 0;\n}", "snippet", "C main function"));
        C_SUGGESTIONS.add(sug("struct", "struct {CURSOR} {\n    int id;\n    char name[100];\n};", "snippet", "C struct definition"));
        C_SUGGESTIONS.add(sug("typedef struct", "typedef struct {\n    int id;\n    char name[100];\n} {CURSOR};", "snippet", "C typedef struct pattern"));
        C_SUGGESTIONS.add(sug("for loop", "for (int i = 0; i < {CURSOR}; i++) {\n    \n}", "snippet", "C for loop"));
        C_SUGGESTIONS.add(sug("ifndef guard", "#ifndef {CURSOR}_H\n#define {CURSOR}_H\n\n#endif", "snippet", "C header include guard"));
        C_SUGGESTIONS.add(sug("dynamic array", "int *arr = (int *)malloc({CURSOR} * sizeof(int));\nif (arr == NULL) exit(1);\nfree(arr);", "snippet", "C dynamic memory allocation pattern"));
        C_SUGGESTIONS.add(sug("file read", "FILE *fp = fopen(\"{CURSOR}\", \"r\");\nif (fp == NULL) { perror(\"Error\"); return -1; }\nchar buffer[256];\nwhile (fgets(buffer, sizeof(buffer), fp)) {\n    printf(\"%s\", buffer);\n}\nfclose(fp);", "snippet", "C file reading pattern"));
        C_SUGGESTIONS.add(sug("function pointer", "int (*{CURSOR})(int, int) = &function;\nint result = (*ptr)(a, b);", "snippet", "C function pointer pattern"));
    }

    // ========================================================================
    //   C++
    // ========================================================================
    private static void loadCpp() {
        String[] cppKeywords = {
            "#include", "#define", "#ifdef", "#ifndef", "#pragma once",
            "int", "char", "float", "double", "void", "long", "short", "unsigned", "signed",
            "bool", "wchar_t",
            "class", "struct", "union", "enum", "typedef", "using", "namespace",
            "public", "private", "protected", "virtual", "override", "final",
            "const", "constexpr", "static", "extern", "mutable", "volatile",
            "auto", "decltype", "sizeof", "NULL", "nullptr",
            "return", "if", "else", "for", "while", "do", "switch", "case", "break",
            "continue", "goto", "try", "catch", "throw", "noexcept",
            "new", "delete", "this", "friend", "explicit",
            "template", "typename", "class", "export",
            "cout", "cin", "endl",
            "std::string", "std::vector", "std::map", "std::set", "std::pair",
            "std::shared_ptr", "std::unique_ptr", "std::make_shared", "std::make_unique",
            "std::function", "std::bind", "std::lambda",
        };
        for (String kw : cppKeywords) {
            CPP_SUGGESTIONS.add(sug(kw, kw, "keyword", "C++ keyword / STL type / function"));
        }

        CPP_SUGGESTIONS.add(sug("main", "int main(int argc, char *argv[]) {\n    {CURSOR}\n    return 0;\n}", "snippet", "C++ main function"));
        CPP_SUGGESTIONS.add(sug("class", "class {CURSOR} {\npublic:\n    {CURSOR}() = default;\n    ~{CURSOR}() = default;\nprivate:\n    int member;\n};", "snippet", "C++ class skeleton"));
        CPP_SUGGESTIONS.add(sug("template class", "template<typename T>\nclass {CURSOR} {\npublic:\n    T get() const { return value; }\n    void set(const T& val) { value = val; }\nprivate:\n    T value;\n};", "snippet", "C++ template class"));
        CPP_SUGGESTIONS.add(sug("vector loop", "std::vector<int> vec = {1, 2, 3};\nfor (const auto& item : {CURSOR}) {\n    std::cout << item << std::endl;\n}", "snippet", "C++ range-based for loop"));
        CPP_SUGGESTIONS.add(sug("lambda", "auto lambda = [&](const auto& {CURSOR}) -> void {\n    \n};", "snippet", "C++ lambda expression"));
        CPP_SUGGESTIONS.add(sug("smart pointer", "auto ptr = std::make_shared<{CURSOR}>();\nptr->method();", "snippet", "C++ shared_ptr smart pointer"));
        CPP_SUGGESTIONS.add(sug("fstream", "std::ifstream file(\"{CURSOR}\");\nif (file.is_open()) {\n    std::string line;\n    while (std::getline(file, line)) {\n        std::cout << line << std::endl;\n    }\n    file.close();\n}", "snippet", "C++ file read with fstream"));
        CPP_SUGGESTIONS.add(sug("vector push_back", "std::vector<{CURSOR}> vec;\nvec.push_back(value);", "snippet", "C++ vector push_back pattern"));
        CPP_SUGGESTIONS.add(sug("map insert", "std::map<std::string, int> m;\nm[\"{CURSOR}\"] = value;", "snippet", "C++ map insert pattern"));
        CPP_SUGGESTIONS.add(sug("try/catch", "try {\n    {CURSOR}\n} catch (const std::exception& e) {\n    std::cerr << e.what() << std::endl;\n}", "snippet", "C++ exception handling"));
    }

    // ========================================================================
    //   MARKDOWN
    // ========================================================================
    private static void loadMarkdown() {
        String[][] mdElements = {
            {"# H1", "# {CURSOR}", "keyword"},
            {"## H2", "## {CURSOR}", "keyword"},
            {"### H3", "### {CURSOR}", "keyword"},
            {"**bold**", "**{CURSOR}**", "keyword"},
            {"*italic*", "*{CURSOR}*", "keyword"},
            {"`code`", "`{CURSOR}`", "keyword"},
            {"```fence```", "```{CURSOR}\n\n```", "keyword"},
            {"- list", "- {CURSOR}", "keyword"},
            {"1. list", "1. {CURSOR}", "keyword"},
            {"[link](url)", "[{CURSOR}](url)", "keyword"},
            {"![image](url)", "![{CURSOR}](image.png)", "keyword"},
            {"> blockquote", "> {CURSOR}", "keyword"},
            {"--- hr", "---", "keyword"},
            {"| table |", "| {CURSOR} | col2 |\n|-------|------|\n| val   | val  |", "keyword"},
            {"[x] task", "- [x] {CURSOR}", "keyword"},
            {"[ ] task", "- [ ] {CURSOR}", "keyword"},
        };
        for (String[] v : mdElements) {
            MARKDOWN_SUGGESTIONS.add(sug(v[0], v[1], v[2], "Markdown formatting element"));
        }
        MARKDOWN_SUGGESTIONS.add(sug("README template", "# {CURSOR}\n\n## Description\n\n## Installation\n\n## Usage\n\n## License", "snippet", "Markdown README template"));
        MARKDOWN_SUGGESTIONS.add(sug("code block with lang", "```{CURSOR}\ncode here\n```", "snippet", "Markdown fenced code block with language"));
        MARKDOWN_SUGGESTIONS.add(sug("admonition", "> **Note:** {CURSOR}\n> \n> Additional details here", "snippet", "Markdown admonition / callout box"));
        MARKDOWN_SUGGESTIONS.add(sug("footnote", "Here is text[^1]\n\n[^1]: {CURSOR}", "snippet", "Markdown footnote reference"));
        MARKDOWN_SUGGESTIONS.add(sug("definition list", "Term\n: {CURSOR}", "snippet", "Markdown definition list"));
        MARKDOWN_SUGGESTIONS.add(sug("strikethrough", "~~{CURSOR}~~", "snippet", "Markdown strikethrough text"));
        MARKDOWN_SUGGESTIONS.add(sug("mermaid diagram", "```mermaid\ngraph TD;\n    A-->B;\n    {CURSOR}-->C;\n```", "snippet", "Mermaid JS diagram in Markdown"));
        MARKDOWN_SUGGESTIONS.add(sug("table with alignment", "| Left | Center | Right |\n| :--- | :----: | ----: |\n| {CURSOR} | text  | text  |", "snippet", "Markdown table with column alignment"));
    }

    // ========================================================================
    //   KOTLIN
    // ========================================================================
    private static void loadKotlin() {
        String[] kotlinKeywords = {
            "fun", "val", "var", "class", "object", "interface", "enum", "sealed", "data",
            "abstract", "open", "override", "final", "private", "protected", "public", "internal",
            "if", "else", "when", "for", "while", "do", "break", "continue", "return",
            "try", "catch", "finally", "throw", "null", "true", "false", "this", "super",
            "in", "!in", "is", "!is", "as", "as?", "typeof",
            "package", "import", "typealias", "companion", "init", "constructor",
            "suspend", "inline", "infix", "operator", "tailrec", "external",
            "reified", "crossinline", "noinline",
            "Unit", "Any", "Nothing", "String", "Int", "Long", "Double", "Float",
            "Boolean", "Char", "Byte", "Short",
            "List", "MutableList", "ArrayList",
            "Map", "MutableMap", "HashMap",
            "Set", "MutableSet", "HashSet",
            "Array", "IntArray", "LongArray", "DoubleArray", "BooleanArray",
            "println", "print", "readLine",
        };
        for (String kw : kotlinKeywords) {
            KOTLIN_SUGGESTIONS.add(sug(kw, kw, "keyword", "Kotlin keyword / built-in type"));
        }
        KOTLIN_SUGGESTIONS.add(sug("fun main", "fun main() {\n    println(\"{CURSOR}\")\n}", "snippet", "Kotlin main function entry point"));
        KOTLIN_SUGGESTIONS.add(sug("class", "class {CURSOR} {\n    \n}", "snippet", "Kotlin class skeleton"));
        KOTLIN_SUGGESTIONS.add(sug("data class", "data class {CURSOR}(\n    val id: Int,\n    val name: String\n)", "snippet", "Kotlin data class"));
        KOTLIN_SUGGESTIONS.add(sug("sealed class", "sealed class {CURSOR} {\n    data class Success(val data: Any) : {CURSOR}()\n    data class Error(val msg: String) : {CURSOR}()\n}", "snippet", "Kotlin sealed class pattern"));
        KOTLIN_SUGGESTIONS.add(sug("when expression", "when ({CURSOR}) {\n    is Type -> // handle\n    else -> // default\n}", "snippet", "Kotlin when expression"));
        KOTLIN_SUGGESTIONS.add(sug("for loop", "for (item in {CURSOR}) {\n    println(item)\n}", "snippet", "Kotlin for loop"));
        KOTLIN_SUGGESTIONS.add(sug("lambda", "val lambda: (Type) -> ReturnType = { ${CURSOR} }", "snippet", "Kotlin lambda expression"));
        KOTLIN_SUGGESTIONS.add(sug("filter/map", "list.filter { ${CURSOR} }.map { it }", "snippet", "Kotlin filter/map chain"));
        KOTLIN_SUGGESTIONS.add(sug("coroutine scope", "GlobalScope.launch {\n    ${CURSOR}\n}", "snippet", "Kotlin coroutine launch"));
        KOTLIN_SUGGESTIONS.add(sug("companion object", "companion object {\n    const val TAG = \"{CURSOR}\"\n}", "snippet", "Kotlin companion object"));
        KOTLIN_SUGGESTIONS.add(sug("extension function", "fun {CURSOR}.extension(): ReturnType {\n    \n}", "snippet", "Kotlin extension function"));
        KOTLIN_SUGGESTIONS.add(sug("listOf", "listOf({CURSOR})", "snippet", "Kotlin listOf factory"));
        KOTLIN_SUGGESTIONS.add(sug("mapOf", "mapOf(\"key\" to {CURSOR})", "snippet", "Kotlin mapOf factory"));
        KOTLIN_SUGGESTIONS.add(sug("apply/also", "val obj = {CURSOR}().apply {\n    \n}", "snippet", "Kotlin apply scope function"));
    }

    // ========================================================================
    //   GO
    // ========================================================================
    private static void loadGo() {
        String[] goKeywords = {
            "package", "import", "func", "var", "const", "type", "struct", "interface", "map",
            "chan", "go", "defer", "select", "range",
            "if", "else", "for", "break", "continue", "switch", "case", "default", "fallthrough",
            "return", "goto",
            "true", "false", "nil", "iota",
            "int", "int8", "int16", "int32", "int64",
            "uint", "uint8", "uint16", "uint32", "uint64", "uintptr",
            "float32", "float64", "complex64", "complex128",
            "bool", "string", "byte", "rune", "error",
            "make", "new", "len", "cap", "append", "copy", "close", "delete",
            "panic", "recover",
            "Print", "Printf", "Println", "Sprintf", "Errorf",
            "fmt", "http", "json", "io", "os", "time", "strings", "strconv",
            "nil", "true", "false",
        };
        for (String kw : goKeywords) {
            GO_SUGGESTIONS.add(sug(kw, kw, "keyword", "Go keyword / built-in"));
        }
        GO_SUGGESTIONS.add(sug("func main", "func main() {\n    {CURSOR}\n}", "snippet", "Go main function"));
        GO_SUGGESTIONS.add(sug("func", "func {CURSOR}(params) ReturnType {\n    \n}", "snippet", "Go function declaration"));
        GO_SUGGESTIONS.add(sug("struct", "type {CURSOR} struct {\n    Field Type\n}", "snippet", "Go struct definition"));
        GO_SUGGESTIONS.add(sug("interface", "type {CURSOR} interface {\n    Method() ReturnType\n}", "snippet", "Go interface definition"));
        GO_SUGGESTIONS.add(sug("for loop", "for i := 0; i < {CURSOR}; i++ {\n    \n}", "snippet", "Go for loop"));
        GO_SUGGESTIONS.add(sug("for range", "for i, v := range {CURSOR} {\n    \n}", "snippet", "Go for range loop"));
        GO_SUGGESTIONS.add(sug("if err != nil", "if err != nil {\n    return nil, err\n}", "snippet", "Go error check pattern"));
        GO_SUGGESTIONS.add(sug("defer", "defer {CURSOR}()", "snippet", "Go defer statement"));
        GO_SUGGESTIONS.add(sug("goroutine", "go {CURSOR}()", "snippet", "Go goroutine launch"));
        GO_SUGGESTIONS.add(sug("channel", "ch := make(chan {CURSOR}, 100)", "snippet", "Go channel creation"));
        GO_SUGGESTIONS.add(sug("select", "select {\n    case <-ch1:\n        {CURSOR}\n    case <-ch2:\n        \n}", "snippet", "Go select multiplexing"));
        GO_SUGGESTIONS.add(sug("error handling", "if err != nil {\n    log.Fatal(err)\n}", "snippet", "Go fatal error handling"));
        GO_SUGGESTIONS.add(sug("http handler", "http.HandleFunc(\"/{CURSOR}\", func(w http.ResponseWriter, r *http.Request) {\n    w.Write([]byte(\"OK\"))\n})", "snippet", "Go HTTP handler pattern"));
        GO_SUGGESTIONS.add(sug("http server", "http.ListenAndServe(\":8080\", nil)", "snippet", "Go HTTP server start"));
        GO_SUGGESTIONS.add(sug("json marshal", "json.Marshal({CURSOR})", "snippet", "Go JSON marshal"));
        GO_SUGGESTIONS.add(sug("json unmarshal", "json.Unmarshal(data, &{CURSOR})", "snippet", "Go JSON unmarshal"));
        GO_SUGGESTIONS.add(sug("switch", "switch {CURSOR} {\n    case value:\n        \n    default:\n        \n}", "snippet", "Go switch statement"));
        GO_SUGGESTIONS.add(sug("map literal", "map[string]Type{\n    \"{CURSOR}\": value,\n}", "snippet", "Go map literal"));
        GO_SUGGESTIONS.add(sug("slice literal", "[]Type{\n    {CURSOR},\n}", "snippet", "Go slice literal"));
        GO_SUGGESTIONS.add(sug("fmt.Println", "fmt.Println({CURSOR})", "snippet", "Go fmt.Println"));
        GO_SUGGESTIONS.add(sug("fmt.Sprintf", "fmt.Sprintf(\"{CURSOR}\", args)", "snippet", "Go fmt.Sprintf"));
        GO_SUGGESTIONS.add(sug("context", "ctx := context.Background()\nctx, cancel := context.WithTimeout(ctx, 5*time.Second)\ndefer cancel()", "snippet", "Go context with timeout"));
        GO_SUGGESTIONS.add(sug("testing", "func Test{CURSOR}(t *testing.T) {\n    \n}", "snippet", "Go test function"));
    }

    // ========================================================================
    //   RUST
    // ========================================================================
    private static void loadRust() {
        String[] rustKeywords = {
            "fn", "let", "mut", "const", "static", "type", "struct", "enum", "trait", "impl",
            "mod", "use", "pub", "crate", "self", "Self", "super",
            "if", "else", "for", "while", "loop", "break", "continue", "return",
            "match", "in", "if let", "while let",
            "true", "false", "Some", "None", "Ok", "Err",
            "as", "ref", "move", "unsafe", "async", "await", "dyn",
            "i8", "i16", "i32", "i64", "i128", "isize",
            "u8", "u16", "u32", "u64", "u128", "usize",
            "f32", "f64", "bool", "char", "str",
            "String", "Vec", "Option", "Result", "Box", "Rc", "Arc",
            "Cell", "RefCell", "HashMap", "HashSet", "BTreeMap", "BTreeSet",
            "println!", "print!", "eprintln!", "format!",
            "vec!", "panic!", "unreachable!", "unimplemented!",
            "assert!", "assert_eq!", "assert_ne!",
            "clone", "copy", "into", "from", "unwrap", "expect",
            "map", "and_then", "is_some", "is_none", "is_ok", "is_err",
            "iter", "into_iter", "iter_mut", "collect", "filter", "for_each", "fold",
            "mod", "use", "fn", "let", "mut", "const", "static",
            "impl", "trait", "struct", "enum",
        };
        for (String kw : rustKeywords) {
            RUST_SUGGESTIONS.add(sug(kw, kw, "keyword", "Rust keyword / built-in type"));
        }
        RUST_SUGGESTIONS.add(sug("fn main", "fn main() {\n    println!(\"{CURSOR}\");\n}", "snippet", "Rust main function"));
        RUST_SUGGESTIONS.add(sug("fn", "fn {CURSOR}(params) -> ReturnType {\n    \n}", "snippet", "Rust function declaration"));
        RUST_SUGGESTIONS.add(sug("struct", "struct {CURSOR} {\n    field: Type,\n}", "snippet", "Rust struct definition"));
        RUST_SUGGESTIONS.add(sug("enum", "enum {CURSOR} {\n    Variant,\n}", "snippet", "Rust enum definition"));
        RUST_SUGGESTIONS.add(sug("impl", "impl {CURSOR} {\n    fn method(&self) {\n        \n    }\n}", "snippet", "Rust impl block"));
        RUST_SUGGESTIONS.add(sug("match", "match {CURSOR} {\n    Pattern => {},\n    _ => {},\n}", "snippet", "Rust match expression"));
        RUST_SUGGESTIONS.add(sug("for loop", "for item in {CURSOR} {\n    println!(\"{}\", item);\n}", "snippet", "Rust for loop"));
        RUST_SUGGESTIONS.add(sug("if let", "if let Some({CURSOR}) = value {\n    \n}", "snippet", "Rust if let pattern matching"));
        RUST_SUGGESTIONS.add(sug("let mut", "let mut {CURSOR} = value;", "snippet", "Rust mutable variable"));
        RUST_SUGGESTIONS.add(sug("trait", "trait {CURSOR} {\n    fn method(&self);\n}", "snippet", "Rust trait definition"));
        RUST_SUGGESTIONS.add(sug("impl trait for", "impl {CURSOR} for MyType {\n    fn method(&self) {\n        \n    }\n}", "snippet", "Rust trait implementation"));
        RUST_SUGGESTIONS.add(sug("vec!", "vec![{CURSOR}]", "snippet", "Rust vec! macro"));
        RUST_SUGGESTIONS.add(sug("HashMap", "use std::collections::HashMap;\nlet mut map = HashMap::new();\nmap.insert({CURSOR}, value);", "snippet", "Rust HashMap pattern"));
        RUST_SUGGESTIONS.add(sug("Result", "fn {CURSOR}() -> Result<(), Box<dyn std::error::Error>> {\n    Ok(())\n}", "snippet", "Rust Result return type"));
        RUST_SUGGESTIONS.add(sug("Option", "fn {CURSOR}() -> Option<Type> {\n    Some(value)\n}", "snippet", "Rust Option return type"));
        RUST_SUGGESTIONS.add(sug("error handling", "match result {\n    Ok(val) => { {CURSOR} },\n    Err(e) => return Err(e.into()),\n}", "snippet", "Rust error handling pattern"));
        RUST_SUGGESTIONS.add(sug("async fn", "async fn {CURSOR}() -> Result<(), Box<dyn std::error::Error>> {\n    Ok(())\n}", "snippet", "Rust async function"));
        RUST_SUGGESTIONS.add(sug("mod", "mod {CURSOR};\n// in {CURSOR}.rs", "snippet", "Rust module declaration"));
        RUST_SUGGESTIONS.add(sug("use", "use std::{CURSOR};", "snippet", "Rust use import"));
        RUST_SUGGESTIONS.add(sug("cfg test", "#[cfg(test)]\nmod tests {\n    use super::*;\n\n    #[test]\n    fn test_{CURSOR}() {\n        assert_eq!(2 + 2, 4);\n    }\n}", "snippet", "Rust unit test module"));
    }

    // ========================================================================
    //   VUE
    // ========================================================================
    private static void loadVue() {
        String[] vueAttributes = {
            "v-bind:", "v-model", "v-if", "v-else", "v-else-if", "v-for", "v-show",
            "v-on:", "v-on:click", "v-on:submit", "v-on:input", "v-on:change",
            "v-on:keyup", "v-on:mouseover", "v-on:focus", "v-on:blur",
            "v-text", "v-html", "v-pre", "v-once", "v-cloak", "v-slot:",
            ":key", ":ref", ":is", ":class", ":style", ":src", ":href",
            "@click", "@submit", "@input", "@change", "@keyup", "@mouseover",
            "@focus", "@blur", "@scroll", "@resize",
        };
        for (String attr : vueAttributes) {
            VUE_SUGGESTIONS.add(sug(attr, attr + "=\"{CURSOR}\"", "attr", "Vue directive / binding"));
        }
        VUE_SUGGESTIONS.add(sug("v-for item in", "v-for=\"item in {CURSOR}\" :key=\"item.id\"", "attr", "Vue v-for directive"));
        VUE_SUGGESTIONS.add(sug("v-if/else", "v-if=\"condition\"\n{content}\ndiv(v-else)", "snippet", "Vue conditional rendering"));
        VUE_SUGGESTIONS.add(sug("component", "Vue.component('{CURSOR}', {\n    template: `<div></div>`,\n    props: {},\n    data() {\n        return {}\n    }\n})", "snippet", "Vue component definition"));
        VUE_SUGGESTIONS.add(sug("computed", "computed: {\n    {CURSOR}() {\n        return this.value\n    }\n}", "snippet", "Vue computed property"));
        VUE_SUGGESTIONS.add(sug("watch", "watch: {\n    {CURSOR}(newVal, oldVal) {\n        \n    }\n}", "snippet", "Vue watcher"));
        VUE_SUGGESTIONS.add(sug("methods", "methods: {\n    {CURSOR}() {\n        \n    }\n}", "snippet", "Vue methods block"));
        VUE_SUGGESTIONS.add(sug("props", "props: {\n    {CURSOR}: {\n        type: String,\n        required: true\n    }\n}", "snippet", "Vue props definition"));
        VUE_SUGGESTIONS.add(sug("emits", "emits: ['{CURSOR}']", "snippet", "Vue emits declaration"));
        VUE_SUGGESTIONS.add(sug("this.$emit", "this.$emit('{CURSOR}', payload)", "snippet", "Vue emit event"));
        VUE_SUGGESTIONS.add(sug("this.$refs", "this.$refs.{CURSOR}", "snippet", "Vue refs access"));
        VUE_SUGGESTIONS.add(sug("this.$route", "this.$route.params.{CURSOR}", "snippet", "Vue route params"));
        VUE_SUGGESTIONS.add(sug("template ref", "this.$refs.{CURSOR}.focus()", "snippet", "Vue template ref usage"));
        VUE_SUGGESTIONS.add(sug("mounted", "mounted() {\n    this.{CURSOR}\n}", "snippet", "Vue mounted lifecycle hook"));
        VUE_SUGGESTIONS.add(sug("created", "created() {\n    this.{CURSOR}\n}", "snippet", "Vue created lifecycle hook"));
        VUE_SUGGESTIONS.add(sug("updated", "updated() {\n    this.{CURSOR}\n}", "snippet", "Vue updated lifecycle hook"));
        VUE_SUGGESTIONS.add(sug("Vuex mapState", "import { mapState } from 'vuex';\n\ncomputed: {\n    ...mapState(['{CURSOR}'])\n}", "snippet", "Vuex mapState helper"));
        VUE_SUGGESTIONS.add(sug("Vuex mapActions", "import { mapActions } from 'vuex';\n\nmethods: {\n    ...mapActions(['{CURSOR}'])\n}", "snippet", "Vuex mapActions helper"));
        VUE_SUGGESTIONS.add(sug("Vue Router", "const router = new VueRouter({\n    routes: [\n        { path: '/{CURSOR}', component: Component }\n    ]\n})", "snippet", "Vue Router setup"));
        VUE_SUGGESTIONS.add(sug("Vue Router beforeEach", "router.beforeEach((to, from, next) => {\n    if (to.meta.requiresAuth && !isAuthenticated) {\n        next({ path: '/login' });\n    } else {\n        next();\n    }\n});", "snippet", "Vue Router navigation guard"));
        VUE_SUGGESTIONS.add(sug("Vue Router navigation guard", "beforeRouteEnter(to, from, next) {\n    next(vm => {\n        vm.loadData({CURSOR});\n    });\n}", "snippet", "Vue in-component navigation guard"));
        VUE_SUGGESTIONS.add(sug("Vuex store", "import Vue from 'vue';\nimport Vuex from 'vuex';\n\nVue.use(Vuex);\n\nexport default new Vuex.Store({\n    state: {\n        {CURSOR}: ''\n    },\n    mutations: {},\n    actions: {},\n    getters: {}\n});", "snippet", "Vuex store definition"));
        VUE_SUGGESTIONS.add(sug("Vuex mutation", "mutations: {\n    SET_{CURSOR}(state, payload) {\n        state.{CURSOR} = payload;\n    }\n}", "snippet", "Vuex mutation definition"));
        VUE_SUGGESTIONS.add(sug("Vuex action", "actions: {\n    async fetch{CURSOR}({ commit }, params) {\n        const data = await api.get('/{CURSOR}');\n        commit('SET_{CURSOR}', data);\n    }\n}", "snippet", "Vuex async action"));
        VUE_SUGGESTIONS.add(sug("Vuex getter", "getters: {\n    {CURSOR}: (state) => {\n        return state.{CURSOR};\n    }\n}", "snippet", "Vuex computed getter"));
        VUE_SUGGESTIONS.add(sug("Vue nextTick", "this.$nextTick(() => {\n    {CURSOR}\n});", "snippet", "Vue nextTick after DOM update"));
        VUE_SUGGESTIONS.add(sug("Vue slot scoped", "<slot name=\"{CURSOR}\" :data=\"value\">\n    <div>Default content</div>\n</slot>", "snippet", "Vue scoped slot"));
        VUE_SUGGESTIONS.add(sug("Vue v-slot", "<template v-slot:{CURSOR}=\"slotProps\">\n    {{ slotProps.data }}\n</template>", "snippet", "Vue named slot with props"));
        VUE_SUGGESTIONS.add(sug("Vue provide/inject options", "provide: {\n    {CURSOR}: value\n},\ninject: ['{CURSOR}'],", "snippet", "Vue options API provide/inject"));
        VUE_SUGGESTIONS.add(sug("Vue mixin", "const {CURSOR}Mixin = {\n    data() {\n        return {}\n    },\n    methods: {},\n    created() {\n        \n    }\n};", "snippet", "Vue mixin definition"));
        VUE_SUGGESTIONS.add(sug("Vue plugin", "const MyPlugin = {\n    install(Vue, options) {\n        Vue.prototype.${CURSOR} = options || {};\n    }\n};\nVue.use(MyPlugin);", "snippet", "Vue plugin definition"));
        VUE_SUGGESTIONS.add(sug("Vue render function", "render(h) {\n    return h('div', {\n        class: { active: this.isActive },\n        on: { click: this.handleClick }\n    }, [\n        h('span', 'Content')\n    ]);\n}", "snippet", "Vue render function"));
        VUE_SUGGESTIONS.add(sug("Vue functional component", "Vue.component('{CURSOR}', {\n    functional: true,\n    props: ['label'],\n    render(h, { props }) {\n        return h('button', props.label);\n    }\n});", "snippet", "Vue functional component"));
        VUE_SUGGESTIONS.add(sug("Vue 3 script setup", "<script setup>\nimport { ref } from 'vue';\n\nconst {CURSOR} = ref('value');\n</script>", "snippet", "Vue 3 script setup syntax"));
        VUE_SUGGESTIONS.add(sug("Vue 3 defineProps", "const props = defineProps({\n    {CURSOR}: {\n        type: String,\n        required: true\n    }\n});", "snippet", "Vue 3 defineProps in script setup"));
        VUE_SUGGESTIONS.add(sug("Vue 3 defineEmits", "const emit = defineEmits(['{CURSOR}', 'update']);\nemit('{CURSOR}', payload);", "snippet", "Vue 3 defineEmits in script setup"));
        VUE_SUGGESTIONS.add(sug("Vue 3 defineExpose", "defineExpose({\n    {CURSOR}: ref(0)\n});", "snippet", "Vue 3 defineExpose in script setup"));
        VUE_SUGGESTIONS.add(sug("Vue 3 <script setup> full", "<script setup lang=\"ts\">\nimport { ref, computed, onMounted } from 'vue';\n\nconst props = defineProps<{ {CURSOR}: string }>();\nconst emit = defineEmits<{ (e: 'update', val: string): void }>();\n\nconst count = ref(0);\nconst double = computed(() => count.value * 2);\n\nonMounted(() => {\n    console.log('Mounted');\n});\n</script>\n\n<template>\n    <div>{{ count }}</div>\n</template>", "snippet", "Vue 3 full script setup template"));
        VUE_SUGGESTIONS.add(sug("Vue 3 defineModel", "const model = defineModel({ type: String, default: '' });{CURSOR}", "snippet", "Vue 3 v-model binding"));
        VUE_SUGGESTIONS.add(sug("Vue 3 useCssModule", "const classes = useCssModule();{CURSOR}", "snippet", "Vue 3 CSS modules"));
        VUE_SUGGESTIONS.add(sug("Vue 3 custom directive", "const vFocus = {\n    mounted(el) { el.focus(); }\n};{CURSOR}", "snippet", "Vue 3 custom directive"));
        VUE_SUGGESTIONS.add(sug("Vue 3 async component", "const AsyncComp = defineAsyncComponent(() =>\n    import('./{CURSOR}.vue')\n);", "snippet", "Vue 3 async component"));
        VUE_SUGGESTIONS.add(sug("Vue 3 composable", "import { ref, onMounted, onUnmounted } from 'vue';\n\nexport function use{CURSOR}() {\n    const data = ref(null);\n    \n    onMounted(() => {\n        fetchData();\n    });\n    \n    async function fetchData() {\n        data.value = await (await fetch('/api/{CURSOR}')).json();\n    }\n    \n    return { data, fetchData };\n}", "snippet", "Vue 3 composable function"));
        VUE_SUGGESTIONS.add(sug("Vue 3 globalProperties", "app.config.globalProperties.${CURSOR} = 'value';", "snippet", "Vue 3 global property"));
        VUE_SUGGESTIONS.add(sug("Vue 3 app mount", "import { createApp } from 'vue';\nimport App from './App.vue';\n\nconst app = createApp(App);\napp.mount('#{CURSOR}');", "snippet", "Vue 3 app initialization"));
    }

    // ========================================================================
    //   SVELTE
    // ========================================================================
    private static void loadSvelte() {
        String[] svelteAttributes = {
            "{#if}", "{:else if}", "{:else}", "{/if}",
            "{#each}", "{:else}", "{/each}",
            "{#await}", "{:then}", "{:catch}", "{/await}",
            "{#key}", "{/key}",
            "bind:value", "bind:checked", "bind:this", "bind:group",
            "on:click", "on:submit", "on:input", "on:change", "on:keydown",
            "on:mouseover", "on:focus", "on:blur",
            "use:", "transition:", "animate:", "in:", "out:",
            "class:", "style:",
            "$:", "$store",
        };
        for (String attr : svelteAttributes) {
            SVELTE_SUGGESTIONS.add(sug(attr, attr, "attr", "Svelte template syntax / directive"));
        }
        SVELTE_SUGGESTIONS.add(sug("{#if}", "{#if {CURSOR}}\n    <p>Content</p>\n{/if}", "snippet", "Svelte conditional block"));
        SVELTE_SUGGESTIONS.add(sug("{#each}", "{#each {CURSOR} as item, index}\n    <p>{item}</p>\n{/each}", "snippet", "Svelte each loop"));
        SVELTE_SUGGESTIONS.add(sug("{#await}", "{#await {CURSOR}}\n    <p>Loading...</p>\n{:then data}\n    <p>{data}</p>\n{:catch error}\n    <p>{error.message}</p>\n{/await}", "snippet", "Svelte await block"));
        SVELTE_SUGGESTIONS.add(sug("script lang=ts", "<script lang=\"typescript\">\n    let {CURSOR}: Type = value;\n</script>", "snippet", "Svelte TypeScript script tag"));
        SVELTE_SUGGESTIONS.add(sug("reactive assignment", "$: {CURSOR} = derivedValue;", "snippet", "Svelte reactive declaration"));
        SVELTE_SUGGESTIONS.add(sug("on:click handler", "function handleClick() {\n    {CURSOR}\n}", "snippet", "Svelte click handler function"));
        SVELTE_SUGGESTIONS.add(sug("bind:this", "let element;\n\nbind:this={element}", "snippet", "Svelte bind:this reference"));
        SVELTE_SUGGESTIONS.add(sug("store writable", "import { writable } from 'svelte/store';\n\nexport const {CURSOR} = writable(initialValue);", "snippet", "Svelte writable store"));
        SVELTE_SUGGESTIONS.add(sug("store readable", "import { readable } from 'svelte/store';\n\nexport const {CURSOR} = readable(initial, (set) => {\n    return () => {};\n});", "snippet", "Svelte readable store"));
        SVELTE_SUGGESTIONS.add(sug("store derived", "import { derived } from 'svelte/store';\n\nexport const {CURSOR} = derived(store, ($) => {\n    return $ * 2;\n});", "snippet", "Svelte derived store"));
        SVELTE_SUGGESTIONS.add(sug("store auto-subscribe", "${CURSOR}", "snippet", "Svelte auto-subscription syntax"));
        SVELTE_SUGGESTIONS.add(sug("svelte:head", "<svelte:head>\n    <title>{CURSOR}</title>\n</svelte:head>", "snippet", "Svelte head element"));
        SVELTE_SUGGESTIONS.add(sug("svelte:body", "<svelte:body on:load=\"{}\" />", "snippet", "Svelte body element"));
        SVELTE_SUGGESTIONS.add(sug("svelte:window", "<svelte:window bind:scrollY={y} />", "snippet", "Svelte window binding"));
        SVELTE_SUGGESTIONS.add(sug("transition:fade", "transition:fade={{ duration: 300 }}", "snippet", "Svelte fade transition"));
        SVELTE_SUGGESTIONS.add(sug("transition:slide", "transition:slide", "snippet", "Svelte slide transition"));
        SVELTE_SUGGESTIONS.add(sug("animation:flip", "animation:flip", "snippet", "Svelte flip animation"));
        SVELTE_SUGGESTIONS.add(sug("use:action", "use:{CURSOR}", "snippet", "Svelte use action"));
        SVELTE_SUGGESTIONS.add(sug("onMount", "import { onMount } from 'svelte';\n\nonMount(() => {\n    {CURSOR}\n});", "snippet", "Svelte onMount lifecycle"));
        SVELTE_SUGGESTIONS.add(sug("onDestroy", "import { onDestroy } from 'svelte';\n\nonDestroy(() => {\n    {CURSOR}\n});", "snippet", "Svelte onDestroy lifecycle"));
        SVELTE_SUGGESTIONS.add(sug("beforeUpdate", "import { beforeUpdate } from 'svelte';\n\nbeforeUpdate(() => {\n    {CURSOR}\n});", "snippet", "Svelte beforeUpdate lifecycle"));
        SVELTE_SUGGESTIONS.add(sug("afterUpdate", "import { afterUpdate } from 'svelte';\n\nafterUpdate(() => {\n    {CURSOR}\n});", "snippet", "Svelte afterUpdate lifecycle"));
        SVELTE_SUGGESTIONS.add(sug("tick", "import { tick } from 'svelte';\n\nawait tick();\n{CURSOR}", "snippet", "Svelte tick after state change"));
        SVELTE_SUGGESTIONS.add(sug("createEventDispatcher", "import { createEventDispatcher } from 'svelte';\n\nconst dispatch = createEventDispatcher();\n\nfunction handleClick() {\n    dispatch('{CURSOR}', { data: value });\n}", "snippet", "Svelte event dispatcher"));
        SVELTE_SUGGESTIONS.add(sug("setContext", "import { setContext } from 'svelte';\n\nsetContext('{CURSOR}', reactiveValue);", "snippet", "Svelte context provider"));
        SVELTE_SUGGESTIONS.add(sug("getContext", "import { getContext } from 'svelte';\n\nconst value = getContext('{CURSOR}');", "snippet", "Svelte context consumer"));
        SVELTE_SUGGESTIONS.add(sug("hasContext", "import { hasContext } from 'svelte';\n\nif (hasContext('{CURSOR}')) {\n    const value = getContext('{CURSOR}');\n}", "snippet", "Svelte context existence check"));
        SVELTE_SUGGESTIONS.add(sug("svelte:self", "<svelte:self {CURSOR} />", "snippet", "Svelte recursive component"));
        SVELTE_SUGGESTIONS.add(sug("svelte:component", "<svelte:component this={selectedComponent} {CURSOR} />", "snippet", "Svelte dynamic component"));
        SVELTE_SUGGESTIONS.add(sug("svelte:fragment", "<svelte:fragment slot=\"{CURSOR}\">\n    <div>Content</div>\n</svelte:fragment>", "snippet", "Svelte named slot fragment"));
        SVELTE_SUGGESTIONS.add(sug("svelte:options", "<svelte:options accessors=\"{true}\" immutable=\"{true}\" />{CURSOR}", "snippet", "Svelte component options"));
        SVELTE_SUGGESTIONS.add(sug("svelte:element", "<svelte:element this=\"{CURSOR}\">Content</svelte:element>", "snippet", "Svelte dynamic element tag"));
        SVELTE_SUGGESTIONS.add(sug("reactive statement", "$: {CURSOR} = derivedValue;", "snippet", "Svelte reactive statement"));
        SVELTE_SUGGESTIONS.add(sug("reactive block", "$: {\n    console.log('{CURSOR} changed to', {CURSOR});\n}", "snippet", "Svelte reactive block"));
        SVELTE_SUGGESTIONS.add(sug("class directive", "class:{CURSOR}={condition}", "snippet", "Svelte class directive"));
        SVELTE_SUGGESTIONS.add(sug("style directive", "style:--{CURSOR}=\"{value}px\"", "snippet", "Svelte style directive"));
        SVELTE_SUGGESTIONS.add(sug("bind:value", "bind:value={CURSOR}", "snippet", "Svelte two-way binding"));
        SVELTE_SUGGESTIONS.add(sug("bind:this", "bind:this={elementRef}{CURSOR}", "snippet", "Svelte element reference"));
        SVELTE_SUGGESTIONS.add(sug("bind:group", "bind:group={selected}{CURSOR}", "snippet", "Svelte checkbox/radio group binding"));
        SVELTE_SUGGESTIONS.add(sug("transition:fade params", "transition:fade={{ duration: 300, delay: 100 }}", "snippet", "Svelte fade transition with params"));
        SVELTE_SUGGESTIONS.add(sug("transition:fly", "transition:fly={{ x: 200, duration: 500 }}", "snippet", "Svelte fly transition"));
        SVELTE_SUGGESTIONS.add(sug("transition:slide", "transition:slide={{ duration: 300 }}", "snippet", "Svelte slide transition"));
        SVELTE_SUGGESTIONS.add(sug("transition:scale", "transition:scale={{ start: 0.5, duration: 300 }}", "snippet", "Svelte scale transition"));
        SVELTE_SUGGESTIONS.add(sug("transition:draw", "transition:draw={{ duration: 500 }}", "snippet", "Svelte SVG draw transition"));
        SVELTE_SUGGESTIONS.add(sug("transition:crossfade", "transition:crossfade={{CURSOR}}", "snippet", "Svelte crossfade transition"));
        SVELTE_SUGGESTIONS.add(sug("transition:blur", "transition:blur={{ duration: 300 }}", "snippet", "Svelte blur transition"));
        SVELTE_SUGGESTIONS.add(sug("motion tweened", "import { tweened } from 'svelte/motion';\n\nconst progress = tweened(0, { duration: 400 });{CURSOR}", "snippet", "Svelte tweened motion store"));
        SVELTE_SUGGESTIONS.add(sug("motion spring", "import { spring } from 'svelte/motion';\n\nconst size = spring(100, { stiffness: 0.15, damping: 0.8 });{CURSOR}", "snippet", "Svelte spring motion store"));
        SVELTE_SUGGESTIONS.add(sug("easing", "import { cubicInOut } from 'svelte/easing';", "snippet", "Svelte easing import"));
        SVELTE_SUGGESTIONS.add(sug("store subscribe", "const unsubscribe = {CURSOR}.subscribe(value => {\n    console.log(value);\n});", "snippet", "Svelte store subscribe pattern"));
        SVELTE_SUGGESTIONS.add(sug("store update", "{CURSOR}.update(n => n + 1);", "snippet", "Svelte store update with callback"));
        SVELTE_SUGGESTIONS.add(sug("store set", "{CURSOR}.set(newValue);", "snippet", "Svelte store set value"));
        SVELTE_SUGGESTIONS.add(sug("store derived multi", "import { derived } from 'svelte/store';\n\nexport const summary = derived([store1, store2], ([$store1, $store2]) => {\n    return { $store1, $store2 };{CURSOR}\n});", "snippet", "Svelte derived store from multiple stores"));
        SVELTE_SUGGESTIONS.add(sug("script module context=\"module\"", "<script context=\"module\">\n    export function preload({ params }) {\n        return { props: { {CURSOR}: params.id } };\n    }\n</script>", "snippet", "Svelte module context script"));
        SVELTE_SUGGESTIONS.add(sug("svelte:head full", "<svelte:head>\n    <title>{CURSOR}</title>\n    <meta name=\"description\" content=\"\" />\n    <link rel=\"canonical\" href=\"\" />\n</svelte:head>", "snippet", "Svelte head with meta tags"));
        SVELTE_SUGGESTIONS.add(sug("svelte:body", "<svelte:body on:mouseenter=\"{handleEnter}\" />", "snippet", "Svelte body event listeners"));
        SVELTE_SUGGESTIONS.add(sug("svelte:window", "<svelte:window bind:scrollY={scrollY} on:keydown=\"{handleKey}\" />", "snippet", "Svelte window bindings"));
        SVELTE_SUGGESTIONS.add(sug("use:action fn", "function myAction(node, params) {\n    node.addEventListener('click', () => {\n        console.log('Clicked');\n        {CURSOR}\n    });\n    \n    return {\n        update(newParams) {},\n        destroy() { node.removeEventListener('click', () => {}); }\n    };\n}", "snippet", "Svelte custom action function"));
        SVELTE_SUGGESTIONS.add(sug("use:action params", "use:myAction={{ key: '{CURSOR}', value: 42 }}", "snippet", "Svelte action with parameters"));
    }

    // ========================================================================
    //   ENV
    // ========================================================================
    private static void loadEnv() {
        String[][] envItems = {
            {"NODE_ENV", "NODE_ENV={CURSOR}", "keyword"},
            {"PORT", "PORT={CURSOR}", "keyword"},
            {"DATABASE_URL", "DATABASE_URL={CURSOR}", "keyword"},
            {"DB_HOST", "DB_HOST={CURSOR}", "keyword"},
            {"DB_PORT", "DB_PORT={CURSOR}", "keyword"},
            {"DB_NAME", "DB_NAME={CURSOR}", "keyword"},
            {"DB_USER", "DB_USER={CURSOR}", "keyword"},
            {"DB_PASS", "DB_PASS={CURSOR}", "keyword"},
            {"REDIS_URL", "REDIS_URL={CURSOR}", "keyword"},
            {"REDIS_PORT", "REDIS_PORT={CURSOR}", "keyword"},
            {"JWT_SECRET", "JWT_SECRET={CURSOR}", "keyword"},
            {"JWT_EXPIRES_IN", "JWT_EXPIRES_IN={CURSOR}", "keyword"},
            {"API_KEY", "API_KEY={CURSOR}", "keyword"},
            {"API_URL", "API_URL={CURSOR}", "keyword"},
            {"SECRET_KEY", "SECRET_KEY={CURSOR}", "keyword"},
            {"APP_NAME", "APP_NAME={CURSOR}", "keyword"},
            {"APP_ENV", "APP_ENV={CURSOR}", "keyword"},
            {"APP_DEBUG", "APP_DEBUG={CURSOR}", "keyword"},
            {"APP_URL", "APP_URL={CURSOR}", "keyword"},
            {"LOG_LEVEL", "LOG_LEVEL={CURSOR}", "keyword"},
            {"SMTP_HOST", "SMTP_HOST={CURSOR}", "keyword"},
            {"SMTP_PORT", "SMTP_PORT={CURSOR}", "keyword"},
            {"SMTP_USER", "SMTP_USER={CURSOR}", "keyword"},
            {"SMTP_PASS", "SMTP_PASS={CURSOR}", "keyword"},
            {"AWS_ACCESS_KEY_ID", "AWS_ACCESS_KEY_ID={CURSOR}", "keyword"},
            {"AWS_SECRET_ACCESS_KEY", "AWS_SECRET_ACCESS_KEY={CURSOR}", "keyword"},
            {"AWS_REGION", "AWS_REGION={CURSOR}", "keyword"},
            {"AWS_S3_BUCKET", "AWS_S3_BUCKET={CURSOR}", "keyword"},
            {"GOOGLE_CLIENT_ID", "GOOGLE_CLIENT_ID={CURSOR}", "keyword"},
            {"GOOGLE_CLIENT_SECRET", "GOOGLE_CLIENT_SECRET={CURSOR}", "keyword"},
            {"SENTRY_DSN", "SENTRY_DSN={CURSOR}", "keyword"},
            {"STRIPE_KEY", "STRIPE_KEY={CURSOR}", "keyword"},
            {"STRIPE_SECRET", "STRIPE_SECRET={CURSOR}", "keyword"},
            {"MAIL_DRIVER", "MAIL_DRIVER={CURSOR}", "keyword"},
            {"MAIL_HOST", "MAIL_HOST={CURSOR}", "keyword"},
            {"MAIL_PORT", "MAIL_PORT={CURSOR}", "keyword"},
            {"MAIL_USERNAME", "MAIL_USERNAME={CURSOR}", "keyword"},
            {"MAIL_PASSWORD", "MAIL_PASSWORD={CURSOR}", "keyword"},
            {"MAIL_ENCRYPTION", "MAIL_ENCRYPTION={CURSOR}", "keyword"},
            {"CACHE_DRIVER", "CACHE_DRIVER={CURSOR}", "keyword"},
            {"SESSION_DRIVER", "SESSION_DRIVER={CURSOR}", "keyword"},
            {"QUEUE_CONNECTION", "QUEUE_CONNECTION={CURSOR}", "keyword"},
            {"FILESYSTEM_DISK", "FILESYSTEM_DISK={CURSOR}", "keyword"},
            {"LARAVEL_SAIL", "LARAVEL_SAIL={CURSOR}", "keyword"},
            {"MONGODB_URI", "MONGODB_URI={CURSOR}", "keyword"},
            {"ELASTICSEARCH_HOST", "ELASTICSEARCH_HOST={CURSOR}", "keyword"},
            {"RABBITMQ_HOST", "RABBITMQ_HOST={CURSOR}", "keyword"},
            {"RABBITMQ_PORT", "RABBITMQ_PORT={CURSOR}", "keyword"},
            {"RABBITMQ_USER", "RABBITMQ_USER={CURSOR}", "keyword"},
            {"RABBITMQ_PASS", "RABBITMQ_PASS={CURSOR}", "keyword"},
        };
        for (String[] v : envItems) {
            ENV_SUGGESTIONS.add(sug(v[0], v[1], v[2], "Environment variable"));
        }
        ENV_SUGGESTIONS.add(sug("# comment", "# {CURSOR}", "snippet", "ENV comment"));
    }

    // ========================================================================
    //   GITIGNORE
    // ========================================================================
    private static void loadGitignore() {
        String[][] gitignoreItems = {
            {"node_modules/", "node_modules/", "keyword"},
            {".env", ".env", "keyword"},
            {".env.local", ".env.local", "keyword"},
            {".env.*.local", ".env.*.local", "keyword"},
            {"dist/", "dist/", "keyword"},
            {"build/", "build/", "keyword"},
            {"target/", "target/", "keyword"},
            {"bin/", "bin/", "keyword"},
            {"obj/", "obj/", "keyword"},
            {"__pycache__/", "__pycache__/", "keyword"},
            {"*.pyc", "*.pyc", "keyword"},
            {"*.pyo", "*.pyo", "keyword"},
            {"*.class", "*.class", "keyword"},
            {"*.jar", "*.jar", "keyword"},
            {"*.war", "*.war", "keyword"},
            {"*.log", "*.log", "keyword"},
            {"*.tmp", "*.tmp", "keyword"},
            {"*.swp", "*.swp", "keyword"},
            {".DS_Store", ".DS_Store", "keyword"},
            {"Thumbs.db", "Thumbs.db", "keyword"},
            {"*.exe", "*.exe", "keyword"},
            {"*.dll", "*.dll", "keyword"},
            {"*.so", "*.so", "keyword"},
            {"*.dylib", "*.dylib", "keyword"},
            {"*.o", "*.o", "keyword"},
            {"*.obj", "*.obj", "keyword"},
            {"*.lib", "*.lib", "keyword"},
            {"vendor/", "vendor/", "keyword"},
            {".gradle/", ".gradle/", "keyword"},
            {"!.gradle/", "!.gradle/", "keyword"},
            {"*.iml", "*.iml", "keyword"},
            {".idea/", ".idea/", "keyword"},
            {".vscode/", ".vscode/", "keyword"},
            {"*.sublime-*", "*.sublime-*", "keyword"},
            {"coverage/", "coverage/", "keyword"},
            {".nyc_output/", ".nyc_output/", "keyword"},
            {"*.tsbuildinfo", "*.tsbuildinfo", "keyword"},
            {"next-env.d.ts", "next-env.d.ts", "keyword"},
            {".next/", ".next/", "keyword"},
            {"out/", "out/", "keyword"},
            {".nuxt/", ".nuxt/", "keyword"},
            {"*.nuxtignore", "*.nuxtignore", "keyword"},
            {"package-lock.json", "package-lock.json", "keyword"},
            {"yarn.lock", "yarn.lock", "keyword"},
            {".pnpm-store/", ".pnpm-store/", "keyword"},
            {"*.tgz", "*.tgz", "keyword"},
            {".serverless/", ".serverless/", "keyword"},
            {"terraform.tfstate", "terraform.tfstate", "keyword"},
            {"*.tfstate", "*.tfstate", "keyword"},
            {"crashlytics-*", "crashlytics-*", "keyword"},
            {"*.hprof", "*.hprof", "keyword"},
        };
        for (String[] v : gitignoreItems) {
            GITIGNORE_SUGGESTIONS.add(sug(v[0], v[1], v[2], "Gitignore pattern"));
        }
        GITIGNORE_SUGGESTIONS.add(sug("# comment", "# {CURSOR}", "snippet", "Gitignore comment"));
    }

    // ========================================================================
    //   ASTRO
    // ========================================================================
    private static void loadAstro() {
        String[] astroKeywords = {
            "---", "Astro.glob", "Astro.props", "Astro.slots", "Astro.request",
            "Astro.url", "Astro.cookies", "Astro.redirect", "Astro.response",
            "Astro.params", "Astro.params.id", "Astro.params.slug",
            "Astro.params.username",
            "Astro.params",
            "Astro.params.",
            "Astro.params.id",
            "Astro.params.slug",
            "Astro.params.username",
            "Astro.params.pages",
        };
        for (String kw : astroKeywords) {
            ASTRO_SUGGESTIONS.add(sug(kw, kw, "keyword", "Astro global / built-in"));
        }
        ASTRO_SUGGESTIONS.add(sug("Astro frontmatter", "---\nimport Layout from '../layouts/Layout.astro';\nconst { CURSOR } = Astro.props;\n---\n", "snippet", "Astro component frontmatter block"));
        ASTRO_SUGGESTIONS.add(sug("Astro page", "---\nimport Layout from '../layouts/Layout.astro';\nconst { CURSOR } = Astro.props;\n---\n<Layout title=\"Page\">\n    <main>\n        <slot />\n    </main>\n</Layout>", "snippet", "Astro page component with layout"));
        ASTRO_SUGGESTIONS.add(sug("Astro Layout", "---\nexport interface Props {\n    title: string;\n}\nconst { title } = Astro.props;\n---\n<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n    <meta charset=\"UTF-8\" />\n    <title>{title}{CURSOR}</title>\n</head>\n<body>\n    <slot />\n</body>\n</html>", "snippet", "Astro layout component"));
        ASTRO_SUGGESTIONS.add(sug("Astro component", "---\ninterface Props {\n    {CURSOR}: string;\n}\nconst { } = Astro.props;\n---\n<div class=\"component\">\n    <slot />\n</div>\n<style>\n    .component { color: inherit; }\n</style>", "snippet", "Astro reusable component"));
        ASTRO_SUGGESTIONS.add(sug("Astro fetch", "---\nconst response = await fetch('{CURSOR}');\nconst data = await response.json();\n---\n<pre>{JSON.stringify(data, null, 2)}</pre>", "snippet", "Astro server-side fetch"));
        ASTRO_SUGGESTIONS.add(sug("Astro Markdown", "---\nimport Layout from '../layouts/Layout.astro';\nconst { CURSOR } = Astro.props;\n---\n<Layout>\n    <article class=\"prose\">\n        <slot />\n    </article>\n</Layout>", "snippet", "Astro Markdown/MDX integration"));
        ASTRO_SUGGESTIONS.add(sug("Astro islands", "---\nimport ReactComponent from '../components/ReactComponent.jsx';\n---\n<ReactComponent client:load />{CURSOR}", "snippet", "Astro client island with React"));
        ASTRO_SUGGESTIONS.add(sug("Astro dynamic route", "---\nexport async function getStaticPaths() {\n    const posts = await Astro.glob('./posts/*.md');\n    return posts.map(post => ({\n        params: { slug: post.frontmatter.slug },\n    }));\n}\nconst { slug } = Astro.params;\n---\n<h1>{CURSOR}</h1>", "snippet", "Astro dynamic route with getStaticPaths"));
        ASTRO_SUGGESTIONS.add(sug("Astro endpoint", "---\nexport async function GET({ params, request }) {\n    return new Response(JSON.stringify({ message: '{CURSOR}' }), {\n        headers: { 'Content-Type': 'application/json' }\n    });\n}\n---", "snippet", "Astro API endpoint"));
        ASTRO_SUGGESTIONS.add(sug("Astro env", "---\nconst API_URL = import.meta.env.{CURSOR};\n---", "snippet", "Astro environment variable access"));
        ASTRO_SUGGESTIONS.add(sug("Astro view transitions", "---\n---\n<html lang=\"en\">\n<head>\n    <meta name=\"astro-view-transitions\" />\n</head>\n<body>\n    <slot />{CURSOR}\n</body>\n</html>", "snippet", "Astro view transitions enabled layout"));
        ASTRO_SUGGESTIONS.add(sug("Astro content collection", "---\nimport { getCollection } from 'astro:content';\n\nexport async function getStaticPaths() {\n    const posts = await getCollection('{CURSOR}');\n    return posts.map(post => ({\n        params: { slug: post.slug },\n        props: { post },\n    }));\n}\n\nconst { post } = Astro.props;\nconst { Content } = await post.render();\n---\n<h1>{post.data.title}</h1>\n<Content />", "snippet", "Astro content collection SSG"));
        ASTRO_SUGGESTIONS.add(sug("Astro content collection entry", "---\nimport { getEntry } from 'astro:content';\n\nconst post = await getEntry('{CURSOR}', 'slug');\nconst { Content } = await post.render();\n---\n<Content />", "snippet", "Astro get single content entry"));
        ASTRO_SUGGESTIONS.add(sug("Astro content collection query", "---\nimport { getCollection } from 'astro:content';\n\nconst posts = await getCollection('{CURSOR}', ({ data }) => {\n    return data.draft !== true;\n});\n---\n{posts.map(post => <li>{post.data.title}</li>)}", "snippet", "Astro filtered content collection"));
        ASTRO_SUGGESTIONS.add(sug("Astro content config", "import { defineCollection, z } from 'astro:content';\n\nexport const collections = {\n    '{CURSOR}': defineCollection({\n        schema: z.object({\n            title: z.string(),\n            date: z.date(),\n            description: z.string(),\n        }),\n    }),\n};", "snippet", "Astro content collection schema config"));
        ASTRO_SUGGESTIONS.add(sug("Astro Image", "---\nimport { Image } from 'astro:assets';\nimport hero from '../images/{CURSOR}';\n---\n<Image src={hero} alt=\"Hero\" width={800} height={400} />", "snippet", "Astro Image optimization"));
        ASTRO_SUGGESTIONS.add(sug("Astro Picture", "---\nimport { Picture } from 'astro:assets';\nimport image from '../images/{CURSOR}';\n---\n<Picture src={image} formats={['avif', 'webp']} alt=\"\" />", "snippet", "Astro Picture component with formats"));
        ASTRO_SUGGESTIONS.add(sug("Astro ViewTransitions API", "---\nimport { ViewTransitions } from 'astro:transitions';\n---\n<html lang=\"en\">\n<head>\n    <ViewTransitions />\n    <title>{CURSOR}</title>\n</head>\n<body>\n    <slot />\n</body>\n</html>", "snippet", "Astro ViewTransitions component"));
        ASTRO_SUGGESTIONS.add(sug("Astro transition:animate", "<a href=\"/{CURSOR}\" transition:animate=\"slide\">Link</a>", "snippet", "Astro view transition animation per link"));
        ASTRO_SUGGESTIONS.add(sug("Astro middleware", "// src/middleware.ts\nimport { defineMiddleware } from 'astro:middleware';\n\nexport const onRequest = defineMiddleware(async (context, next) => {\n    const url = new URL(context.request.url);\n    \n    if (url.pathname.startsWith('/api/{CURSOR}')) {\n        const auth = context.request.headers.get('Authorization');\n        if (!auth) {\n            return new Response('Unauthorized', { status: 401 });\n        }\n    }\n    \n    return next();\n});", "snippet", "Astro middleware definition"));
        ASTRO_SUGGESTIONS.add(sug("Astro middleware redirect", "// src/middleware.ts\nimport { defineMiddleware } from 'astro:middleware';\n\nexport const onRequest = defineMiddleware((context, next) => {\n    if (context.url.pathname === '/old') {\n        return context.redirect('/new');\n    }\n    return next();\n});", "snippet", "Astro middleware redirect"));
        ASTRO_SUGGESTIONS.add(sug("Astro server island", "---\nimport HeavyComponent from '../components/HeavyComponent.tsx';\n---\n<HeavyComponent server:defer />{CURSOR}", "snippet", "Astro server island (defer)"));
        ASTRO_SUGGESTIONS.add(sug("Astro client:* directives", "<MyComponent client:load client:visible client:idle client:media=\"(max-width: 768px)\" client:only=\"react\" />{CURSOR}", "snippet", "Astro client hydration directives"));
        ASTRO_SUGGESTIONS.add(sug("Astro i18n routing", "---\nimport { getRelativeLocaleUrl } from 'astro:i18n';\n---\n<a href={getRelativeLocaleUrl('{CURSOR}', '/page')}>English</a>", "snippet", "Astro i18n routing helper"));
        ASTRO_SUGGESTIONS.add(sug("Astro env variables", "---\nconst API_URL = import.meta.env.{CURSOR};\nconst PUBLIC_KEY = import.meta.env.PUBLIC_API_KEY;\n---\n<div>{API_URL}</div>", "snippet", "Astro env variable access"));
        ASTRO_SUGGESTIONS.add(sug("Astro integration", "// astro.config.mjs\nimport { defineConfig } from 'astro/config';\nimport react from '@astrojs/react';\nimport tailwind from '@astrojs/tailwind';\n\nexport default defineConfig({\n    integrations: [\n        react(),\n        tailwind(),\n        {CURSOR}\n    ]\n});", "snippet", "Astro config with integrations"));
        ASTRO_SUGGESTIONS.add(sug("Astro Tailwind config", "// astro.config.mjs\nimport { defineConfig } from 'astro/config';\nimport tailwind from '@astrojs/tailwind';\n\nexport default defineConfig({\n    integrations: [tailwind({CURSOR})]\n});", "snippet", "Astro with Tailwind CSS"));
        ASTRO_SUGGESTIONS.add(sug("Astro React config", "// astro.config.mjs\nimport { defineConfig } from 'astro/config';\nimport react from '@astrojs/react';\n\nexport default defineConfig({\n    integrations: [react({ include: ['**/{CURSOR}*'] })]\n});", "snippet", "Astro React integration config"));
        ASTRO_SUGGESTIONS.add(sug("Astro RSS", "---\nimport rss from '@astrojs/rss';\n\nexport async function GET(context) {\n    return rss({\n        title: '{CURSOR}',\n        description: 'Site description',\n        site: context.site,\n        items: [],\n        customData: '<language>en-us</language>',\n    });\n}\n---", "snippet", "Astro RSS feed endpoint"));
        ASTRO_SUGGESTIONS.add(sug("Astro sitemap", "// astro.config.mjs\nimport sitemap from '@astrojs/sitemap';\n\nexport default defineConfig({\n    site: 'https://{CURSOR}',\n    integrations: [sitemap()]\n});", "snippet", "Astro sitemap integration"));
        ASTRO_SUGGESTIONS.add(sug("Astro prefetch", "<a href=\"/{CURSOR}\" data-astro-prefetch>Link</a>", "snippet", "Astro prefetch link"));
        ASTRO_SUGGESTIONS.add(sug("Astro form endpoint", "---\nexport async function POST({ request }) {\n    const formData = await request.formData();\n    const name = formData.get('{CURSOR}');\n    \n    return new Response(JSON.stringify({ success: true }), {\n        status: 200,\n        headers: { 'Content-Type': 'application/json' }\n    });\n}\n---", "snippet", "Astro POST form endpoint"));
        ASTRO_SUGGESTIONS.add(sug("Astro streaming", "---\nexport async function GET() {\n    const stream = new ReadableStream({\n        start(controller) {\n            controller.enqueue(JSON.stringify({ message: '{CURSOR}' }));\n            controller.close();\n        }\n    });\n    return new Response(stream);\n}\n---", "snippet", "Astro streaming response"));
    }

    // ========================================================================
    //   PUBLIC API
    // ========================================================================

    public static List<Suggestion> getSuggestions(LanguageType lang, String prefix) {
        List<Suggestion> source = getSource(lang);
        if (prefix == null || prefix.isEmpty()) return Collections.emptyList();
        String lowerPrefix = prefix.toLowerCase();
        List<Suggestion> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Suggestion s : source) {
            String lowerLabel = s.label.toLowerCase();
            if (lowerLabel.startsWith(lowerPrefix) || lowerLabel.contains(lowerPrefix)) {
                if (seen.add(lowerLabel)) {
                    result.add(s);
                }
            }
        }
        result.sort((a, b) -> {
            boolean aStarts = a.label.toLowerCase().startsWith(lowerPrefix);
            boolean bStarts = b.label.toLowerCase().startsWith(lowerPrefix);
            if (aStarts && !bStarts) return -1;
            if (!aStarts && bStarts) return 1;
            return a.label.compareToIgnoreCase(b.label);
        });
        return result.size() > 18 ? result.subList(0, 18) : result;
    }

    public static List<Suggestion> getAllSuggestions(LanguageType lang) {
        return new ArrayList<>(getSource(lang));
    }

    public static void addSuggestion(LanguageType lang, Suggestion s) {
        getSource(lang).add(s);
        saveUserSnippets();
    }

    public static void removeSuggestion(LanguageType lang, Suggestion s) {
        getSource(lang).remove(s);
        saveUserSnippets();
    }

    public static void removeSuggestionByLabel(LanguageType lang, String label) {
        List<Suggestion> source = getSource(lang);
        Iterator<Suggestion> it = source.iterator();
        while (it.hasNext()) {
            if (it.next().label.equals(label)) {
                it.remove();
                break;
            }
        }
        saveUserSnippets();
    }

    public static List<LanguageType> getSupportedLanguages() {
        return java.util.Arrays.asList(LanguageType.HTML, LanguageType.CSS,
                LanguageType.JAVASCRIPT, LanguageType.TYPESCRIPT, LanguageType.JSX, LanguageType.TSX,
                LanguageType.JAVA, LanguageType.KOTLIN, LanguageType.GO, LanguageType.RUST,
                LanguageType.XML, LanguageType.SVG,
                LanguageType.JSON, LanguageType.PHP, LanguageType.PYTHON, LanguageType.SQL,
                LanguageType.SCSS, LanguageType.LESS, LanguageType.SASS,
                LanguageType.YAML, LanguageType.SH, LanguageType.DOCKERFILE,
                LanguageType.C, LanguageType.CPP, LanguageType.MARKDOWN,
                LanguageType.VUE, LanguageType.SVELTE, LanguageType.ASTRO,
                LanguageType.ENV, LanguageType.GITIGNORE);
    }

    /** Get suggestions filtered by exact label prefix match and category. */
    public static List<Suggestion> getSuggestionsByExactPrefix(LanguageType lang, String prefix, String category) {
        List<Suggestion> source = getSource(lang);
        if (prefix == null || prefix.isEmpty()) return Collections.emptyList();
        String lowerPrefix = prefix.toLowerCase();
        List<Suggestion> result = new ArrayList<>();
        for (Suggestion s : source) {
            if (!s.category.equals(category)) continue;
            String lowerLabel = s.label.toLowerCase();
            if (lowerLabel.startsWith(lowerPrefix)) {
                result.add(s);
            }
        }
        result.sort((a, b) -> a.label.compareToIgnoreCase(b.label));
        return result.size() > 18 ? result.subList(0, 18) : result;
    }

    private static List<Suggestion> getSource(LanguageType lang) {
        switch (lang) {
            case HTML: return HTML_SUGGESTIONS;
            case CSS: return CSS_SUGGESTIONS;
            case JAVASCRIPT:
            case TYPESCRIPT:
            case JSX:
            case TSX: return JS_SUGGESTIONS;
            case JAVA: return JAVA_SUGGESTIONS;
            case XML:
            case SVG: return XML_SUGGESTIONS;
            case JSON: return JSON_SUGGESTIONS;
            case PHP: return PHP_SUGGESTIONS;
            case PYTHON: return PYTHON_SUGGESTIONS;
            case SQL: return SQL_SUGGESTIONS;
            case SCSS:
            case LESS:
            case SASS: return SCSS_SUGGESTIONS;
            case YAML: return YAML_SUGGESTIONS;
            case SH:
            case DOCKERFILE: return SH_SUGGESTIONS;
            case C: return C_SUGGESTIONS;
            case CPP: return CPP_SUGGESTIONS;
            case MARKDOWN: return MARKDOWN_SUGGESTIONS;
            case VUE: return VUE_SUGGESTIONS;
            case SVELTE: return SVELTE_SUGGESTIONS;
            case ASTRO: return ASTRO_SUGGESTIONS;
            case ENV: return ENV_SUGGESTIONS;
            case GITIGNORE: return GITIGNORE_SUGGESTIONS;
            case KOTLIN: return KOTLIN_SUGGESTIONS;
            case GO: return GO_SUGGESTIONS;
            case RUST: return RUST_SUGGESTIONS;
            default: return Collections.emptyList();
        }
    }

    // ========================================================================
    //   SIGNATURE HELP (parameter hints for common functions)
    // ========================================================================

    public static class Signature {
        public final String label;
        public final String[] params;
        public final String returnType;
        public final String description;

        public Signature(String label, String[] params, String returnType, String description) {
            this.label = label;
            this.params = params;
            this.returnType = returnType;
            this.description = description;
        }
    }

    private static final Map<String, Signature[]> JS_SIGNATURES = new HashMap<>();
    private static final Map<String, Signature[]> HTML_SIGNATURES = new HashMap<>();
    private static final Map<String, Signature[]> JAVA_SIGNATURES = new HashMap<>();
    private static final Map<String, Signature[]> PYTHON_SIGNATURES = new HashMap<>();
    private static final Map<String, Signature[]> CSS_SIGNATURES = new HashMap<>();

    static {
        loadSignatures();
    }

    private static void loadSignatures() {
        JS_SIGNATURES.put("console.log", new Signature[]{
            new Signature("console.log(message?, ...optionalParams?)", new String[]{"message?: any", "optionalParams?: any[]"}, "void", "Logs a message to the console")
        });
        JS_SIGNATURES.put("console.error", new Signature[]{
            new Signature("console.error(message?, ...optionalParams?)", new String[]{"message?: any", "optionalParams?: any[]"}, "void", "Logs an error to the console")
        });
        JS_SIGNATURES.put("console.warn", new Signature[]{
            new Signature("console.warn(message?, ...optionalParams?)", new String[]{"message?: any", "optionalParams?: any[]"}, "void", "Logs a warning to the console")
        });
        JS_SIGNATURES.put("console.table", new Signature[]{
            new Signature("console.table(data?)", new String[]{"data?: any"}, "void", "Displays tabular data as a table")
        });
        JS_SIGNATURES.put("fetch", new Signature[]{
            new Signature("fetch(url, options?)", new String[]{"url: string", "options?: RequestInit"}, "Promise<Response>", "Starts the process of fetching a resource")
        });
        JS_SIGNATURES.put("document.querySelector", new Signature[]{
            new Signature("document.querySelector(selectors)", new String[]{"selectors: string"}, "Element|null", "Returns the first matching Element")
        });
        JS_SIGNATURES.put("document.querySelectorAll", new Signature[]{
            new Signature("document.querySelectorAll(selectors)", new String[]{"selectors: string"}, "NodeList", "Returns all matching elements")
        });
        JS_SIGNATURES.put("document.getElementById", new Signature[]{
            new Signature("document.getElementById(id)", new String[]{"id: string"}, "HTMLElement|null", "Returns the element with the given ID")
        });
        JS_SIGNATURES.put("document.createElement", new Signature[]{
            new Signature("document.createElement(tagName)", new String[]{"tagName: string"}, "HTMLElement", "Creates a new HTML element")
        });
        JS_SIGNATURES.put("Array.from", new Signature[]{
            new Signature("Array.from(arrayLike, mapFn?)", new String[]{"arrayLike: ArrayLike", "mapFn?: (value, index) => any"}, "any[]", "Creates an array from an iterable")
        });
        JS_SIGNATURES.put("Array.isArray", new Signature[]{
            new Signature("Array.isArray(value)", new String[]{"value: any"}, "boolean", "Checks if value is an array")
        });
        JS_SIGNATURES.put("JSON.parse", new Signature[]{
            new Signature("JSON.parse(text, reviver?)", new String[]{"text: string", "reviver?: (key, value) => any"}, "any", "Parses a JSON string")
        });
        JS_SIGNATURES.put("JSON.stringify", new Signature[]{
            new Signature("JSON.stringify(value, replacer?, space?)", new String[]{"value: any", "replacer?: Function|any[]", "space?: number|string"}, "string", "Converts a value to JSON string")
        });
        JS_SIGNATURES.put("Math.max", new Signature[]{
            new Signature("Math.max(...values)", new String[]{"values: number[]"}, "number", "Returns the largest of given numbers")
        });
        JS_SIGNATURES.put("Math.min", new Signature[]{
            new Signature("Math.min(...values)", new String[]{"values: number[]"}, "number", "Returns the smallest of given numbers")
        });
        JS_SIGNATURES.put("Math.floor", new Signature[]{
            new Signature("Math.floor(x)", new String[]{"x: number"}, "number", "Rounds down to nearest integer")
        });
        JS_SIGNATURES.put("Math.ceil", new Signature[]{
            new Signature("Math.ceil(x)", new String[]{"x: number"}, "number", "Rounds up to nearest integer")
        });
        JS_SIGNATURES.put("Math.round", new Signature[]{
            new Signature("Math.round(x)", new String[]{"x: number"}, "number", "Rounds to nearest integer")
        });
        JS_SIGNATURES.put("Math.random", new Signature[]{
            new Signature("Math.random()", new String[]{}, "number", "Returns a random number between 0 and 1")
        });
        JS_SIGNATURES.put("Math.abs", new Signature[]{
            new Signature("Math.abs(x)", new String[]{"x: number"}, "number", "Returns absolute value")
        });
        JS_SIGNATURES.put("Math.sqrt", new Signature[]{
            new Signature("Math.sqrt(x)", new String[]{"x: number"}, "number", "Returns square root")
        });
        JS_SIGNATURES.put("Math.pow", new Signature[]{
            new Signature("Math.pow(base, exp)", new String[]{"base: number", "exp: number"}, "number", "Returns base raised to exponent")
        });
        JS_SIGNATURES.put("parseInt", new Signature[]{
            new Signature("parseInt(string, radix?)", new String[]{"string: string", "radix?: number"}, "number", "Parses a string to integer")
        });
        JS_SIGNATURES.put("parseFloat", new Signature[]{
            new Signature("parseFloat(string)", new String[]{"string: string"}, "number", "Parses a string to float")
        });
        JS_SIGNATURES.put("setTimeout", new Signature[]{
            new Signature("setTimeout(callback, delay?, ...args?)", new String[]{"callback: Function", "delay?: number", "args?: any[]"}, "number", "Calls a function after a delay")
        });
        JS_SIGNATURES.put("setInterval", new Signature[]{
            new Signature("setInterval(callback, delay?, ...args?)", new String[]{"callback: Function", "delay?: number", "args?: any[]"}, "number", "Calls a function repeatedly at intervals")
        });
        JS_SIGNATURES.put("encodeURI", new Signature[]{
            new Signature("encodeURI(uri)", new String[]{"uri: string"}, "string", "Encodes a URI")
        });
        JS_SIGNATURES.put("encodeURIComponent", new Signature[]{
            new Signature("encodeURIComponent(component)", new String[]{"component: string"}, "string", "Encodes a URI component")
        });
        JS_SIGNATURES.put("decodeURI", new Signature[]{
            new Signature("decodeURI(encodedURI)", new String[]{"encodedURI: string"}, "string", "Decodes a URI")
        });
        JS_SIGNATURES.put("decodeURIComponent", new Signature[]{
            new Signature("decodeURIComponent(encodedComponent)", new String[]{"encodedComponent: string"}, "string", "Decodes a URI component")
        });
        JS_SIGNATURES.put("String.prototype.substr", new Signature[]{
            new Signature("str.substr(start, length?)", new String[]{"start: number", "length?: number"}, "string", "Returns substring starting at index")
        });
        JS_SIGNATURES.put("String.prototype.substring", new Signature[]{
            new Signature("str.substring(start, end?)", new String[]{"start: number", "end?: number"}, "string", "Returns substring between indices")
        });
        JS_SIGNATURES.put("String.prototype.slice", new Signature[]{
            new Signature("str.slice(begin, end?)", new String[]{"begin: number", "end?: number"}, "string", "Extracts a section of a string")
        });
        JS_SIGNATURES.put("String.prototype.split", new Signature[]{
            new Signature("str.split(separator, limit?)", new String[]{"separator: string|RegExp", "limit?: number"}, "string[]", "Splits string into array")
        });
        JS_SIGNATURES.put("String.prototype.replace", new Signature[]{
            new Signature("str.replace(pattern, replacement)", new String[]{"pattern: string|RegExp", "replacement: string"}, "string", "Replaces matched text")
        });
        JS_SIGNATURES.put("String.prototype.replaceAll", new Signature[]{
            new Signature("str.replaceAll(pattern, replacement)", new String[]{"pattern: string|RegExp", "replacement: string"}, "string", "Replaces all matched text")
        });
        JS_SIGNATURES.put("String.prototype.match", new Signature[]{
            new Signature("str.match(regexp)", new String[]{"regexp: RegExp"}, "RegExpMatchArray|null", "Matches string against regex")
        });
        JS_SIGNATURES.put("String.prototype.search", new Signature[]{
            new Signature("str.search(regexp)", new String[]{"regexp: RegExp"}, "number", "Searches for match, returns index")
        });
        JS_SIGNATURES.put("String.prototype.includes", new Signature[]{
            new Signature("str.includes(search, position?)", new String[]{"search: string", "position?: number"}, "boolean", "Checks if string contains substring")
        });
        JS_SIGNATURES.put("String.prototype.startsWith", new Signature[]{
            new Signature("str.startsWith(search, position?)", new String[]{"search: string", "position?: number"}, "boolean", "Checks if string starts with substring")
        });
        JS_SIGNATURES.put("String.prototype.endsWith", new Signature[]{
            new Signature("str.endsWith(search, length?)", new String[]{"search: string", "length?: number"}, "boolean", "Checks if string ends with substring")
        });
        JS_SIGNATURES.put("String.prototype.trim", new Signature[]{
            new Signature("str.trim()", new String[]{}, "string", "Trims whitespace from both ends")
        });
        JS_SIGNATURES.put("String.prototype.toLowerCase", new Signature[]{
            new Signature("str.toLowerCase()", new String[]{}, "string", "Converts to lowercase")
        });
        JS_SIGNATURES.put("String.prototype.toUpperCase", new Signature[]{
            new Signature("str.toUpperCase()", new String[]{}, "string", "Converts to uppercase")
        });
        JS_SIGNATURES.put("String.prototype.charAt", new Signature[]{
            new Signature("str.charAt(index)", new String[]{"index: number"}, "string", "Returns character at index")
        });
        JS_SIGNATURES.put("String.prototype.charCodeAt", new Signature[]{
            new Signature("str.charCodeAt(index)", new String[]{"index: number"}, "number", "Returns Unicode value at index")
        });
        JS_SIGNATURES.put("String.prototype.concat", new Signature[]{
            new Signature("str.concat(...strings)", new String[]{"strings: string[]"}, "string", "Concatenates strings")
        });
        JS_SIGNATURES.put("String.prototype.repeat", new Signature[]{
            new Signature("str.repeat(count)", new String[]{"count: number"}, "string", "Repeats string count times")
        });
        JS_SIGNATURES.put("String.prototype.padStart", new Signature[]{
            new Signature("str.padStart(targetLength, pad?)", new String[]{"targetLength: number", "pad?: string"}, "string", "Pads string from start")
        });
        JS_SIGNATURES.put("String.prototype.padEnd", new Signature[]{
            new Signature("str.padEnd(targetLength, pad?)", new String[]{"targetLength: number", "pad?: string"}, "string", "Pads string from end")
        });
        JS_SIGNATURES.put("String.prototype.localeCompare", new Signature[]{
            new Signature("str.localeCompare(other)", new String[]{"other: string"}, "number", "Compares strings lexicographically")
        });
        JS_SIGNATURES.put("Number.prototype.toFixed", new Signature[]{
            new Signature("num.toFixed(digits?)", new String[]{"digits?: number"}, "string", "Formats number with fixed decimals")
        });
        JS_SIGNATURES.put("Number.prototype.toPrecision", new Signature[]{
            new Signature("num.toPrecision(precision?)", new String[]{"precision?: number"}, "string", "Formats number with precision")
        });
        JS_SIGNATURES.put("Number.prototype.toExponential", new Signature[]{
            new Signature("num.toExponential(fractionDigits?)", new String[]{"fractionDigits?: number"}, "string", "Formats number in exponential notation")
        });
        JS_SIGNATURES.put("Date.now", new Signature[]{
            new Signature("Date.now()", new String[]{}, "number", "Returns current timestamp in ms")
        });
        JS_SIGNATURES.put("Promise.all", new Signature[]{
            new Signature("Promise.all(iterable)", new String[]{"iterable: Iterable<Promise>"}, "Promise<any[]>", "Resolves when all promises resolve")
        });
        JS_SIGNATURES.put("Promise.allSettled", new Signature[]{
            new Signature("Promise.allSettled(iterable)", new String[]{"iterable: Iterable<Promise>"}, "Promise<PromiseSettledResult[]>", "Resolves when all promises settle")
        });
        JS_SIGNATURES.put("Promise.race", new Signature[]{
            new Signature("Promise.race(iterable)", new String[]{"iterable: Iterable<Promise>"}, "Promise<any>", "Resolves/rejects with first promise result")
        });
        JS_SIGNATURES.put("Promise.any", new Signature[]{
            new Signature("Promise.any(iterable)", new String[]{"iterable: Iterable<Promise>"}, "Promise<any>", "Resolves with first fulfilled promise")
        });
        JS_SIGNATURES.put("localStorage.getItem", new Signature[]{
            new Signature("localStorage.getItem(key)", new String[]{"key: string"}, "string|null", "Returns stored value for key")
        });
        JS_SIGNATURES.put("localStorage.setItem", new Signature[]{
            new Signature("localStorage.setItem(key, value)", new String[]{"key: string", "value: string"}, "void", "Stores a value")
        });
        JS_SIGNATURES.put("localStorage.removeItem", new Signature[]{
            new Signature("localStorage.removeItem(key)", new String[]{"key: string"}, "void", "Removes stored value")
        });
        JS_SIGNATURES.put("localStorage.clear", new Signature[]{
            new Signature("localStorage.clear()", new String[]{}, "void", "Clears all stored values")
        });
        JS_SIGNATURES.put("sessionStorage.getItem", new Signature[]{
            new Signature("sessionStorage.getItem(key)", new String[]{"key: string"}, "string|null", "Returns stored value for key")
        });
        JS_SIGNATURES.put("sessionStorage.setItem", new Signature[]{
            new Signature("sessionStorage.setItem(key, value)", new String[]{"key: string", "value: string"}, "void", "Stores a value")
        });
        JS_SIGNATURES.put("sessionStorage.removeItem", new Signature[]{
            new Signature("sessionStorage.removeItem(key)", new String[]{"key: string"}, "void", "Removes stored value")
        });
        JS_SIGNATURES.put("navigator.clipboard.writeText", new Signature[]{
            new Signature("navigator.clipboard.writeText(text)", new String[]{"text: string"}, "Promise<void>", "Copies text to clipboard")
        });
        JS_SIGNATURES.put("navigator.clipboard.readText", new Signature[]{
            new Signature("navigator.clipboard.readText()", new String[]{}, "Promise<string>", "Reads text from clipboard")
        });
        JS_SIGNATURES.put("URL.createObjectURL", new Signature[]{
            new Signature("URL.createObjectURL(object)", new String[]{"object: Blob|MediaSource"}, "string", "Creates a blob URL")
        });
        JS_SIGNATURES.put("atob", new Signature[]{
            new Signature("atob(data)", new String[]{"data: string"}, "string", "Decodes base64 to string")
        });
        JS_SIGNATURES.put("btoa", new Signature[]{
            new Signature("btoa(data)", new String[]{"data: string"}, "string", "Encodes string to base64")
        });
        JS_SIGNATURES.put("crypto.randomUUID", new Signature[]{
            new Signature("crypto.randomUUID()", new String[]{}, "string", "Generates a random UUID")
        });

        HTML_SIGNATURES.put("document.createElement", new Signature[]{
            new Signature("document.createElement(tagName)", new String[]{"tagName: string"}, "HTMLElement", "Creates a new HTML element")
        });

        // Java signatures
        JAVA_SIGNATURES.put("System.out.println", new Signature[]{
            new Signature("System.out.println(x)", new String[]{"x: any"}, "void", "Prints a value to stdout with newline")
        });
        JAVA_SIGNATURES.put("System.out.print", new Signature[]{
            new Signature("System.out.print(x)", new String[]{"x: any"}, "void", "Prints a value to stdout without newline")
        });
        JAVA_SIGNATURES.put("System.out.printf", new Signature[]{
            new Signature("System.out.printf(format, args)", new String[]{"format: String", "args: Object..."}, "void", "Prints formatted output")
        });
        JAVA_SIGNATURES.put("String.format", new Signature[]{
            new Signature("String.format(format, args)", new String[]{"format: String", "args: Object..."}, "String", "Returns formatted string")
        });
        JAVA_SIGNATURES.put("Integer.parseInt", new Signature[]{
            new Signature("Integer.parseInt(s, radix?)", new String[]{"s: String", "radix?: int"}, "int", "Parses string to int")
        });
        JAVA_SIGNATURES.put("Integer.valueOf", new Signature[]{
            new Signature("Integer.valueOf(s, radix?)", new String[]{"s: String", "radix?: int"}, "Integer", "Returns Integer from string")
        });
        JAVA_SIGNATURES.put("Double.parseDouble", new Signature[]{
            new Signature("Double.parseDouble(s)", new String[]{"s: String"}, "double", "Parses string to double")
        });
        JAVA_SIGNATURES.put("String.valueOf", new Signature[]{
            new Signature("String.valueOf(obj)", new String[]{"obj: Object"}, "String", "Returns string representation")
        });
        JAVA_SIGNATURES.put("String.join", new Signature[]{
            new Signature("String.join(delimiter, elements)", new String[]{"delimiter: CharSequence", "elements: CharSequence..."}, "String", "Joins strings with delimiter")
        });
        JAVA_SIGNATURES.put("List.add", new Signature[]{
            new Signature("list.add(element)", new String[]{"element: E"}, "boolean", "Appends element to list")
        });
        JAVA_SIGNATURES.put("List.get", new Signature[]{
            new Signature("list.get(index)", new String[]{"index: int"}, "E", "Returns element at index")
        });
        JAVA_SIGNATURES.put("List.size", new Signature[]{
            new Signature("list.size()", new String[]{}, "int", "Returns number of elements")
        });
        JAVA_SIGNATURES.put("Map.put", new Signature[]{
            new Signature("map.put(key, value)", new String[]{"key: K", "value: V"}, "V", "Puts key-value pair")
        });
        JAVA_SIGNATURES.put("Map.get", new Signature[]{
            new Signature("map.get(key)", new String[]{"key: Object"}, "V", "Returns value for key")
        });
        JAVA_SIGNATURES.put("Map.containsKey", new Signature[]{
            new Signature("map.containsKey(key)", new String[]{"key: Object"}, "boolean", "Checks if key exists")
        });
        JAVA_SIGNATURES.put("Arrays.asList", new Signature[]{
            new Signature("Arrays.asList(array)", new String[]{"array: T..."}, "List<T>", "Returns list backed by array")
        });
        JAVA_SIGNATURES.put("Collections.sort", new Signature[]{
            new Signature("Collections.sort(list, comparator?)", new String[]{"list: List<T>", "comparator?: Comparator<? super T>"}, "void", "Sorts a list")
        });
        JAVA_SIGNATURES.put("Optional.ofNullable", new Signature[]{
            new Signature("Optional.ofNullable(value)", new String[]{"value: T"}, "Optional<T>", "Returns Optional for possibly-null value")
        });
        JAVA_SIGNATURES.put("Optional.of", new Signature[]{
            new Signature("Optional.of(value)", new String[]{"value: T"}, "Optional<T>", "Returns Optional for non-null value")
        });
        JAVA_SIGNATURES.put("Optional.orElse", new Signature[]{
            new Signature("Optional.orElse(other)", new String[]{"other: T"}, "T", "Returns value or default")
        });
        JAVA_SIGNATURES.put("Optional.orElseThrow", new Signature[]{
            new Signature("Optional.orElseThrow(exceptionSupplier?)", new String[]{"exceptionSupplier?: Supplier<? extends X>"}, "T", "Returns value or throws")
        });
        JAVA_SIGNATURES.put("Optional.ifPresent", new Signature[]{
            new Signature("Optional.ifPresent(consumer)", new String[]{"consumer: Consumer<? super T>"}, "void", "Executes action if value present")
        });
        JAVA_SIGNATURES.put("Stream.filter", new Signature[]{
            new Signature("stream.filter(predicate)", new String[]{"predicate: Predicate<? super T>"}, "Stream<T>", "Filters elements by predicate")
        });
        JAVA_SIGNATURES.put("Stream.map", new Signature[]{
            new Signature("stream.map(mapper)", new String[]{"mapper: Function<? super T, ? extends R>"}, "Stream<R>", "Transforms elements")
        });
        JAVA_SIGNATURES.put("Stream.collect", new Signature[]{
            new Signature("stream.collect(collector)", new String[]{"collector: Collector<? super T, A, R>"}, "R", "Accumulates elements into result")
        });
        JAVA_SIGNATURES.put("Stream.forEach", new Signature[]{
            new Signature("stream.forEach(action)", new String[]{"action: Consumer<? super T>"}, "void", "Performs action on each element")
        });
        JAVA_SIGNATURES.put("Stream.sorted", new Signature[]{
            new Signature("stream.sorted(comparator?)", new String[]{"comparator?: Comparator<? super T>"}, "Stream<T>", "Returns sorted stream")
        });
        JAVA_SIGNATURES.put("Stream.toArray", new Signature[]{
            new Signature("stream.toArray(generator?)", new String[]{"generator?: IntFunction<A[]>"}, "Object[]", "Returns array from stream")
        });
        JAVA_SIGNATURES.put("Path.of", new Signature[]{
            new Signature("Path.of(first, more)", new String[]{"first: String", "more: String..."}, "Path", "Converts path string to Path")
        });
        JAVA_SIGNATURES.put("Files.readString", new Signature[]{
            new Signature("Files.readString(path)", new String[]{"path: Path"}, "String", "Reads file content as string")
        });
        JAVA_SIGNATURES.put("Files.writeString", new Signature[]{
            new Signature("Files.writeString(path, str, openOptions?)", new String[]{"path: Path", "str: CharSequence", "openOptions?: OpenOption..."}, "Path", "Writes string to file")
        });
        JAVA_SIGNATURES.put("Files.list", new Signature[]{
            new Signature("Files.list(dir)", new String[]{"dir: Path"}, "Stream<Path>", "Lists directory entries")
        });
        JAVA_SIGNATURES.put("Files.walk", new Signature[]{
            new Signature("Files.walk(start, maxDepth?)", new String[]{"start: Path", "maxDepth?: int"}, "Stream<Path>", "Walks file tree")
        });

        // Python signatures
        PYTHON_SIGNATURES.put("print", new Signature[]{
            new Signature("print(*objects, sep=' ', end='\\n', file=sys.stdout, flush=False)", new String[]{"*objects", "sep: str = ' '", "end: str = '\\n'", "file: _Writer = sys.stdout", "flush: bool = False"}, "None", "Prints objects to stdout")
        });
        PYTHON_SIGNATURES.put("len", new Signature[]{
            new Signature("len(obj)", new String[]{"obj: Sized"}, "int", "Returns the length of a sequence or collection")
        });
        PYTHON_SIGNATURES.put("range", new Signature[]{
            new Signature("range(stop) / range(start, stop, step=1)", new String[]{"start: int = 0", "stop: int", "step: int = 1"}, "range", "Returns an iterable range of numbers")
        });
        PYTHON_SIGNATURES.put("open", new Signature[]{
            new Signature("open(file, mode='r', buffering=-1, encoding=None, errors=None, newline=None, closefd=True, opener=None)", new String[]{"file: str", "mode: str = 'r'", "buffering: int = -1", "encoding: str | None = None", "errors: str | None = None", "newline: str | None = None", "closefd: bool = True", "opener: _Opener | None = None"}, "IO", "Opens a file and returns a file object")
        });
        PYTHON_SIGNATURES.put("isinstance", new Signature[]{
            new Signature("isinstance(obj, class_or_tuple)", new String[]{"obj: object", "class_or_tuple: type | tuple"}, "bool", "Checks if object is instance of class")
        });
        PYTHON_SIGNATURES.put("type", new Signature[]{
            new Signature("type(object) / type(name, bases, dict)", new String[]{"object: object | str", "bases: tuple = ()", "dict: dict = {}"}, "type", "Returns the type of an object or creates a new type")
        });
        PYTHON_SIGNATURES.put("enumerate", new Signature[]{
            new Signature("enumerate(iterable, start=0)", new String[]{"iterable: Iterable", "start: int = 0"}, "enumerate", "Returns (index, value) pairs from iterable")
        });
        PYTHON_SIGNATURES.put("zip", new Signature[]{
            new Signature("zip(*iterables, strict=False)", new String[]{"*iterables: Iterable", "strict: bool = False"}, "zip", "Aggregates elements from multiple iterables")
        });
        PYTHON_SIGNATURES.put("map", new Signature[]{
            new Signature("map(func, *iterables)", new String[]{"func: Callable", "*iterables: Iterable"}, "map", "Applies function to every item of iterable(s)")
        });
        PYTHON_SIGNATURES.put("filter", new Signature[]{
            new Signature("filter(func, iterable)", new String[]{"func: Callable | None", "iterable: Iterable"}, "filter", "Filters iterable by function")
        });
        PYTHON_SIGNATURES.put("sorted", new Signature[]{
            new Signature("sorted(iterable, *, key=None, reverse=False)", new String[]{"iterable: Iterable", "key: Callable | None = None", "reverse: bool = False"}, "list", "Returns sorted list from iterable")
        });
        PYTHON_SIGNATURES.put("reversed", new Signature[]{
            new Signature("reversed(sequence)", new String[]{"sequence: Sequence"}, "reversed", "Returns reverse iterator")
        });
        PYTHON_SIGNATURES.put("sum", new Signature[]{
            new Signature("sum(iterable, start=0)", new String[]{"iterable: Iterable", "start: int = 0"}, "int | float", "Returns sum of iterable elements")
        });
        PYTHON_SIGNATURES.put("any", new Signature[]{
            new Signature("any(iterable)", new String[]{"iterable: Iterable"}, "bool", "True if any element is truthy")
        });
        PYTHON_SIGNATURES.put("all", new Signature[]{
            new Signature("all(iterable)", new String[]{"iterable: Iterable"}, "bool", "True if all elements are truthy")
        });
        PYTHON_SIGNATURES.put("abs", new Signature[]{
            new Signature("abs(x)", new String[]{"x: number"}, "number", "Returns absolute value")
        });
        PYTHON_SIGNATURES.put("round", new Signature[]{
            new Signature("round(number, ndigits=None)", new String[]{"number: float", "ndigits: int | None = None"}, "int | float", "Rounds a number")
        });
        PYTHON_SIGNATURES.put("min", new Signature[]{
            new Signature("min(iterable, *, key=None, default=...)", new String[]{"iterable: Iterable", "key: Callable | None = None", "default: Any = ..."}, "object", "Returns minimum element")
        });
        PYTHON_SIGNATURES.put("max", new Signature[]{
            new Signature("max(iterable, *, key=None, default=...)", new String[]{"iterable: Iterable", "key: Callable | None = None", "default: Any = ..."}, "object", "Returns maximum element")
        });
        PYTHON_SIGNATURES.put("str.join", new Signature[]{
            new Signature("str.join(iterable)", new String[]{"iterable: Iterable[str]"}, "str", "Joins strings with separator")
        });
        PYTHON_SIGNATURES.put("str.split", new Signature[]{
            new Signature("str.split(sep=None, maxsplit=-1)", new String[]{"sep: str | None = None", "maxsplit: int = -1"}, "list[str]", "Splits string into list")
        });
        PYTHON_SIGNATURES.put("str.replace", new Signature[]{
            new Signature("str.replace(old, new, count=-1)", new String[]{"old: str", "new: str", "count: int = -1"}, "str", "Replace occurrences of substring")
        });
        PYTHON_SIGNATURES.put("str.strip", new Signature[]{
            new Signature("str.strip(chars=None)", new String[]{"chars: str | None = None"}, "str", "Remove leading/trailing whitespace")
        });
        PYTHON_SIGNATURES.put("str.startswith", new Signature[]{
            new Signature("str.startswith(prefix, start?, end?)", new String[]{"prefix: str | tuple", "start: int | None = None", "end: int | None = None"}, "bool", "Check if string starts with prefix")
        });
        PYTHON_SIGNATURES.put("str.endswith", new Signature[]{
            new Signature("str.endswith(suffix, start?, end?)", new String[]{"suffix: str | tuple", "start: int | None = None", "end: int | None = None"}, "bool", "Check if string ends with suffix")
        });
        PYTHON_SIGNATURES.put("dict.get", new Signature[]{
            new Signature("dict.get(key, default=None)", new String[]{"key: K", "default: V | None = None"}, "V | None", "Returns value for key or default")
        });
        PYTHON_SIGNATURES.put("dict.keys", new Signature[]{
            new Signature("dict.keys()", new String[]{}, "dict_keys", "Returns view of dictionary keys")
        });
        PYTHON_SIGNATURES.put("dict.values", new Signature[]{
            new Signature("dict.values()", new String[]{}, "dict_values", "Returns view of dictionary values")
        });
        PYTHON_SIGNATURES.put("list.append", new Signature[]{
            new Signature("list.append(object)", new String[]{"object: T"}, "None", "Appends element to end of list")
        });
        PYTHON_SIGNATURES.put("list.extend", new Signature[]{
            new Signature("list.extend(iterable)", new String[]{"iterable: Iterable[T]"}, "None", "Extends list with elements from iterable")
        });
        PYTHON_SIGNATURES.put("list.pop", new Signature[]{
            new Signature("list.pop(index=-1)", new String[]{"index: int = -1"}, "T", "Removes and returns element at index")
        });
        PYTHON_SIGNATURES.put("super", new Signature[]{
            new Signature("super(type, object_or_type=None)", new String[]{"type: type | None = None", "object_or_type: Any = None"}, "super", "Returns proxy object for method delegation")
        });

        // CSS signatures
        CSS_SIGNATURES.put("url", new Signature[]{
            new Signature("url(path)", new String[]{"path: String"}, "url()", "URL reference for resources")
        });
        CSS_SIGNATURES.put("rgba", new Signature[]{
            new Signature("rgba(r, g, b, a)", new String[]{"r: number (0-255)", "g: number (0-255)", "b: number (0-255)", "a: number (0-1)"}, "color", "RGBA color value with alpha")
        });
        CSS_SIGNATURES.put("rgb", new Signature[]{
            new Signature("rgb(r, g, b)", new String[]{"r: number (0-255)", "g: number (0-255)", "b: number (0-255)"}, "color", "RGB color value")
        });
        CSS_SIGNATURES.put("hsl", new Signature[]{
            new Signature("hsl(h, s, l)", new String[]{"h: number (0-360)", "s: percentage", "l: percentage"}, "color", "HSL color value")
        });
        CSS_SIGNATURES.put("hsla", new Signature[]{
            new Signature("hsla(h, s, l, a)", new String[]{"h: number (0-360)", "s: percentage", "l: percentage", "a: number (0-1)"}, "color", "HSLA color value with alpha")
        });
        CSS_SIGNATURES.put("calc", new Signature[]{
            new Signature("calc(expression)", new String[]{"expression: calculation"}, "number", "CSS calculation function")
        });
        CSS_SIGNATURES.put("min", new Signature[]{
            new Signature("min(values)", new String[]{"values: number list"}, "number", "Returns minimum value")
        });
        CSS_SIGNATURES.put("max", new Signature[]{
            new Signature("max(values)", new String[]{"values: number list"}, "number", "Returns maximum value")
        });
        CSS_SIGNATURES.put("clamp", new Signature[]{
            new Signature("clamp(min, preferred, max)", new String[]{"min: number", "preferred: number", "max: number"}, "number", "Clamps value to range")
        });
        CSS_SIGNATURES.put("var", new Signature[]{
            new Signature("var(name, fallback?)", new String[]{"name: --custom-property", "fallback?: any"}, "any", "CSS custom property reference")
        });
        CSS_SIGNATURES.put("translate", new Signature[]{
            new Signature("translate(x, y)", new String[]{"x: length/percentage", "y: length/percentage"}, "transform", "Moves element in 2D")
        });
        CSS_SIGNATURES.put("translateX", new Signature[]{
            new Signature("translateX(x)", new String[]{"x: length/percentage"}, "transform", "Moves element horizontally")
        });
        CSS_SIGNATURES.put("translateY", new Signature[]{
            new Signature("translateY(y)", new String[]{"y: length/percentage"}, "transform", "Moves element vertically")
        });
        CSS_SIGNATURES.put("rotate", new Signature[]{
            new Signature("rotate(angle)", new String[]{"angle: deg/rad/turn"}, "transform", "Rotates element")
        });
        CSS_SIGNATURES.put("scale", new Signature[]{
            new Signature("scale(x, y?)", new String[]{"x: number", "y: number = x"}, "transform", "Scales element")
        });
        CSS_SIGNATURES.put("blur", new Signature[]{
            new Signature("blur(radius)", new String[]{"radius: length"}, "filter", "Applies blur effect")
        });
        CSS_SIGNATURES.put("brightness", new Signature[]{
            new Signature("brightness(amount)", new String[]{"amount: number/percentage"}, "filter", "Adjusts brightness")
        });
        CSS_SIGNATURES.put("contrast", new Signature[]{
            new Signature("contrast(amount)", new String[]{"amount: number/percentage"}, "filter", "Adjusts contrast")
        });
        CSS_SIGNATURES.put("drop-shadow", new Signature[]{
            new Signature("drop-shadow(x, y, blur, color)", new String[]{"x: length", "y: length", "blur: length", "color: color"}, "filter", "Applies drop shadow")
        });
        CSS_SIGNATURES.put("linear-gradient", new Signature[]{
            new Signature("linear-gradient(angle, color-stops)", new String[]{"angle: deg/direction", "color-stops: color list"}, "background", "Creates linear gradient")
        });
        CSS_SIGNATURES.put("radial-gradient", new Signature[]{
            new Signature("radial-gradient(shape, color-stops)", new String[]{"shape: circle/ellipse", "color-stops: color list"}, "background", "Creates radial gradient")
        });
        CSS_SIGNATURES.put("conic-gradient", new Signature[]{
            new Signature("conic-gradient(from angle, color-stops)", new String[]{"from: angle", "color-stops: color list"}, "background", "Creates conic gradient")
        });
        CSS_SIGNATURES.put("repeat", new Signature[]{
            new Signature("repeat(count, track-list)", new String[]{"count: number|auto-fill|auto-fit", "track-list: track values"}, "grid", "Repeats grid track pattern")
        });
        CSS_SIGNATURES.put("minmax", new Signature[]{
            new Signature("minmax(min, max)", new String[]{"min: length", "max: length"}, "grid", "Defines min/max grid track size")
        });
        CSS_SIGNATURES.put("fit-content", new Signature[]{
            new Signature("fit-content(limit)", new String[]{"limit: length"}, "grid", "Fits content within limit")
        });
    }

    public static Signature[] getSignatures(LanguageType lang, String funcName) {
        List<Signature[]> candidates = new ArrayList<>();
        switch (lang) {
            case JAVASCRIPT:
            case TYPESCRIPT:
            case JSX:
            case TSX:
                candidates.add(JS_SIGNATURES.get(funcName));
                break;
            case HTML:
            case VUE:
            case SVELTE:
            case ASTRO:
                candidates.add(JS_SIGNATURES.get(funcName));
                candidates.add(HTML_SIGNATURES.get(funcName));
                break;
            case JAVA:
                candidates.add(JAVA_SIGNATURES.get(funcName));
                break;
            case PYTHON:
                candidates.add(PYTHON_SIGNATURES.get(funcName));
                break;
            case CSS:
            case SCSS:
            case LESS:
                candidates.add(CSS_SIGNATURES.get(funcName));
                break;
        }
        for (Signature[] arr : candidates) {
            if (arr != null) return arr;
        }
        return null;
    }

    // ========================================================================
    //   HOVER INFO (tooltip on hover)
    // ========================================================================

    private static final Map<String, String> JS_HOVER = new HashMap<>();
    private static final Map<String, String> HTML_HOVER = new HashMap<>();
    private static final Map<String, String> CSS_HOVER = new HashMap<>();
    private static final Map<String, String> JAVA_HOVER = new HashMap<>();
    private static final Map<String, String> PYTHON_HOVER = new HashMap<>();
    private static final Map<String, String> PHP_HOVER = new HashMap<>();
    private static final Map<String, String> SQL_HOVER = new HashMap<>();
    private static final Map<String, String> SH_HOVER = new HashMap<>();
    private static final Map<String, String> C_HOVER = new HashMap<>();
    private static final Map<String, String> CPP_HOVER = new HashMap<>();
    private static final Map<String, String> KOTLIN_HOVER = new HashMap<>();
    private static final Map<String, String> GO_HOVER = new HashMap<>();
    private static final Map<String, String> RUST_HOVER = new HashMap<>();
    private static final Map<String, String> YAML_HOVER = new HashMap<>();
    private static final Map<String, String> XML_HOVER = new HashMap<>();
    private static final Map<String, String> MARKDOWN_HOVER = new HashMap<>();

    static {
        loadHoverData();
    }

    private static void loadHoverData() {
        JS_HOVER.put("fetch", "fetch(url, options?) → Promise<Response>\nStarts fetching a resource from the network.");
        JS_HOVER.put("console", "console — Browser debugging console.\nProvides methods like log(), error(), warn(), table()");
        JS_HOVER.put("console.log", "console.log(message?, ...optionalParams?) → void\nLogs a message to the console.");
        JS_HOVER.put("console.error", "console.error(message?, ...optionalParams?) → void\nLogs an error message.");
        JS_HOVER.put("console.warn", "console.warn(message?, ...optionalParams?) → void\nLogs a warning message.");
        JS_HOVER.put("console.table", "console.table(data?) → void\nDisplays tabular data as a table.");
        JS_HOVER.put("document", "document — The DOM Document object.\nEntry point for manipulating the page content.");
        JS_HOVER.put("window", "window — The global Window object.\nRepresents the browser window.");
        JS_HOVER.put("localStorage", "localStorage — Persistent key-value storage.\nData survives page reloads and browser restarts.");
        JS_HOVER.put("sessionStorage", "sessionStorage — Session-only key-value storage.\nData is cleared when the tab closes.");
        JS_HOVER.put("JSON", "JSON — Static object for parsing and serializing JSON.\nMethods: parse(), stringify()");
        JS_HOVER.put("Math", "Math — Static object with mathematical functions.\nProperties: PI, E, ... Methods: random(), floor(), ceil(), round(), ...");
        JS_HOVER.put("Array", "Array — Global array constructor.\nMethods: from(), isArray(), of()");
        JS_HOVER.put("Promise", "Promise — Represents an asynchronous operation.\nMethods: then(), catch(), finally(). Static: all(), race(), any()");
        JS_HOVER.put("setTimeout", "setTimeout(callback, delay?, ...args?) → number\nCalls a function after a specified delay (ms).");
        JS_HOVER.put("setInterval", "setInterval(callback, delay?, ...args?) → number\nCalls a function repeatedly at intervals (ms).");
        JS_HOVER.put("clearTimeout", "clearTimeout(id) → void\nCancels a timeout.");
        JS_HOVER.put("clearInterval", "clearInterval(id) → void\nCancels an interval.");
        JS_HOVER.put("parseInt", "parseInt(string, radix?) → number\nParses a string and returns an integer.");
        JS_HOVER.put("parseFloat", "parseFloat(string) → number\nParses a string and returns a float.");
        JS_HOVER.put("encodeURI", "encodeURI(uri) → string\nEncodes a URI.");
        JS_HOVER.put("encodeURIComponent", "encodeURIComponent(component) → string\nEncodes a URI component.");
        JS_HOVER.put("decodeURI", "decodeURI(encodedURI) → string\nDecodes a URI.");
        JS_HOVER.put("decodeURIComponent", "decodeURIComponent(encodedComponent) → string\nDecodes a URI component.");
        JS_HOVER.put("atob", "atob(data) → string\nDecodes base64 to string.");
        JS_HOVER.put("btoa", "btoa(data) → string\nEncodes string to base64.");
        JS_HOVER.put("navigator", "navigator — Information about the browser.\nProperties: userAgent, platform, language, onLine, clipboard, geolocation");
        JS_HOVER.put("history", "history — Browser session history.\nMethods: back(), forward(), go()");
        JS_HOVER.put("location", "location — Current URL information.\nProperties: href, hostname, pathname, search, hash");
        JS_HOVER.put("crypto", "crypto — Cryptographic primitives.\nMethods: randomUUID(), subtle.digest()");
        JS_HOVER.put("Intl", "Intl — Internationalization API.\nConstructors: DateTimeFormat, NumberFormat, Collator");
        JS_HOVER.put("URL", "URL — Parses and constructs URLs.\nProperties: href, origin, hostname, pathname, searchParams");
        JS_HOVER.put("Blob", "Blob — Represents binary data.\nnew Blob(parts, options?)");
        JS_HOVER.put("FileReader", "FileReader — Reads file contents.\nMethods: readAsText(), readAsDataURL(), readAsArrayBuffer()");
        JS_HOVER.put("FormData", "FormData — Form data for multipart requests.\nMethods: append(), get(), getAll(), has(), delete()");
        JS_HOVER.put("WebSocket", "WebSocket — Bidirectional communication.\nnew WebSocket(url)");
        JS_HOVER.put("MutationObserver", "MutationObserver — Watches DOM changes.\nnew MutationObserver(callback)");
        JS_HOVER.put("IntersectionObserver", "IntersectionObserver — Watches element visibility.\nnew IntersectionObserver(callback)");
        JS_HOVER.put("ResizeObserver", "ResizeObserver — Watches element size changes.\nnew ResizeObserver(callback)");
        JS_HOVER.put("AbortController", "AbortController — Aborts fetch requests.\nconst { signal } = new AbortController()");
        JS_HOVER.put("EventSource", "EventSource — Server-Sent Events.\nnew EventSource(url)");
        JS_HOVER.put("BroadcastChannel", "BroadcastChannel — Cross-tab messaging.\nnew BroadcastChannel(name)");
        JS_HOVER.put("ServiceWorker", "ServiceWorker — Offline/proxy capabilities.\nnavigator.serviceWorker.register('/sw.js')");
        JS_HOVER.put("Notification", "Notification — Desktop notifications.\nnew Notification(title, options?)");
        JS_HOVER.put("Performance", "Performance — Performance measurement.\nmethods: mark(), measure(), getEntries()");
        JS_HOVER.put("structuredClone", "structuredClone(value) → any\nDeep-clones a value using structured clone algorithm.");
        JS_HOVER.put("Symbol", "Symbol — Unique identifier primitive.\nSymbol(), Symbol.for(), Symbol.keyFor()");
        JS_HOVER.put("Reflect", "Reflect — Meta-programming methods.\nMethods: get(), set(), has(), defineProperty(), deleteProperty()");
        JS_HOVER.put("Proxy", "Proxy — Creates a proxy for an object.\nnew Proxy(target, handler)");
        JS_HOVER.put("Map", "Map — Key-value map.\nnew Map(). Methods: set(), get(), has(), delete(), clear()");
        JS_HOVER.put("Set", "Set — Unique values collection.\nnew Set(). Methods: add(), has(), delete(), clear()");
        JS_HOVER.put("WeakMap", "WeakMap — Weak key-value map (keys are objects).\nnew WeakMap()");
        JS_HOVER.put("WeakSet", "WeakSet — Weak set of objects.\nnew WeakSet()");
        JS_HOVER.put("Error", "Error — Generic error constructor.\nnew Error(message?)");
        JS_HOVER.put("TypeError", "TypeError — Type error constructor.\nnew TypeError(message?)");
        JS_HOVER.put("RangeError", "RangeError — Range error constructor.\nnew RangeError(message?)");
        JS_HOVER.put("SyntaxError", "SyntaxError — Syntax error constructor.\nnew SyntaxError(message?)");
        JS_HOVER.put("Date", "Date — Date/time handling.\nnew Date(). Methods: getFullYear(), getMonth(), getDate(), ...");
        JS_HOVER.put("RegExp", "RegExp — Regular expression.\n/regex/flags or new RegExp(pattern, flags)");
        JS_HOVER.put("String", "String — Text handling.\nMethods: slice(), split(), replace(), match(), trim(), includes(), ...");
        JS_HOVER.put("Number", "Number — Numeric handling.\nMethods: toFixed(), toPrecision(). Static: isNaN(), isFinite(), isInteger()");
        JS_HOVER.put("Boolean", "Boolean — Boolean wrapper.\nnew Boolean(value)");
        JS_HOVER.put("Object", "Object — Base object type.\nStatic: keys(), values(), entries(), assign(), freeze(), seal()");
        JS_HOVER.put("Function", "Function — Function constructor.\nnew Function('return ...')");
        JS_HOVER.put("this", "this — The current execution context.\nDepends on how a function is called.");
        JS_HOVER.put("async", "async — Declares an asynchronous function.\nUsed with await to handle promises.");
        JS_HOVER.put("await", "await — Waits for a promise to resolve.\nCan only be used inside async functions.");
        JS_HOVER.put("class", "class — Defines a class.\nclass Name { constructor() {} method() {} }");
        JS_HOVER.put("extends", "extends — Inherits from a parent class.\nclass Child extends Parent {}");
        JS_HOVER.put("import", "import — Imports from a module.\nimport { name } from 'module'");
        JS_HOVER.put("export", "export — Exports values from a module.\nexport default name / export const name");
        JS_HOVER.put("return", "return — Returns a value from a function.");
        JS_HOVER.put("typeof", "typeof — Returns the type of a value as a string.");
        JS_HOVER.put("instanceof", "instanceof — Checks if object is instance of a class.");
        JS_HOVER.put("new", "new — Creates a new instance of a constructor.");
        JS_HOVER.put("delete", "delete — Deletes a property from an object.");
        JS_HOVER.put("void", "void — Evaluates expression and returns undefined.");
        JS_HOVER.put("null", "null — Represents the intentional absence of a value.");
        JS_HOVER.put("undefined", "undefined — Represents an uninitialized value.");
        JS_HOVER.put("true", "true — Boolean true value.");
        JS_HOVER.put("false", "false — Boolean false value.");
        JS_HOVER.put("NaN", "NaN — Not-a-Number value.");
        JS_HOVER.put("Infinity", "Infinity — Represents mathematical infinity.");
        JS_HOVER.put("var", "var — Declares a variable (function-scoped).\nPrefer let or const instead.");
        JS_HOVER.put("let", "let — Declares a block-scoped variable.\nCan be reassigned.");
        JS_HOVER.put("const", "const — Declares a block-scoped constant.\nCannot be reassigned.");
        JS_HOVER.put("function", "function — Declares a function.\nfunction name(params) { ... }");
        JS_HOVER.put("if", "if — Conditional statement.\nif (condition) { ... } else { ... }");
        JS_HOVER.put("else", "else — Alternative branch in condition.");
        JS_HOVER.put("for", "for — Loop statement.\nfor (init; condition; increment) { ... }");
        JS_HOVER.put("while", "while — Loop statement.\nwhile (condition) { ... }");
        JS_HOVER.put("do", "do...while — Loop statement (runs at least once).");
        JS_HOVER.put("switch", "switch — Multi-branch conditional.\nswitch (value) { case X: break; }");
        JS_HOVER.put("case", "case — Branch in a switch statement.");
        JS_HOVER.put("break", "break — Exits a loop or switch.");
        JS_HOVER.put("continue", "continue — Skips to next iteration.");
        JS_HOVER.put("try", "try/catch — Error handling.\ntry { ... } catch (err) { ... }");
        JS_HOVER.put("catch", "catch — Catches exceptions in try block.");
        JS_HOVER.put("finally", "finally — Runs after try/catch regardless.");
        JS_HOVER.put("throw", "throw — Throws an exception.\nthrow new Error('message')");
        JS_HOVER.put("debugger", "debugger — Pauses execution at the debugger statement.");
        JS_HOVER.put("yield", "yield — Pauses/resumes a generator function.");
        JS_HOVER.put("yield*", "yield* — Delegates to another generator.");
        JS_HOVER.put("with", "with — Extends scope chain (deprecated).");
        JS_HOVER.put("in", "in — Checks if property exists in object.\n'key' in obj");
        JS_HOVER.put("of", "of — Iterates over values.\nfor (const val of iterable) { ... }");

        HTML_HOVER.put("div", "div — Generic container for flow content.");
        HTML_HOVER.put("span", "span — Inline container for phrasing content.");
        HTML_HOVER.put("a", "a — Anchor/hyperlink element.\n<a href=\"url\">link</a>");
        HTML_HOVER.put("img", "img — Embeds an image.\n<img src=\"url\" alt=\"description\">");
        HTML_HOVER.put("input", "input — Form input field.\n<input type=\"text\" name=\"...\">");
        HTML_HOVER.put("button", "button — Clickable button.\n<button type=\"submit\">Click</button>");
        HTML_HOVER.put("form", "form — Form element for user input.\n<form action=\"url\" method=\"POST\">...</form>");
        HTML_HOVER.put("table", "table — Table element.\n<table><tr><td>...</td></tr></table>");
        HTML_HOVER.put("script", "script — Embeds or references JavaScript.\n<script src=\"app.js\"></script>");
        HTML_HOVER.put("style", "style — Embeds CSS styles.\n<style>/* CSS */</style>");
        HTML_HOVER.put("link", "link — Links external resources (CSS, etc).\n<link rel=\"stylesheet\" href=\"style.css\">");
        HTML_HOVER.put("meta", "meta — Metadata for the document.\n<meta charset=\"UTF-8\">");
        HTML_HOVER.put("head", "head — Container for metadata.");
        HTML_HOVER.put("body", "body — Document body.");
        HTML_HOVER.put("header", "header — Introductory content.");
        HTML_HOVER.put("footer", "footer — Footer content.");
        HTML_HOVER.put("nav", "nav — Navigation links.");
        HTML_HOVER.put("section", "section — Thematic grouping of content.");
        HTML_HOVER.put("article", "article — Self-contained content.");
        HTML_HOVER.put("aside", "aside — Tangentially related content.");
        HTML_HOVER.put("main", "main — Dominant content of the document.");
        HTML_HOVER.put("h1", "h1-h6 — Section headings.");
        HTML_HOVER.put("p", "p — Paragraph.");
        HTML_HOVER.put("ul", "ul — Unordered list.");
        HTML_HOVER.put("ol", "ol — Ordered list.");
        HTML_HOVER.put("li", "li — List item.");
        HTML_HOVER.put("video", "video — Embeds video content.");
        HTML_HOVER.put("audio", "audio — Embeds audio content.");
        HTML_HOVER.put("canvas", "canvas — Drawing canvas via JavaScript.");
        HTML_HOVER.put("iframe", "iframe — Embeds another document.");

        CSS_HOVER.put("display", "display — Sets the display behavior of an element.\nValues: block, inline, flex, grid, none, contents, ...");
        CSS_HOVER.put("position", "position — Sets positioning method.\nValues: static, relative, absolute, fixed, sticky");
        CSS_HOVER.put("flex", "flex — Shorthand for flex-grow, flex-shrink, flex-basis.");
        CSS_HOVER.put("flex-direction", "flex-direction — Defines flex direction.\nValues: row, column, row-reverse, column-reverse");
        CSS_HOVER.put("justify-content", "justify-content — Aligns items horizontally.\nValues: flex-start, center, space-between, space-around");
        CSS_HOVER.put("align-items", "align-items — Aligns items vertically.\nValues: stretch, center, flex-start, flex-end");
        CSS_HOVER.put("grid", "grid — Shorthand for grid layout.");
        CSS_HOVER.put("grid-template-columns", "grid-template-columns — Defines grid column sizes.\nrepeat(3, 1fr) = 3 equal columns");
        CSS_HOVER.put("gap", "gap — Gap between grid/flex items.\nShorthand for row-gap and column-gap");
        CSS_HOVER.put("transition", "transition — Smooth property changes.\ntransition: property duration timing-function delay");
        CSS_HOVER.put("transform", "transform — Transforms an element.\ntransform: translate(), rotate(), scale(), skew()");
        CSS_HOVER.put("animation", "animation — Complex animations.\nanimation: name duration timing-function delay iteration-count");
        CSS_HOVER.put("box-shadow", "box-shadow — Adds shadow to an element.\nbox-shadow: h-offset v-offset blur spread color");
        CSS_HOVER.put("text-shadow", "text-shadow — Adds shadow to text.\ntext-shadow: h-offset v-offset blur color");
        CSS_HOVER.put("filter", "filter — Applies visual effects.\nfilter: blur(), brightness(), contrast(), grayscale()");
        CSS_HOVER.put("backdrop-filter", "backdrop-filter — Applies filter to element background.");
        CSS_HOVER.put("clip-path", "clip-path — Clips element to a shape.\nclip-path: circle(), polygon(), url()");
        CSS_HOVER.put("overflow", "overflow — Controls content overflow.\nValues: visible, hidden, scroll, auto");
        CSS_HOVER.put("cursor", "cursor — Sets cursor style.\nValues: pointer, default, text, move, not-allowed");
        CSS_HOVER.put("opacity", "opacity — Sets element transparency (0-1).");
        CSS_HOVER.put("z-index", "z-index — Stacking order.\nOnly works on positioned elements.");
        CSS_HOVER.put("box-sizing", "box-sizing — Box model behavior.\nborder-box includes padding and border in size.");
        CSS_HOVER.put("object-fit", "object-fit — How content fits in element.\nValues: cover, contain, fill, none, scale-down");
        CSS_HOVER.put("aspect-ratio", "aspect-ratio — Sets aspect ratio.\naspect-ratio: 16/9");
        CSS_HOVER.put("@media", "@media — Media query for responsive design.\n@media (max-width: 768px) { ... }");
        CSS_HOVER.put("@keyframes", "@keyframes — Defines animation keyframes.\n@keyframes name { 0% { ... } 100% { ... } }");
        CSS_HOVER.put("@font-face", "@font-face — Defines custom fonts.\n@font-face { font-family: 'Name'; src: url('...'); }");
        CSS_HOVER.put("@import", "@import — Imports another CSS file.\n@import url('file.css');");
        CSS_HOVER.put("@supports", "@supports — Feature query.\n@supports (display: grid) { ... }");

        // ---- Java hover ----
        JAVA_HOVER.put("public", "public — Access modifier.\nAccessible from any other class.");
        JAVA_HOVER.put("private", "private — Access modifier.\nAccessible only within the same class.");
        JAVA_HOVER.put("protected", "protected — Access modifier.\nAccessible within package + subclasses.");
        JAVA_HOVER.put("static", "static — Class-level member.\nBelongs to the class, not instances.");
        JAVA_HOVER.put("final", "final — Cannot be overridden/subclassed/changed.");
        JAVA_HOVER.put("abstract", "abstract — Cannot be instantiated; must be subclassed.");
        JAVA_HOVER.put("void", "void — Indicates no return value.");
        JAVA_HOVER.put("String", "String — Immutable sequence of characters.\njava.lang.String");
        JAVA_HOVER.put("System.out.println", "System.out.println(message) → void\nPrints a line to standard output.");
        JAVA_HOVER.put("System.out.print", "System.out.print(message) → void\nPrints to standard output without newline.");
        JAVA_HOVER.put("System.out.printf", "System.out.printf(format, args) → void\nPrints formatted output.");
        JAVA_HOVER.put("List", "List<E> — Ordered collection (interface).\nImplementations: ArrayList, LinkedList");
        JAVA_HOVER.put("ArrayList", "ArrayList<E> — Resizable array implementation of List.");
        JAVA_HOVER.put("Map", "Map<K,V> — Key-value mapping (interface).\nImplementations: HashMap, TreeMap, LinkedHashMap");
        JAVA_HOVER.put("HashMap", "HashMap<K,V> — Hash table based Map implementation.");
        JAVA_HOVER.put("Set", "Set<E> — Collection without duplicates (interface).\nImplementations: HashSet, TreeSet, LinkedHashSet");
        JAVA_HOVER.put("Optional", "Optional<T> — Container that may or may not contain a value.\nHelps avoid NullPointerException.");
        JAVA_HOVER.put("Stream", "Stream<T> — Sequence of elements supporting functional operations.\nMethods: filter(), map(), collect(), forEach()");
        JAVA_HOVER.put("throws", "throws — Declares exceptions that a method can throw.");
        JAVA_HOVER.put("throw", "throw — Throws an exception explicitly.");
        JAVA_HOVER.put("try", "try — Starts a block for exception handling.\nUsed with catch and/or finally.");
        JAVA_HOVER.put("catch", "catch — Handles exceptions thrown in a try block.");
        JAVA_HOVER.put("finally", "finally — Block always executed after try/catch.");
        JAVA_HOVER.put("class", "class — Defines a new class.\npublic class MyClass { ... }");
        JAVA_HOVER.put("interface", "interface — Defines a contract for classes to implement.");
        JAVA_HOVER.put("enum", "enum — Defines a set of named constants.");
        JAVA_HOVER.put("extends", "extends — Indicates inheritance from a superclass.");
        JAVA_HOVER.put("implements", "implements — Indicates implementation of an interface.");
        JAVA_HOVER.put("new", "new — Creates a new object instance.");
        JAVA_HOVER.put("this", "this — Reference to the current object.");
        JAVA_HOVER.put("super", "super — Reference to the parent class.");
        JAVA_HOVER.put("return", "return — Exits a method and optionally returns a value.");
        JAVA_HOVER.put("int", "int — 32-bit primitive integer type.\nRange: -2^31 to 2^31-1");
        JAVA_HOVER.put("long", "long — 64-bit primitive integer type.\nRange: -2^63 to 2^63-1");
        JAVA_HOVER.put("double", "double — 64-bit floating-point primitive.");
        JAVA_HOVER.put("float", "float — 32-bit floating-point primitive.");
        JAVA_HOVER.put("boolean", "boolean — Primitive type with true/false values.");
        JAVA_HOVER.put("char", "char — 16-bit Unicode character primitive.");
        JAVA_HOVER.put("for", "for — Loop construct.\nfor (int i = 0; i < n; i++) { ... }");
        JAVA_HOVER.put("while", "while — Loop construct.\nwhile (condition) { ... }");
        JAVA_HOVER.put("if", "if — Conditional branch.\nif (condition) { ... } else { ... }");
        JAVA_HOVER.put("else", "else — Alternative branch for if statement.");
        JAVA_HOVER.put("switch", "switch — Multi-way conditional branch.");
        JAVA_HOVER.put("break", "break — Exits a loop or switch block.");
        JAVA_HOVER.put("continue", "continue — Skips to next loop iteration.");
        JAVA_HOVER.put("null", "null — Represents absence of a value.");
        JAVA_HOVER.put("true", "true — Boolean literal for true.");
        JAVA_HOVER.put("false", "false — Boolean literal for false.");
        JAVA_HOVER.put("import", "import — Brings a class/package into scope.\nimport java.util.*;");
        JAVA_HOVER.put("package", "package — Declares the namespace of a class.\npackage com.example;");
        JAVA_HOVER.put("synchronized", "synchronized — Ensures thread-safe access to a block/method.");
        JAVA_HOVER.put("volatile", "volatile — Ensures visibility of changes across threads.");
        JAVA_HOVER.put("transient", "transient — Marks field to be skipped during serialization.");
        JAVA_HOVER.put("instanceof", "instanceof — Tests if object is instance of a class/interface.");
        JAVA_HOVER.put("assert", "assert — Assertion for debugging.\nassert condition : \"message\";");

        // ---- Python hover ----
        PYTHON_HOVER.put("def", "def — Defines a function.\ndef function_name(params):");
        PYTHON_HOVER.put("class", "class — Defines a class.\nclass ClassName:");
        PYTHON_HOVER.put("return", "return — Returns a value from a function.");
        PYTHON_HOVER.put("if", "if — Conditional statement.\nif condition:");
        PYTHON_HOVER.put("elif", "elif — Else-if conditional.\nelif condition:");
        PYTHON_HOVER.put("else", "else — Default branch.\nelse:");
        PYTHON_HOVER.put("for", "for — Loop over iterable.\nfor item in iterable:");
        PYTHON_HOVER.put("while", "while — Loop while condition is true.\nwhile condition:");
        PYTHON_HOVER.put("try", "try — Exception handling block.\ntry:");
        PYTHON_HOVER.put("except", "except — Catches exceptions.\nexcept ExceptionType as e:");
        PYTHON_HOVER.put("finally", "finally — Always executed block.\nfinally:");
        PYTHON_HOVER.put("raise", "raise — Raises an exception.\nraise Exception(\"msg\")");
        PYTHON_HOVER.put("with", "with — Context manager.\nwith open('file') as f:");
        PYTHON_HOVER.put("import", "import — Imports a module.\nimport module_name");
        PYTHON_HOVER.put("from", "from — Imports specific names from a module.\nfrom module import name");
        PYTHON_HOVER.put("as", "as — Creates an alias.\nimport module as alias");
        PYTHON_HOVER.put("pass", "pass — No-op placeholder statement.");
        PYTHON_HOVER.put("yield", "yield — Returns a generator value.\nyield value");
        PYTHON_HOVER.put("lambda", "lambda — Anonymous function.\nlambda x: x * 2");
        PYTHON_HOVER.put("None", "None — Represents absence of value (null).");
        PYTHON_HOVER.put("True", "True — Boolean literal for true.");
        PYTHON_HOVER.put("False", "False — Boolean literal for false.");
        PYTHON_HOVER.put("self", "self — Reference to the current instance (convention).");
        PYTHON_HOVER.put("cls", "cls — Reference to the class (in classmethods).");
        PYTHON_HOVER.put("print", "print(*objects, sep=' ', end='\\n') — Prints to stdout.");
        PYTHON_HOVER.put("len", "len(s) → int — Returns length of a sequence/collection.");
        PYTHON_HOVER.put("range", "range(start, stop, step) → iterable — Generates a sequence of numbers.");
        PYTHON_HOVER.put("type", "type(obj) → type — Returns the type of an object.");
        PYTHON_HOVER.put("isinstance", "isinstance(obj, type) → bool — Checks if object is instance of a type.");
        PYTHON_HOVER.put("open", "open(file, mode='r') → file — Opens a file.\nModes: r, w, a, rb, wb, ...");
        PYTHON_HOVER.put("list", "list — Mutable ordered collection.\n[1, 2, 3] or list()");
        PYTHON_HOVER.put("dict", "dict — Mutable key-value mapping.\n{'key': 'value'} or dict()");
        PYTHON_HOVER.put("set", "set — Mutable unordered collection of unique elements.\n{1, 2, 3}");
        PYTHON_HOVER.put("tuple", "tuple — Immutable ordered collection.\n(1, 2, 3)");
        PYTHON_HOVER.put("str", "str — Immutable sequence of characters.");
        PYTHON_HOVER.put("int", "int — Integer type (arbitrary precision).");
        PYTHON_HOVER.put("float", "float — Floating-point number type.");
        PYTHON_HOVER.put("bool", "bool — Boolean type: True / False.");
        PYTHON_HOVER.put("super", "super() — Returns a proxy to the parent class.");
        PYTHON_HOVER.put("property", "property — Decorator for getter/setter.\n@property\ndef name(self): ...");
        PYTHON_HOVER.put("staticmethod", "@staticmethod — Method without self/cls parameter.");
        PYTHON_HOVER.put("classmethod", "@classmethod — Method taking cls as first parameter.");
        PYTHON_HOVER.put("async", "async — Declares an asynchronous function.\nasync def func():");
        PYTHON_HOVER.put("await", "await — Awaits an async operation.\nawait async_func()");
        PYTHON_HOVER.put("match", "match — Structural pattern matching (Python 3.10+).\nmatch value:");
        PYTHON_HOVER.put("case", "case — Pattern case in match statement.");
        PYTHON_HOVER.put("__init__", "__init__ — Constructor method.\nCalled when a class is instantiated.");
        PYTHON_HOVER.put("__str__", "__str__ — Returns human-readable string representation.");
        PYTHON_HOVER.put("__repr__", "__repr__ — Returns unambiguous string representation.");
        PYTHON_HOVER.put("enumerate", "enumerate(iterable, start=0) → (index, value) pairs.");
        PYTHON_HOVER.put("zip", "zip(*iterables) → Aggregates elements from multiple iterables.");
        PYTHON_HOVER.put("map", "map(function, iterable) → Applies function to every item.");
        PYTHON_HOVER.put("filter", "filter(function, iterable) → Filters items by function.");
        PYTHON_HOVER.put("sorted", "sorted(iterable, key=None, reverse=False) → Sorted list.");
        PYTHON_HOVER.put("reversed", "reversed(seq) → Returns reversed iterator.");
        PYTHON_HOVER.put("any", "any(iterable) → True if any element is truthy.");
        PYTHON_HOVER.put("all", "all(iterable) → True if all elements are truthy.");
        PYTHON_HOVER.put("sum", "sum(iterable, start=0) → Total sum of elements.");
        PYTHON_HOVER.put("min", "min(iterable) → Smallest element.");
        PYTHON_HOVER.put("max", "max(iterable) → Largest element.");
        PYTHON_HOVER.put("abs", "abs(x) → Absolute value.");
        PYTHON_HOVER.put("round", "round(number, ndigits) → Rounded number.");

        // ---- PHP hover ----
        PHP_HOVER.put("echo", "echo — Outputs one or more strings.\necho \"text\";");
        PHP_HOVER.put("print", "print — Outputs a string (returns 1).");
        PHP_HOVER.put("die", "die(message) — Outputs message and terminates script.");
        PHP_HOVER.put("exit", "exit — Terminates the current script.");
        PHP_HOVER.put("return", "return — Returns from a function or script.");
        PHP_HOVER.put("function", "function — Declares a function.\nfunction name($params) { ... }");
        PHP_HOVER.put("class", "class — Defines a class.\nclass ClassName { ... }");
        PHP_HOVER.put("interface", "interface — Defines an interface.");
        PHP_HOVER.put("trait", "trait — Reusable code fragment.\ntrait TraitName { ... }");
        PHP_HOVER.put("abstract", "abstract — Cannot be instantiated; must be extended.");
        PHP_HOVER.put("final", "final — Prevents overriding/inheritance.");
        PHP_HOVER.put("public", "public — Accessible from anywhere.");
        PHP_HOVER.put("private", "private — Accessible only within the class.");
        PHP_HOVER.put("protected", "protected — Accessible within class and subclasses.");
        PHP_HOVER.put("static", "static — Class-level property/method.");
        PHP_HOVER.put("const", "const — Defines a class constant.");
        PHP_HOVER.put("new", "new — Creates an object instance.\n$obj = new ClassName();");
        PHP_HOVER.put("this", "this — Reference to the current object.");
        PHP_HOVER.put("self", "self — Reference to the current class.");
        PHP_HOVER.put("parent", "parent — Reference to the parent class.");
        PHP_HOVER.put("extends", "extends — Inherits from a parent class.");
        PHP_HOVER.put("implements", "implements — Implements an interface.");
        PHP_HOVER.put("use", "use — Imports a class/trait or closes over variables.");
        PHP_HOVER.put("namespace", "namespace — Declares the namespace.\nnamespace App\\Controller;");
        PHP_HOVER.put("include", "include — Includes and evaluates a file (warning on fail).");
        PHP_HOVER.put("require", "require — Includes a file (fatal error on fail).");
        PHP_HOVER.put("include_once", "include_once — Includes file once.");
        PHP_HOVER.put("require_once", "require_once — Requires file once.");
        PHP_HOVER.put("if", "if — Conditional statement.\nif (condition) { ... }");
        PHP_HOVER.put("else", "else — Alternative branch.");
        PHP_HOVER.put("elseif", "elseif — Else-if conditional.");
        PHP_HOVER.put("for", "for — Loop.\nfor ($i = 0; $i < n; $i++) { ... }");
        PHP_HOVER.put("foreach", "foreach — Loop over array.\nforeach ($array as $key => $value) { ... }");
        PHP_HOVER.put("while", "while — Loop while condition is true.");
        PHP_HOVER.put("switch", "switch — Multi-way conditional.");
        PHP_HOVER.put("try", "try — Exception handling block.");
        PHP_HOVER.put("catch", "catch — Catches exceptions.");
        PHP_HOVER.put("finally", "finally — Always executed after try/catch.");
        PHP_HOVER.put("throw", "throw — Throws an exception.");
        PHP_HOVER.put("$this", "$this — Current object reference.");
        PHP_HOVER.put("true", "true — Boolean true.");
        PHP_HOVER.put("false", "false — Boolean false.");
        PHP_HOVER.put("null", "null — Null value.");
        PHP_HOVER.put("isset", "isset($var) → bool — Checks if variable is set and not null.");
        PHP_HOVER.put("empty", "empty($var) → bool — Checks if variable is empty.");
        PHP_HOVER.put("unset", "unset($var) — Destroys a variable.");
        PHP_HOVER.put("echo", "echo — Outputs one or more strings.");
        PHP_HOVER.put("array", "array($key => $value) — Creates an array.");
        PHP_HOVER.put("var_dump", "var_dump($var) — Dumps variable info for debugging.");
        PHP_HOVER.put("print_r", "print_r($var) — Prints human-readable array/object info.");

        // ---- SQL hover ----
        SQL_HOVER.put("SELECT", "SELECT — Retrieves data from a table.\nSELECT column1, column2 FROM table;");
        SQL_HOVER.put("FROM", "FROM — Specifies the table to query.");
        SQL_HOVER.put("WHERE", "WHERE — Filters rows by condition.\nWHERE column = value");
        SQL_HOVER.put("INSERT", "INSERT — Adds new rows.\nINSERT INTO table (col1, col2) VALUES (v1, v2);");
        SQL_HOVER.put("UPDATE", "UPDATE — Modifies existing rows.\nUPDATE table SET col = value WHERE condition;");
        SQL_HOVER.put("DELETE", "DELETE — Removes rows.\nDELETE FROM table WHERE condition;");
        SQL_HOVER.put("CREATE TABLE", "CREATE TABLE — Creates a new table.\nCREATE TABLE table_name (col TYPE);");
        SQL_HOVER.put("ALTER TABLE", "ALTER TABLE — Modifies a table schema.");
        SQL_HOVER.put("DROP TABLE", "DROP TABLE — Deletes a table permanently.");
        SQL_HOVER.put("JOIN", "JOIN — Combines rows from two tables based on a condition.");
        SQL_HOVER.put("LEFT JOIN", "LEFT JOIN — All rows from left table, matched rows from right.");
        SQL_HOVER.put("RIGHT JOIN", "RIGHT JOIN — All rows from right table, matched rows from left.");
        SQL_HOVER.put("INNER JOIN", "INNER JOIN — Only rows that match in both tables.");
        SQL_HOVER.put("ON", "ON — Join condition.\nJOIN t2 ON t1.id = t2.fk");
        SQL_HOVER.put("GROUP BY", "GROUP BY — Groups rows by column(s).\nGROUP BY column");
        SQL_HOVER.put("HAVING", "HAVING — Filters groups (like WHERE for GROUP BY).");
        SQL_HOVER.put("ORDER BY", "ORDER BY — Sorts result set.\nORDER BY column ASC|DESC");
        SQL_HOVER.put("LIMIT", "LIMIT — Limits number of returned rows.\nLIMIT 10 OFFSET 20");
        SQL_HOVER.put("DISTINCT", "DISTINCT — Removes duplicate rows from result.");
        SQL_HOVER.put("AS", "AS — Creates an alias for column/table.\nSELECT col AS alias");
        SQL_HOVER.put("COUNT", "COUNT(expr) — Returns number of rows.");
        SQL_HOVER.put("SUM", "SUM(column) — Returns sum of values.");
        SQL_HOVER.put("AVG", "AVG(column) — Returns average of values.");
        SQL_HOVER.put("MIN", "MIN(column) — Returns minimum value.");
        SQL_HOVER.put("MAX", "MAX(column) — Returns maximum value.");
        SQL_HOVER.put("BETWEEN", "BETWEEN — Range check.\nWHERE col BETWEEN low AND high");
        SQL_HOVER.put("LIKE", "LIKE — Pattern matching.\nWHERE col LIKE '%pattern%'");
        SQL_HOVER.put("IN", "IN — Checks if value is in a list.\nWHERE col IN (1, 2, 3)");
        SQL_HOVER.put("IS NULL", "IS NULL — Checks for null values.");
        SQL_HOVER.put("IS NOT NULL", "IS NOT NULL — Checks for non-null values.");
        SQL_HOVER.put("EXISTS", "EXISTS — Checks if subquery returns any rows.");
        SQL_HOVER.put("UNION", "UNION — Combines results of two queries (removes duplicates).");
        SQL_HOVER.put("UNION ALL", "UNION ALL — Combines results including duplicates.");
        SQL_HOVER.put("INDEX", "INDEX — Database index for faster queries.\nCREATE INDEX idx_name ON table(col);");
        SQL_HOVER.put("PRIMARY KEY", "PRIMARY KEY — Unique identifier for each row.");
        SQL_HOVER.put("FOREIGN KEY", "FOREIGN KEY — References a column in another table.");
        SQL_HOVER.put("UNIQUE", "UNIQUE — Ensures all values in a column are distinct.");
        SQL_HOVER.put("CHECK", "CHECK — Enforces a condition on column values.");
        SQL_HOVER.put("DEFAULT", "DEFAULT — Default value for a column.");
        SQL_HOVER.put("CASE", "CASE — Conditional expression in SQL.\nCASE WHEN cond THEN val ELSE val_end END");
        SQL_HOVER.put("VARCHAR", "VARCHAR(n) — Variable-length character string (max n).");
        SQL_HOVER.put("INT", "INT — Integer type.");
        SQL_HOVER.put("BOOLEAN", "BOOLEAN — True/false type.");
        SQL_HOVER.put("DATE", "DATE — Date type (YYYY-MM-DD).");
        SQL_HOVER.put("TIMESTAMP", "TIMESTAMP — Date and time type.");
        SQL_HOVER.put("TEXT", "TEXT — Large text data.");
        SQL_HOVER.put("BEGIN", "BEGIN — Starts a transaction block.");
        SQL_HOVER.put("COMMIT", "COMMIT — Saves the current transaction.");
        SQL_HOVER.put("ROLLBACK", "ROLLBACK — Undoes the current transaction.");
    }

    public static String getHoverText(LanguageType lang, String word) {
        String key = word.startsWith(".") ? word.substring(1) : word;
        String h;
        switch (lang) {
            case JAVASCRIPT:
            case TYPESCRIPT:
            case JSX:
            case TSX:
            case HTML:
            case VUE:
            case SVELTE:
            case ASTRO: {
                h = JS_HOVER.get(key);
                if (h != null) return h;
                h = JS_HOVER.get(word);
                if (h != null) return h;
                h = HTML_HOVER.get(key);
                if (h != null) return h;
                break;
            }
            case CSS:
            case SCSS:
            case LESS:
            case SASS: {
                h = CSS_HOVER.get(key);
                if (h != null) return h;
                break;
            }
            case JAVA: {
                h = JAVA_HOVER.get(key);
                if (h != null) return h;
                break;
            }
            case PYTHON: {
                h = PYTHON_HOVER.get(key);
                if (h != null) return h;
                break;
            }
            case PHP: {
                h = PHP_HOVER.get(key);
                if (h != null) return h;
                break;
            }
            case SQL: {
                h = SQL_HOVER.get(key);
                if (h != null) return h;
                break;
            }
        }
        return null;
    }

    // ── User Snippets Persistence ──────────────────────────────────────

    public static void saveUserSnippets() {
        try {
            USER_SNIPPETS_FILE.getParentFile().mkdirs();
            JSONObject root = new JSONObject();
            for (LanguageType lang : getSupportedLanguages()) {
                JSONArray arr = new JSONArray();
                for (Suggestion s : getAllSuggestions(lang)) {
                    JSONObject obj = new JSONObject();
                    obj.put("label", s.label);
                    obj.put("insertText", s.insertText);
                    obj.put("category", s.category);
                    if (s.description != null) obj.put("description", s.description);
                    arr.add(obj);
                }
                root.put(lang.name(), arr);
            }
            String json = root.toString();
            Files.write(USER_SNIPPETS_FILE.toPath(), json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) { /* fail silently */ }
    }

    public static void loadUserSnippets() {
        if (!USER_SNIPPETS_FILE.exists()) return;
        try {
            String content = new String(Files.readAllBytes(USER_SNIPPETS_FILE.toPath()), StandardCharsets.UTF_8);
            JSONObject root = (JSONObject) new JSONParser().parse(content);
            for (LanguageType lang : getSupportedLanguages()) {
                JSONArray arr = (JSONArray) root.get(lang.name());
                if (arr == null) continue;
                for (Object item : arr) {
                    JSONObject obj = (JSONObject) item;
                    String label = (String) obj.get("label");
                    String insertText = (String) obj.get("insertText");
                    String category = (String) obj.get("category");
                    String description = (String) obj.get("description");
                    if (label == null || insertText == null || category == null) continue;
                    // Use internal methods to avoid triggering save during load
                    removeSuggestionByLabelInternal(lang, label);
                    addSuggestionInternal(lang, new Suggestion(label, insertText, category, null, description, null, null));
                }
            }
        } catch (Exception ignored) { /* fail silently */ }
    }

    private static void addSuggestionInternal(LanguageType lang, Suggestion s) {
        getSource(lang).add(s);
    }

    private static void removeSuggestionByLabelInternal(LanguageType lang, String label) {
        List<Suggestion> source = getSource(lang);
        Iterator<Suggestion> it = source.iterator();
        while (it.hasNext()) {
            if (it.next().label.equals(label)) {
                it.remove();
                break;
            }
        }
    }

    private CompletionProvider() {}
}
