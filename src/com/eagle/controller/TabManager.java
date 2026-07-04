package com.eagle.controller;

import com.eagle.editor.CodeEditor;
import com.eagle.editor.LanguageType;
import com.eagle.editor.MediaViewer;
import com.eagle.editor.SplitEditorPane;
import com.eagle.util.EditorSettings;
import com.eagle.util.FileIconUtil;
import com.eagle.util.ThemeManager;
import com.eagle.icons.IconManager;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.Node;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Owns the lifecycle of editor tabs: opening files (code or media), tracking
 * dirty state, saving, closing with unsaved-changes confirmation, and keeping
 * the File &lt;-&gt; Tab &lt;-&gt; content maps in sync on rename/delete.
 *
 * Extracted out of EditorController as a pure refactor (Phase 1 of the IDE
 * roadmap) — behavior is unchanged from the original inline implementation;
 * this class just owns the bookkeeping so EditorController can stay focused
 * on wiring the rest of the window (tree, preview, debugger, toolbar).
 */
public class TabManager {

    private final TabPane tabPane;

    private final Map<File, Tab> openTabs = new HashMap<>();
    private final Map<Tab, CodeEditor> tabEditors = new HashMap<>();
    private final Map<Tab, SplitEditorPane> tabSplitPanes = new HashMap<>();
    private final Map<Tab, MediaViewer> tabMediaViewers = new HashMap<>();
    private final Map<Tab, File> tabFiles = new HashMap<>();
    private final Map<File, Long> fileLastModifiedTimes = new HashMap<>();

    /** Called whenever a code tab's content changes (debounced preview refresh hook, dirty marker already handled internally). */
    private Consumer<File> onContentChanged;
    /** Called whenever the caret moves in the active code editor (for the status bar). */
    private Consumer<CodeEditor> onCaretMoved;
    /** Called when a breakpoint gutter dot is toggled: (file, lineIndex). */
    private BiConsumer<File, Integer> onBreakpointToggled;
    /** Called after a file is successfully opened (for status bar / file-type label updates). */
    private Consumer<File> onFileOpened;
    /** Called after a save completes successfully. */
    private Consumer<File> onFileSaved;
    /** Supplies an error display mechanism owned by the caller (keeps this class UI-toolkit-light but still consistent). */
    private Consumer<String> errorHandler = msg -> { };
    /** Called when a new CodeEditor is created for a tab (to wire debugger, etc.). */
    private Consumer<CodeEditor> onEditorCreated;

    public TabManager(TabPane tabPane) {
        this.tabPane = tabPane;
    }

    public void setOnContentChanged(Consumer<File> callback) { this.onContentChanged = callback; }
    public void setOnCaretMoved(Consumer<CodeEditor> callback) { this.onCaretMoved = callback; }
    public void setOnBreakpointToggled(BiConsumer<File, Integer> callback) { this.onBreakpointToggled = callback; }
    public void setOnFileOpened(Consumer<File> callback) { this.onFileOpened = callback; }
    public void setOnFileSaved(Consumer<File> callback) { this.onFileSaved = callback; }
    public void setErrorHandler(Consumer<String> handler) { this.errorHandler = handler; }
    public void setOnEditorCreated(Consumer<CodeEditor> callback) { this.onEditorCreated = callback; }

    public boolean isOpen(File file) {
        return openTabs.containsKey(file);
    }

    public void selectIfOpen(File file) {
        Tab tab = openTabs.get(file);
        if (tab != null) {
            tabPane.getSelectionModel().select(tab);
        }
    }

    /** Creates the tab header node with file icon + toggle button. */
    private Node createTabGraphic(File file, CodeEditor editor) {
        Node fileIcon = FileIconUtil.iconViewFor(file);
        Button modeBtn = new Button();
        modeBtn.setGraphic(IconManager.imageView(IconManager.FILE_CODE, 12));
        modeBtn.getStyleClass().add("tab-toggle-btn");
        modeBtn.setOnAction(e -> editor.toggleEditorMode());
        HBox box = new HBox(3, fileIcon, modeBtn);
        box.setStyle("-fx-alignment: center-left;");
        return box;
    }

    /** Opens a code file in a new tab (caller must have already verified it isn't a media file and isn't already open). */
    public CodeEditor openCodeFile(File file) {
        try {
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            LanguageType lang = LanguageType.fromFile(file);
            CodeEditor editor = new CodeEditor(lang);
            editor.applySettings();
            editor.setText(content);

            Tab tab = new Tab(file.getName());
            tab.setGraphic(createTabGraphic(file, editor));
            tab.setContent(editor);
            tab.setOnCloseRequest(e -> {
                if (isDirty(tab)) {
                    e.consume();
                    confirmCloseDirty(tab, file);
                } else {
                    cleanup(tab, file);
                }
            });
            tab.setContextMenu(createTabContextMenu(tab, file));

            editor.setOnContentChanged(() -> {
                if (!tab.getText().endsWith("●")) {
                    tab.setText(tab.getText() + " ●");
                }
                if (onContentChanged != null) onContentChanged.accept(file);
            });

            editor.setOnCaretMoved(pos -> {
                if (onCaretMoved != null) onCaretMoved.accept(editor);
            });

            editor.setOnBreakpointToggled(lineIndex -> {
                if (onBreakpointToggled != null) onBreakpointToggled.accept(file, lineIndex);
            });

            tabPane.getTabs().add(tab);
            tabPane.getSelectionModel().select(tab);

            openTabs.put(file, tab);
            tabEditors.put(tab, editor);
            tabFiles.put(tab, file);
            fileLastModifiedTimes.put(file, file.lastModified());

            if (onEditorCreated != null) onEditorCreated.accept(editor);
            if (onFileOpened != null) onFileOpened.accept(file);
            return editor;

        } catch (IOException e) {
            errorHandler.accept("Failed to open file: " + e.getMessage());
            return null;
        }
    }

    public void openMediaFile(File file) {
        MediaViewer viewer = new MediaViewer(file);
        Tab tab = new Tab(file.getName());
        tab.setGraphic(FileIconUtil.iconViewFor(file));
        tab.setContent(viewer);
        tab.setOnCloseRequest(e -> {
            viewer.dispose();
            cleanup(tab, file);
        });
        tab.setContextMenu(createTabContextMenu(tab, file));

        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);

        openTabs.put(file, tab);
        tabMediaViewers.put(tab, viewer);
        tabFiles.put(tab, file);

        if (onFileOpened != null) onFileOpened.accept(file);
    }

    public boolean isDirty(Tab tab) {
        return tab.getText().contains("●");
    }

    private void confirmCloseDirty(Tab tab, File file) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText(file.getName() + " has unsaved changes");
        alert.setContentText("Do you want to save before closing?");
        ButtonType save = new ButtonType("Save");
        ButtonType discard = new ButtonType("Discard");
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(save, discard, cancel);
        ThemeManager.getInstance().applyTheme(alert.getDialogPane().getScene());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == save) {
                save(tab, file);
                cleanup(tab, file);
                tabPane.getTabs().remove(tab);
            } else if (result.get() == discard) {
                cleanup(tab, file);
                tabPane.getTabs().remove(tab);
            }
        }
    }

    private void cleanup(Tab tab, File file) {
        openTabs.remove(file);
        tabEditors.remove(tab);
        tabSplitPanes.remove(tab);
        tabMediaViewers.remove(tab);
        tabFiles.remove(tab);
        fileLastModifiedTimes.remove(file);
    }

    public void saveActive() {
        Tab current = tabPane.getSelectionModel().getSelectedItem();
        if (current == null) return;
        File file = tabFiles.get(current);
        if (file != null) save(current, file);
    }

    public void saveAll() {
        for (Map.Entry<Tab, File> entry : tabFiles.entrySet()) {
            save(entry.getKey(), entry.getValue());
        }
    }

    public void save(Tab tab, File file) {
        CodeEditor editor = tabEditors.get(tab);
        if (editor == null) {
            SplitEditorPane sep = tabSplitPanes.get(tab);
            if (sep != null) editor = sep.getActiveEditor();
        }
        if (editor == null) return; // media tabs aren't saved
        try {
            Files.write(file.toPath(), editor.getText().getBytes(StandardCharsets.UTF_8));
            tab.setText(tab.getText().replace(" ●", ""));
            fileLastModifiedTimes.put(file, file.lastModified());
            if (onFileSaved != null) onFileSaved.accept(file);
        } catch (IOException e) {
            errorHandler.accept("Failed to save file: " + e.getMessage());
        }
    }

    /** Closes all open tabs, prompting save for dirty files. */
    public void closeAll() {
        java.util.ArrayList<Tab> tabs = new java.util.ArrayList<>(tabPane.getTabs());
        for (Tab tab : tabs) {
            File file = tabFiles.get(tab);
            if (file != null) {
                if (isDirty(tab)) {
                    confirmCloseDirty(tab, file);
                } else {
                    tabPane.getTabs().remove(tab);
                    cleanup(tab, file);
                }
            } else {
                tabPane.getTabs().remove(tab);
            }
        }
    }

    /** Closes and forgets a tab for a file that was deleted on disk (no save prompt). */
    public void forceCloseForDeletedFile(File file) {
        Tab tab = openTabs.get(file);
        if (tab != null) {
            tabPane.getTabs().remove(tab);
            cleanup(tab, file);
        }
    }

    /** Keeps the File<->Tab maps in sync after an on-disk rename. */
    public void onFileRenamed(File oldFile, File newFile) {
        if (openTabs.containsKey(oldFile)) {
            Tab tab = openTabs.remove(oldFile);
            tabFiles.put(tab, newFile);
            openTabs.put(newFile, tab);
            tab.setText(newFile.getName());
            tab.setGraphic(createTabGraphic(newFile, tabEditors.get(tab)));
            Long lastMod = fileLastModifiedTimes.remove(oldFile);
            if (lastMod != null) {
                fileLastModifiedTimes.put(newFile, lastMod);
            } else {
                fileLastModifiedTimes.put(newFile, newFile.lastModified());
            }
        }
    }

    public CodeEditor getActiveEditor() {
        Tab current = tabPane.getSelectionModel().getSelectedItem();
        if (current == null) return null;
        CodeEditor ed = tabEditors.get(current);
        if (ed != null) return ed;
        SplitEditorPane sp = tabSplitPanes.get(current);
        return sp != null ? sp.getActiveEditor() : null;
    }

    public void splitEditorHorizontal() {
        Tab current = tabPane.getSelectionModel().getSelectedItem();
        if (current == null) return;
        CodeEditor ed = tabEditors.get(current);
        if (ed == null) return;
        SplitEditorPane sep = new SplitEditorPane(ed);
        tabSplitPanes.put(current, sep);
        current.setContent(sep);
        tabEditors.remove(current);
    }

    public void splitEditorVertical() {
        Tab current = tabPane.getSelectionModel().getSelectedItem();
        if (current == null) return;
        CodeEditor ed = tabEditors.get(current);
        if (ed == null) return;
        SplitEditorPane sep = new SplitEditorPane(ed);
        tabSplitPanes.put(current, sep);
        current.setContent(sep);
        tabEditors.remove(current);
    }

    public void closeSplitEditor() {
        Tab current = tabPane.getSelectionModel().getSelectedItem();
        if (current == null) return;
        SplitEditorPane sep = tabSplitPanes.get(current);
        if (sep == null) return;
        CodeEditor first = sep.getActiveEditor();
        if (first != null) {
            current.setContent(first);
            tabEditors.put(current, first);
        }
        tabSplitPanes.remove(current);
    }

    public boolean isSplitEditor() {
        Tab current = tabPane.getSelectionModel().getSelectedItem();
        return current != null && tabSplitPanes.containsKey(current);
    }

    public File getActiveFile() {
        Tab current = tabPane.getSelectionModel().getSelectedItem();
        return current != null ? tabFiles.get(current) : null;
    }

    public File fileFor(Tab tab) {
        return tabFiles.get(tab);
    }

    public CodeEditor editorFor(Tab tab) {
        return tabEditors.get(tab);
    }

    /** Returns the editor's in-memory content if the file is open (unsaved edits included), else reads from disk. */
    public String getEditableContentOrDisk(File file) throws IOException {
        Tab tab = openTabs.get(file);
        if (tab != null) {
            CodeEditor editor = tabEditors.get(tab);
            if (editor != null) return editor.getText();
        }
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    public java.util.Collection<CodeEditor> allOpenEditors() {
        return tabEditors.values();
    }

    public java.util.Collection<File> allOpenFiles() {
        return tabFiles.values();
    }

    public Tab tabFor(File file) {
        return openTabs.get(file);
    }

    public CodeEditor getEditorForFile(File file) {
        Tab tab = openTabs.get(file);
        return tab != null ? tabEditors.get(tab) : null;
    }

    public CodeEditor getEditorForTab(Tab tab) {
        return tab != null ? tabEditors.get(tab) : null;
    }

    public Long getFileLastModifiedTime(File file) {
        return fileLastModifiedTimes.get(file);
    }

    public void updateFileLastModifiedTime(File file, long time) {
        fileLastModifiedTimes.put(file, time);
    }

    public void reloadFileFromDisk(File file) {
        Tab tab = openTabs.get(file);
        if (tab == null) return;
        CodeEditor editor = tabEditors.get(tab);
        if (editor == null) return;
        try {
            if (file.exists()) {
                String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                if (!content.equals(editor.getText())) {
                    int caret = editor.getCaretPosition();
                    editor.setText(content);
                    tab.setText(file.getName()); // Clears the dirty marker
                    if (caret <= content.length()) {
                        editor.moveTo(caret);
                    } else {
                        editor.moveTo(content.length());
                    }
                }
                fileLastModifiedTimes.put(file, file.lastModified());
            }
        } catch (IOException e) {
            // Ignore
        }
    }

    private ContextMenu createTabContextMenu(Tab tab, File file) {
        MenuItem close = new MenuItem("Close");
        close.setOnAction(e -> {
            tab.getOnCloseRequest().handle(null);
            tabPane.getTabs().remove(tab);
        });

        MenuItem closeOthers = new MenuItem("Close Others");
        closeOthers.setOnAction(e -> {
            java.util.ArrayList<Tab> copy = new java.util.ArrayList<>(tabPane.getTabs());
            for (Tab t : copy) {
                if (t != tab) {
                    t.getOnCloseRequest().handle(null);
                    tabPane.getTabs().remove(t);
                }
            }
        });

        MenuItem closeAll = new MenuItem("Close All");
        closeAll.setOnAction(e -> {
            java.util.ArrayList<Tab> copy = new java.util.ArrayList<>(tabPane.getTabs());
            for (Tab t : copy) {
                t.getOnCloseRequest().handle(null);
                tabPane.getTabs().remove(t);
            }
        });

        SeparatorMenuItem sep = new SeparatorMenuItem();

        MenuItem copyPath = new MenuItem("Copy Full Path");
        copyPath.setOnAction(e -> {
            ClipboardContent cc = new ClipboardContent();
            cc.putString(file.getAbsolutePath());
            Clipboard.getSystemClipboard().setContent(cc);
        });

        MenuItem copyName = new MenuItem("Copy File Name");
        copyName.setOnAction(e -> {
            ClipboardContent cc = new ClipboardContent();
            cc.putString(file.getName());
            Clipboard.getSystemClipboard().setContent(cc);
        });

        return new ContextMenu(close, closeOthers, closeAll, sep, copyPath, copyName);
    }
}
