package com.eagle.tools;

import com.eagle.util.ThemeManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class LanguageSupportDialog {

    private final Stage stage = new Stage();
    private final TableView<LangRow> table = new TableView<>();
    private final ObservableList<LangRow> rows = FXCollections.observableArrayList();
    private CheckBox enableCheck;
    private Label runtimeStatus, linterStatus, formatterStatus;
    private TextField runtimePathField, linterPathField, formatterPathField;
    private Button installRuntimeBtn, installLinterBtn, installFormatterBtn;
    private LangRow selected;

    public LanguageSupportDialog() {
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Language Support Manager");
        stage.setMinWidth(800);
        stage.setMinHeight(550);

        LanguageSupportManager.detectAll();
        LanguageSupportManager.loadSettings();

        buildTable();
        buildDetailPanel();

        SplitPane split = new SplitPane(table, buildDetailPanel());
        split.setDividerPositions(0.55);

        ButtonBar bar = new ButtonBar();
        Button closeBtn = new Button("Close");
        closeBtn.setOnAction(e -> { saveSettings(); stage.close(); });
        ButtonBar.setButtonData(closeBtn, ButtonBar.ButtonData.RIGHT);
        bar.getButtons().add(closeBtn);

        VBox root = new VBox(10, split, bar);
        root.setPadding(new Insets(10));
        VBox.setVgrow(split, Priority.ALWAYS);

        Scene scene = new Scene(root);
        ThemeManager.getInstance().applyTheme(scene);
        stage.setScene(scene);
    }

    public void show() {
        refreshTable();
        stage.showAndWait();
    }

    private void saveSettings() {
        for (LangRow r : rows) {
            LanguageSupportManager.LangConfig c = LanguageSupportManager.get(r.name.get());
            if (c != null) c.enabled = r.enabled.isSelected();
        }
        LanguageSupportManager.saveSettings();
    }

    private void refreshTable() {
        rows.clear();
        for (LanguageSupportManager.LangConfig c : LanguageSupportManager.getAll()) {
            rows.add(new LangRow(c));
        }
        table.setItems(rows);
    }

    // ── Table ─────────────────────────────────────────────────────────────────
    private static class LangRow {
        SimpleStringProperty name = new SimpleStringProperty();
        SimpleStringProperty runtime = new SimpleStringProperty();
        SimpleStringProperty linter = new SimpleStringProperty();
        SimpleStringProperty formatter = new SimpleStringProperty();
        CheckBox enabled = new CheckBox();

        LangRow(LanguageSupportManager.LangConfig c) {
            name.set(c.name);
            runtime.set(c.runtimeFound ? "✓ " + c.runtimePath : "✗ Not found");
            linter.set(c.linterFound ? "✓ " + c.linterPath : (c.linterCheck.isEmpty() ? "—" : "✗ Not found"));
            formatter.set(c.formatterFound ? "✓ " + c.formatterPath : (c.formatterCheck.isEmpty() ? "—" : "✗ Not found"));
            enabled.setSelected(c.enabled);
        }
    }

    private void buildTable() {
        TableColumn<LangRow, String> nameCol = new TableColumn<>("Language");
        nameCol.setCellValueFactory(d -> d.getValue().name);
        nameCol.setPrefWidth(110);

        TableColumn<LangRow, String> runtimeCol = new TableColumn<>("Runtime");
        runtimeCol.setCellValueFactory(d -> d.getValue().runtime);
        runtimeCol.setPrefWidth(200);

        TableColumn<LangRow, String> linterCol = new TableColumn<>("Linter");
        linterCol.setCellValueFactory(d -> d.getValue().linter);
        linterCol.setPrefWidth(180);

        TableColumn<LangRow, String> formatterCol = new TableColumn<>("Formatter");
        formatterCol.setCellValueFactory(d -> d.getValue().formatter);
        formatterCol.setPrefWidth(180);

        TableColumn<LangRow, Void> enableCol = new TableColumn<>("Enabled");
        enableCol.setPrefWidth(70);
        enableCol.setCellFactory(d -> new TableCell<LangRow, Void>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                LangRow row = (getTableRow() != null) ? (LangRow) getTableRow().getItem() : null;
                if (empty || row == null) setGraphic(null);
                else setGraphic(row.enabled);
            }
        });

        table.getColumns().addAll(nameCol, runtimeCol, linterCol, formatterCol, enableCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) showDetail(n);
        });
        table.setRowFactory(tv -> {
            TableRow<LangRow> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && row.getItem() != null) installLanguage(row.getItem().name.get());
            });
            return row;
        });
    }

    // ── Detail Panel ─────────────────────────────────────────────────────────
    private VBox buildDetailPanel() {
        enableCheck = new CheckBox("Enable language support");
        enableCheck.setOnAction(e -> { if (selected != null) selected.enabled.setSelected(enableCheck.isSelected()); });

        runtimeStatus = new Label("—");
        linterStatus = new Label("—");
        formatterStatus = new Label("—");
        runtimePathField = new TextField(); runtimePathField.setEditable(false);
        linterPathField = new TextField(); linterPathField.setEditable(false);
        formatterPathField = new TextField(); formatterPathField.setEditable(false);

        installRuntimeBtn = new Button("Download Runtime");
        installRuntimeBtn.setOnAction(e -> { if (selected != null) installLanguage(selected.name.get()); });

        installLinterBtn = new Button("Install Linter");
        installLinterBtn.setOnAction(e -> {
            LanguageSupportManager.LangConfig c = selected != null ? LanguageSupportManager.get(selected.name.get()) : null;
            if (c != null) showInstallInstructions(c.name, c.linterInstall);
        });

        installFormatterBtn = new Button("Install Formatter");
        installFormatterBtn.setOnAction(e -> {
            LanguageSupportManager.LangConfig c = selected != null ? LanguageSupportManager.get(selected.name.get()) : null;
            if (c != null) showInstallInstructions(c.name, c.formatterInstall);
        });

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(8);
        grid.setPadding(new Insets(12));
        grid.add(enableCheck, 0, 0, 3, 1);

        grid.addRow(1, new Label("Runtime:"), runtimeStatus, installRuntimeBtn);
        grid.addRow(2, new Label("Path:"), runtimePathField);

        grid.addRow(3, new Label("Linter:"), linterStatus, installLinterBtn);
        grid.addRow(4, new Label("Path:"), linterPathField);

        grid.addRow(5, new Label("Formatter:"), formatterStatus, installFormatterBtn);
        grid.addRow(6, new Label("Path:"), formatterPathField);

        VBox vb = new VBox(10, new Label("Language Details"), grid);
        vb.setPadding(new Insets(10));
        vb.setStyle("-fx-border-color: -fx-box-border; -fx-border-width: 0 0 0 1;");
        VBox.setVgrow(grid, Priority.ALWAYS);
        return vb;
    }

    private void showDetail(LangRow row) {
        selected = row;
        LanguageSupportManager.LangConfig c = LanguageSupportManager.get(row.name.get());
        if (c == null) return;
        enableCheck.setSelected(c.enabled);
        runtimeStatus.setText(c.runtimeFound ? "✓ Installed" : "✗ Not installed");
        runtimePathField.setText(c.runtimePath);
        linterStatus.setText(c.linterFound ? "✓ Installed" : (c.linterCheck.isEmpty() ? "— Not needed" : "✗ Not installed"));
        linterPathField.setText(c.linterPath);
        formatterStatus.setText(c.formatterFound ? "✓ Installed" : (c.formatterCheck.isEmpty() ? "— Not needed" : "✗ Not installed"));
        formatterPathField.setText(c.formatterPath);
        installRuntimeBtn.setVisible(!c.runtimeUrl.isEmpty());
        installLinterBtn.setVisible(!c.linterInstall.isEmpty());
        installFormatterBtn.setVisible(!c.formatterInstall.isEmpty());
    }

    private void installLanguage(String name) {
        LanguageSupportManager.LangConfig c = LanguageSupportManager.get(name);
        if (c == null) return;
        if (!c.runtimeFound && !c.runtimeUrl.isEmpty()) {
            LanguageSupportManager.installLanguage(name);
        }
    }

    private void showInstallInstructions(String lang, String instructions) {
        if (instructions.isEmpty()) return;
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Install " + lang + " Tool");
        dlg.setHeaderText("Installation Instructions");
        dlg.setResizable(true);
        dlg.getDialogPane().setPrefSize(500, 200);
        if (DialogUtil.getOwnerWindow() != null) dlg.initOwner(DialogUtil.getOwnerWindow());
        DialogUtil.applyTheme(dlg);

        TextArea area = new TextArea(instructions);
        area.setEditable(false);
        area.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        area.setWrapText(true);
        dlg.getDialogPane().setContent(area);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dlg.showAndWait();
    }

    public static void showDialog() {
        LanguageSupportDialog dlg = new LanguageSupportDialog();
        dlg.show();
    }
}
