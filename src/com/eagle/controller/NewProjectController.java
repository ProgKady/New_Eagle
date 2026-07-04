package com.eagle.controller;

import com.eagle.model.ProjectType;
import com.eagle.templates.TemplateProvider;
import com.eagle.templates.WebTemplate;
import com.eagle.util.ProjectsStore;
import com.eagle.util.ThemeManager;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class NewProjectController {

    @FXML private javafx.scene.layout.BorderPane rootPane;
    @FXML private TextField projectNameField;
    @FXML private ListView<ProjectCategory> categoryList;
    @FXML private ListView<ProjectSubType> projectTypeList;
    @FXML private Label typeDescLabel;
    @FXML private FlowPane templateGrid;
    @FXML private Label templateHeaderLabel;
    @FXML private VBox templateSection;

    private ProjectSubType selectedType;
    private WebTemplate selectedTemplate;
    private VBox selectedCard;
    private boolean templateMode = true;

    /** Result fields read by the caller after the dialog closes. */
    private File createdProjectDir;
    private boolean cancelled = true;

    /** IDs of templates to show for generic static-web selection */
    private static final Set<String> STATIC_TEMPLATE_IDS = new HashSet<>(Arrays.asList(
        "blank", "landing", "portfolio", "blog", "admin", "login",
        "ecommerce", "docs", "contact", "restaurant", "coming-soon"
    ));

    /** All available project sub-types */
    public static class ProjectSubType {
        public final String id;
        public final String displayName;
        public final String description;
        public final String icon;
        public final String category; // "static", "spa", "ssr", "mobile-hybrid", "android-js", "visual"
        public final String templateFilter; // template id to select, or null for custom scaffold

        public ProjectSubType(String id, String displayName, String description, String icon, String category, String templateFilter) {
            this.id = id; this.displayName = displayName; this.description = description;
            this.icon = icon; this.category = category; this.templateFilter = templateFilter;
        }

        @Override public String toString() { return icon + "  " + displayName; }
    }

    private static final ObservableList<ProjectSubType> ALL_TYPES = FXCollections.observableArrayList(
        // ---- Static Web ----
        new ProjectSubType("blank", "Blank HTML/CSS/JS", "Empty starter with index.html, style.css, script.js", "html", "static", null),
        new ProjectSubType("landing", "Landing Page", "Hero, features, pricing, CTA section", "rocket", "static", "landing"),
        new ProjectSubType("portfolio", "Portfolio", "Personal portfolio with projects grid", "palette", "static", "portfolio"),
        new ProjectSubType("blog", "Blog", "Article list with sidebar layout", "article", "static", "blog"),
        new ProjectSubType("admin", "Admin Dashboard", "Sidebar nav, stat cards, table", "chart", "static", "admin"),
        new ProjectSubType("login", "Login Page", "Centered card login/sign-up form", "lock", "static", "login"),
        new ProjectSubType("ecommerce", "E-commerce", "Product grid, cart, and checkout", "cart", "static", "ecommerce"),
        new ProjectSubType("docs", "Documentation", "Sidebar docs layout with search", "book", "static", "docs"),
        new ProjectSubType("contact", "Contact Form", "Contact page with form and info", "mail", "static", "contact"),
        // ---- SPA Frameworks (Vite-based) ----
        new ProjectSubType("react-vite", "React (Vite)", "React 18 + Vite SPA with JSX", "react", "spa", "react-vite"),
        new ProjectSubType("vue-vite", "Vue 3 (Vite)", "Vue 3 + Vite SPA with Composition API", "vue", "spa", "vue-vite"),
        new ProjectSubType("svelte-vite", "Svelte (Vite)", "Svelte 4 + Vite SPA with reactive components", "svelte", "spa", "svelte-vite"),
        new ProjectSubType("solid-vite", "Solid.js (Vite)", "Reactive UI library with Vite", "solid", "spa", "solid-vite"),
        new ProjectSubType("lit-vite", "Lit (Vite)", "Web Components library with Vite", "lit", "spa", "lit-vite"),
        new ProjectSubType("vite-ts", "Vite + TypeScript", "Vanilla TypeScript with Vite", "ts", "spa", "vite-ts"),
        new ProjectSubType("alpine", "Alpine.js", "Lightweight reactive JS framework", "alpine", "spa", "alpine"),
        // ---- SSR / Meta-Frameworks ----
        new ProjectSubType("nextjs", "Next.js", "React SSR framework with pages router", "next", "ssr", "nextjs"),
        new ProjectSubType("nuxt", "Nuxt 3", "Vue 3 meta-framework with file-based routing", "nuxt", "ssr", "nuxt"),
        new ProjectSubType("astro", "Astro", "Content-focused static site generator", "astro", "ssr", "astro"),
        // ---- Mobile-Hybrid (Android convertible) ----
        new ProjectSubType("quasar", "Quasar (Vue)", "Vue 3 + Quasar with mobile/Cordova support", "quasar", "mobile-hybrid", "quasar"),
        new ProjectSubType("framework7", "Framework7", "Mobile-first UI framework iOS/Material", "f7", "mobile-hybrid", "framework7"),
        new ProjectSubType("onsenui", "Onsen UI", "Mobile UI components Material/iOS", "onsen", "mobile-hybrid", "onsenui"),
        new ProjectSubType("cordova", "Cordova", "Hybrid mobile app with Apache Cordova", "cordova", "mobile-hybrid", "cordova"),
        new ProjectSubType("capacitor", "Capacitor", "Modern hybrid mobile runtime", "capacitor", "mobile-hybrid", "capacitor"),
        // ---- Android JS (DroidScript) ----
        new ProjectSubType("android-blank", "Blank Android JS", "Empty DroidScript app", "android", "android-js", "android-blank"),
        new ProjectSubType("android-todo", "Todo App (Android)", "DroidScript todo with add/delete", "check", "android-js", "android-todo"),
        new ProjectSubType("android-weather", "Weather App (Android)", "DroidScript weather display", "sun", "android-js", "android-weather"),
        new ProjectSubType("android-calc", "Calculator (Android)", "DroidScript calculator", "calc", "android-js", "android-calc"),
        new ProjectSubType("android-list", "List View (Android)", "DroidScript list view demo", "list", "android-js", "android-list"),
        // ---- Visual Builder ----
        new ProjectSubType("visual", "Drag & Drop Builder", "Build pages visually by dragging components", "visual", "visual", null)
    );

    /** Category data */
    public static class ProjectCategory {
        public final String id;
        public final String displayName;
        public final String description;
        public final String icon;
        public ProjectCategory(String id, String displayName, String description, String icon) {
            this.id = id; this.displayName = displayName; this.description = description; this.icon = icon;
        }
        @Override public String toString() { return icon + "  " + displayName; }
    }

    private static final ObservableList<ProjectCategory> CATEGORIES = FXCollections.observableArrayList(
        new ProjectCategory("static", "Static Websites", "HTML/CSS/JS — landing, blog, portfolio, docs, etc.", "html"),
        new ProjectCategory("spa", "SPA Frameworks", "React, Vue, Svelte, Solid, Lit, Alpine + Vite", "react"),
        new ProjectCategory("ssr", "SSR / Meta", "Next.js, Nuxt 3, Astro", "next"),
        new ProjectCategory("mobile-hybrid", "Mobile Hybrid", "Cordova, Capacitor, Quasar, F7, Onsen UI", "phone"),
        new ProjectCategory("android-js", "Android JS", "DroidScript apps — blank, todo, weather, etc.", "android"),
        new ProjectCategory("visual", "Visual Builder", "Drag & drop page builder", "visual")
    );

    @FXML
    public void initialize() {
        categoryList.setItems(CATEGORIES);
        categoryList.setCellFactory(param -> new CategoryListCell());
        categoryList.getSelectionModel().selectedItemProperty().addListener((obs, old, cat) -> {
            if (cat != null) onCategorySelected(cat);
        });

        projectTypeList.setCellFactory(param -> new TypeListCell());
        projectTypeList.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            if (val != null) onTypeSelected(val);
        });

        categoryList.getSelectionModel().select(0);
    }

    private static class CategoryListCell extends ListCell<ProjectCategory> {
        private final Label iconLabel = new Label();
        private final Label nameLabel = new Label();
        private final Label descLabel = new Label();
        private final VBox root;

        CategoryListCell() {
            iconLabel.setMinWidth(24);
            iconLabel.setAlignment(Pos.CENTER);
            iconLabel.setStyle("-fx-font-size: 14px;");
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
            descLabel.setStyle("-fx-font-size: 9px; -fx-opacity: 0.6;");
            descLabel.setWrapText(true);
            VBox info = new VBox(0);
            info.getChildren().addAll(nameLabel, descLabel);
            HBox hb = new HBox(6, iconLabel, info);
            hb.setAlignment(Pos.CENTER_LEFT);
            hb.setPadding(new Insets(3, 6, 3, 6));
            root = new VBox(hb);
        }

        @Override
        protected void updateItem(ProjectCategory item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                iconLabel.setText(item.icon);
                nameLabel.setText(item.displayName);
                descLabel.setText(item.description);
                setGraphic(root);
            }
        }
    }

    private void onCategorySelected(ProjectCategory cat) {
        ObservableList<ProjectSubType> filtered = FXCollections.observableArrayList();
        for (ProjectSubType t : ALL_TYPES) {
            if (t.category.equals(cat.id)) {
                filtered.add(t);
            }
        }
        projectTypeList.setItems(filtered);
        if (!filtered.isEmpty()) {
            projectTypeList.getSelectionModel().select(0);
        }
    }

    private static class TypeListCell extends ListCell<ProjectSubType> {
        private final Label iconLabel = new Label();
        private final Label nameLabel = new Label();
        private final Label descLabel = new Label();
        private final VBox root;

        TypeListCell() {
            iconLabel.setMinWidth(28);
            iconLabel.setAlignment(Pos.CENTER);
            iconLabel.setStyle("-fx-font-size: 16px;");
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
            descLabel.setStyle("-fx-font-size: 10px; -fx-opacity: 0.7;");
            descLabel.setWrapText(true);
            VBox info = new VBox(0);
            info.getChildren().addAll(nameLabel, descLabel);
            HBox hb = new HBox(8, iconLabel, info);
            hb.setAlignment(Pos.CENTER_LEFT);
            hb.setPadding(new Insets(4, 8, 4, 8));
            root = new VBox(hb);
        }

        @Override
        protected void updateItem(ProjectSubType item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                iconLabel.setText(item.icon);
                nameLabel.setText(item.displayName);
                descLabel.setText(item.description);
                setGraphic(root);
            }
        }
    }

    private void onTypeSelected(ProjectSubType type) {
        selectedType = type;
        typeDescLabel.setText(type.description);

        if ("visual".equals(type.id)) {
            templateSection.setVisible(false);
            templateSection.setManaged(false);
            templateMode = false;
            return;
        }

        templateSection.setVisible(true);
        templateSection.setManaged(true);
        templateMode = true;

        templateGrid.getChildren().clear();
        selectedCard = null;
        selectedTemplate = null;

        List<WebTemplate> all = TemplateProvider.getAll();

        // If a specific template filter is set, use only that template
        if (type.templateFilter != null) {
            for (WebTemplate t : all) {
                if (t.getId().equals(type.templateFilter)) {
                    VBox card = buildTemplateCard(t);
                    templateGrid.getChildren().add(card);
                    selectTemplate(t, card);
                    templateHeaderLabel.setText("Starter template");
                    break;
                }
            }
        } else {
            templateHeaderLabel.setText("Choose a starting template");
            for (WebTemplate t : all) {
                if (!STATIC_TEMPLATE_IDS.contains(t.getId())) continue;
                VBox card = buildTemplateCard(t);
                templateGrid.getChildren().add(card);
                if (selectedTemplate == null) {
                    selectTemplate(t, card);
                }
            }
        }
    }

    private VBox buildTemplateCard(WebTemplate tmpl) {
        VBox card = new VBox(6);
        card.getStyleClass().addAll("card", "card-hover", "template-card");
        card.setPrefWidth(190);
        card.setPrefHeight(100);
        card.setStyle("-fx-cursor: hand; -fx-padding: 16;");

        Label iconLabel = new Label(tmpl.getIcon());
        iconLabel.setStyle("-fx-font-size: 26px;");
        Label nameLabel = new Label(tmpl.getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        Label descLabel = new Label(tmpl.getDescription());
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-font-size: 11px; -fx-opacity: 0.7;");

        card.getChildren().addAll(iconLabel, nameLabel, descLabel);
        card.setOnMouseClicked(e -> selectTemplate(tmpl, card));
        return card;
    }

    private void selectTemplate(WebTemplate tmpl, VBox card) {
        if (selectedCard != null) {
            selectedCard.setStyle("-fx-cursor: hand; -fx-padding: 16;");
        }
        selectedTemplate = tmpl;
        selectedCard = card;
        card.setStyle("-fx-cursor: hand; -fx-padding: 15; -fx-border-color: -accent; -fx-border-width: 2; -fx-border-radius: 14;");
    }

    @FXML
    private void onCreate() {
        String name = projectNameField.getText() == null ? "" : projectNameField.getText().trim();
        if (name.isEmpty()) {
            showError("Please enter a project name.");
            return;
        }
        if (selectedType == null) {
            showError("Please select a project type.");
            return;
        }

        // Determine ProjectType enum from category
        ProjectType pt;
        switch (selectedType.category) {
            case "visual": pt = ProjectType.VISUAL; break;
            case "android-js": pt = ProjectType.ANDROID_JS; break;
            default: pt = ProjectType.CODE; break;
        }

        File projectDir = ProjectsStore.createProject(name, pt);
        if (projectDir == null) {
            showError("A project named '" + name + "' already exists.");
            return;
        }

        try {
            if ("visual".equals(selectedType.id)) {
                scaffoldVisualProject(projectDir);
            } else if (selectedTemplate != null) {
                writeTemplateFiles(projectDir, selectedTemplate);
            } else {
                scaffoldBasicFiles(projectDir);
            }
            writeReadme(projectDir, name, selectedType);
        } catch (IOException e) {
            showError("Failed to create project files: " + e.getMessage());
            return;
        }

        this.createdProjectDir = projectDir;
        this.cancelled = false;
        closeDialog();
    }

    private void scaffoldBasicFiles(File projectDir) throws IOException {
        String name = projectDir.getName();
        Files.write(new File(projectDir, "index.html").toPath(),
            ("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n" +
             "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
             "  <title>" + name + "</title>\n  <link rel=\"stylesheet\" href=\"style.css\">\n" +
             "</head>\n<body>\n  <h1>" + name + "</h1>\n  <script src=\"script.js\"></script>\n" +
             "</body>\n</html>\n").getBytes(StandardCharsets.UTF_8));
        Files.write(new File(projectDir, "style.css").toPath(),
            ("/* " + name + " styles */\nbody {\n    font-family: system-ui, sans-serif;\n" +
             "    max-width: 800px;\n    margin: 0 auto;\n    padding: 20px;\n}\n").getBytes(StandardCharsets.UTF_8));
        Files.write(new File(projectDir, "script.js").toPath(),
            ("// " + name + "\nconsole.log('Ready!');\n").getBytes(StandardCharsets.UTF_8));
    }

    private void writeTemplateFiles(File projectDir, WebTemplate tmpl) throws IOException {
        for (java.util.Map.Entry<String, String> entry : tmpl.getFiles().entrySet()) {
            File f = new File(projectDir, entry.getKey());
            File parent = f.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            Files.write(f.toPath(), entry.getValue().getBytes(StandardCharsets.UTF_8));
        }
    }

    private void scaffoldVisualProject(File projectDir) throws IOException {
        File layoutJson = new File(projectDir, "layout.json");
        Files.write(layoutJson.toPath(), "{\n  \"components\": []\n}\n".getBytes(StandardCharsets.UTF_8));

        File indexHtml = new File(projectDir, "index.html");
        Files.write(indexHtml.toPath(), (
                "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n" +
                "<title>" + projectDir.getName() + "</title>\n<link rel=\"stylesheet\" href=\"style.css\">\n</head>\n" +
                "<body>\n<!-- Generated by Visual Builder -->\n</body>\n</html>\n"
        ).getBytes(StandardCharsets.UTF_8));

        File styleCss = new File(projectDir, "style.css");
        Files.write(styleCss.toPath(), "/* Generated by Visual Builder */\n".getBytes(StandardCharsets.UTF_8));
    }

    private void writeReadme(File projectDir, String name, ProjectSubType type) throws IOException {
        String desc = type.description;
        String cat = type.category;
        String setupCmd, runCmd, tech;
        switch (cat) {
            case "spa":
                setupCmd = "npm install";
                runCmd = "npm run dev";
                tech = "Vite, npm";
                break;
            case "ssr":
                setupCmd = "npm install";
                runCmd = "npm run dev";
                tech = "Node.js, npm";
                break;
            case "mobile-hybrid":
                setupCmd = "npm install";
                runCmd = "npm start";
                tech = "Hybrid Mobile, npm";
                break;
            case "android-js":
                setupCmd = "Import into DroidScript app on your Android device";
                runCmd = "Run from DroidScript app";
                tech = "DroidScript JavaScript";
                break;
            case "visual":
                setupCmd = "Open layout.json in the Visual Builder";
                runCmd = "Open index.html in a browser";
                tech = "Visual Builder, HTML";
                break;
            default:
                setupCmd = "No special setup required";
                runCmd = "Open index.html in a browser";
                tech = "HTML, CSS, JavaScript";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(name).append("\n\n");
        sb.append(desc).append("\n\n");
        sb.append("## Tech Stack\n\n");
        sb.append("- **Language/Framework**: ").append(tech).append("\n");
        sb.append("- **Project Type**: ").append(type.displayName).append("\n\n");
        sb.append("## Getting Started\n\n");
        sb.append("### Installation\n\n");
        sb.append("```bash\n").append(setupCmd).append("\n```\n\n");
        sb.append("### Running\n\n");
        sb.append("```bash\n").append(runCmd).append("\n```\n\n");
        sb.append("## Project Structure\n\n```\n").append(name).append("/\n");
        String[] listing = projectDir.list();
        if (listing != null) {
            java.util.Arrays.sort(listing);
            int count = 0;
            for (String fn : listing) {
                if (count++ >= 20) { sb.append("  ... (more files)\n"); break; }
                sb.append("  ").append(fn).append(new File(projectDir, fn).isDirectory() ? "/\n" : "\n");
            }
        }
        sb.append("```\n");
        Files.write(new File(projectDir, "README.md").toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void closeDialog() {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void onCancel() {
        this.cancelled = true;
        closeDialog();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(null);
        ThemeManager.getInstance().applyTheme(alert.getDialogPane().getScene());
        alert.showAndWait();
    }

    // ---- Result accessors ----
    public File getCreatedProjectDir() { return createdProjectDir; }
    public boolean isCancelled() { return cancelled; }
}
