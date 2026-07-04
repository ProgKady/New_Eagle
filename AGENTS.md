# Progress

## Goal
Build a full-featured IDE with AI-assisted project development, multi-provider AI chat, 30-language support, and integrated developer tools — now with all tools enhanced (Clipboard History, Figma→HTML, Voice→Code, Export Flutter, LiveShare, DatabaseViewer) and AiPanel side panel / project mode fixed.

## Constraints & Preferences
- Java 8: no `var`, effectively-final lambdas, no try-with-resources reassignment
- All UI elements use `setGraphic(ImageView)` not emoji text
- CodeMirror 5.65.16 loads from `cdnjs.cloudflare.com` (ES5-compatible, works in Java 8 WebKit)
- Settings as a single dialog with 7 tabs: General, Editor, AI, Languages, Monaco, Registry, Debugger
- All tool dialogs use `ThemeManager.getInstance().applyTheme()`
- Long flags (`--model`, `--ctx-size`) preferred over short flags (`-m`, `-c`) for GGUF compatibility
- Arabic comments require `-encoding UTF-8` to compile from command line

## Done
### Phase 1 — Project & AI Features
- **AI Project Generator (Modular Architecture)** — `onGenerateProject()` shows a dialog with 22+ project types. AI generation delegated to `AiProjectEngine` (orchestrator) → `ArchitecturePlanner` (AI returns JSON file list → `ProjectPlan`) → `BatchFileGenerator` (parallel batch, 5 files/batch) → `ProjectAuditor` (self-review: missing imports, broken refs, missing files) → auto-fix loop. Unlimited files, AI decides structure. Progress bar in status bar shows real-time progress. Falls back to templates when AI unavailable.
- **22 template generators** — in `EditorController.java`: HTML, React (JS/TS), Vue, Next, Node API, Flask, Django, Spring Boot, Swing, Java Console, Python, JS CLI, PHP, C++, C#, Go, Rust, Kotlin, Docker, MERN.
- **Project right-click context menu** — "Develop Project with AI..." and "AI Chat About Project..." in `EditorController.java:1046-1051`
- **Live colored log viewer (`GeneratorLogViewer.java`)** — new class in `com.eagle.tools`; live-updating dialog (500ms refresh) with colored entries, Export Log, Save Prompt, Saved Prompts, Close buttons
- **Thread-safe log list** — `generatorLog` changed to `Collections.synchronizedList(new ArrayList<>())`
- **`onDevelopProjectWithAi()` rewritten** — project-wide context with file tree, auto-backup to `~/.webide/backups/{project}_{timestamp}/`, colored log entries, auto-prompt saving, progress, summary alert
- **`ProjectAiChat.java` rewritten** — tech stack detection (Node, Python, Java, Rust, Go, PHP, C#, Docker, Vue, React), file sizes, larger content limit (10KB), sorted tree, prompt saving checkbox

### Phase 2 — Run & Terminal
- **Run button + menu** — "Run" button in toolbar; "Run Project" menu item with F5 accelerator
- **`onRunProject()` + `detectRunCommand()`** — detects project type (Node, Python, Java, Maven, Gradle, Go, Rust, C#, PHP, C++, C, Docker); `isToolAvailable()` + `showToolMissingDialog()` with download link
- **`TerminalPanel.runCommand()`** — new public method; `TerminalSession` stores `sessionDir` field

### Phase 3 — Language Support (30 Languages)
- **Syntax highlighting for all 30 languages** — added `C_PATTERN`, `CPP_PATTERN`, `KOTLIN_PATTERN`, `GO_PATTERN`, `RUST_PATTERN`, `MARKDOWN_PATTERN`, `XML_PATTERN`; 12 new named groups (PREPROC, LIFETIME, MACRO, HEADER, BOLD, ITALIC, CODE, FENCED, LINK, IMAGE, LIST, BLOCKQUOTE, HR, STRIKETHROUGH, TABLE via shared groups)
- **Completion data for empty lists** — `loadKotlin()`, `loadGo()`, `loadRust()`, `loadVue()`, `loadSvelte()`, `loadEnv()`, `loadGitignore()` in `CompletionProvider.java` with 10–20+ snippets each
- **Linting for C, C++, Kotlin, Go, Rust** — `lintC()`, `lintCpp()`, `lintKotlin()`, `lintGo()`, `lintRust()` in `CodeLinter.java` with brace balancing, common anti-patterns
- **Hover info for Java, Python, PHP, SQL** — `JAVA_HOVER` (~60 entries), `PYTHON_HOVER` (~50), `PHP_HOVER` (~40), `SQL_HOVER` (~45) in `CompletionProvider.java`
- **Signature help for Java, Python, CSS** — `JAVA_SIGNATURES` (28), `PYTHON_SIGNATURES` (32), `CSS_SIGNATURES` (24) with overloaded signatures
- **Format text for 15 languages** — `formatText()` in `CodeEditor.java` covers CSS, SCSS, LESS, SASS, C, C++, Kotlin, Go, Rust, Python; `pythonIndentFormat()` method

### Phase 4 — Tool Enhancements
- **AiPanel side panel toggle fixed** — proper SplitPane collapse/expand via `sideScroll.setVisible(false)` + `setManaged(false)`; stored `sideScroll` and `splitPane` field references
- **AiPanel project mode enhancement** — auto-enables project mode when `setProjectRoot()` is called; sets `projectMode=true` + `projectModeBtn.setSelected(true)`
- **ClipboardHistoryPanel.java rewritten** — search/filter field, copy on single-click (moves to top), delete selected, pin to top, right-click context menu, `LinkedHashSet` with 100-item limit, auto-polling every 500ms, pin icon display
- **FigmaToHtml.java rewritten** — drag-drop `.json`/`.fig`, file/URL loading, export HTML, browser preview, gradient CSS (linear/radial), shadow effects (drop-shadow/inner-shadow/layer-blur), flex layouts (HORIZONTAL/VERTICAL with alignment)
- **VoiceToCode.java rewritten** — multi-provider from `ai.properties` (gemini/openai/ollama), audio file upload (drag-drop + chooser for `.wav`/`.mp3`/`.m4a`/`.ogg`/`.flac`/`.webm`), provider selector in UI, text-to-code fallback with language picker, save-to-editor button
- **ExportFlutter.java enhanced** — 15+ new widgets (table, form, input types, textarea, select, label, blockquote, pre, code, figure, video, iframe, svg/canvas, strong/em/u/del); stateful/responsive/dark mode options; style mapping (padding, margin, background, border-radius, box-shadow, width, height)
- **LiveShare.java enhanced** — file comparison/diff tab (line-by-line + directory compare), two-file browser, diff result viewer, server URL auto-copy to clipboard, file count display, sorted directory listing with size/date, 45+ MIME types
- **DatabaseViewerDialog.java enhanced** — multi-DB support: SQLite, MySQL, PostgreSQL, MariaDB with network connection fields (host/port/DB/user/pass); CRUD operations: Insert Row dialog (auto-detect auto-increment), Edit Selected Row (uses primary keys), Delete Selected with confirmation; SQL Query tab with run/result for any statement, auto-refresh table list on INSERT/UPDATE/DELETE
- **`\$` compilation fix** — replaced all `\$` with `$` in `CompletionProvider.java` (30+ errors in Vue/Svelte/PHP strings)
- **ExportFlutter fixed** — added missing `child:` prefix in single-child Container output (was generating invalid Dart); `ul`/`ol` now passes inner HTML through `convertNode()` instead of `stripTags()` so nested formatting is preserved
- **FigmaToHtml fixed** — CSS now includes `position: absolute; left: Xpx; top: Ypx;` for each element (x,y were read but never written to CSS output)
- **ThreeDViewerPlugin** — new builtin plugin (`com.eagle.plugin.builtin.ThreeDViewerPlugin`) with FXML dialog + Controller; loads and renders 3D models (glTF/GLB, OBJ, STL) via WebView + Three.js (CDN); controls: OrbitControls (rotate/zoom/pan), color picker, wireframe mode, background color, camera reset; registered in toolbar, menu, and command palette
- **Project compiles with 0 errors** — full `javac` build succeeds across all packages

### Legacy (pre-existing, stable)
- Status bar progress + log viewer, AiFileManager, Split pane UI, File path parsing, File dedup with Set, Detailed change report, Auto-save session, Auto-scroll, Git auto-commit, Terminal command execution, Web search, CodeFormatterTools, DepVisualizer, Snippets persistence, Import modes, Preview images, New language types, WebView dashboard, CodeFormatterTools

## In Progress
- (none)

## Blocked
- Full LSP – CodeMirror 5 has no LSP integration; RichTextFX LSP needs LSP4J client + TextDocumentService
- Package Manager – no Maven/Gradle/npm/pip UI
- Smart Workspaces – `SessionManager` saves open files only
- Docking UI – no floating/dockable windows
- Macro/Automation – `CommandPalette` executes single commands only
- Remote Development – SSH/Docker/WSL tunnels not implemented

## Key Decisions
- **`GeneratorLogViewer` as separate class** — reusable live log viewer with colored cells, thread-safe refresh, export, prompt saving
- **Backup before AI development** — copies sources to `~/.webide/backups/{project}_{timestamp}` for rollback
- **Tool checking before execution** — `isToolAvailable()` verifies tool exists via `--version` exit code
- **VoiceToCode multi-provider** — reads `provider`, `gemini.*`, `openai.*`, `ollama.*` from `ai.properties`; respects `provider` setting; audio stored in `~/.webide/voice/`
- **FigmaToHtml gradient support** — parses gradient-angle, `gradientStops`, `linear-gradient`/`radial-gradient` from Figma JSON
- **AiPanel side panel** — wrapped in `sideScroll` (`ScrollPane`); toggle uses `setVisible(false)` + `setManaged(false)` instead of SplitPane item removal
- **DatabaseViewer CRUD** — uses `DatabaseMetaData.getPrimaryKeys()` for where clauses; falls back to all-columns WHERE when no PK found; quotes identifiers per DB type (`` ` `` for MySQL/MariaDB, `"` for PostgreSQL)
- **LiveShare compare** — directory compare uses `TreeSet` for sorted paths, content comparison by reading both files, size check optimization
- **`\$` fix** — Java strings don't need `$` escaped; all `\$` in Vue/Svelte/PHP snippets replaced with `$`

## Next Steps
1. ✨ Populate hover data for C, C++, Kotlin, Go, Rust, YAML, XML, Markdown, Shell (empty maps exist)
2. ✨ Add inline documentation / code actions — quick-fix suggestions via context menu
3. ✨ Explore LSP integration alternatives (execa + language server protocol over stdio)
4. Add more web project template variations (Astro, SvelteKit, Remix, etc.)
5. Allow custom project type input in the generator dialog
6. Add rollback button to development summary alert

## Critical Context
- **"Uncompilable code"** — NetBeans incremental-compile issue when V: drive jar unavailable; use Clean & Build or `java -jar dist/Webide.jar`
- **RichTextFX 0.9.1** path: `V:\KADINIO\RECETA_MAIN\lib\richtextfx-0.9.1.jar`
- **Groq**: key starts with `gsk_`, endpoint `https://api.groq.com/openai/v1/chat/completions`
- **GitHub Models**: classic token with `read:user` scope, endpoint `https://models.inference.ai.azure.com/chat/completions`
- **GGUF**: `--model` + `--predict -1` + `--file <temp>` with `redirectErrorStream(true)`
- **Ollama**: `/api/chat` with SSE, falls back to `/api/generate` on 404
- **Streaming flush**: 25ms interval for smooth typewriter effect
- **Session files**: `~/.webide/sessions/` as JSON
- **Saved prompts**: `~/.webide/saved_prompts.json` (JSON array of {source, timestamp, project, prompt})
- **Backup directory**: `~/.webide/backups/{project}_{timestamp}/` created automatically before AI development
- **JavaFX thread safety**: `setGeneratorProgress` uses `Platform.runLater`
- **AiPanel side panel** uses ScrollPane (`sideScroll`) not bare VBox; toggle calls `setVisible(false)` + `setManaged(false)` to collapse SplitPane
- **`~/.webide/ai.properties`**: stores `provider`, `gemini.*`, `openai.*`, `ollama.*` keys; VoiceToCode reads this for multi-provider audio transcription
- **Build status**: full `javac` build with 0 errors; only pre-existing deprecation/unchecked warnings from JavaFX 8 API usage
- **DatabaseViewer multi-DB**: expects JDBC drivers on classpath (`org.sqlite.JDBC`, `com.mysql.cj.jdbc.Driver`, `org.postgresql.Driver`, `org.mariadb.jdbc.Driver`); SQLite bundled, others need manual addition

## Relevant Files
- `src/com/eagle/editor/AiPanel.java` — side panel toggle fixed (line 202-215), auto-project mode on setProjectRoot() (line 120-139), `sideScroll` + `splitPane` fields
- `src/com/eagle/editor/ClipboardHistoryPanel.java` — rewrite: search, copy-on-click, delete, pin, context menu, 100-item limit, 500ms auto-poll
- `src/com/eagle/tools/FigmaToHtml.java` — rewrite: drag-drop, file/URL load, export, preview, gradient/shadow CSS, flex layout, auto-headings
- `src/com/eagle/tools/VoiceToCode.java` — rewrite: Gemini/OpenAI/Ollama via ai.properties, audio upload, wave viz, text-to-code fallback
- `src/com/eagle/tools/ExportFlutter.java` — enhanced: 15+ widgets, stateful/responsive/dark mode, style mapping
- `src/com/eagle/tools/LiveShare.java` — enhanced: file compare/diff tab, directory compare, auto-copy URL, sorted listing with size/date
- `src/com/eagle/editor/DatabaseViewerDialog.java` — enhanced: SQLite/MySQL/PostgreSQL/MariaDB, CRUD (Insert/Edit/Delete), SQL query tab
- `src/com/eagle/plugin/builtin/ThreeDViewerPlugin.java` — builtin plugin: 3D model viewer (glTF/OBJ/STL), toolbar/menu/command registration
- `src/com/eagle/plugin/builtin/ThreeDViewerController.java` — controller: WebView + Three.js rendering, model loading, camera reset, color/wireframe/bg controls
- `src/com/eagle/plugin/builtin/ThreeDViewerDialog.fxml` — FXML layout: file browser, format choice, color pickers, wireframe toggle, controls
- `src/com/eagle/editor/CompletionProvider.java` — `\$` → `$` fixed; all 30-language hover/signature/completion data
- `src/com/eagle/tools/GeneratorLogViewer.java` — live colored log viewer with export, save prompt, saved prompts viewer
- `src/com/eagle/tools/ProjectAiChat.java` — rewritten with tech stack detection, file sizes, prompt saving
- `src/com/eagle/editor/TerminalPanel.java` — `runCommand(String cmd)` + `TerminalSession.sessionDir`
- `src/com/eagle/editor/CodeEditor.java` — `formatText()` covers 15 languages including Python `pythonIndentFormat()`
- `src/com/eagle/editor/CodeLinter.java` — 5 new lint methods for C, C++, Kotlin, Go, Rust
- `src/com/eagle/editor/SyntaxHighlighter.java` — 7 new patterns + 12 named groups for all 30 languages
- `src/com/eagle/controller/EditorController.java` — `onRunProject()`, `detectRunCommand()`, `isToolAvailable()`, `showToolMissingDialog()`, rewritten `onDevelopProjectWithAi()`, `saveAiPrompt()`, `createDevelopmentBackup()`, thread-safe log
