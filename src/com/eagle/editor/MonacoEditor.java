package com.eagle.editor;

import com.eagle.util.EditorSettings;
import java.util.List;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

public class MonacoEditor extends StackPane {

    private final WebView webView = new WebView();
    private final WebEngine engine = webView.getEngine();
    private LanguageType language;
    private boolean ready = false;
    private String pendingText;
    private double pendingFontSize = 14;
    private Runnable onReady;
    private Consumer<String> onContentChanged;
    private Consumer<Integer> onCaretMoved;
    private Consumer<Integer> onBreakpointToggled;

    private static final String CM_VER = "5.65.16";
    private static final String CM_CDN = "https://cdnjs.cloudflare.com/ajax/libs/codemirror/" + CM_VER;

    public static final String[] AVAILABLE_THEMES = {
        "darcula", "material", "monokai", "eclipse", "ambiance", "base16-dark",
        "3024-day", "neat", "neo", "night", "paraiso-dark", "rubyblue",
        "seti", "solarized", "the-matrix", "tomorrow-night-bright",
        "vibrant-ink", "xq-light", "bespin", "dracula", "liquibyte"
    };

    public MonacoEditor(LanguageType lang) {
        this.language = lang;
        webView.setPrefSize(800, 600);
        getChildren().add(webView);

        String html = buildHtml(lang);
        engine.loadContent(html);
        engine.getLoadWorker().stateProperty().addListener((obs, old, st) -> {
            if (st == javafx.concurrent.Worker.State.SUCCEEDED) {
                JSObject win = (JSObject) engine.executeScript("window");
                win.setMember("javaBridge", new JavaBridge());
            }
        });
    }

    public WebView getWebView() { return webView; }
    public WebEngine getEngine() { return engine; }
    public boolean isReady() { return ready; }

    public void setOnReady(Runnable r) {
        if (ready) {
            Platform.runLater(r);
        } else {
            this.onReady = r;
        }
    }
    public void setOnContentChanged(Runnable r) {
        this.onContentChanged = s -> { if (r != null) r.run(); };
    }
    public void setOnContentChanged(Consumer<String> c) { this.onContentChanged = c; }
    public void setOnCaretMoved(Consumer<Integer> c) { this.onCaretMoved = c; }
    public void setOnBreakpointToggled(Consumer<Integer> c) { this.onBreakpointToggled = c; }

    public void setText(String text) {
        if (!ready) { 
            pendingText = text; 
            return; 
        }
        String escaped = escapeForJson(text);
        try {
            engine.executeScript("editor.setValue(JSON.parse(\"" + escaped + "\"));");
        } catch (Exception ignored) { }
    }

    public String getText() {
        if (!ready) {
            System.out.println("Monaco.getText: not ready, returning pending text length: " + (pendingText != null ? pendingText.length() : 0));
            return pendingText != null ? pendingText : "";
        }
        String text = (String) engine.executeScript("editor.getValue();");
        System.out.println("Monaco.getText: executed, text length: " + (text != null ? text.length() : 0));
        return text;
    }

    public String getSelectedText() {
        if (!ready) return "";
        return (String) engine.executeScript("editor.getSelection();");
    }

    public void setLanguage(LanguageType lang) {
        this.language = lang;
        if (ready) {
            engine.executeScript("editor.setOption('mode','" + toCodeMirrorMode(lang) + "');");
        }
    }

    public LanguageType getLanguage() { return language; }

    public void setFontSize(double size) {
        if (!ready) { pendingFontSize = size; return; }
        engine.executeScript("editor.getWrapperElement().style.fontSize='" + size + "px';editor.refresh();");
    }

    public double getFontSize() { return pendingFontSize; }

    public void setWrapText(boolean wrap) {
        if (ready) {
            engine.executeScript("editor.setOption('lineWrapping'," + wrap + ");");
        }
    }

    public void setMinimapEnabled(boolean enabled) {
        // CodeMirror has no minimap
    }

    public void setTheme(String theme) {
        if (ready) {
            // Load theme CSS dynamically if not already loaded
            engine.executeScript(
                "(function(name){var links=document.getElementsByTagName('link');" +
                "for(var i=0;i<links.length;i++){" +
                "  if(links[i].href.indexOf('theme/'+name+'.css')>=0)" +
                "    {editor.setOption('theme',name);return;}" +
                "}" +
                "var l=document.createElement('link');" +
                "l.rel='stylesheet';l.href='" + CM_CDN + "/theme/'+name+'.css';" +
                "l.onload=function(){editor.setOption('theme',name);};" +
                "document.head.appendChild(l);" +
                "})('" + theme + "');"
            );
        }
    }

    public void enableMultiCursor() {
        // CodeMirror has multi-cursor via Ctrl+D / Alt-Click built-in
    }

    public void setBreakpoints(List<Integer> lines) {
        if (!ready) return;
        StringBuilder js = new StringBuilder("editor.clearGutter('breakpoints');");
        for (int i = 0; i < lines.size(); i++) {
            int l = lines.get(i);
            js.append("editor.setGutterMarker(").append(l).append(",'breakpoints',");
            js.append("(function(){var m=document.createElement('div');");
            js.append("m.className='breakpoint-marker';return m;})());");
        }
        try { engine.executeScript(js.toString()); } catch (Exception ignored) {}
    }

    public void setProblems(List<CodeLinter.Problem> problems) {
        if (!ready) return;
        StringBuilder js = new StringBuilder(
            "editor.clearGutter('problems');"
            + "var markers={};"
        );
        for (int i = 0; i < problems.size(); i++) {
            CodeLinter.Problem p = problems.get(i);
            int line = p.line;
            String sev = p.severity == CodeLinter.Problem.Severity.ERROR ? "error"
                       : p.severity == CodeLinter.Problem.Severity.WARNING ? "warning" : "info";
            String msg = escapeForSingleQuote(p.message);
            js.append("editor.setGutterMarker(").append(line).append(",'problems',");
            js.append("(function(){var m=document.createElement('div');");
            js.append("m.className='problem-marker problem-").append(sev).append("';");
            js.append("m.title='").append(sev).append(": ").append(msg).append("';return m;})());");
        }
        try { engine.executeScript(js.toString()); } catch (Exception ignored) {}
    }

    public int[] getCursorPosition() {
        if (!ready) return new int[]{0, 0};
        try {
            String pos = (String) engine.executeScript(
                "JSON.stringify({line:editor.getCursor().line,column:editor.getCursor().ch});");
            int line = Integer.parseInt(pos.replaceAll(".*\"line\":(\\d+).*", "$1"));
            int col = Integer.parseInt(pos.replaceAll(".*\"column\":(\\d+).*", "$1"));
            return new int[]{line + 1, col};
        } catch (Exception e) { return new int[]{0, 0}; }
    }

    public void focus() {
        if (ready) engine.executeScript("editor.focus();");
    }

    /** Re-applies pending text if editor just became ready. Call after ready. */
    public void reapplyText() {
        if (ready && pendingText != null) {
            setText(pendingText);
            pendingText = null;
        }
    }

    public void undo() { if (ready) engine.executeScript("editor.undo();"); }
    public void redo() { if (ready) engine.executeScript("editor.redo();"); }
    public void cut() { if (ready) engine.executeScript("document.execCommand('cut');"); }
    public void copy() { if (ready) engine.executeScript("document.execCommand('copy');"); }
    public void paste() { if (ready) engine.executeScript("document.execCommand('paste');"); }
    public void selectAll() { if (ready) engine.executeScript("editor.execCommand('selectAll');"); }

    public void formatCode() {
        // CodeMirror has no built-in formatter; auto-indent selection
        if (ready) engine.executeScript("editor.execCommand('indentAuto');");
    }

    public void toggleComment() {
        if (ready) engine.executeScript("editor.toggleComment();");
    }

    public void insertText(String text) {
        if (ready) {
            String escaped = escapeForJson(text);
            engine.executeScript("editor.replaceRange(JSON.parse(\"" + escaped + "\"),editor.getCursor());");
        }
    }

    public void goToLine(int line) {
        if (ready) {
            engine.executeScript(
                "editor.setCursor(" + line + ",0);"
                + "editor.scrollIntoView({line:" + line + ",ch:0});"
                + "editor.focus();");
        }
    }

    public void showFind() {
        if (ready) engine.executeScript("editor.execCommand('find');");
    }

    private String escapeForJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private String escapeForSingleQuote(String s) {
        return s.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private String toCodeMirrorMode(LanguageType lang) {
        switch (lang) {
            case JAVASCRIPT: case JSX: return "javascript";
            case TYPESCRIPT: case TSX: return "text/typescript";
            case HTML: case VUE: case SVELTE: return "htmlmixed";
            case CSS: case SCSS: case LESS: case SASS: return "css";
            case JAVA: return "text/x-java";
            case PYTHON: return "text/x-python";
            case PHP: return "application/x-httpd-php";
            case JSON: return "application/json";
            case XML: case SVG: return "xml";
            case SQL: return "text/x-sql";
            case MARKDOWN: return "text/x-markdown";
            case YAML: return "text/x-yaml";
            case SH: case DOCKERFILE: return "text/x-sh";
            default: return "text/plain";
        }
    }

    private String buildHtml(LanguageType lang) {
        String mode = toCodeMirrorMode(lang);
        return "<!DOCTYPE html>\n<html><head><meta charset=\"utf-8\">\n"
            + "<link rel=\"stylesheet\" href=\"" + CM_CDN + "/codemirror.min.css\">\n"
            + "<link rel=\"stylesheet\" href=\"" + CM_CDN + "/theme/darcula.css\">\n"
            + "<link rel=\"stylesheet\" href=\"" + CM_CDN + "/addon/dialog/dialog.css\">\n"
            + "<link rel=\"stylesheet\" href=\"" + CM_CDN + "/addon/fold/foldgutter.css\">\n"
            + "<link rel=\"stylesheet\" href=\"" + CM_CDN + "/addon/hint/show-hint.css\">\n"
            + "<link rel=\"stylesheet\" href=\"" + CM_CDN + "/addon/tern/tern.css\">\n"
            + "<style>"
            + "body{margin:0;overflow:hidden;background:#2b2b2b}"
            + "::-webkit-scrollbar{width:10px;height:10px}"
            + "::-webkit-scrollbar-track{background:#2b2b2b}"
            + "::-webkit-scrollbar-thumb{background:#555;border-radius:5px}"
            + "::-webkit-scrollbar-thumb:hover{background:#777}"
            + ".CodeMirror{height:100vh;font-size:14px;line-height:1.5;font-family:'Consolas','Monaco',monospace}"
            + ".CodeMirror-gutters{background:#313335;border-right:1px solid #444}"
            + ".CodeMirror-linenumber{color:#858585;padding:0 3px;min-width:2.5em;text-align:right}"
            + ".CodeMirror-cursor{border-left:2px solid #aeafad}"
            + ".CodeMirror-selected{background:#214283 !important}"
            + ".CodeMirror-activeline-background{background:#2c313c !important}"
            + ".CodeMirror-matchingbracket{color:#50fa7b !important;font-weight:bold}"
            + ".CodeMirror-nonmatchingbracket{color:#ff5555 !important;font-weight:bold}"
            + ".breakpoint-marker{width:10px;height:10px;border-radius:50%;background:#e74c3c;margin:4px 3px;cursor:pointer}"
            + ".breakpoint-marker:hover{background:#c0392b}"
            + ".problem-marker{width:8px;height:8px;margin:5px 4px;border-radius:50%;cursor:pointer}"
            + ".problem-error{background:#e74c3c}"
            + ".problem-warning{background:#f1c40f}"
            + ".problem-info{background:#3498db}"
            + ".CodeMirror-hints{z-index:1000;background:#1e1e1e;border:1px solid #444;border-radius:4px;box-shadow:0 2px 8px rgba(0,0,0,0.3)}"
            + ".CodeMirror-hint{color:#abb2bf;padding:2px 6px}"
            + ".CodeMirror-hint-active{background:#094771;color:#fff}"
            + ".CodeMirror-Tern-completion-arg{color:#ff79c6}"
            + ".CodeMirror-Tern-completion-type{color:#8be9fd}"
            + "#loading{position:absolute;top:50%;left:50%;transform:translate(-50%,-50%);color:#888;font-family:sans-serif;font-size:14px;text-align:center;z-index:10}"
            + "#loading .spinner{border:3px solid #444;border-top-color:#4fc3f7;border-radius:50%;width:28px;height:28px;animation:spin .8s linear infinite;margin:0 auto 12px}"
            + "@keyframes spin{to{transform:rotate(360deg)}}"
            + "#error{display:none;position:absolute;top:50%;left:50%;transform:translate(-50%,-50%);color:#e74c3c;font-family:sans-serif;font-size:14px;text-align:center;z-index:10;max-width:80%}"
            + "</style>\n"
            + "</head><body>\n"
            + "<div id=\"loading\"><div class=\"spinner\"></div><span>Loading editor\u2026</span></div>\n"
            + "<div id=\"error\"></div>\n"
            + "<script>\n"
            + "window.onerror=function(m,u,l,c,e){\n"
            + "  var el=document.getElementById('error');\n"
            + "  if(el){el.innerHTML='<b>JS Error:</b> '+m;el.style.display='block'}\n"
            + "  document.getElementById('loading').style.display='none';\n"
            + "};\n"
            + "</script>\n"
            + "<script src=\"" + CM_CDN + "/codemirror.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/mode/javascript/javascript.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/mode/htmlmixed/htmlmixed.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/mode/xml/xml.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/mode/css/css.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/mode/clike/clike.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/mode/python/python.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/mode/php/php.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/mode/sql/sql.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/mode/markdown/markdown.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/mode/yaml/yaml.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/mode/shell/shell.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/mode/jsx/jsx.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/mode/tsx/tsx.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/mode/vue/vue.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/addon/comment/comment.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/addon/search/search.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/addon/search/searchcursor.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/addon/dialog/dialog.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/addon/edit/matchbrackets.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/addon/edit/closebrackets.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/addon/edit/matchtags.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/addon/fold/foldcode.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/addon/fold/foldgutter.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/addon/fold/brace-fold.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/addon/fold/xml-fold.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/addon/fold/comment-fold.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/addon/fold/indent-fold.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/addon/selection/active-line.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/addon/hint/show-hint.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/addon/hint/anyword-hint.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/addon/hint/javascript-hint.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/addon/hint/xml-hint.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/addon/hint/html-hint.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/addon/hint/css-hint.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/addon/hint/sql-hint.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/addon/hint/python-hint.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/addon/scroll/annotatescrollbar.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/addon/scroll/simplescrollbars.min.js\"></script>\n"
            + "<script src=\"" + CM_CDN + "/addon/scroll/scrollpastend.min.js\"></script>\n"
            + "<script>\n"
            + "(function(){\n"
            + "  var loadingEl=document.getElementById('loading');\n"
            + "  var errorEl=document.getElementById('error');\n"
            + "  var retries=0,maxRetries=60;\n"
            + "  function init(){\n"
            + "    if(typeof CodeMirror==='undefined'){\n"
            + "      if(++retries>maxRetries){\n"
            + "        if(errorEl){errorEl.innerHTML='<b>Timeout:</b> Failed to load CodeMirror from CDN.<br>Check your internet connection and reload.';errorEl.style.display='block'}\n"
            + "        if(loadingEl)loadingEl.style.display='none';\n"
            + "        return;\n"
            + "      }\n"
            + "      setTimeout(init,100);return;\n"
            + "    }\n"
            + "    try{\n"
            + "      window.editor=CodeMirror(document.body,{\n"
            + "        value:'',\n"
            + "        mode:'" + mode + "',\n"
            + "        theme:'darcula',\n"
            + "        indentUnit:4,\n"
            + "        tabSize:4,\n"
            + "        indentWithTabs:false,\n"
            + "        lineNumbers:true,\n"
            + "        lineWrapping:false,\n"
            + "        matchBrackets:true,\n"
            + "        autoCloseBrackets:true,\n"
            + "        styleActiveLine:true,\n"
            + "        foldGutter:true,\n"
            + "        gutters:['CodeMirror-linenumbers','breakpoints','problems','CodeMirror-foldgutter'],\n"
            + "        highlightSelectionMatches:{showToken:/\\w/,annotateScrollbar:true},\n"
            + "        scrollbarStyle:'native',\n"
            + "        scrollPastEnd:true,\n"
            + "        extraKeys:{\n"
            + "          'Ctrl-/':'toggleComment','Cmd-/':'toggleComment',\n"
            + "          'Ctrl-F':'findPersistent','Cmd-F':'findPersistent',\n"
            + "          'Ctrl-H':'replace','Cmd-H':'replace',\n"
            + "          'Ctrl-G':'jumpToLine','Cmd-G':'jumpToLine',\n"
            + "          'Ctrl-D':function(cm){cm.selectWordAtCursor(cm.getCursor());},\n"
            + "          'Ctrl-Space':function(cm){cm.showHint({completeSingle:false})},\n"
            + "          'Alt-/':'indentAuto',\n"
            + "          'Ctrl-[':'indentLess','Ctrl-]':'indentMore',\n"
            + "          'Ctrl-K':function(cm){var pos=cm.getCursor();cm.replaceRange('',CodeMirror.Pos(pos.line,0),pos);},\n"
            + "          'Ctrl-Shift-K':function(cm){var pos=cm.getCursor();cm.replaceRange('',pos,CodeMirror.Pos(pos.line+1,0));},\n"
            + "          'Shift-Tab':'indentAuto'\n"
            + "        },\n"
            + "        hintOptions:{completeSingle:false},\n"
            + "        matchTags:{bothTags:true}\n"
            + "      });\n"
            + "      editor.on('change',function(){try{javaBridge.onContentChanged()}catch(e){}});\n"
            + "      editor.on('cursorActivity',function(){\n"
            + "        var pos=editor.getCursor();\n"
            + "        try{javaBridge.onCursorMoved(pos.line+1,pos.ch)}catch(e2){}\n"
            + "      });\n"
            + "      editor.on('gutterClick',function(cm,line,gutter){\n"
            + "        if(gutter==='breakpoints'){\n"
            + "          try{javaBridge.onGlyphClick(line)}catch(e2){}\n"
            + "        }\n"
            + "      });\n"
            + "      editor.on('inputRead',function(cm,change){\n"
            + "        if(change.text&&change.text.length===1&&change.text[0].length>0){\n"
            + "          var mode = cm.getOption('mode');\n"
            + "          if(mode==='text/plain') return;\n"
            + "          CodeMirror.commands.autocomplete(cm);\n"
            + "        }\n"
            + "      });\n"
            + "      CodeMirror.commands.autocomplete = function(cm) {\n"
            + "        cm.showHint({completeSingle:false});\n"
            + "      };\n"
            + "      if(window.javaBridge&&window.javaBridge.onEditorReady){\n"
            + "        window.javaBridge.onEditorReady();\n"
            + "      }\n"
            + "      if(loadingEl)loadingEl.style.display='none';\n"
            + "    }catch(e){\n"
            + "      if(errorEl){errorEl.innerHTML='<b>Editor error:</b><br>'+e.message;errorEl.style.display='block'}\n"
            + "      if(loadingEl)loadingEl.style.display='none';\n"
            + "    }\n"
            + "  }\n"
            + "  init();\n"
            + "})();\n"
            + "</script>\n</body></html>";
    }

    public class JavaBridge {
        public void onContentChanged() {
            if (MonacoEditor.this.onContentChanged != null) {
                Platform.runLater(() -> MonacoEditor.this.onContentChanged.accept(getText()));
            }
        }
        public void onCursorMoved(int line, int col) {
            if (MonacoEditor.this.onCaretMoved != null) {
                Platform.runLater(() -> MonacoEditor.this.onCaretMoved.accept(line));
            }
        }
        public void onSelectionChanged(int startLine, int startCol, int endLine, int endCol) {}
        public void onGlyphClick(int lineNumber) {
            if (MonacoEditor.this.onBreakpointToggled != null) {
                Platform.runLater(() -> MonacoEditor.this.onBreakpointToggled.accept(lineNumber));
            }
        }
        public void onEditorReady() {
            final String pending = pendingText;
            final double pfs = pendingFontSize;
            ready = true;
            pendingText = null;
            if (pending != null) {
                setText(pending);
            }
            if (pfs != 14) {
                setFontSize(pfs);
            }
            applyAllSettings();
            if (MonacoEditor.this.onReady != null) {
                Platform.runLater(MonacoEditor.this.onReady);
            }
        }
        public void onContextMenu(Object menu, int x, int y) {
            // handled by JavaFX context menu in CodeEditor.setupContextMenu()
        }
    }
    
    
    
    
    
    
    
        // ====================== Settings Methods ======================

    public void setFontFamily(String family) {
        if (!ready) return;
        if (family == null || family.isEmpty()) family = "Consolas";
        String safeFamily = family.replace("'", "\\'");
        engine.executeScript(
            "editor.getWrapperElement().style.fontFamily = '" + safeFamily + "'; " +
            "editor.refresh();"
        );
    }

    public void setLineNumbers(boolean show) {
        if (ready) {
            engine.executeScript("editor.setOption('lineNumbers', " + show + ");");
        }
    }

    public void setHighlightCurrentLine(boolean highlight) {
        if (ready) {
            engine.executeScript("editor.setOption('styleActiveLine', " + highlight + ");");
        }
    }

    public void setMatchBrackets(boolean match) {
        if (ready) {
            engine.executeScript("editor.setOption('matchBrackets', " + match + ");");
        }
    }

    public void setShowWhitespace(boolean show) {
        if (ready) {
            engine.executeScript("editor.setOption('showTrailingSpace', " + show + ");");
        }
    }

    public void setIndentGuide(boolean show) {
        if (ready) {
            engine.executeScript("editor.setOption('indentWithTabs', false);"); // CodeMirror يدعم indent guides بصعوبة
            // يمكن إضافة addon لاحقًا
        }
    }

    public void setTabSize(int size) {
        if (ready) {
            engine.executeScript(
                "editor.setOption('tabSize', " + size + ");" +
                "editor.setOption('indentUnit', " + size + ");"
            );
        }
    }

    /** Overload يقبل double — قادم من Spinner<Double> في SettingsDialog */
    public void setTabSize(double size) {
        setTabSize((int) size);
    }

    public void setLineHeight(double height) {
        if (ready) {
            engine.executeScript(
                "editor.getWrapperElement().style.lineHeight = '" + height + "';" +
                "editor.refresh();"
            );
        }
    }

    public void setAutoCloseBrackets(boolean autoClose) {
        if (ready) {
            engine.executeScript("editor.setOption('autoCloseBrackets', " + autoClose + ");");
        }
    }

    public void setCursorBlinking(String style) {
        if (ready) {
            String jsStyle = "solid".equals(style) ? "solid" : "blink";
            engine.executeScript(
                "editor.getWrapperElement().style.caretColor = 'currentColor';" +
                "editor.refresh();"
            );
        }
    }

    public void refreshSettings() {
        if (!ready) return;
        try {
            engine.executeScript("editor.refresh();");
        } catch (Exception ignored) {}
    }

    public void applyAllSettings() {
        if (!ready) return;
        setFontSize(EditorSettings.getFontSize());
        setFontFamily(EditorSettings.getFontFamily());
        setWrapText(EditorSettings.isWordWrap());
        setLineNumbers(EditorSettings.isShowLineNumbers());
        setHighlightCurrentLine(EditorSettings.isHighlightCurrentLine());
        setMatchBrackets(EditorSettings.isHighlightMatchingBrackets());
        setShowWhitespace(EditorSettings.isShowWhitespace());
        setTabSize(EditorSettings.getTabSize());
        setLineHeight(EditorSettings.getLineHeight());
        setAutoCloseBrackets(EditorSettings.isAutoCloseBrackets());
        setTheme(EditorSettings.getMonacoTheme());
        setCursorBlinking(EditorSettings.getCursorBlink());
        refreshSettings();
    }
    
    
    
    
    
    
    
}