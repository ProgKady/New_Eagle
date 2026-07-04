package com.eagle.controller;

import com.eagle.builder.ComponentType;
import com.eagle.builder.HtmlExporter;
import com.eagle.builder.VisualComponent;
import com.eagle.editor.CodeEditor;
import com.eagle.editor.LanguageType;
import com.eagle.editor.ProblemsPanel;
import com.eagle.util.ThemeManager;
import com.eagle.icons.IconManager;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import javafx.animation.TranslateTransition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

public class BuilderController {

    @FXML private BorderPane rootPane;
    @FXML private SplitPane mainSplit;
    @FXML private VBox paletteBox;
    @FXML private Pane canvas;
    @FXML private Pane gridPane;
    @FXML private Pane selectionRectPane;
    @FXML private Pane guidesPane;
    @FXML private Pane handlesPane;
    @FXML private ScrollPane canvasScroll;
    @FXML private VBox propertiesBox;
    @FXML private VBox layersBox;
    @FXML private TreeView<Object> componentTree;
    @FXML private Label projectNameLabel;
    @FXML private Label statusLabel;
    @FXML private Label posLabel;
    @FXML private Label zoomLabel;
    @FXML private Slider zoomSlider;
    @FXML private StackPane themeTogglePane;
    @FXML private Circle toggleKnob;
    @FXML private CheckBox snapGridCheck;
    @FXML private CheckBox smartGuidesCheck;

    // ---- Core Data ----
    private File projectRoot;
    private final VisualComponent rootComponent = new VisualComponent(ComponentType.CONTAINER);
    private final Map<Node, VisualComponent> nodeToComponent = new HashMap<>();
    private final Map<VisualComponent, Node> componentToNode = new HashMap<>();
    private final Map<VisualComponent, Pane> containerContentPanes = new HashMap<>();
    private final Set<Node> selectedNodes = new HashSet<>();
    private final Set<Node> multiSelectedNodes = new HashSet<>();
    private VisualComponent selectedComponent;
    private VisualComponent selectedChildComponent;
    private Node selectedNode;
    private boolean multiSelectMode = false;

    // ---- Drag State ----
    private double dragStartX, dragStartY;
    private double dragOrigX, dragOrigY;
    private boolean isDragging = false;
    private Node dragNode = null;
    private final Map<Node, double[]> dragStartPositions = new HashMap<>();
    private static final int GRID_SIZE = 20;
    private boolean snapEnabled = true;

    // ---- Resize State ----
    private enum ResizeDir { NW, N, NE, W, E, SW, S, SE }
    private ResizeDir currentResizeDir = null;
    private boolean isResizing = false;
    private double resizeStartX, resizeStartY;
    private double resizeOrigX, resizeOrigY, resizeOrigW, resizeOrigH;

    // ---- Rubber Band State ----
    private boolean isRubberBanding = false;
    private double rubberStartX, rubberStartY;
    private Rectangle rubberBandRect;

    // ---- Zoom ----
    private final DoubleProperty zoomFactor = new SimpleDoubleProperty(1.0);
    private final Scale zoomScale = new Scale(1, 1);

    // ---- Clipboard ----
    private String clipboardData = null;

    // ---- Undo/Redo ----
    private final Deque<String> undoStack = new ArrayDeque<>();
    private final Deque<String> redoStack = new ArrayDeque<>();
    private static final int MAX_UNDO = 50;

    // ---- Smart Guides ----
    private final List<Line> guideLines = new ArrayList<>();

    // ---- Context Menu ----
    private ContextMenu contextMenu;

    // ---- Handle Nodes (tracked for repositioning without remove/add) ----
    private final List<Circle> handleNodes = new ArrayList<>();

    private static final Color SELECTION_COLOR = Color.rgb(0, 120, 215, 0.12);
    private static final Color SELECTION_BORDER = Color.rgb(0, 120, 215);
    private static final Color HANDLE_COLOR = Color.WHITE;
    private static final Color HANDLE_BORDER = Color.rgb(0, 120, 215);
    private static final int HANDLE_SIZE = 8;
    private static final double MIN_COMPONENT_SIZE = 20;

    // ================================================================
    //   INITIALIZATION
    // ================================================================

    public static void openProject(File projectDir) throws IOException {
        FXMLLoader loader = new FXMLLoader(BuilderController.class.getResource("/com/eagle/fxml/Builder.fxml"));
        Parent root = loader.load();
        BuilderController controller = loader.getController();
        controller.initProject(projectDir);

        Stage stage = com.eagle.Main.getPrimaryStage();
        Scene scene = new Scene(root, 1400, 850);
        ThemeManager.getInstance().applyTheme(scene);
        stage.setScene(scene);
        stage.setTitle("Web IDE Builder - " + projectDir.getName());
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
    }

    @FXML
    public void initialize() {
        positionKnob(false);
        buildPalette();
        setupCanvas();
        setupKeyboardHandlers();
        buildContextMenu();
        showEmptyProperties();

        zoomSlider.valueProperty().addListener((obs, ov, nv) -> setZoomLevel(nv.doubleValue()));
        snapGridCheck.selectedProperty().addListener((obs, ov, nv) -> snapEnabled = nv);
        snapEnabled = snapGridCheck.isSelected();

        componentTree.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            if (val != null && val.getValue() instanceof VisualComponent) {
                VisualComponent vc = (VisualComponent) val.getValue();
                // Search all canvas children recursively for the matching node
                Node found = findWrapperFor(canvas.getChildren(), vc);
                if (found != null) {
                    clearSelection();
                    addToSelection(found, vc);
                }
            }
        });

        canvas.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);
                newScene.addEventFilter(KeyEvent.KEY_RELEASED, this::onKeyReleased);
            }
        });

        updateLayersPanel();
        buildTree();
        drawGrid();
    }

    private void initProject(File projectDir) {
        this.projectRoot = projectDir;
        projectNameLabel.setText(projectDir.getName());
        statusLabel.setText("Project: " + projectDir.getAbsolutePath());
    }

    // ================================================================
    //   PALETTE
    // ================================================================

    private TextField paletteSearchField;

    private void buildPalette() {
        paletteBox.getChildren().clear();

        // ---- Search field ----
        paletteSearchField = new TextField();
        paletteSearchField.setPromptText("Search components...");
        paletteSearchField.getStyleClass().add("palette-search");
        paletteSearchField.setMaxWidth(Double.MAX_VALUE);
        paletteSearchField.textProperty().addListener((obs, ov, nv) -> filterPalette(nv));
        paletteBox.getChildren().add(paletteSearchField);

        paletteAllItems = new ArrayList<>();

        // Group components by category
        java.util.LinkedHashMap<ComponentType.Category, java.util.List<ComponentType>> grouped = new java.util.LinkedHashMap<>();
        for (ComponentType type : ComponentType.values()) {
            ComponentType.Category cat = type.getCategory();
            if (!grouped.containsKey(cat)) grouped.put(cat, new java.util.ArrayList<>());
            grouped.get(cat).add(type);
        }

        for (java.util.Map.Entry<ComponentType.Category, java.util.List<ComponentType>> entry : grouped.entrySet()) {
            // Section header
            Label sectionHeader = new Label(entry.getKey().getDisplayName());
            sectionHeader.getStyleClass().add("palette-section-header");
            sectionHeader.setMaxWidth(Double.MAX_VALUE);
            paletteBox.getChildren().add(sectionHeader);

            for (ComponentType type : entry.getValue()) {
                HBox item = new HBox(8);
                item.getStyleClass().add("palette-item");
                item.setMaxWidth(Double.MAX_VALUE);
                item.setAlignment(Pos.CENTER_LEFT);
                item.setUserData(type);

                javafx.scene.Node iconNode = type.getIconView(18);
                if (iconNode != null) item.getChildren().add(iconNode);

                Label nameLabel = new Label(type.getLabel());
                nameLabel.getStyleClass().add("name-label");
                item.getChildren().add(nameLabel);

                item.setOnDragDetected(e -> {
                    Dragboard db = item.startDragAndDrop(TransferMode.COPY);
                    ClipboardContent content = new ClipboardContent();
                    content.putString(type.name());
                    db.setContent(content);
                    e.consume();
                });

                paletteBox.getChildren().add(item);
                paletteAllItems.add(item);
            }
        }
    }

    private List<HBox> paletteAllItems = new ArrayList<>();

    private void filterPalette(String query) {
        if (query == null || query.isEmpty()) {
            for (HBox item : paletteAllItems) {
                item.setVisible(true);
                item.setManaged(true);
            }
            // Show all section headers
            for (Node child : paletteBox.getChildren()) {
                if (child instanceof Label) child.setVisible(true);
            }
            return;
        }
        String lower = query.toLowerCase();
        boolean sectionVisible = false;
        for (Node child : paletteBox.getChildren()) {
            if (child instanceof Label) {
                // Hide section headers with no visible items below
                child.setVisible(sectionVisible = false);
            } else if (child instanceof HBox) {
                ComponentType type = (ComponentType) child.getUserData();
                boolean match = type.getLabel().toLowerCase().contains(lower)
                    || type.getCategory().getDisplayName().toLowerCase().contains(lower);
                child.setVisible(match);
                ((HBox) child).setManaged(match);
                if (match) sectionVisible = true;
            }
        }
    }

    // ================================================================
    //   CANVAS SETUP
    // ================================================================

    private Pane dropZoneOverlay;

    private void setupCanvas() {
        canvas.setStyle("-fx-background-color: -bg-tertiary;");

        // Drop zone indicator overlay
        dropZoneOverlay = new Pane();
        dropZoneOverlay.setMouseTransparent(true);
        dropZoneOverlay.setPickOnBounds(false);
        dropZoneOverlay.getStyleClass().add("drop-zone-indicator");
        dropZoneOverlay.setVisible(false);
        canvas.getChildren().add(dropZoneOverlay);

        canvas.setOnDragOver(e -> {
            if (e.getGestureSource() != canvas && e.getDragboard().hasString()) {
                e.acceptTransferModes(TransferMode.COPY);

                // Show drop zone indicator over content areas
                Pane contentArea = findContentAreaAt(e.getX(), e.getY());
                if (contentArea != null) {
                    double cx = wrapperAbsoluteX(contentArea);
                    double cy = wrapperAbsoluteY(contentArea);
                    dropZoneOverlay.setLayoutX(cx);
                    dropZoneOverlay.setLayoutY(cy);
                    dropZoneOverlay.setPrefSize(contentArea.getWidth(), contentArea.getHeight());
                    dropZoneOverlay.setVisible(true);
                    dropZoneOverlay.setOpacity(1.0);
                } else {
                    dropZoneOverlay.setVisible(false);
                }
            }
            e.consume();
        });

        canvas.setOnDragExited(e -> {
            dropZoneOverlay.setVisible(false);
        });

        canvas.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                try {
                    ComponentType type = ComponentType.valueOf(db.getString());

                    // Check if drop is over any container content area
                    Pane targetContentArea = findContentAreaAt(e.getX(), e.getY());

                    pushUndoState();
                    VisualComponent newComp = new VisualComponent(type);

                    double x = e.getX() - 50;
                    double y = e.getY() - 20;
                    if (snapEnabled) { x = snap(x); y = snap(y); }

                    boolean isBlock = type == ComponentType.HEADER_BLOCK || type == ComponentType.HERO_BLOCK ||
                        type == ComponentType.FEATURES_BLOCK || type == ComponentType.PRICING_BLOCK ||
                        type == ComponentType.TESTIMONIALS_BLOCK || type == ComponentType.FAQ_BLOCK ||
                        type == ComponentType.CONTACT_BLOCK || type == ComponentType.FOOTER_BLOCK ||
                        type == ComponentType.TEAM_BLOCK || type == ComponentType.STATS_BLOCK ||
                        type == ComponentType.CTA_BLOCK || type == ComponentType.NEWSLETTER_BLOCK;

                    if (targetContentArea != null) {
                        // Drop into container content area
                        VisualComponent owner = (VisualComponent) containerContentPanes.entrySet().stream()
                            .filter(entry -> entry.getValue() == targetContentArea)
                            .findFirst().get().getKey();
                        owner.getChildren().add(newComp);

                        // Coordinates relative to container
                        newComp.getAttributes().put("x", String.valueOf((int)(x - wrapperAbsoluteX(targetContentArea))));
                        newComp.getAttributes().put("y", String.valueOf((int)(y - wrapperAbsoluteY(targetContentArea))));

                        if (isBlock) {
                            newComp.getStyles().put("width", "280");
                            newComp.getStyles().put("height", "200");
                        } else {
                            newComp.getStyles().put("width", "160");
                            newComp.getStyles().put("height", "40");
                        }

                        Node wrapper = renderComponentWrapper(newComp);
                        targetContentArea.getChildren().add(wrapper);
                    } else {
                        // Drop onto canvas root
                        rootComponent.getChildren().add(newComp);
                        newComp.getAttributes().put("x", String.valueOf((int)x));
                        newComp.getAttributes().put("y", String.valueOf((int)y));

                        if (isBlock) {
                            newComp.getStyles().put("width", "280");
                            newComp.getStyles().put("height", "200");
                        } else {
                            newComp.getStyles().put("width", "160");
                            newComp.getStyles().put("height", "40");
                        }

                        Node wrapper = renderComponentWrapper(newComp);
                        canvas.getChildren().add(wrapper);
                    }

                    success = true;
                    clearSelection();
                    // find the wrapper we just created (last child of target or canvas)
                    java.util.Optional<Node> lastWrapper;
                    if (targetContentArea != null) {
                        lastWrapper = targetContentArea.getChildren().stream()
                            .filter(n -> n.getUserData() == newComp).findFirst();
                    } else {
                        lastWrapper = canvas.getChildren().stream()
                            .filter(n -> n.getUserData() == newComp).findFirst();
                    }
                    if (lastWrapper.isPresent()) {
                        addToSelection(lastWrapper.get(), newComp);
                    }
                    statusLabel.setText("Added " + type.getLabel());
                } catch (IllegalArgumentException ignored) { }
            }
            e.setDropCompleted(success);
            e.consume();
        });

        canvas.setOnMousePressed(e -> {
            if (e.isPrimaryButtonDown() && !e.isControlDown()) {
                Node picked = e.getPickResult().getIntersectedNode();
                if (picked == canvas || picked == gridPane || picked == selectionRectPane || picked == guidesPane) {
                    clearSelection();
                    isRubberBanding = true;
                    rubberStartX = e.getX();
                    rubberStartY = e.getY();
                    if (rubberBandRect == null) {
                        rubberBandRect = new Rectangle(0, 0, 0, 0);
                        rubberBandRect.setFill(Color.rgb(0, 120, 215, 0.1));
                        rubberBandRect.setStroke(Color.rgb(0, 120, 215, 0.8));
                        rubberBandRect.setStrokeWidth(1);
                        rubberBandRect.getStrokeDashArray().addAll(5.0, 5.0);
                    }
                    selectionRectPane.getChildren().clear();
                    selectionRectPane.getChildren().add(rubberBandRect);
                }
            }
        });

        canvas.setOnMouseMoved(e -> {
            posLabel.setText("X: " + (int)e.getX() + "  Y: " + (int)e.getY() + "  Z: " + (int)(zoomFactor.get() * 100) + "%");
        });

        canvas.setOnMouseDragged(e -> {
            if (isRubberBanding) {
                double z = zoomFactor.get();
                double x = Math.min(rubberStartX, e.getX()) * z;
                double y = Math.min(rubberStartY, e.getY()) * z;
                double w = Math.abs(e.getX() - rubberStartX) * z;
                double h = Math.abs(e.getY() - rubberStartY) * z;
                rubberBandRect.setX(x);
                rubberBandRect.setY(y);
                rubberBandRect.setWidth(w);
                rubberBandRect.setHeight(h);
            }
        });

        canvas.setOnMouseReleased(e -> {
            if (isRubberBanding) {
                isRubberBanding = false;
                if (rubberBandRect != null && rubberBandRect.getWidth() > 5 && rubberBandRect.getHeight() > 5) {
                    Bounds selectionBounds = rubberBandRect.getBoundsInParent();
                    double z = zoomFactor.get();
                    clearSelection();
                    selectAllRubberBand(canvas.getChildren(), selectionBounds, z);
                    if (!selectedNodes.isEmpty()) {
                        Node first = selectedNodes.iterator().next();
                        selectedNode = first;
                        selectedComponent = (VisualComponent) first.getUserData();
                        selectComponent(selectedComponent, selectedNode);
                        showHandles();
                        updateLayersPanel();
                        buildTree();
                    } else {
                        showEmptyProperties();
                    }
                }
                selectionRectPane.getChildren().clear();
                rubberBandRect = null;
            }
            if (isDragging) {
                finishDrag();
            }
            if (isResizing) {
                isResizing = false;
                canvas.setCursor(Cursor.DEFAULT);
            }
        });
    }

    // ================================================================
    //   KEYBOARD HANDLERS
    // ================================================================

    private boolean ctrlPressed = false;

    private void setupKeyboardHandlers() {
    }

    // ================================================================
    //   MENU ACTIONS
    // ================================================================

    @FXML private void onNewProject() {
        statusLabel.setText("New Project — use File > Open to open a project");
    }

    @FXML private void onOpenProject() {
        // Delegate to the existing project opening mechanism
        statusLabel.setText("Use the main window to open a project");
    }

    @FXML private void onExit() {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.close();
    }

    @FXML private void onSelectAllFromMenu() {
        selectAll();
    }

    @FXML private void onZoomReset() {
        zoomSlider.setValue(100);
    }

    @FXML private void onToggleSnap() {
        snapGridCheck.setSelected(!snapGridCheck.isSelected());
    }

    @FXML private void onToggleGuides() {
        smartGuidesCheck.setSelected(!smartGuidesCheck.isSelected());
    }

    @FXML private void onAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About Eagle Builder");
        alert.setHeaderText("Eagle Visual Builder v1.0");
        alert.setContentText("A drag-and-drop visual HTML builder.\nBuilt with JavaFX.");
        ThemeManager.getInstance().applyTheme(alert.getDialogPane().getScene());
        alert.showAndWait();
    }

    // ================================================================
    //   KEYBOARD
    // ================================================================

    private void onKeyPressed(KeyEvent e) {
        if (e.getCode() == KeyCode.CONTROL) ctrlPressed = true;
        if (e.getCode() == KeyCode.SHIFT) multiSelectMode = true;

        if (ctrlPressed) {
            if (e.getCode() == KeyCode.Z) { onUndo(); e.consume(); }
            if (e.getCode() == KeyCode.Y) { onRedo(); e.consume(); }
            if (e.getCode() == KeyCode.C) { onCopy(); e.consume(); }
            if (e.getCode() == KeyCode.V) { onPaste(); e.consume(); }
            if (e.getCode() == KeyCode.D) { onDuplicate(); e.consume(); }
            if (e.getCode() == KeyCode.A) { selectAll(); e.consume(); }
            if (e.getCode() == KeyCode.S) { e.consume(); }
        }

        if (!ctrlPressed && !selectedNodes.isEmpty()) {
            Node target = e.getTarget() instanceof Node ? (Node) e.getTarget() : null;
            if (target instanceof TextInputControl) return;

            double dx = 0, dy = 0;
            double step = snapEnabled ? GRID_SIZE : 1;
            if (e.getCode() == KeyCode.UP) { dy = -step; e.consume(); }
            if (e.getCode() == KeyCode.DOWN) { dy = step; e.consume(); }
            if (e.getCode() == KeyCode.LEFT) { dx = -step; e.consume(); }
            if (e.getCode() == KeyCode.RIGHT) { dx = step; e.consume(); }
            if (dx != 0 || dy != 0) {
                pushUndoState();
                for (Node n : selectedNodes) {
                    if (n instanceof Pane) {
                        Pane p = (Pane) n;
                        double nx = p.getLayoutX() + dx;
                        double ny = p.getLayoutY() + dy;
                        if (snapEnabled) { nx = snap(nx); ny = snap(ny); }
                        p.setLayoutX(nx);
                        p.setLayoutY(ny);
                        if (n.getUserData() instanceof VisualComponent) {
                            VisualComponent vc = (VisualComponent) n.getUserData();
                            vc.getAttributes().put("x", String.valueOf((int)nx));
                            vc.getAttributes().put("y", String.valueOf((int)ny));
                        }
                    }
                }
                updateLayersPanel();
                buildTree();
            }
            if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) {
                onDeleteSelected(); e.consume();
            }
        }
    }

    private void onKeyReleased(KeyEvent e) {
        if (e.getCode() == KeyCode.CONTROL) ctrlPressed = false;
        if (e.getCode() == KeyCode.SHIFT) multiSelectMode = false;
    }

    // ================================================================
    //   RENDERING: COMPONENT WRAPPER
    // ================================================================

    private Node renderComponentWrapper(VisualComponent comp) {
        Pane wrapper = new Pane();
        wrapper.setUserData(comp);

        String xs = comp.getAttributes().get("x");
        String ys = comp.getAttributes().get("y");
        double x = xs != null ? Double.parseDouble(xs) : 100;
        double y = ys != null ? Double.parseDouble(ys) : 100;
        wrapper.setLayoutX(x);
        wrapper.setLayoutY(y);

        String ws = comp.getStyles().get("width");
        String hs = comp.getStyles().get("height");
        double w = ws != null ? Double.parseDouble(ws) : 160;
        double h = hs != null ? Double.parseDouble(hs) : 40;

        if (comp.isContainer() && comp.getType() != ComponentType.CAROUSEL && comp.getType() != ComponentType.GALLERY) {
            // Container: content area + visual overlay (background/outline)
            Pane contentArea = new Pane();
            contentArea.getStyleClass().add("container-content-area");
            contentArea.setPickOnBounds(true);

            Node visualNode = renderComponent(comp);
            visualNode.setMouseTransparent(true);

            wrapper.getChildren().add(contentArea);
            wrapper.getChildren().add(visualNode);

            containerContentPanes.put(comp, contentArea);
            setupContainerDropTarget(contentArea, comp);

            w = ws != null ? Double.parseDouble(ws) : 300;
            h = hs != null ? Double.parseDouble(hs) : 200;
            wrapper.setPrefSize(w, h);
            wrapper.setMinSize(MIN_COMPONENT_SIZE, MIN_COMPONENT_SIZE);
            contentArea.setPrefSize(w, h);

            // Render existing children into content area
            for (VisualComponent child : comp.getChildren()) {
                Node childWrapper = renderComponentWrapper(child);
                contentArea.getChildren().add(childWrapper);
            }

            nodeToComponent.put(contentArea, comp);
            componentToNode.put(comp, contentArea);
        } else {
            Node contentNode = renderComponent(comp);
            wrapper.getChildren().add(contentNode);
            contentNode.setMouseTransparent(true);
            wrapper.setPickOnBounds(true);

            contentNode.resizeRelocate(0, 0, w, h);
            wrapper.setPrefSize(w, h);
            wrapper.setMinSize(MIN_COMPONENT_SIZE, MIN_COMPONENT_SIZE);

            nodeToComponent.put(contentNode, comp);
            componentToNode.put(comp, contentNode);
        }

        setupWrapperDrag(wrapper, comp);
        setupWrapperClick(wrapper, comp);
        setupWrapperContextMenu(wrapper);

        return wrapper;
    }

  
private void setupWrapperDrag(Pane wrapper, VisualComponent comp) {
    wrapper.setOnMousePressed(e -> {
        if (!e.isPrimaryButtonDown()) return;

        if (!selectedNodes.contains(wrapper) && !e.isControlDown()) {
            clearSelection();
            addToSelection(wrapper, comp);
        }

        dragStartX = e.getScreenX();
        dragStartY = e.getScreenY();
        dragNode = wrapper;
        isDragging = false;

        dragStartPositions.clear();
        
        // Java 8 compatible version
        for (Node n : selectedNodes) {
            if (n instanceof Pane) {
                Pane p = (Pane) n;
                dragStartPositions.put(n, new double[]{p.getLayoutX(), p.getLayoutY()});
            }
        }
        e.consume();
    });

    wrapper.setOnMouseDragged(e -> {
        if (!e.isPrimaryButtonDown() || !selectedNodes.contains(wrapper)) return;

        if (!isDragging) {
            isDragging = true;
            pushUndoState();
            hideSmartGuides();
        }

        double z = zoomFactor.get();
        double dx = (e.getScreenX() - dragStartX) / z;
        double dy = (e.getScreenY() - dragStartY) / z;

        for (Node n : new ArrayList<>(selectedNodes)) {
            double[] orig = dragStartPositions.get(n);
            if (orig != null) {
                double nx = orig[0] + dx;
                double ny = orig[1] + dy;
                
                if (snapEnabled) {
                    nx = snap(nx);
                    ny = snap(ny);
                }
                
                n.setLayoutX(nx);
                n.setLayoutY(ny);

                if (n.getUserData() instanceof VisualComponent) {
                    VisualComponent vc = (VisualComponent) n.getUserData();
                    vc.getAttributes().put("x", String.valueOf((int) nx));
                    vc.getAttributes().put("y", String.valueOf((int) ny));
                }
            }
        }

        if (smartGuidesCheck.isSelected()) {
            showSmartGuides(wrapper);
        }
        repositionHandles();
        e.consume();
    });

    wrapper.setOnMouseReleased(e -> {
        if (isDragging) {
            finishDrag();
        }
    });
}  
    

    private void setupWrapperClick(Pane wrapper, VisualComponent comp) {
        wrapper.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                if (e.isControlDown()) {
                    toggleSelection(wrapper, comp);
                } else if (!selectedNodes.contains(wrapper)) {
                    clearSelection();
                    addToSelection(wrapper, comp);
                }
                if (selectedNodes.size() == 1) {
                    selectComponent(comp, wrapper);
                }
                e.consume();
            }
        });
    }

    private void setupWrapperContextMenu(Pane wrapper) {
        wrapper.setOnContextMenuRequested(e -> {
            if (!selectedNodes.contains(wrapper)) {
                clearSelection();
                VisualComponent vc = (VisualComponent) wrapper.getUserData();
                addToSelection(wrapper, vc);
            }
            contextMenu.show(wrapper, e.getScreenX(), e.getScreenY());
            e.consume();
        });
    }

    // ================================================================
    //   SELECTION
    // ================================================================

    private void clearSelection() {
        clearChildSelection();
        for (Node n : selectedNodes) {
            removeSelectionHighlight(n);
        }
        selectedNodes.clear();
        multiSelectedNodes.clear();
        selectedComponent = null;
        selectedNode = null;
        removeHandles();
        hideSmartGuides();
        showEmptyProperties();
    }

    private void addToSelection(Node wrapper, VisualComponent comp) {
        selectedNodes.add(wrapper);
        if (selectedNodes.size() == 1) {
            selectedComponent = comp;
            selectedNode = wrapper;
            selectComponent(comp, wrapper);
        }
        addSelectionHighlight(wrapper);
        showHandles();
        updateLayersPanel();
        buildTree();
    }

    private void toggleSelection(Node wrapper, VisualComponent comp) {
        if (selectedNodes.contains(wrapper)) {
            selectedNodes.remove(wrapper);
            removeSelectionHighlight(wrapper);
            if (selectedNode == wrapper) {
                if (!selectedNodes.isEmpty()) {
                    Node first = selectedNodes.iterator().next();
                    selectedNode = first;
                    selectedComponent = (VisualComponent) first.getUserData();
                    selectComponent(selectedComponent, selectedNode);
                } else {
                    selectedComponent = null;
                    selectedNode = null;
                    showEmptyProperties();
                }
            }
        } else {
            addToSelection(wrapper, comp);
        }
        showHandles();
        updateLayersPanel();
        buildTree();
    }

    private void selectAll() {
        clearSelection();
        selectAllRecursive(canvas.getChildren());
        if (!selectedNodes.isEmpty()) {
            Node first = selectedNodes.iterator().next();
            selectedComponent = (VisualComponent) first.getUserData();
            selectedNode = first;
            selectComponent(selectedComponent, selectedNode);
            showHandles();
            updateLayersPanel();
            buildTree();
        } else {
            showEmptyProperties();
        }
    }

    private void selectAllRecursive(java.util.List<Node> children) {
        for (Node child : children) {
            if (child.getUserData() instanceof VisualComponent) {
                selectedNodes.add(child);
                addSelectionHighlight(child);
            }
            if (child instanceof Pane) {
                selectAllRecursive(((Pane) child).getChildren());
            }
        }
    }

    private void addSelectionHighlight(Node wrapper) {
        wrapper.getStyleClass().add("selection-highlight");
    }

    private void removeSelectionHighlight(Node wrapper) {
        wrapper.getStyleClass().remove("selection-highlight");
    }

    private void removeHandles() {
        canvas.getChildren().removeAll(handleNodes);
        handleNodes.clear();
    }

    private void showHandles() {
        removeHandles();
        for (Node n : selectedNodes) {
            if (n instanceof Pane) {
                Pane p = (Pane) n;
                VisualComponent vc = (VisualComponent) p.getUserData();
                double w, h;
                if (vc != null && vc.getStyles().containsKey("width")) {
                    w = Double.parseDouble(vc.getStyles().get("width"));
                } else {
                    w = p.getWidth() > 0 ? p.getWidth() : (p.getPrefWidth() > 0 ? p.getPrefWidth() : 160);
                }
                if (vc != null && vc.getStyles().containsKey("height")) {
                    h = Double.parseDouble(vc.getStyles().get("height"));
                } else {
                    h = p.getHeight() > 0 ? p.getHeight() : (p.getPrefHeight() > 0 ? p.getPrefHeight() : 40);
                }
                if (w <= 0) w = 160;
                if (h <= 0) h = 40;

                // Use absolute position accounting for container nesting
                double bx = wrapperAbsoluteX(n);
                double by = wrapperAbsoluteY(n);

                addHandle(bx, by, ResizeDir.NW, p);           // top-left
                addHandle(bx + w/2, by, ResizeDir.N, p);       // top-center
                addHandle(bx + w, by, ResizeDir.NE, p);        // top-right
                addHandle(bx, by + h/2, ResizeDir.W, p);       // middle-left
                addHandle(bx + w, by + h/2, ResizeDir.E, p);   // middle-right
                addHandle(bx, by + h, ResizeDir.SW, p);        // bottom-left
                addHandle(bx + w/2, by + h, ResizeDir.S, p);   // bottom-center
                addHandle(bx + w, by + h, ResizeDir.SE, p);    // bottom-right
            }
        }
    }

    private void repositionHandles() {
        // Just move existing handles in-place without removing them (safe during drag)
        int idx = 0;
        for (Node n : selectedNodes) {
            if (n instanceof Pane && idx + 7 <= handleNodes.size()) {
                Pane p = (Pane) n;
                VisualComponent vc = (VisualComponent) p.getUserData();
                double w, h;
                if (vc != null && vc.getStyles().containsKey("width")) {
                    w = Double.parseDouble(vc.getStyles().get("width"));
                } else {
                    w = p.getWidth() > 0 ? p.getWidth() : (p.getPrefWidth() > 0 ? p.getPrefWidth() : 160);
                }
                if (vc != null && vc.getStyles().containsKey("height")) {
                    h = Double.parseDouble(vc.getStyles().get("height"));
                } else {
                    h = p.getHeight() > 0 ? p.getHeight() : (p.getPrefHeight() > 0 ? p.getPrefHeight() : 40);
                }
                if (w <= 0) w = 160;
                if (h <= 0) h = 40;

                double bx = wrapperAbsoluteX(n);
                double by = wrapperAbsoluteY(n);

                double[][] pos = {
                    {bx, by},
                    {bx + w/2, by},
                    {bx + w, by},
                    {bx, by + h/2},
                    {bx + w, by + h/2},
                    {bx, by + h},
                    {bx + w/2, by + h},
                    {bx + w, by + h}
                };
                for (int i = 0; i < 8; i++, idx++) {
                    handleNodes.get(idx).setCenterX(pos[i][0]);
                    handleNodes.get(idx).setCenterY(pos[i][1]);
                }
            }
        }
    }

    private void addHandle(double cx, double cy, ResizeDir dir, Pane target) {
        Circle handle = new Circle(cx, cy, HANDLE_SIZE / 2.0);
        handle.getStyleClass().add("selection-handle");

        switch (dir) {
            case NW: case SE: handle.setCursor(Cursor.NW_RESIZE); break;
            case NE: case SW: handle.setCursor(Cursor.NE_RESIZE); break;
            case N: case S: handle.setCursor(Cursor.V_RESIZE); break;
            case W: case E: handle.setCursor(Cursor.H_RESIZE); break;
        }

        handle.setOnMousePressed(e -> {
            currentResizeDir = dir;
            isResizing = true;
            resizeStartX = e.getScreenX();
            resizeStartY = e.getScreenY();
            resizeOrigX = target.getLayoutX();
            resizeOrigY = target.getLayoutY();
            resizeOrigW = target.getWidth() > 0 ? target.getWidth() : target.getPrefWidth();
            resizeOrigH = target.getHeight() > 0 ? target.getHeight() : target.getPrefHeight();
            if (resizeOrigW <= 0) resizeOrigW = 160;
            if (resizeOrigH <= 0) resizeOrigH = 40;
            pushUndoState();
            e.consume();
        });

        handle.setOnMouseDragged(e -> {
            if (!isResizing || currentResizeDir == null) return;
            double z = zoomFactor.get();
            double dx = (e.getScreenX() - resizeStartX) / z;
            double dy = (e.getScreenY() - resizeStartY) / z;
            double newX = resizeOrigX, newY = resizeOrigY;
            double newW = resizeOrigW, newH = resizeOrigH;

            switch (currentResizeDir) {
                case NW: newX = resizeOrigX + dx; newY = resizeOrigY + dy; newW = resizeOrigW - dx; newH = resizeOrigH - dy; break;
                case N: newY = resizeOrigY + dy; newH = resizeOrigH - dy; break;
                case NE: newY = resizeOrigY + dy; newW = resizeOrigW + dx; newH = resizeOrigH - dy; break;
                case W: newX = resizeOrigX + dx; newW = resizeOrigW - dx; break;
                case E: newW = resizeOrigW + dx; break;
                case SW: newX = resizeOrigX + dx; newW = resizeOrigW - dx; newH = resizeOrigH + dy; break;
                case S: newH = resizeOrigH + dy; break;
                case SE: newW = resizeOrigW + dx; newH = resizeOrigH + dy; break;
            }

            if (newW < MIN_COMPONENT_SIZE) { newW = MIN_COMPONENT_SIZE; }
            if (newH < MIN_COMPONENT_SIZE) { newH = MIN_COMPONENT_SIZE; }

            if (snapEnabled) {
                newW = snap(newW);
                newH = snap(newH);
            }

            target.setLayoutX(newX);
            target.setLayoutY(newY);
            target.setPrefSize(newW, newH);
            target.resize(newW, newH);

            Node content = target.getChildren().isEmpty() ? null : target.getChildren().get(0);
            if (content != null) {
                content.resizeRelocate(0, 0, newW, newH);
            }

            if (target.getUserData() instanceof VisualComponent) {
                VisualComponent vc = (VisualComponent) target.getUserData();
                vc.getAttributes().put("x", String.valueOf((int)newX));
                vc.getAttributes().put("y", String.valueOf((int)newY));
                vc.getStyles().put("width", String.valueOf((int)newW));
                vc.getStyles().put("height", String.valueOf((int)newH));
            }

            // Don't call showHandles() during drag (breaks the event chain)
            // Instead, just reposition existing handles in-place
            repositionHandles();
            e.consume();
        });

        handle.setOnMouseReleased(e -> {
            if (isResizing) {
                isResizing = false;
                currentResizeDir = null;
                canvas.setCursor(Cursor.DEFAULT);
                e.consume();
            }
        });

        handleNodes.add(handle);
        canvas.getChildren().add(handle);
    }

    // ================================================================
    //   RENDER COMPONENT
    // ================================================================

    private Node renderComponent(VisualComponent comp) {
        Node node;
        String ws = comp.getStyles().get("width");
        String hs = comp.getStyles().get("height");
        double w = ws != null ? Double.parseDouble(ws) : 160;
        double h = hs != null ? Double.parseDouble(hs) : 40;

        switch (comp.getType()) {
            case HEADING: {
                Label l = new Label(comp.getText());
                l.setStyle(buildInlineStyle(comp));
                l.setFont(Font.font("System", FontWeight.BOLD, 24));
                node = l;
                break;
            }
            case SUBHEADING: {
                Label l = new Label(comp.getText());
                l.setStyle(buildInlineStyle(comp));
                l.setFont(Font.font("System", FontWeight.SEMI_BOLD, 20));
                node = l;
                break;
            }
            case PARAGRAPH:
            case RICH_TEXT: {
                Label l = new Label(comp.getText());
                l.setWrapText(true);
                l.setStyle(buildInlineStyle(comp));
                node = l;
                break;
            }
            case QUOTE: {
                Label l = new Label(comp.getText());
                l.setWrapText(true);
                l.setStyle(buildInlineStyle(comp) + "-fx-font-style: italic; -fx-padding: 12 20; -fx-border-color: #6c5ce7; -fx-border-width: 0 0 0 4;");
                node = l;
                break;
            }
            case BUTTON:
            case ICON_BUTTON: {
                String icon = comp.getIcon() != null ? comp.getIcon() : "";
                String text = comp.getText() != null ? comp.getText() : "";
                String display = icon + (icon.isEmpty() || text.isEmpty() ? "" : " ") + text;
                if (display.isEmpty()) display = comp.getType() == ComponentType.ICON_BUTTON ? "🔘" : "Button";
                Button b = new Button(display);
                b.setStyle(buildInlineStyle(comp));
                node = b;
                break;
            }
            case LINK: {
                Hyperlink hLink = new Hyperlink(comp.getText());
                hLink.setStyle(buildInlineStyle(comp));
                node = hLink;
                break;
            }
            case INPUT: {
                TextField tf = new TextField();
                tf.setPromptText(comp.getAttributes().getOrDefault("placeholder", "Enter text..."));
                tf.setStyle(buildInlineStyle(comp));
                node = tf;
                break;
            }
            case TEXTAREA: {
                TextArea ta = new TextArea();
                ta.setPromptText(comp.getAttributes().getOrDefault("placeholder", "Write here..."));
                ta.setPrefRowCount(4);
                ta.setStyle(buildInlineStyle(comp));
                node = ta;
                break;
            }
            case SELECT:
            case MULTISELECT: {
                ComboBox<String> combo = new ComboBox<>();
                combo.getItems().addAll("Option 1", "Option 2", "Option 3");
                combo.setPromptText("Select...");
                combo.setStyle(buildInlineStyle(comp));
                node = combo;
                break;
            }
            case CHECKBOX: {
                CheckBox cb = new CheckBox(comp.getText() != null ? comp.getText() : "Checkbox");
                cb.setStyle(buildInlineStyle(comp));
                node = cb;
                break;
            }
            case RADIO: {
                RadioButton rb = new RadioButton(comp.getText() != null ? comp.getText() : "Radio");
                rb.setStyle(buildInlineStyle(comp));
                node = rb;
                break;
            }
            case TOGGLE: {
                CheckBox toggle = new CheckBox(comp.getText() != null ? comp.getText() : "Toggle");
                toggle.setStyle(buildInlineStyle(comp) + "-fx-padding: 8;");
                node = toggle;
                break;
            }
            case DATE_PICKER: {
                DatePicker dp = new DatePicker();
                dp.setStyle(buildInlineStyle(comp));
                node = dp;
                break;
            }
            case FILE_UPLOAD: {
                Button uploadBtn = new Button("Upload File");
                uploadBtn.setStyle(buildInlineStyle(comp));
                node = uploadBtn;
                break;
            }
            case SLIDER: {
                Slider slider = new Slider(0, 100, 50);
                slider.setStyle(buildInlineStyle(comp));
                slider.setShowTickLabels(true);
                node = slider;
                break;
            }
            case RATING: {
                HBox ratingBox = new HBox(4);
                ratingBox.setAlignment(Pos.CENTER);
                ratingBox.getChildren().addAll(
                    new Label("star"), new Label("star"), new Label("star"), new Label("star"), new Label("star")
                );
                ratingBox.setStyle(buildInlineStyle(comp));
                node = ratingBox;
                break;
            }
            case IMAGE: {
                String src = comp.getAttributes().getOrDefault("src", "");
                String alt = comp.getAttributes().getOrDefault("alt", "Image");
                Label imgLabel = new Label(src.isEmpty() ? "🖼️ " + alt : "🖼️ " + alt);
                VBox placeholder = new VBox(imgLabel);
                placeholder.setAlignment(Pos.CENTER);
                placeholder.setMinHeight(120);
                placeholder.setStyle(buildInlineStyle(comp) + "-fx-background-color: #f1f5f9; -fx-border-color: #e5e7eb; -fx-border-style: dashed;");
                node = placeholder;
                break;
            }
            case VIDEO: {
                String src = comp.getAttributes().getOrDefault("src", "");
                String labelText = src.isEmpty() ? "🎬 Video Player" : "🎬 " + src.substring(Math.max(0, src.lastIndexOf("/") + 1));
                VBox placeholder = new VBox(new Label(labelText));
                placeholder.setAlignment(Pos.CENTER);
                placeholder.setMinHeight(160);
                placeholder.setStyle(buildInlineStyle(comp) + "-fx-background-color: #1f2937; -fx-text-fill: white;");
                node = placeholder;
                break;
            }
            case AUDIO: {
                String src = comp.getAttributes().getOrDefault("src", "");
                String labelText = src.isEmpty() ? "🎵 Audio Player" : "🎵 " + src.substring(Math.max(0, src.lastIndexOf("/") + 1));
                VBox placeholder = new VBox(new Label(labelText));
                placeholder.setAlignment(Pos.CENTER);
                placeholder.setMinHeight(60);
                placeholder.setStyle(buildInlineStyle(comp));
                node = placeholder;
                break;
            }
            case CAROUSEL:
            case GALLERY: {
                HBox carousel = new HBox(8);
                carousel.setStyle(buildInlineStyle(comp) + "-fx-padding: 8;");
                node = carousel;
                break;
            }
            case CONTAINER:
            case ROW:
            case SECTION:
            case CARD:
            case HERO:
            case FEATURE_SECTION:
            case FORM:
            case TESTIMONIALS:
            case PRICING:
            case FAQ:
            case STATS:
            case HEADER:
            case FOOTER:
            case ASIDE:
            case SIDEBAR:
            case FLEXBOX:
            case STACK:
            case WRAP: {
                javafx.scene.layout.StackPane stack = new javafx.scene.layout.StackPane();
                stack.setStyle("-fx-background-radius: 8; -fx-border-radius: 8;");

                Pane visual = new Pane();
                visual.setStyle(buildInlineStyle(comp) + "-fx-border-color: derive(-text-muted, 50%); -fx-border-width: 1.5; -fx-border-style: dashed; -fx-background-color: rgba(200,200,255,0.04); -fx-background-radius: 8; -fx-border-radius: 8;");
                visual.setMouseTransparent(true);

                Pane contentArea = new Pane();
                contentArea.setStyle("-fx-background-color: transparent; -fx-background-radius: 8;");
                contentArea.setPickOnBounds(true);

                stack.getChildren().addAll(contentArea, visual);
                containerContentPanes.put(comp, contentArea);
                setupContainerDropTarget(contentArea, comp);

                node = stack;
                break;
            }
            case GRID: {
                GridPane grid = new GridPane();
                grid.setHgap(8);
                grid.setVgap(8);
                grid.setStyle(buildInlineStyle(comp) + "-fx-border-color: #ccc; -fx-border-width: 1; -fx-border-style: dashed;");
                setupContainerDropTarget(grid, comp);
                node = grid;
                break;
            }
            case DIVIDER: {
                Separator s = new Separator();
                s.setStyle(buildInlineStyle(comp));
                node = s;
                break;
            }
            case SPACER: {
                Region r = new Region();
                r.setPrefHeight(parsePx(comp.getStyles().getOrDefault("height", "30px")));
                r.setStyle(buildInlineStyle(comp));
                node = r;
                break;
            }
            case NAVIGATION:
            case TABS:
            case BREADCRUMB:
            case DROPDOWN: {
                HBox nav = new HBox(8);
                nav.getChildren().add(new Label("Nav Item 1  Nav Item 2  Nav Item 3"));
                nav.setStyle(buildInlineStyle(comp));
                node = nav;
                break;
            }
            case PAGINATION:
            case STEPPER: {
                HBox box = new HBox(8);
                box.getChildren().add(new Label("1  2  3 ..."));
                box.setStyle(buildInlineStyle(comp));
                node = box;
                break;
            }
            case TABLE:
            case DATA_TABLE: {
                TableView<String> table = new TableView<>();
                table.setPlaceholder(new Label("Table Content"));
                table.setStyle(buildInlineStyle(comp));
                node = table;
                break;
            }
            case LIST: {
                VBox list = new VBox(4);
                list.setStyle(buildInlineStyle(comp));
                node = list;
                break;
            }
            case ACCORDION: {
                TitledPane titled = new TitledPane(comp.getText(), new Label("Content..."));
                titled.setStyle(buildInlineStyle(comp));
                node = titled;
                break;
            }
            case MODAL:
            case DRAWER:
            case POPOVER: {
                VBox modal = new VBox(8);
                modal.setStyle(buildInlineStyle(comp) + "-fx-background-color: white; -fx-border-color: #d1d5db; -fx-padding: 20;");
                modal.getChildren().add(new Label("Modal/Dialog Content"));
                node = modal;
                break;
            }
            case ALERT:
            case TOAST: {
                Label alert = new Label(comp.getText() != null ? comp.getText() : "Alert Message");
                alert.setStyle(buildInlineStyle(comp) + "-fx-background-color: #fef3c7; -fx-padding: 12 16; -fx-border-radius: 6;");
                node = alert;
                break;
            }
            case PROGRESS: {
                ProgressBar pb = new ProgressBar(0.65);
                pb.setPrefWidth(w);
                pb.setStyle(buildInlineStyle(comp));
                node = pb;
                break;
            }
            case SPINNER: {
                ProgressIndicator spinner = new ProgressIndicator();
                spinner.setStyle(buildInlineStyle(comp));
                node = spinner;
                break;
            }
            case TOOLTIP: {
                Label tooltip = new Label("Hover me");
                tooltip.setStyle(buildInlineStyle(comp));
                node = tooltip;
                break;
            }
            case BADGE:
            case CHIP: {
                Label badge = new Label(comp.getText() != null ? comp.getText() : "Badge");
                badge.setStyle(buildInlineStyle(comp) + "-fx-background-radius: 9999; -fx-padding: 4 12;");
                node = badge;
                break;
            }
            case AVATAR: {
                Circle avatar = new Circle(32);
                avatar.setStyle(buildInlineStyle(comp) + "-fx-fill: #e5e7eb;");
                node = avatar;
                break;
            }
            case ICON: {
                String iconChar = comp.getIcon() != null ? comp.getIcon() : "⭐";
                String text = comp.getText() != null ? comp.getText() : "";
                String display = iconChar + (text.isEmpty() ? "" : " " + text);
                Label icon = new Label(display);
                icon.setStyle(buildInlineStyle(comp) + "-fx-font-size: 28px;");
                node = icon;
                break;
            }
            case MAP:
            case CHART:
            case EMBED:
            case QR_CODE:
            case CALENDAR: {
                VBox placeholder = new VBox(new Label("[" + comp.getType().getLabel() + "]"));
                placeholder.setAlignment(Pos.CENTER);
                placeholder.setMinHeight(180);
                placeholder.setStyle(buildInlineStyle(comp) + "-fx-background-color: #f3f4f6; -fx-border-style: dashed;");
                node = placeholder;
                break;
            }
            case HEADER_BLOCK:
            case HERO_BLOCK:
            case FEATURES_BLOCK:
            case PRICING_BLOCK:
            case TESTIMONIALS_BLOCK:
            case FAQ_BLOCK:
            case CONTACT_BLOCK:
            case FOOTER_BLOCK:
            case TEAM_BLOCK:
            case STATS_BLOCK:
            case CTA_BLOCK:
            case NEWSLETTER_BLOCK: {
                VBox bp = new VBox(4);
                bp.setStyle(buildInlineStyle(comp));
                bp.setAlignment(Pos.TOP_CENTER);
                bp.setFillWidth(true);
                // عرض محتوى البلوك: عرض كل طفل بشكل بسيط
                for (VisualComponent child : comp.getChildren()) {
                    Node childNode = createBlockChildPreview(child);
                    if (childNode != null) {
                        bp.getChildren().add(childNode);
                    }
                }
                if (bp.getChildren().isEmpty()) {
                    Label emptyLbl = new Label(comp.getType().getLabel());
                    emptyLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: rgba(0,0,0,0.5);");
                    bp.getChildren().add(emptyLbl);
                }
                node = bp;
                break;
            }
            default: {
                Label unknown = new Label("[" + comp.getType().getLabel() + "]");
                unknown.setStyle(buildInlineStyle(comp) + "-fx-border-color: red; -fx-border-style: dashed;");
                node = unknown;
                break;
            }
        }

        nodeToComponent.put(node, comp);
        return node;
    }

    private Node createBlockChildPreview(VisualComponent child) {
        String text = child.getText();
        if (text == null || text.isEmpty()) text = "";
        if (text.length() > 40) text = text.substring(0, 37) + "...";
        String display = child.getType().getLabel() + ": " + text;
        Label label = new Label(display);
        javafx.scene.Node icon = child.getType().getIconView(14);
        String bg;
        switch (child.getType()) {
            case HEADING: case SUBHEADING: case PARAGRAPH:
            case RICH_TEXT: case QUOTE:
                bg = "-fx-background-color: rgba(99,102,241,0.08);"; break;
            case BUTTON: case ICON_BUTTON: case LINK:
                bg = "-fx-background-color: rgba(34,197,94,0.08);"; break;
            case INPUT: case TEXTAREA: case SELECT: case CHECKBOX:
            case RADIO: case TOGGLE: case DATE_PICKER: case FILE_UPLOAD:
            case SLIDER:
                bg = "-fx-background-color: rgba(234,179,8,0.08);"; break;
            case IMAGE: case VIDEO: case AUDIO:
                bg = "-fx-background-color: rgba(239,68,68,0.08);"; break;
            case DIVIDER: case SPACER:
                bg = "-fx-background-color: rgba(107,114,128,0.08);"; break;
            default:
                bg = "-fx-background-color: rgba(107,114,128,0.05);";
        }
        label.setStyle(bg + "-fx-padding: 2 6; -fx-font-size: 10px; -fx-text-fill: rgba(0,0,0,0.7); -fx-max-width: Infinity; -fx-background-radius: 3;");
        Tooltip tt = new Tooltip(child.getType().getLabel() + "\n" + child.getText());
        Tooltip.install(label, tt);
        label.setUserData(child);
        label.setOnMouseClicked(e -> {
            selectChildComponent(child, label);
            e.consume();
        });
        if (icon != null) {
            javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(4, icon, label);
            box.setUserData(child);
            box.setOnMouseClicked(label.getOnMouseClicked());
            Tooltip.install(box, tt);
            return box;
        }
        return label;
    }

    private void selectChildComponent(VisualComponent child, Label label) {
        clearChildSelection();
        this.selectedChildComponent = child;
        label.setStyle(label.getStyle() + "-fx-border-color: #3b82f6; -fx-border-width: 2; -fx-border-radius: 3;");
        buildPropertiesPanel(child, label);
    }

    private void clearChildSelection() {
        if (selectedChildComponent != null) {
            if (selectedNode != null && selectedNode instanceof Pane) {
                for (Node n : ((Pane) selectedNode).getChildren()) {
                    if (n instanceof Label && n.getUserData() == selectedChildComponent) {
                        String s = n.getStyle();
                        s = s.replace("-fx-border-color: #3b82f6;", "")
                             .replace("-fx-border-width: 2;", "")
                             .replace("-fx-border-radius: 3;", "");
                        n.setStyle(s);
                        break;
                    }
                }
            }
            selectedChildComponent = null;
        }
    }

    // ================================================================
    //   CONTAINER DROP TARGET
    // ================================================================

    private void setupContainerDropTarget(Pane contentArea, VisualComponent owner) {
        contentArea.setOnDragOver(e -> {
            if (e.getGestureSource() != contentArea && e.getDragboard().hasString()) {
                e.acceptTransferModes(TransferMode.COPY);
            }
            e.consume();
        });

        contentArea.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                try {
                    ComponentType type = ComponentType.valueOf(db.getString());
                    pushUndoState();
                    VisualComponent newComp = new VisualComponent(type);
                    owner.getChildren().add(newComp);

                    // Make coordinates relative to this container
                    double relX = e.getX() - 50;
                    double relY = e.getY() - 20;
                    if (relX < 0) relX = 10;
                    if (relY < 0) relY = 10;
                    newComp.getAttributes().put("x", String.valueOf((int)relX));
                    newComp.getAttributes().put("y", String.valueOf((int)relY));

                    boolean isBlock = type == ComponentType.HEADER_BLOCK || type == ComponentType.HERO_BLOCK ||
                        type == ComponentType.FEATURES_BLOCK || type == ComponentType.PRICING_BLOCK ||
                        type == ComponentType.TESTIMONIALS_BLOCK || type == ComponentType.FAQ_BLOCK ||
                        type == ComponentType.CONTACT_BLOCK || type == ComponentType.FOOTER_BLOCK ||
                        type == ComponentType.TEAM_BLOCK || type == ComponentType.STATS_BLOCK ||
                        type == ComponentType.CTA_BLOCK || type == ComponentType.NEWSLETTER_BLOCK;
                    if (isBlock) {
                        newComp.getStyles().put("width", "280");
                        newComp.getStyles().put("height", "200");
                    } else {
                        newComp.getStyles().put("width", "160");
                        newComp.getStyles().put("height", "40");
                    }

                    // Add wrapper to the container's content area (not canvas)
                    Node wrapper = renderComponentWrapper(newComp);
                    contentArea.getChildren().add(wrapper);

                    success = true;
                    clearSelection();
                    addToSelection(wrapper, newComp);
                    statusLabel.setText("Added " + type.getLabel() + " to container");
                } catch (IllegalArgumentException ignored) { }
            }
            e.setDropCompleted(success);
            e.consume();
        });
    }

    // ================================================================
    //   CONTEXT MENU
    // ================================================================

    private void buildContextMenu() {
        contextMenu = new ContextMenu();

        MenuItem copyItem = new MenuItem("Copy");
        copyItem.setOnAction(e -> onCopy());
        
        MenuItem groupItem = new MenuItem("Group");
        groupItem.setOnAction(e -> groupSelected());

        MenuItem ungroupItem = new MenuItem("Ungroup");
        ungroupItem.setOnAction(e -> ungroupSelected());
        
        
        MenuItem pasteItem = new MenuItem("Paste");
        pasteItem.setOnAction(e -> onPaste());
        MenuItem duplicateItem = new MenuItem("Duplicate");
        duplicateItem.setOnAction(e -> onDuplicate());
        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> onDeleteSelected());
        SeparatorMenuItem sep1 = new SeparatorMenuItem();
        MenuItem bringFrontItem = new MenuItem("Bring to Front");
        bringFrontItem.setOnAction(e -> onBringToFront());
        MenuItem sendBackItem = new MenuItem("Send to Back");
        sendBackItem.setOnAction(e -> onSendToBack());
        SeparatorMenuItem sep2 = new SeparatorMenuItem();
        Menu alignMenu = new Menu("Align");
        MenuItem alignLeftItem = new MenuItem("Align Left");
        alignLeftItem.setOnAction(e -> onAlignLeft());
        MenuItem alignCenterItem = new MenuItem("Align Center H");
        alignCenterItem.setOnAction(e -> onAlignCenter());
        MenuItem alignRightItem = new MenuItem("Align Right");
        alignRightItem.setOnAction(e -> onAlignRight());
        MenuItem alignTopItem = new MenuItem("Align Top");
        alignTopItem.setOnAction(e -> onAlignTop());
        MenuItem alignMiddleItem = new MenuItem("Align Middle V");
        alignMiddleItem.setOnAction(e -> onAlignMiddle());
        MenuItem alignBottomItem = new MenuItem("Align Bottom");
        alignBottomItem.setOnAction(e -> onAlignBottom());
        alignMenu.getItems().addAll(alignLeftItem, alignCenterItem, alignRightItem, new SeparatorMenuItem(), alignTopItem, alignMiddleItem, alignBottomItem);

        contextMenu.getItems().addAll(groupItem,ungroupItem,copyItem, pasteItem, duplicateItem, deleteItem, sep1, bringFrontItem, sendBackItem, sep2, alignMenu);
    }

    // ================================================================
    //   CONTEXT MENU ACTIONS
    // ================================================================

    
@FXML
private void groupSelected() {
    if (selectedNodes.size() < 2) {
        statusLabel.setText("اختر عنصرين أو أكثر للتجميع");
        return;
    }

    pushUndoState();

    List<Node> selected = new ArrayList<>(selectedNodes);
    double minX = selected.stream().mapToDouble(Node::getLayoutX).min().orElse(0);
    double minY = selected.stream().mapToDouble(Node::getLayoutY).min().orElse(0);

    VisualComponent group = new VisualComponent(ComponentType.CONTAINER);
    group.getStyles().put("border", "1");
    group.getStyles().put("border-radius", "8");
    group.getStyles().put("background-color", "rgba(0,120,215,0.08)");

    group.getAttributes().put("x", String.valueOf((int)minX));
    group.getAttributes().put("y", String.valueOf((int)minY));

    for (Node n : selected) {
        VisualComponent vc = (VisualComponent) n.getUserData();
        removeFromTree(rootComponent, vc);   // إزالة من المكان القديم

        double relX = n.getLayoutX() - minX;
        double relY = n.getLayoutY() - minY;

        vc.getAttributes().put("x", String.valueOf((int)relX));
        vc.getAttributes().put("y", String.valueOf((int)relY));

        group.getChildren().add(vc);
        canvas.getChildren().remove(n);
    }

    rootComponent.getChildren().add(group);
    Node groupWrapper = renderComponentWrapper(group);
    canvas.getChildren().add(groupWrapper);

    clearSelection();
    addToSelection(groupWrapper, group);

    statusLabel.setText("تم تجميع " + selected.size() + " عنصر");
    updateLayersPanel();
    buildTree();
}   
    
    
    
    
   @FXML
private void ungroupSelected() {
    if (selectedNodes.size() != 1) {
        statusLabel.setText("اختر مجموعة واحدة فقط لإلغاء التجميع");
        return;
    }

    Node wrapper = selectedNodes.iterator().next();
    
    // Java 8 compatible
    if (!(wrapper.getUserData() instanceof VisualComponent)) {
        return;
    }
    
    VisualComponent groupComp = (VisualComponent) wrapper.getUserData();

    if (groupComp.getType() != ComponentType.CONTAINER || 
        groupComp.getChildren().isEmpty()) {
        statusLabel.setText("هذا العنصر ليس مجموعة");
        return;
    }

    pushUndoState();

    VisualComponent parent = findParent(rootComponent, groupComp);
    if (parent == null) parent = rootComponent;

    double groupX = groupComp.getAttributes().containsKey("x") ? 
                    Double.parseDouble(groupComp.getAttributes().get("x")) : 0;
    double groupY = groupComp.getAttributes().containsKey("y") ? 
                    Double.parseDouble(groupComp.getAttributes().get("y")) : 0;

    List<VisualComponent> childrenToUngroup = new ArrayList<>(groupComp.getChildren());

    clearSelection();

    for (VisualComponent child : childrenToUngroup) {
        double childX = child.getAttributes().containsKey("x") ? 
                        Double.parseDouble(child.getAttributes().get("x")) : 0;
        double childY = child.getAttributes().containsKey("y") ? 
                        Double.parseDouble(child.getAttributes().get("y")) : 0;

        child.getAttributes().put("x", String.valueOf((int)(groupX + childX)));
        child.getAttributes().put("y", String.valueOf((int)(groupY + childY)));

        parent.getChildren().add(child);

        Node childWrapper = renderComponentWrapper(child);
        canvas.getChildren().add(childWrapper);
    }

    // حذف المجموعة
    removeFromTree(rootComponent, groupComp);
    canvas.getChildren().remove(wrapper);
    nodeToComponent.values().remove(groupComp);
    componentToNode.remove(groupComp);

    statusLabel.setText("تم إلغاء تجميع " + childrenToUngroup.size() + " عنصر");
    updateLayersPanel();
    buildTree();
}


private VisualComponent findParent(VisualComponent parent, VisualComponent target) {
    for (VisualComponent child : parent.getChildren()) {
        if (child == target) {
            return parent;
        }
        VisualComponent found = findParent(child, target);
        if (found != null) {
            return found;
        }
    }
    return null;
}
    
    
    
    @FXML private void onBringToFront() {
        if (selectedNodes.isEmpty()) return;
        pushUndoState();
        for (Node n : selectedNodes) {
            n.toFront();
        }
        showHandles();
        updateLayersPanel();
        buildTree();
        statusLabel.setText("Brought to front");
    }

    @FXML private void onSendToBack() {
        if (selectedNodes.isEmpty()) return;
        pushUndoState();
        List<Node> reversed = new ArrayList<>(selectedNodes);
        Collections.reverse(reversed);
        for (Node n : reversed) {
            n.toBack();
        }
        showHandles();
        updateLayersPanel();
        buildTree();
        statusLabel.setText("Sent to back");
    }

    // ================================================================
    //   ALIGNMENT
    // ================================================================

    @FXML private void onAlignLeft() {
        if (selectedNodes.size() < 2) return;
        pushUndoState();
        double minX = Double.MAX_VALUE;
        for (Node n : selectedNodes) minX = Math.min(minX, n.getLayoutX());
        for (Node n : selectedNodes) {
            n.setLayoutX(minX);
            if (n.getUserData() instanceof VisualComponent) {
                VisualComponent vc = (VisualComponent) n.getUserData();
                vc.getAttributes().put("x", String.valueOf((int)minX));
            }
        }
        showHandles();
        statusLabel.setText("Aligned left");
    }

    @FXML private void onAlignCenter() {
        if (selectedNodes.size() < 2) return;
        pushUndoState();
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        for (Node n : selectedNodes) {
            minX = Math.min(minX, n.getLayoutX());
            maxX = Math.max(maxX, n.getLayoutX() + n.getBoundsInParent().getWidth());
        }
        double center = (minX + maxX) / 2;
        for (Node n : selectedNodes) {
            double w = n.getBoundsInParent().getWidth();
            n.setLayoutX(center - w / 2);
            if (n.getUserData() instanceof VisualComponent) {
                VisualComponent vc = (VisualComponent) n.getUserData();
                vc.getAttributes().put("x", String.valueOf((int)(center - w / 2)));
            }
        }
        showHandles();
        statusLabel.setText("Aligned center");
    }

    @FXML private void onAlignRight() {
        if (selectedNodes.size() < 2) return;
        pushUndoState();
        double maxX = -Double.MAX_VALUE;
        for (Node n : selectedNodes) {
            double w = n.getBoundsInParent().getWidth();
            maxX = Math.max(maxX, n.getLayoutX() + w);
        }
        for (Node n : selectedNodes) {
            double w = n.getBoundsInParent().getWidth();
            n.setLayoutX(maxX - w);
            if (n.getUserData() instanceof VisualComponent) {
                VisualComponent vc = (VisualComponent) n.getUserData();
                vc.getAttributes().put("x", String.valueOf((int)(maxX - w)));
            }
        }
        showHandles();
        statusLabel.setText("Aligned right");
    }

    @FXML private void onAlignTop() {
        if (selectedNodes.size() < 2) return;
        pushUndoState();
        double minY = Double.MAX_VALUE;
        for (Node n : selectedNodes) minY = Math.min(minY, n.getLayoutY());
        for (Node n : selectedNodes) {
            n.setLayoutY(minY);
            if (n.getUserData() instanceof VisualComponent) {
                VisualComponent vc = (VisualComponent) n.getUserData();
                vc.getAttributes().put("y", String.valueOf((int)minY));
            }
        }
        showHandles();
        statusLabel.setText("Aligned top");
    }

    @FXML private void onAlignMiddle() {
        if (selectedNodes.size() < 2) return;
        pushUndoState();
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Node n : selectedNodes) {
            minY = Math.min(minY, n.getLayoutY());
            maxY = Math.max(maxY, n.getLayoutY() + n.getBoundsInParent().getHeight());
        }
        double middle = (minY + maxY) / 2;
        for (Node n : selectedNodes) {
            double h = n.getBoundsInParent().getHeight();
            n.setLayoutY(middle - h / 2);
            if (n.getUserData() instanceof VisualComponent) {
                VisualComponent vc = (VisualComponent) n.getUserData();
                vc.getAttributes().put("y", String.valueOf((int)(middle - h / 2)));
            }
        }
        showHandles();
        statusLabel.setText("Aligned middle");
    }

    @FXML private void onAlignBottom() {
        if (selectedNodes.size() < 2) return;
        pushUndoState();
        double maxY = -Double.MAX_VALUE;
        for (Node n : selectedNodes) {
            double h = n.getBoundsInParent().getHeight();
            maxY = Math.max(maxY, n.getLayoutY() + h);
        }
        for (Node n : selectedNodes) {
            double h = n.getBoundsInParent().getHeight();
            n.setLayoutY(maxY - h);
            if (n.getUserData() instanceof VisualComponent) {
                VisualComponent vc = (VisualComponent) n.getUserData();
                vc.getAttributes().put("y", String.valueOf((int)(maxY - h)));
            }
        }
        showHandles();
        statusLabel.setText("Aligned bottom");
    }

    // ================================================================
    //   COPY / PASTE / DUPLICATE
    // ================================================================

    @FXML private void onCopy() {
        if (selectedNodes.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        for (Node n : selectedNodes) {
            if (n.getUserData() instanceof VisualComponent) {
                VisualComponent vc = (VisualComponent) n.getUserData();
                sb.append(serializeSingleComponent(vc)).append("\n---\n");
            }
        }
        clipboardData = sb.toString();
        statusLabel.setText("Copied " + selectedNodes.size() + " component(s)");
    }

    @FXML private void onPaste() {
        if (clipboardData == null || clipboardData.isEmpty()) return;
        pushUndoState();
        clearSelection();

        String[] parts = clipboardData.split("\n---\n");
        double offsetX = 30, offsetY = 30;
        for (String part : parts) {
            if (part.trim().isEmpty()) continue;
            VisualComponent vc = deserializeSingleComponent(part.trim());
            if (vc != null) {
                rootComponent.getChildren().add(vc);
                String xs = vc.getAttributes().get("x");
                String ys = vc.getAttributes().get("y");
                double x = xs != null ? Double.parseDouble(xs) + offsetX : 200;
                double y = ys != null ? Double.parseDouble(ys) + offsetY : 200;
                vc.getAttributes().put("x", String.valueOf((int)x));
                vc.getAttributes().put("y", String.valueOf((int)y));

                Node wrapper = renderComponentWrapper(vc);
                canvas.getChildren().add(wrapper);
                addToSelection(wrapper, vc);
                offsetX += 30;
                offsetY += 30;
            }
        }
        updateLayersPanel();
        buildTree();
        statusLabel.setText("Pasted components");
    }

    @FXML private void onDuplicate() {
        if (selectedNodes.isEmpty()) return;
        pushUndoState();
        List<Node> toDuplicate = new ArrayList<>(selectedNodes);
        clearSelection();

        double offsetX = 30, offsetY = 30;
        for (Node n : toDuplicate) {
            if (n.getUserData() instanceof VisualComponent) {
                VisualComponent original = (VisualComponent) n.getUserData();
                String serialized = serializeSingleComponent(original);
                VisualComponent copy = deserializeSingleComponent(serialized);
                if (copy != null) {
                    rootComponent.getChildren().add(copy);
                    String xs = copy.getAttributes().get("x");
                    String ys = copy.getAttributes().get("y");
                    double x = xs != null ? Double.parseDouble(xs) + offsetX : 200;
                    double y = ys != null ? Double.parseDouble(ys) + offsetY : 200;
                    copy.getAttributes().put("x", String.valueOf((int)x));
                    copy.getAttributes().put("y", String.valueOf((int)y));

                    Node wrapper = renderComponentWrapper(copy);
                    canvas.getChildren().add(wrapper);
                    addToSelection(wrapper, copy);
                    offsetX += 30;
                    offsetY += 30;
                }
            }
        }
        updateLayersPanel();
        buildTree();
        statusLabel.setText("Duplicated components");
    }

    @FXML private void onDeleteSelected() {
        if (selectedNodes.isEmpty()) return;
        pushUndoState();
        List<Node> toDelete = new ArrayList<>(selectedNodes);
        clearSelection();

        for (Node n : toDelete) {
            if (n.getUserData() instanceof VisualComponent) {
                VisualComponent vc = (VisualComponent) n.getUserData();
                // Remove from model tree (search recursively)
                removeFromTree(rootComponent, vc);
                containerContentPanes.remove(vc);
                nodeToComponent.values().remove(vc);
                componentToNode.remove(vc);
            }
            // Remove from parent (canvas or container content pane)
            Pane parent = (Pane) n.getParent();
            if (parent != null) parent.getChildren().remove(n);
        }
        updateLayersPanel();
        buildTree();
        statusLabel.setText("Deleted " + toDelete.size() + " component(s)");
    }

    // ================================================================
    //   SERIALIZATION (single component)
    // ================================================================

    private String serializeSingleComponent(VisualComponent node) {
        StringBuilder sb = new StringBuilder();
        writeNode(node, sb);
        return sb.toString();
    }

    private VisualComponent deserializeSingleComponent(String s) {
        try {
            int[] pos = {0};
            return readNode(s, pos);
        } catch (Exception e) {
            return null;
        }
    }

    // ================================================================
    //   SMART GUIDES
    // ================================================================

    private void showSmartGuides(Node activeNode) {
        hideSmartGuides();
        if (!smartGuidesCheck.isSelected()) return;

        double ax = activeNode.getLayoutX();
        double ay = activeNode.getLayoutY();
        double aw = activeNode.getBoundsInParent().getWidth();
        double ah = activeNode.getBoundsInParent().getHeight();

        List<double[]> otherRects = new ArrayList<>();
        for (Node n : canvas.getChildren()) {
            if (n != activeNode && !selectedNodes.contains(n) && n.getUserData() instanceof VisualComponent) {
                double x = n.getLayoutX();
                double y = n.getLayoutY();
                double w = n.getBoundsInParent().getWidth();
                double h = n.getBoundsInParent().getHeight();
                otherRects.add(new double[]{x, y, w, h});
            }
        }

        double centerX = ax + aw / 2;
        double centerY = ay + ah / 2;
        double rightX = ax + aw;
        double bottomY = ay + ah;
        double snapThreshold = 5;

        for (double[] rect : otherRects) {
            double ox = rect[0], oy = rect[1], ow = rect[2], oh = rect[3];
            double oCenterX = ox + ow / 2, oCenterY = oy + oh / 2;
            double oRightX = ox + ow, oBottomY = oy + oh;

            if (Math.abs(ax - ox) < snapThreshold) drawGuideLine(ax, Math.min(ay, oy), ax, Math.max(bottomY, oBottomY));
            if (Math.abs(ax - oRightX) < snapThreshold) drawGuideLine(ax, Math.min(ay, oy), ax, Math.max(bottomY, oBottomY));
            if (Math.abs(rightX - ox) < snapThreshold) drawGuideLine(rightX, Math.min(ay, oy), rightX, Math.max(bottomY, oBottomY));
            if (Math.abs(rightX - oRightX) < snapThreshold) drawGuideLine(rightX, Math.min(ay, oy), rightX, Math.max(bottomY, oBottomY));
            if (Math.abs(centerX - oCenterX) < snapThreshold) drawGuideLine(centerX, Math.min(ay, oy), centerX, Math.max(bottomY, oBottomY));
            if (Math.abs(ay - oy) < snapThreshold) drawGuideLine(Math.min(ax, ox), ay, Math.max(rightX, oRightX), ay);
            if (Math.abs(ay - oBottomY) < snapThreshold) drawGuideLine(Math.min(ax, ox), ay, Math.max(rightX, oRightX), ay);
            if (Math.abs(bottomY - oy) < snapThreshold) drawGuideLine(Math.min(ax, ox), bottomY, Math.max(rightX, oRightX), bottomY);
            if (Math.abs(bottomY - oBottomY) < snapThreshold) drawGuideLine(Math.min(ax, ox), bottomY, Math.max(rightX, oRightX), bottomY);
            if (Math.abs(centerY - oCenterY) < snapThreshold) drawGuideLine(Math.min(ax, ox), centerY, Math.max(rightX, oRightX), centerY);
        }
    }

    private void drawGuideLine(double x1, double y1, double x2, double y2) {
        double z = zoomFactor.get();
        Line line = new Line(x1 * z, y1 * z, x2 * z, y2 * z);
        line.setStroke(Color.rgb(255, 0, 0, 0.7));
        line.setStrokeWidth(1);
        line.getStrokeDashArray().addAll(4.0, 4.0);
        guidesPane.getChildren().add(line);
        guideLines.add(line);
    }

    private void hideSmartGuides() {
        guidesPane.getChildren().clear();
        guideLines.clear();
    }

    // ================================================================
    //   SELECTION / PROPERTIES
    // ================================================================

    private void selectComponent(VisualComponent comp, Node node) {
        this.selectedComponent = comp;
        this.selectedNode = node;
        buildPropertiesPanel(comp, node);
        updateLayersPanel();
        buildTree();
    }

    private void showEmptyProperties() {
        propertiesBox.getChildren().clear();
        Label hint = new Label("Select a component on the canvas to edit its properties.");
        hint.setWrapText(true);
        hint.getStyleClass().add("muted");
        propertiesBox.getChildren().add(hint);
    }

    private void finishDrag() {
        isDragging = false;
        dragNode = null;
        hideSmartGuides();
        showHandles();
        updateLayersPanel();
        buildTree();
    }

    // ================================================================
    //   PROPERTIES PANEL
    // ================================================================

    private void buildPropertiesPanel(VisualComponent comp, Node node) {
        propertiesBox.getChildren().clear();

        javafx.scene.Node iconView = comp.getType().getIconView(16);
        Label title = new Label(comp.getType().getLabel());
        javafx.scene.layout.HBox titleRow = new javafx.scene.layout.HBox(6);
        if (iconView != null) titleRow.getChildren().add(iconView);
        titleRow.getChildren().add(title);
        titleRow.getStyleClass().add("property-section-header");
        propertiesBox.getChildren().add(titleRow);

        propertiesBox.getChildren().add(new Separator());

        createSectionTitle("Position & Size");
        addPositionField("X", comp, "x", node);
        addPositionField("Y", comp, "y", node);
        addTextStyleField("Width (px)", comp, "width", node);
        addTextStyleField("Height (px)", comp, "height", node);

        createSectionTitle("Content");
        if (supportsText(comp)) {
            addPropertyLabel("Text");
            TextArea textArea = new TextArea(comp.getText());
            textArea.setPrefRowCount(3);
            textArea.setWrapText(true);
            textArea.textProperty().addListener((obs, old, val) -> {
                comp.setText(val);
                updateNodeText(node, comp, val);
            });
            propertiesBox.getChildren().add(textArea);
        }

        ComponentType t = comp.getType();
        addAttrField("Source URL", comp, "src",
            t == ComponentType.IMAGE || t == ComponentType.VIDEO ||
            t == ComponentType.AUDIO || t == ComponentType.EMBED);
        addAttrField("Link URL (href)", comp, "href", t == ComponentType.LINK);
        addAttrField("Alt Text", comp, "alt", t == ComponentType.IMAGE);
        addAttrField("Placeholder", comp, "placeholder",
            t == ComponentType.INPUT || t == ComponentType.TEXTAREA);
        addAttrField("Min Value", comp, "min", t == ComponentType.SLIDER);
        addAttrField("Max Value", comp, "max", t == ComponentType.SLIDER ||
            t == ComponentType.PROGRESS);
        addAttrField("Step", comp, "step", t == ComponentType.SLIDER);
        addAttrField("Accept (file types)", comp, "accept", t == ComponentType.FILE_UPLOAD);
        addAttrField("Target", comp, "target", t == ComponentType.LINK);

        if (t == ComponentType.ICON || t == ComponentType.ICON_BUTTON) {
            addPropertyLabel("Icon (emoji)");
            TextField iconField = new TextField(comp.getIcon() != null ? comp.getIcon() : "");
            iconField.textProperty().addListener((obs, old, val) -> {
                comp.setIcon(val);
                updateNodeText(node, comp, comp.getText());
            });
            propertiesBox.getChildren().add(iconField);
        }

        if (t == ComponentType.SLIDER) {
            addAttrField("Default Value", comp, "value", true);
        }
        if (t == ComponentType.PROGRESS) {
            addAttrField("Value", comp, "value", true);
        }

        createSectionTitle("Design");
        addColorField("Background Color", comp, "background-color", node);
        addColorField("Text Color", comp, "color", node);
        addTextStyleField("Font Size", comp, "font-size", node);
        addTextStyleField("Font Weight", comp, "font-weight", node);
        addPaddingField(comp, node);
        addTextStyleField("Border Radius", comp, "border-radius", node);
        addTextStyleField("Border", comp, "border", node);
        addTextStyleField("Box Shadow", comp, "box-shadow", node);
        addTextStyleField("Opacity", comp, "opacity", node);
        addTextStyleField("Margin", comp, "margin", node);

        createSectionTitle("JavaScript Events");
        addAttrField("onClick", comp, "onclick", true);
        addAttrField("onMouseOver", comp, "onmouseover", true);
        addAttrField("onMouseOut", comp, "onmouseout", true);
        addAttrField("onChange", comp, "onchange", true);
        addAttrField("onInput", comp, "oninput", true);
        addAttrField("onFocus", comp, "onfocus", true);
        addAttrField("onBlur", comp, "onblur", true);

        createSectionTitle("Custom CSS");
        addPropertyLabel("Extra CSS properties");
        TextArea cssArea = new TextArea(comp.getCustomCss());
        cssArea.setPrefRowCount(4);
        cssArea.setWrapText(true);
        cssArea.setPromptText("background: red;\nborder: 2px solid blue;");
        cssArea.textProperty().addListener((obs, old, val) -> {
            comp.setCustomCss(val);
            applyStylesToWrapper(node);
        });
        propertiesBox.getChildren().add(cssArea);

        propertiesBox.getChildren().add(new Separator());
        Button deleteBtn = new Button("Delete Component");
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        deleteBtn.getStyleClass().add("btn-secondary");
        deleteBtn.setOnAction(e -> onDeleteSelected());
        propertiesBox.getChildren().add(deleteBtn);
    }

    private void addAttrField(String label, VisualComponent comp, String attrKey, boolean condition) {
        if (!condition) return;
        addPropertyLabel(label);
        TextField field = new TextField(comp.getAttributes().getOrDefault(attrKey, ""));
        field.textProperty().addListener((obs, old, val) -> comp.getAttributes().put(attrKey, val));
        propertiesBox.getChildren().add(field);
    }

    private void addPaddingField(VisualComponent comp, Node node) {
        addPropertyLabel("Padding");
        TextField field = new TextField(comp.getStyles().getOrDefault("padding", ""));
        field.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.trim().isEmpty()) {
                comp.getStyles().remove("padding");
            } else {
                comp.getStyles().put("padding", val.trim());
            }
            applyStylesToWrapper(node);
        });
        propertiesBox.getChildren().add(field);
    }

    // ================================================================
    //   PROPERTIES HELPERS
    // ================================================================

    private void createSectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("prop-label");
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 10 0 4 0; -fx-text-fill: -text-muted;");
        propertiesBox.getChildren().add(label);
    }

    private void addPropertyLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("prop-label");
        propertiesBox.getChildren().add(label);
    }

    private void addPositionField(String labelText, VisualComponent comp, String key, Node node) {
        addPropertyLabel(labelText);
        TextField field = new TextField(comp.getAttributes().getOrDefault(key, "0"));
        field.setPrefWidth(80);
        field.textProperty().addListener((obs, old, val) -> {
            try {
                double v = Double.parseDouble(val);
                comp.getAttributes().put(key, val);
                if (node instanceof Pane) {
                    Pane p = (Pane) node;
                    if ("x".equals(key)) p.setLayoutX(v);
                    if ("y".equals(key)) p.setLayoutY(v);
                }
                showHandles();
            } catch (NumberFormatException ignored) { }
        });
        propertiesBox.getChildren().add(field);
    }

    private void addColorField(String labelText, VisualComponent comp, String styleKey, Node node) {
        addPropertyLabel(labelText);
        ColorPicker picker = new ColorPicker();
        String existing = comp.getStyles().get(styleKey);
        if (existing != null) {
            try { picker.setValue(Color.web(existing)); } catch (Exception ignored) { }
        }
        picker.setMaxWidth(Double.MAX_VALUE);
        picker.valueProperty().addListener((obs, old, val) -> {
            String hex = toHex(val);
            comp.getStyles().put(styleKey, hex);
            applyStylesToWrapper(node);
        });
        propertiesBox.getChildren().add(picker);
    }

    private void addTextStyleField(String labelText, VisualComponent comp, String styleKey, Node node) {
        addPropertyLabel(labelText);
        TextField field = new TextField(comp.getStyles().getOrDefault(styleKey, ""));
        field.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.trim().isEmpty()) {
                comp.getStyles().remove(styleKey);
            } else {
                comp.getStyles().put(styleKey, val.trim());
            }
            applyStylesToWrapper(node);
            if ("width".equals(styleKey) || "height".equals(styleKey)) {
                try {
                    double sz = Double.parseDouble(val.replace("px", "").trim());
                    if (node instanceof Pane) {
                        Pane p = (Pane) node;
                        if ("width".equals(styleKey)) p.setPrefWidth(sz);
                        if ("height".equals(styleKey)) p.setPrefHeight(sz);
                        if (!p.getChildren().isEmpty()) {
                            Node c = p.getChildren().get(0);
                            c.resizeRelocate(0, 0, p.getPrefWidth(), p.getPrefHeight());
                        }
                    }
                    showHandles();
                } catch (NumberFormatException ignored) { }
            }
        });
        propertiesBox.getChildren().add(field);
    }

    private void applyStylesToWrapper(Node wrapper) {
        if (wrapper instanceof Label && wrapper.getUserData() instanceof VisualComponent) {
            VisualComponent comp = (VisualComponent) wrapper.getUserData();
            String base = ((Label) wrapper).getText().isEmpty() ? "" : "";
            String style = buildInlineStyle(comp);
            String existing = wrapper.getStyle();
            if (existing.contains("-fx-background-color:")) {
                existing = existing.replaceAll("-fx-background-color:[^;]+;", "");
            }
            wrapper.setStyle(existing + style);
            return;
        }
        if (wrapper instanceof Pane && wrapper.getUserData() instanceof VisualComponent) {
            VisualComponent comp = (VisualComponent) wrapper.getUserData();
            Pane p = (Pane) wrapper;
            if (!p.getChildren().isEmpty()) {
                Node content = p.getChildren().get(0);
                if (content instanceof Region) {
                    ((Region) content).setStyle(buildInlineStyle(comp));
                }
                String ws = comp.getStyles().get("width");
                String hs = comp.getStyles().get("height");
                double w = p.getPrefWidth();
                double h = p.getPrefHeight();
                if (ws != null) {
                    try { w = Double.parseDouble(ws); p.setPrefWidth(w); } catch (Exception ignored) { }
                }
                if (hs != null) {
                    try { h = Double.parseDouble(hs); p.setPrefHeight(h); } catch (Exception ignored) { }
                }
                content.resizeRelocate(0, 0, w, h);
                p.resize(w, h);
                showHandles();
            }
        }
    }

    // ================================================================
    //   STYLE HELPERS
    // ================================================================

    private String buildInlineStyle(VisualComponent comp) {
        StringBuilder sb = new StringBuilder();
        Map<String, String> s = comp.getStyles();
        if (s.containsKey("background-color")) sb.append("-fx-background-color: ").append(s.get("background-color")).append(";");
        if (s.containsKey("background")) {
            String bg = s.get("background");
            if (bg.startsWith("linear-gradient")) {
                // convert CSS gradient to JavaFX
                bg = bg.replace("linear-gradient(", "").replace(")", "");
                String[] parts = bg.split(",");
                if (parts.length >= 2) {
                    String angle = parts[0].trim();
                    String c1 = parts[1].trim();
                    String c2 = parts.length >= 3 ? parts[2].trim() : parts[1].trim();
                    sb.append("-fx-background-color: ").append(c1).append(";");
                    // محاكاة التدرج بلون ثاني شفاف قليلاً
                } else {
                    sb.append("-fx-background-color: ").append(parts[0].trim()).append(";");
                }
            } else {
                sb.append("-fx-background-color: ").append(bg).append(";");
            }
        }
        if (s.containsKey("color")) sb.append("-fx-text-fill: ").append(s.get("color")).append(";");
        if (s.containsKey("font-size")) sb.append("-fx-font-size: ").append(s.get("font-size")).append(";");
        if (s.containsKey("font-weight")) sb.append("-fx-font-weight: ").append(s.get("font-weight")).append(";");
        if (s.containsKey("font-style")) sb.append("-fx-font-style: ").append(s.get("font-style")).append(";");
        if (s.containsKey("padding")) sb.append("-fx-padding: ").append(toFxInsetShorthand(s.get("padding"))).append(";");
        if (s.containsKey("border-radius")) sb.append("-fx-background-radius: ").append(s.get("border-radius")).append(";");
        if (s.containsKey("border")) sb.append("-fx-border-color: derive(-text-muted, 0%); -fx-border-width: 1; -fx-border-style: dashed;");
        if (s.containsKey("opacity")) sb.append("-fx-opacity: ").append(s.get("opacity")).append(";");
        if (s.containsKey("margin")) sb.append("-fx-margin: ").append(s.get("margin")).append(";");
        if (s.containsKey("box-shadow")) sb.append("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 4);");
        if (s.containsKey("text-align")) sb.append("-fx-text-alignment: ").append(s.get("text-align")).append(";");
        if (s.containsKey("min-height")) sb.append("-fx-min-height: ").append(s.get("min-height")).append(";");
        if (s.containsKey("max-width")) sb.append("-fx-max-width: ").append(s.get("max-width")).append(";");
        return sb.toString();
    }

    private String toFxInsetShorthand(String cssPadding) {
        return cssPadding.replace("px", "");
    }

    private String toHex(Color c) {
        return String.format("#%02X%02X%02X",
                (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255));
    }

    private double parsePx(String value) {
        try {
            return Double.parseDouble(value.replace("px", "").trim());
        } catch (NumberFormatException e) {
            return 30;
        }
    }

    private double snap(double val) {
        if (!snapEnabled) return val;
        return Math.round(val / GRID_SIZE) * GRID_SIZE;
    }

    // ================================================================
    //   NESTING HELPERS
    // ================================================================

    /** Find the deepest container content area under a canvas point (x,y). */
    private Pane findContentAreaAt(double canvasX, double canvasY) {
        // Check canvas-level containers first (their wrappers contain content panes)
        for (Node n : canvas.getChildren()) {
            if (n instanceof Pane && n.getUserData() instanceof VisualComponent) {
                Pane wrapperPane = (Pane) n;
                VisualComponent vc = (VisualComponent) n.getUserData();
                if (vc.isContainer()) {
                    Pane contentArea = containerContentPanes.get(vc);
                    if (contentArea != null) {
                        double cx = wrapperPane.getLayoutX();
                        double cy = wrapperPane.getLayoutY();
                        double cw = wrapperPane.getPrefWidth();
                        double ch = wrapperPane.getPrefHeight();
                        if (cw <= 0) cw = 300;
                        if (ch <= 0) ch = 200;
                        if (canvasX >= cx && canvasX <= cx + cw && canvasY >= cy && canvasY <= cy + ch) {
                            return contentArea;
                        }
                    }
                }
            }
        }
        return null;
    }

    /** Absolute X of a node on the canvas, accounting for container nesting. */
    private double wrapperAbsoluteX(Node node) {
        double x = 0;
        Node p = node;
        while (p != null && p != canvas) {
            x += p.getLayoutX();
            p = p.getParent();
        }
        return x;
    }

    /** Absolute Y of a node on the canvas, accounting for container nesting. */
    private double wrapperAbsoluteY(Node node) {
        double y = 0;
        Node p = node;
        while (p != null && p != canvas) {
            y += p.getLayoutY();
            p = p.getParent();
        }
        return y;
    }

    /** Recursively search all canvas children and container content areas for a matching wrapper. */
    private Node findWrapperInAllChildren(VisualComponent vc) {
        return findWrapperRecursive(canvas.getChildren(), vc);
    }

    private Node findWrapperRecursive(java.util.List<Node> children, VisualComponent vc) {
        for (Node n : children) {
            if (n.getUserData() == vc) return n;
            if (n instanceof Pane) {
                Node found = findWrapperRecursive(((Pane) n).getChildren(), vc);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void updateNodeText(Node wrapper, VisualComponent comp, String text) {
        if (wrapper instanceof Label && wrapper.getUserData() == comp) {
            String display = comp.getType().getLabel() + ": " + text;
            ((Label) wrapper).setText(display);
            return;
        }
        if (wrapper instanceof Pane) {
            Pane p = (Pane) wrapper;
            if (!p.getChildren().isEmpty()) {
                Node content = p.getChildren().get(0);
                if (content instanceof Labeled) {
                    String display = text;
                    if (comp.getType() == ComponentType.ICON_BUTTON || comp.getType() == ComponentType.ICON) {
                        String icon = comp.getIcon() != null ? comp.getIcon() : "";
                        display = icon + (icon.isEmpty() || text.isEmpty() ? "" : " ") + text;
                        if (display.trim().isEmpty()) display = comp.getType() == ComponentType.ICON_BUTTON ? "🔘" : "⭐";
                    }
                    ((Labeled) content).setText(display);
                } else if (content instanceof TitledPane) {
                    ((TitledPane) content).setText(text);
                }
            }
        }
    }

    // ================================================================
    //   LAYERS PANEL
    // ================================================================

    private void updateLayersPanel() {
        layersBox.getChildren().clear();
        // Flatten all components with depth info for indentation
        java.util.List<NodeDepth> flat = new java.util.ArrayList<>();
        flattenLayers(canvas.getChildren(), 0, flat);
        Collections.reverse(flat);

        for (NodeDepth nd : flat) {
            Node n = nd.node;
            if (!(n.getUserData() instanceof VisualComponent)) continue;
            VisualComponent vc = (VisualComponent) n.getUserData();
            javafx.scene.Node iconView = vc.getType().getIconView(14);
            HBox layerItem = new HBox(6);
            if (iconView != null) layerItem.getChildren().add(iconView);
            layerItem.setAlignment(Pos.CENTER_LEFT);
            layerItem.setPadding(new Insets(4, 8, 4, 8 + nd.depth * 12));
            layerItem.setMaxWidth(Double.MAX_VALUE);

            boolean isSelected = selectedNodes.contains(n);
            String bg = isSelected ? "-fx-background-color: derive(-accent, 50%);" : "-fx-background-color: transparent;";
            layerItem.setStyle(bg + "-fx-background-radius: 4; -fx-padding: 4 8; -fx-cursor: hand;");

            Label nameLabel = new Label(vc.getType().getLabel() + " (" + vc.getId().substring(0, 6) + ")");
            nameLabel.setStyle("-fx-font-size: 11px;");

            layerItem.getChildren().add(nameLabel);
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            layerItem.getChildren().add(spacer);

            Button visBtn = new Button();
            visBtn.setGraphic(IconManager.imageView(IconManager.SEARCH, 12)); // eye-like icon
            visBtn.setStyle("-fx-font-size: 10px; -fx-padding: 1 6; -fx-background-color: transparent; -fx-cursor: hand;");
            layerItem.getChildren().add(visBtn);

            Pane finalNode = (Pane) n;
            layerItem.setOnMouseClicked(e -> {
                if (e.isControlDown()) {
                    toggleSelection(finalNode, vc);
                } else {
                    clearSelection();
                    addToSelection(finalNode, vc);
                }
                updateLayersPanel();
            });

            visBtn.setOnAction(e -> {
                finalNode.setVisible(!finalNode.isVisible());
                if (finalNode.isVisible()) {
                    visBtn.setGraphic(IconManager.imageView(IconManager.SEARCH, 12));
                } else {
                    visBtn.setGraphic(IconManager.imageView(IconManager.CLOSE, 12));
                }
            });

            layersBox.getChildren().add(layerItem);
        }
    }

    private static class NodeDepth {
        Node node;
        int depth;
        NodeDepth(Node n, int d) { node = n; depth = d; }
    }

    private void flattenLayers(java.util.List<Node> children, int depth, java.util.List<NodeDepth> result) {
        for (Node n : children) {
            if (n.getUserData() instanceof VisualComponent) {
                result.add(new NodeDepth(n, depth));
            }
            if (n instanceof Pane) {
                flattenLayers(((Pane) n).getChildren(), depth + 1, result);
            }
        }
    }


    
    // ================================================================
    //   COMPONENT TREE
    // ================================================================

    private void buildTree() {
        TreeItem<Object> rootItem = new TreeItem<>("Canvas");
        rootItem.setExpanded(true);
        javafx.scene.Node canvasIcon = IconManager.imageView(IconManager.LAYOUT_TOOL, 14);
        if (canvasIcon != null) rootItem.setGraphic(canvasIcon);

        buildTreeRecursive(canvas.getChildren(), rootItem);

        componentTree.setRoot(rootItem);
        componentTree.setShowRoot(true);
        componentTree.setCellFactory(param -> new TreeCell<Object>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else if (item instanceof VisualComponent) {
                    VisualComponent vc = (VisualComponent) item;
                    setText(vc.getType().getLabel() + " (" + vc.getId().substring(0, 6) + ")");
                    setGraphic(vc.getType().getIconView(14));
                    boolean sel = false;
                    for (Node sn : selectedNodes) {
                        if (sn.getUserData() == vc) { sel = true; break; }
                    }
                    setStyle(sel ? "-fx-font-weight: bold; -fx-text-fill: -accent;" : "");
                } else {
                    setText(item.toString());
                }
            }
        });
    }

    private void buildTreeRecursive(java.util.List<Node> children, TreeItem<Object> parentItem) {
        for (Node n : children) {
            if (n.getUserData() instanceof VisualComponent) {
                VisualComponent vc = (VisualComponent) n.getUserData();
                TreeItem<Object> item = new TreeItem<>(vc);
                item.setExpanded(true);
                if (n instanceof Pane) {
                    buildTreeRecursive(((Pane) n).getChildren(), item);
                }
                parentItem.getChildren().add(item);
            }
        }
    }

    private Node findWrapperFor(List<Node> children, VisualComponent vc) {
        return findWrapperRecursive(children, vc);
    }

    // ================================================================
    //   ZOOM
    // ================================================================

    @FXML private void onZoomIn() {
        double val = Math.min(zoomSlider.getValue() + 10, zoomSlider.getMax());
        zoomSlider.setValue(val);
    }

    @FXML private void onZoomOut() {
        double val = Math.max(zoomSlider.getValue() - 10, zoomSlider.getMin());
        zoomSlider.setValue(val);
    }

    private void setZoomLevel(double percent) {
        double factor = percent / 100.0;
        zoomFactor.set(factor);
        zoomScale.setX(factor);
        zoomScale.setY(factor);
        canvas.getTransforms().clear();
        canvas.getTransforms().add(zoomScale);
        zoomLabel.setText((int) percent + "%");
        drawGrid();
    }

    // ================================================================
    //   GRID
    // ================================================================

    private void drawGrid() {
        gridPane.getChildren().clear();
        if (!snapEnabled) return;

        double gridSize = GRID_SIZE * zoomFactor.get();
        if (gridSize < 5) gridSize = 5;

        double w = 4000 * zoomFactor.get();
        double h = 4000 * zoomFactor.get();

        // Dot grid pattern
        double dotRadius = Math.max(0.8, 1.2 * zoomFactor.get());
        Color dotColor = Color.rgb(180, 180, 195, 0.25);

        for (double x = 0; x <= w; x += gridSize) {
            for (double y = 0; y <= h; y += gridSize) {
                Circle dot = new Circle(x, y, dotRadius, dotColor);
                gridPane.getChildren().add(dot);
            }
        }
    }

    // ================================================================
    //   HELPERS
    // ================================================================

    private boolean supportsText(VisualComponent comp) {
        switch (comp.getType()) {
            case HEADING: case SUBHEADING: case PARAGRAPH: case RICH_TEXT:
            case QUOTE: case BUTTON: case ICON_BUTTON: case LINK:
            case CHECKBOX: case RADIO: case TOGGLE:
            case BADGE: case CHIP: case ALERT: case TOAST:
            case ACCORDION:
                return true;
            default: return false;
        }
    }

    private boolean removeFromTree(VisualComponent parent, VisualComponent target) {
        if (parent.getChildren().remove(target)) return true;
        for (VisualComponent child : parent.getChildren()) {
            if (removeFromTree(child, target)) return true;
        }
        return false;
    }

    // ================================================================
    //   TOOLBAR ACTIONS
    // ================================================================

    @FXML private void onExportZip() {
        onSave();
        try {
            File zipFile = new File(projectRoot.getParent(), projectRoot.getName() + ".zip");
            com.eagle.util.ZipExporter.exportDirectory(projectRoot, zipFile);
            statusLabel.setText("📦 Exported: " + zipFile.getAbsolutePath());
            java.awt.Desktop.getDesktop().open(zipFile.getParentFile());
        } catch (Exception e) {
            showError("Export failed: " + e.getMessage());
        }
    }

    @FXML private void onSave() {
    try {
        syncDataModelOrder();
        String html = HtmlExporter.toHtml(rootComponent, projectRoot.getName());
        String css = HtmlExporter.toCss(rootComponent);
        String js = HtmlExporter.toJs(rootComponent);

        Files.write(new File(projectRoot, "index.html").toPath(), html.getBytes(StandardCharsets.UTF_8));
        Files.write(new File(projectRoot, "style.css").toPath(), css.getBytes(StandardCharsets.UTF_8));
        Files.write(new File(projectRoot, "script.js").toPath(), js.getBytes(StandardCharsets.UTF_8));

        statusLabel.setText("✅ Saved: index.html + style.css + script.js");
    } catch (Exception e) {
        showError("Failed to save: " + e.getMessage());
    }
}

    private void syncDataModelOrder() {
        syncChildrenOrder(canvas.getChildren(), rootComponent);
    }

    private void syncChildrenOrder(java.util.List<Node> parentNodes, VisualComponent parentComp) {
        java.util.List<VisualComponent> ordered = new ArrayList<>();
        for (Node n : parentNodes) {
            if (n.getUserData() instanceof VisualComponent) {
                VisualComponent vc = (VisualComponent) n.getUserData();
                ordered.add(vc);
                // Recurse into nested containers
                Pane contentPane = containerContentPanes.get(vc);
                if (contentPane != null) {
                    syncChildrenOrder(contentPane.getChildren(), vc);
                }
            }
        }
        parentComp.getChildren().clear();
        parentComp.getChildren().addAll(ordered);
    }

    private void selectAllRubberBand(java.util.List<Node> children, Bounds selectionBounds, double z) {
        for (Node child : children) {
            if (child instanceof Pane && child.getUserData() instanceof VisualComponent) {
                double cx = wrapperAbsoluteX(child) * z;
                double cy = wrapperAbsoluteY(child) * z;
                VisualComponent vc = (VisualComponent) child.getUserData();
                double cw = 160, ch = 40;
                if (vc.getStyles().containsKey("width")) cw = Double.parseDouble(vc.getStyles().get("width"));
                if (vc.getStyles().containsKey("height")) ch = Double.parseDouble(vc.getStyles().get("height"));
                cw *= z; ch *= z;
                if (selectionBounds.intersects(cx, cy, cw, ch)) {
                    selectedNodes.add(child);
                    addSelectionHighlight(child);
                }
            }
            if (child instanceof Pane) {
                selectAllRubberBand(((Pane) child).getChildren(), selectionBounds, z);
            }
        }
    }

    @FXML private void onClearCanvas() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Clear the entire canvas? This cannot be undone.");
        ThemeManager.getInstance().applyTheme(confirm.getDialogPane().getScene());
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                pushUndoState();
                rootComponent.getChildren().clear();
                canvas.getChildren().clear();
                handleNodes.clear();
                nodeToComponent.clear();
                componentToNode.clear();
                containerContentPanes.clear();
                selectedNodes.clear();
                selectedComponent = null;
                selectedNode = null;
                showEmptyProperties();
                updateLayersPanel();
                buildTree();
                statusLabel.setText("Canvas cleared");
            }
        });
    }

    @FXML private void onViewCode() {
        String html = HtmlExporter.toHtml(rootComponent, projectRoot.getName());
        String css = HtmlExporter.toCss(rootComponent);

        CodeEditor htmlEditor = new CodeEditor(LanguageType.HTML);
        htmlEditor.setText(html);
        CodeEditor cssEditor = new CodeEditor(LanguageType.CSS);
        cssEditor.setText(css);
        cssEditor.hideColorSwatches(); // منع ظهور مربعات الألوان مؤقتاً لحين اختيار تبويب CSS

        TabPane codeTabs = new TabPane(
            tab("index.html", htmlEditor),
            tab("style.css", cssEditor)
        );
        codeTabs.getStyleClass().add("code-viewer-tabs");

        // --- Toolbar ---
        ToggleGroup langGroup = new ToggleGroup();
        ToggleButton htmlBtn = new ToggleButton("HTML");
        htmlBtn.setToggleGroup(langGroup); htmlBtn.setSelected(true);
        ToggleButton cssBtn = new ToggleButton("CSS");
        cssBtn.setToggleGroup(langGroup);
        htmlBtn.setOnAction(e -> codeTabs.getSelectionModel().select(0));
        cssBtn.setOnAction(e -> codeTabs.getSelectionModel().select(1));
        codeTabs.getSelectionModel().selectedIndexProperty().addListener((o, ov, nv) -> {
            if (nv.intValue() == 0) htmlBtn.setSelected(true);
            else cssBtn.setSelected(true);
            if (ov.intValue() == 0) htmlEditor.hideColorSwatches();
            else cssEditor.hideColorSwatches();
            if (nv.intValue() == 1) cssEditor.activateColorSwatches();
        });

        Label zoomLabel = new Label("100%");
        Button zoomInBtn = new Button("+");
        Button zoomOutBtn = new Button("−");
        Button zoomResetBtn = new Button("Reset");
        zoomInBtn.setOnAction(e -> {
            double z = Math.min(32, currentCodeFont + 2);
            currentCodeFont = z;
            htmlEditor.setFontSize(z);
            cssEditor.setFontSize(z);
            zoomLabel.setText((int)(z / 13.5 * 100) + "%");
        });
        zoomOutBtn.setOnAction(e -> {
            double z = Math.max(9, currentCodeFont - 2);
            currentCodeFont = z;
            htmlEditor.setFontSize(z);
            cssEditor.setFontSize(z);
            zoomLabel.setText((int)(z / 13.5 * 100) + "%");
        });
        zoomResetBtn.setOnAction(e -> {
            currentCodeFont = 13.5;
            htmlEditor.setFontSize(13.5);
            cssEditor.setFontSize(13.5);
            zoomLabel.setText("100%");
        });

        CheckMenuItem wordWrapItem = new CheckMenuItem("Word Wrap");
        wordWrapItem.setSelected(false);
        wordWrapItem.setOnAction(e -> {
            boolean wrap = wordWrapItem.isSelected();
            htmlEditor.getCodeArea().setWrapText(wrap);
            cssEditor.getCodeArea().setWrapText(wrap);
        });
        Button wrapBtn = new Button("Wrap");
        wrapBtn.setTooltip(new Tooltip("Toggle word wrap"));
        wrapBtn.setOnAction(e -> {
            wordWrapItem.setSelected(!wordWrapItem.isSelected());
            wordWrapItem.getOnAction().handle(null);
        });

        // --- Syntax theme selector ---
        ComboBox<String> syntaxThemeSelector = new ComboBox<>();
        syntaxThemeSelector.getItems().addAll("App Theme", "Monokai", "Solarized Dark", "GitHub Light", "Dracula", "One Dark", "Nord", "GitHub Dark", "Atom One Light", "Tokyo Night", "Catppuccin", "Ayu Dark", "SynthWave '84", "Noctis Lux");
        syntaxThemeSelector.setValue("App Theme");
        syntaxThemeSelector.setTooltip(new Tooltip("Syntax highlighting theme"));
        String[] syntaxCssFiles = {null, "/com/eagle/css/syntax-monokai.css", "/com/eagle/css/syntax-solarized-dark.css", "/com/eagle/css/syntax-github-light.css", "/com/eagle/css/syntax-dracula.css", "/com/eagle/css/syntax-one-dark.css", "/com/eagle/css/syntax-nord.css", "/com/eagle/css/syntax-github-dark.css", "/com/eagle/css/syntax-atom-one-light.css", "/com/eagle/css/syntax-tokyo-night.css", "/com/eagle/css/syntax-catppuccin.css", "/com/eagle/css/syntax-ayu-dark.css", "/com/eagle/css/syntax-synthwave.css", "/com/eagle/css/syntax-noctis-lux.css"};

        ToolBar toolBar = new ToolBar(
            new Label("Language:"), htmlBtn, cssBtn,
            new Separator(),
            new Label("Zoom:"), zoomOutBtn, zoomLabel, zoomInBtn, zoomResetBtn,
            new Separator(),
            wrapBtn,
            new Separator(),
            new Label("Theme:"), syntaxThemeSelector
        );
        toolBar.getStyleClass().add("code-editor-toolbar");

        // --- MenuBar ---
        MenuItem saveHtmlItem = new MenuItem("Save HTML");
        saveHtmlItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCodeCombination.CONTROL_DOWN));
        saveHtmlItem.setOnAction(e -> saveCodeFile(html, "html"));
        MenuItem saveCssItem = new MenuItem("Save CSS");
        saveCssItem.setOnAction(e -> saveCodeFile(css, "css"));
        MenuItem closeItem = new MenuItem("Close");
        closeItem.setAccelerator(new KeyCodeCombination(KeyCode.W, KeyCodeCombination.CONTROL_DOWN));
        closeItem.setOnAction(e -> ((Stage) codeTabs.getScene().getWindow()).close());

        MenuItem copyItem = new MenuItem("Copy");
        copyItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCodeCombination.CONTROL_DOWN));
        copyItem.setOnAction(e -> {
            CodeEditor ed = activeEditor(codeTabs);
            if (ed != null) {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent cc = new ClipboardContent();
                cc.putString(ed.getCodeArea().getSelectedText());
                clipboard.setContent(cc);
            }
        });
        MenuItem selectAllItem = new MenuItem("Select All");
        selectAllItem.setAccelerator(new KeyCodeCombination(KeyCode.A, KeyCodeCombination.CONTROL_DOWN));
        selectAllItem.setOnAction(e -> {
            CodeEditor ed = activeEditor(codeTabs);
            if (ed != null) ed.getCodeArea().selectAll();
        });
        MenuItem findItem = new MenuItem("Find / Replace");
        findItem.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCodeCombination.CONTROL_DOWN));
        findItem.setOnAction(e -> {
            CodeEditor ed = activeEditor(codeTabs);
            if (ed != null) ed.getCodeArea().requestFocus();
        });

        Menu fileMenu = new Menu("File", null, saveHtmlItem, saveCssItem, new SeparatorMenuItem(), closeItem);
        Menu editMenu = new Menu("Edit", null, copyItem, selectAllItem, new SeparatorMenuItem(), findItem);
        //Menu viewMenu = new Menu("View", zoomInBtn, zoomOutBtn, zoomResetBtn, new SeparatorMenuItem(), wordWrapItem);
        //Menu viewMenu = new Menu("View", null, zoomInBtn, zoomOutBtn, zoomResetBtn, new SeparatorMenuItem(), wordWrapItem);
        MenuBar menuBar = new MenuBar(fileMenu, editMenu);

        // --- Status Bar ---
        Label statusLang = new Label("HTML");
        Label statusPos = new Label("Ln 1, Col 1");
        Label statusSize = new Label(html.length() + " chars");
        statusLang.getStyleClass().add("status-label");
        statusPos.getStyleClass().add("status-label");
        statusSize.getStyleClass().add("status-label");

        htmlEditor.getCodeArea().caretPositionProperty().addListener((o, ov, pos) -> {
            int[] lc = htmlEditor.getLineAndColumn();
            statusPos.setText("Ln " + lc[0] + ", Col " + lc[1]);
        });
        cssEditor.getCodeArea().caretPositionProperty().addListener((o, ov, pos) -> {
            int[] lc = cssEditor.getLineAndColumn();
            statusPos.setText("Ln " + lc[0] + ", Col " + lc[1]);
        });
        codeTabs.getSelectionModel().selectedIndexProperty().addListener((o, ov, idx) -> {
            if (idx.intValue() == 0) {
                statusLang.setText("HTML");
                statusSize.setText(html.length() + " chars");
            } else {
                statusLang.setText("CSS");
                statusSize.setText(css.length() + " chars");
            }
        });

        HBox statusBar = new HBox(12, statusLang, new Separator(javafx.geometry.Orientation.VERTICAL), statusPos, new Separator(javafx.geometry.Orientation.VERTICAL), statusSize);
        statusBar.getStyleClass().add("code-editor-statusbar");
        statusBar.setPadding(new Insets(3, 10, 3, 10));
        statusBar.setAlignment(Pos.CENTER_LEFT);

        // --- Problems Panel ---
        ProblemsPanel problemsPanel = new ProblemsPanel();
        problemsPanel.linkTo(htmlEditor.getCodeArea());
        htmlEditor.setOnProblemsChanged(problems -> problemsPanel.setProblems(problems));
        cssEditor.setOnProblemsChanged(problems -> {
            if (codeTabs.getSelectionModel().getSelectedIndex() == 1) {
                problemsPanel.setProblems(problems);
            }
        });
        codeTabs.getSelectionModel().selectedIndexProperty().addListener((o, ov, idx) -> {
            if (idx.intValue() == 0) problemsPanel.setProblems(htmlEditor.getProblems());
            else problemsPanel.setProblems(cssEditor.getProblems());
        });

        SplitPane centerSplit = new SplitPane(codeTabs, problemsPanel);
        centerSplit.setOrientation(javafx.geometry.Orientation.VERTICAL);
        centerSplit.setDividerPositions(0.75);

        VBox top = new VBox(menuBar, toolBar);
        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(centerSplit);
        root.setBottom(statusBar);
        root.setPrefSize(860, 620);

        Scene scene = new Scene(root);
        Stage stage = new Stage();
        stage.setTitle("Generated Code — " + projectRoot.getName());
        stage.setScene(scene);
        ThemeManager.getInstance().applyTheme(stage.getScene());
        scene.getStylesheets().add(getClass().getResource("/com/eagle/css/code-viewer.css").toExternalForm());

        syntaxThemeSelector.setOnAction(e -> {
            int idx = syntaxThemeSelector.getSelectionModel().getSelectedIndex();
            scene.getStylesheets().removeIf(s -> s.contains("syntax-"));
            if (idx > 0 && idx < syntaxCssFiles.length) {
                String cssPath = syntaxCssFiles[idx];
                try {
                    scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
                } catch (Exception ex) { /* file not found */ }
            }
        });

        stage.initOwner(statusLabel.getScene().getWindow());
        stage.setOnHidden(e -> {
            htmlEditor.hideColorSwatches();
            cssEditor.hideColorSwatches();
        });
        stage.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if (!isShowing) {
                htmlEditor.hideColorSwatches();
                cssEditor.hideColorSwatches();
            }
        });
        stage.show();
    }

    private CodeEditor activeEditor(TabPane tabs) {
        Node n = tabs.getSelectionModel().getSelectedItem().getContent();
        return n instanceof CodeEditor ? (CodeEditor) n : null;
    }

    private Tab tab(String name, CodeEditor ed) {
        Tab t = new Tab(name, ed);
        t.setClosable(false);
        return t;
    }

    private static double currentCodeFont = 13.5;

    private void saveCodeFile(String content, String ext) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save " + ext.toUpperCase());
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(ext.toUpperCase(), "*." + ext));
        File f = fc.showSaveDialog(statusLabel.getScene().getWindow());
        if (f != null) {
            try { Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
                statusLabel.setText("Saved " + f.getName());
            } catch (IOException ex) {
                statusLabel.setText("Save failed: " + ex.getMessage());
            }
        }
    }

    @FXML private void onPreview() {
        onSave();
        try {
            File index = new File(projectRoot, "index.html");
            java.awt.Desktop.getDesktop().browse(index.toURI());
        } catch (Exception e) {
            statusLabel.setText("Saved. (Could not auto-open browser: " + e.getMessage() + ")");
        }
    }

    // ================================================================
    //   UNDO / REDO
    // ================================================================

    @FXML private void onUndo() {
        if (undoStack.isEmpty()) {
            statusLabel.setText("Nothing to undo");
            return;
        }
        redoStack.push(serializeTree(rootComponent));
        String snapshot = undoStack.pop();
        restoreFromSnapshot(snapshot);
        statusLabel.setText("Undid last change");
    }

    @FXML private void onRedo() {
        if (redoStack.isEmpty()) {
            statusLabel.setText("Nothing to redo");
            return;
        }
        undoStack.push(serializeTree(rootComponent));
        String snapshot = redoStack.pop();
        restoreFromSnapshot(snapshot);
        statusLabel.setText("Redid change");
    }

    private void pushUndoState() {
        undoStack.push(serializeTree(rootComponent));
        if (undoStack.size() > MAX_UNDO) undoStack.removeLast();
        redoStack.clear();
    }

    // ================================================================
    //   TREE SERIALIZATION
    // ================================================================

    private String serializeTree(VisualComponent node) {
        StringBuilder sb = new StringBuilder();
        writeNode(node, sb);
        return sb.toString();
    }

    private void writeNode(VisualComponent node, StringBuilder sb) {
        sb.append(node.getType().name()).append('|');
        sb.append(encode(node.getText())).append('|');
        sb.append(node.getStyles().size()).append('|');
        for (Map.Entry<String, String> e : node.getStyles().entrySet()) {
            sb.append(encode(e.getKey())).append('=').append(encode(e.getValue())).append(';');
        }
        sb.append('|').append(node.getAttributes().size()).append('|');
        for (Map.Entry<String, String> e : node.getAttributes().entrySet()) {
            sb.append(encode(e.getKey())).append('=').append(encode(e.getValue())).append(';');
        }
        sb.append('|').append(node.getChildren().size()).append('|');
        for (VisualComponent child : node.getChildren()) {
            sb.append('[');
            writeNode(child, sb);
            sb.append(']');
        }
    }

    private String encode(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("|", "\\p").replace("=", "\\e")
                .replace(";", "\\s").replace("[", "\\b").replace("]", "\\c");
    }

    private String decode(String s) {
        return s.replace("\\b", "[").replace("\\c", "]").replace("\\s", ";")
                .replace("\\e", "=").replace("\\p", "|").replace("\\\\", "\\");
    }

    private void restoreFromSnapshot(String snapshot) {
        int[] pos = {0};
        VisualComponent restoredRoot = readNode(snapshot, pos);

        rootComponent.getChildren().clear();
        rootComponent.getChildren().addAll(restoredRoot.getChildren());

        canvas.getChildren().clear();
        handleNodes.clear();
        nodeToComponent.clear();
        componentToNode.clear();
        containerContentPanes.clear();
        selectedNodes.clear();
        hideSmartGuides();

        for (VisualComponent child : rootComponent.getChildren()) {
            Node wrapper = renderComponentWrapper(child);
            canvas.getChildren().add(wrapper);
            renderChildrenTree(child, wrapper);
        }

        selectedComponent = null;
        selectedNode = null;
        showEmptyProperties();
        updateLayersPanel();
        buildTree();
    }

    private void renderChildrenTree(VisualComponent comp, Node wrapper) {
        for (VisualComponent child : comp.getChildren()) {
            Node childWrapper = renderComponentWrapper(child);
            // If parent is a container with a content pane, nest inside it
            Pane contentPane = containerContentPanes.get(comp);
            if (contentPane != null) {
                contentPane.getChildren().add(childWrapper);
            } else {
                canvas.getChildren().add(childWrapper);
            }
            renderChildrenTree(child, childWrapper);
        }
    }

    private VisualComponent readNode(String s, int[] pos) {
        String typeName = readUntil(s, pos, '|');
        ComponentType type = ComponentType.valueOf(typeName);
        VisualComponent node = new VisualComponent(type);
        node.getStyles().clear();
        node.getAttributes().clear();

        String text = decode(readUntil(s, pos, '|'));
        node.setText(text);

        int styleCount = Integer.parseInt(readUntil(s, pos, '|'));
        String stylesBlob = readUntil(s, pos, '|');
        parsePairs(stylesBlob, node.getStyles());

        int attrCount = Integer.parseInt(readUntil(s, pos, '|'));
        String attrsBlob = readUntil(s, pos, '|');
        parsePairs(attrsBlob, node.getAttributes());

        int childCount = Integer.parseInt(readUntil(s, pos, '|'));
        for (int i = 0; i < childCount; i++) {
            pos[0]++;
            VisualComponent child = readNode(s, pos);
            pos[0]++;
            node.getChildren().add(child);
        }
        return node;
    }

    private void parsePairs(String blob, Map<String, String> target) {
        if (blob.isEmpty()) return;
        for (String part : blob.split(";", -1)) {
            if (part.isEmpty()) continue;
            int eq = findUnescapedEquals(part);
            if (eq < 0) continue;
            String k = decode(part.substring(0, eq));
            String v = decode(part.substring(eq + 1));
            target.put(k, v);
        }
    }

    private int findUnescapedEquals(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '=' && (i == 0 || s.charAt(i - 1) != '\\')) return i;
        }
        return -1;
    }

    private String readUntil(String s, int[] pos, char delim) {
        int start = pos[0];
        int i = start;
        while (i < s.length() && s.charAt(i) != delim) i++;
        pos[0] = i + 1;
        return s.substring(start, i);
    }

    // ================================================================
    //   THEME
    // ================================================================

    @FXML private void onToggleTheme() {
        ThemeManager.getInstance().toggleTheme();
        positionKnob(true);
    }

    private void positionKnob(boolean animate) {
        boolean dark = ThemeManager.getInstance().isDark();
        double targetX = dark ? -12 : 12;
        if (animate) {
            TranslateTransition tt = new TranslateTransition(Duration.millis(150), toggleKnob);
            tt.setToX(targetX);
            tt.play();
        } else {
            toggleKnob.setTranslateX(targetX);
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(null);
        ThemeManager.getInstance().applyTheme(alert.getDialogPane().getScene());
        alert.showAndWait();
    }
}
