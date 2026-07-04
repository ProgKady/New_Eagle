# Web IDE (JavaFX 8)

A modern desktop IDE for HTML/CSS/JS projects built with plain JavaFX 8 — no external libraries.

## Features
- Welcome screen: **New Project** (scaffolds index.html/style.css/script.js) or **Open Project** (any folder), plus a **Recent Projects** list (persisted via `java.util.prefs`).
- Dark / Light theme toggle (top-right switch), synced across windows and saved between runs.
- File explorer tree (left) — open, create file/folder, rename, delete (right-click).
- Tabbed code editor with line numbers, unsaved-changes dot indicator, Save / Save All.
- Live HTML preview pane (right) using `WebView` — inlines your edited (even unsaved) CSS/JS so changes show instantly, debounced ~400ms while typing.
- Status bar: cursor line/column, file type, project path.

## Requirements
- JDK 8 (with bundled JavaFX) — e.g. Oracle JDK 8 or Azul Zulu 8 FX, or any JDK 8 distro that includes `javafx.*` modules on the classpath.
- Maven 3.6+

## Build & Run
```bash
mvn clean package
mvn exec:java
```
or run the built jar directly with a JDK 8 that bundles JavaFX:
```bash
java -jar target/web-ide.jar
```

> If using a JDK that does NOT bundle JavaFX (e.g. OpenJDK 11+), you'll need OpenJFX dependencies and `--module-path`/`--add-modules` flags instead — let me know and I'll adjust the pom for that target.

## Project Structure
```
src/main/java/com/eagle/
  Main.java                  – app entry point
  controller/
    WelcomeController.java   – new/open project screen logic
    EditorController.java    – main IDE logic (tree, tabs, save, preview)
    CodeEditor.java          – TextArea + line-number gutter control
  model/
    RecentProject.java
    FileTreeItem.java
  util/
    ThemeManager.java        – dark/light theme switching + persistence
    RecentProjectsStore.java – recent projects persistence
    FileIconUtil.java        – per-extension icons / editable check
src/main/resources/com/eagle/
  fxml/Welcome.fxml
  fxml/Editor.fxml
  css/base.css       – shared layout/structure styles
  css/dark-theme.css
  css/light-theme.css
```

## Notes
- No third-party syntax-highlighting library is used (kept dependency-free for Java 8); the editor is a clean `TextArea` + gutter. Next step can add real syntax highlighting (regex-based, per file type) if you want it.
- Live preview currently targets `.html`/`.htm` files and inlines sibling `<link rel="stylesheet">` and `<script src="...">` files found relative to that HTML file.
