package com.eagle.plugin.builtin;

import java.io.File;
import java.nio.file.Files;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ThreeDViewerController {

    @FXML private VBox viewerContainer;
    @FXML private TextField filePathField;
    @FXML private ComboBox<String> modelFormatChoice;
    @FXML private CheckBox wireframeCheck;
    @FXML private ColorPicker colorPicker;
    @FXML private ColorPicker bgColorPicker;
    @FXML private Label statusLabel;
    @FXML private Button browseBtn;
    @FXML private Button loadBtn;
    @FXML private Button resetCameraBtn;
    @FXML private Button closeBtn;

    private Stage stage;
    private WebView webView;
    private WebEngine engine;

    void setStage(Stage stage) { this.stage = stage; }

    @FXML
    private void initialize() {
        webView = new WebView();
        webView.setPrefSize(700, 450);
        VBox.setVgrow(webView, Priority.ALWAYS);
        viewerContainer.getChildren().add(0, webView);
        engine = webView.getEngine();

        modelFormatChoice.getItems().addAll("glTF/GLB (Binary)", "OBJ", "STL");
        modelFormatChoice.setValue("glTF/GLB (Binary)");

        colorPicker.setValue(javafx.scene.paint.Color.web("#8b5cf6"));
        bgColorPicker.setValue(javafx.scene.paint.Color.web("#1a1a2e"));

        loadBaseHtml();
    }

    private void loadBaseHtml() {
        String html = "<!DOCTYPE html>\n<html>\n<head>\n"
            + "<meta charset=\"UTF-8\">\n"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "<title>3D Viewer</title>\n"
            + "<style>\n"
            + "  * { margin: 0; padding: 0; box-sizing: border-box; }\n"
            + "  body { overflow: hidden; background: #1a1a2e; }\n"
            + "  canvas { display: block; }\n"
            + "  #info { position: absolute; bottom: 12px; left: 50%; transform: translateX(-50%);\n"
            + "    color: rgba(255,255,255,0.5); font: 12px sans-serif;\n"
            + "    background: rgba(0,0,0,0.4); padding: 4px 12px; border-radius: 4px; }\n"
            + "</style>\n</head>\n<body>\n"
            + "<div id=\"info\">Drag to rotate · Scroll to zoom · Right-drag to pan</div>\n"
            + "<script type=\"importmap\">\n"
            + "  { \"imports\": { \"three\": \"https://cdn.jsdelivr.net/npm/three@0.170.0/build/three.module.js\",\n"
            + "    \"three/addons/\": \"https://cdn.jsdelivr.net/npm/three@0.170.0/examples/jsm/\" } }\n"
            + "</script>\n"
            + "<script type=\"module\">\n"
            + "  import * as THREE from 'three';\n"
            + "  import { OrbitControls } from 'three/addons/controls/OrbitControls.js';\n"
            + "  import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';\n"
            + "  import { OBJLoader } from 'three/addons/loaders/OBJLoader.js';\n"
            + "  import { STLLoader } from 'three/addons/loaders/STLLoader.js';\n"
            + "  import { RGBELoader } from 'three/addons/loaders/RGBELoader.js';\n"
            + "\n"
            + "  const scene = new THREE.Scene();\n"
            + "  scene.background = new THREE.Color(0x1a1a2e);\n"
            + "\n"
            + "  const camera = new THREE.PerspectiveCamera(45, 1, 0.1, 1000);\n"
            + "  camera.position.set(5, 5, 5);\n"
            + "\n"
            + "  const renderer = new THREE.WebGLRenderer({ antialias: true });\n"
            + "  renderer.setPixelRatio(window.devicePixelRatio);\n"
            + "  renderer.shadowMap.enabled = true;\n"
            + "  renderer.shadowMap.type = THREE.PCFSoftShadowMap;\n"
            + "  renderer.toneMapping = THREE.ACESFilmicToneMapping;\n"
            + "  renderer.toneMappingExposure = 1.2;\n"
            + "  document.body.appendChild(renderer.domElement);\n"
            + "\n"
            + "  const controls = new OrbitControls(camera, renderer.domElement);\n"
            + "  controls.enableDamping = true;\n"
            + "  controls.dampingFactor = 0.1;\n"
            + "  controls.minDistance = 0.5;\n"
            + "  controls.maxDistance = 50;\n"
            + "\n"
            + "  const ambient = new THREE.AmbientLight(0x404060, 0.6);\n"
            + "  scene.add(ambient);\n"
            + "  const dirLight = new THREE.DirectionalLight(0xffffff, 2);\n"
            + "  dirLight.position.set(10, 15, 10);\n"
            + "  dirLight.castShadow = true;\n"
            + "  scene.add(dirLight);\n"
            + "  const fillLight = new THREE.DirectionalLight(0x8888ff, 0.5);\n"
            + "  fillLight.position.set(-5, 0, 10);\n"
            + "  scene.add(fillLight);\n"
            + "  const rimLight = new THREE.DirectionalLight(0xffffff, 0.4);\n"
            + "  rimLight.position.set(-5, 10, -10);\n"
            + "  scene.add(rimLight);\n"
            + "  const hemi = new THREE.HemisphereLight(0xffffff, 0x080820, 0.5);\n"
            + "  scene.add(hemi);\n"
            + "\n"
            + "  const grid = new THREE.GridHelper(20, 20, 0x444488, 0x333366);\n"
            + "  scene.add(grid);\n"
            + "\n"
            + "  let currentModel = null;\n"
            + "  let wireframeMode = false;\n"
            + "  let modelColor = 0x8b5cf6;\n"
            + "\n"
            + "  function resize() {\n"
            + "    const w = window.innerWidth;\n"
            + "    const h = window.innerHeight;\n"
            + "    camera.aspect = w / h;\n"
            + "    camera.updateProjectionMatrix();\n"
            + "    renderer.setSize(w, h);\n"
            + "  }\n"
            + "  window.addEventListener('resize', resize);\n"
            + "  resize();\n"
            + "\n"
            + "  function animate() {\n"
            + "    requestAnimationFrame(animate);\n"
            + "    controls.update();\n"
            + "    renderer.render(scene, camera);\n"
            + "  }\n"
            + "  animate();\n"
            + "\n"
            + "  function setModelColor(hex) {\n"
            + "    modelColor = hex;\n"
            + "    if (currentModel) {\n"
            + "      currentModel.traverse(child => {\n"
            + "        if (child.isMesh) {\n"
            + "          child.material.color.setHex(hex);\n"
            + "        }\n"
            + "      });\n"
            + "    }\n"
            + "  }\n"
            + "\n"
            + "  function setWireframe(wf) {\n"
            + "    wireframeMode = wf;\n"
            + "    if (currentModel) {\n"
            + "      currentModel.traverse(child => {\n"
            + "        if (child.isMesh) {\n"
            + "          child.material.wireframe = wf;\n"
            + "        }\n"
            + "      });\n"
            + "    }\n"
            + "  }\n"
            + "\n"
            + "  function setBgColor(hex) {\n"
            + "    scene.background = new THREE.Color(hex);\n"
            + "  }\n"
            + "\n"
            + "  function fitCamera(obj) {\n"
            + "    const box = new THREE.Box3().setFromObject(obj);\n"
            + "    const center = box.getCenter(new THREE.Vector3());\n"
            + "    const size = box.getSize(new THREE.Vector3());\n"
            + "    const maxDim = Math.max(size.x, size.y, size.z);\n"
            + "    const dist = maxDim * 2.5;\n"
            + "    camera.position.set(center.x + dist * 0.7, center.y + dist * 0.5, center.z + dist);\n"
            + "    controls.target.copy(center);\n"
            + "    controls.update();\n"
            + "  }\n"
            + "\n"
            + "  function clearModel() {\n"
            + "    if (currentModel) {\n"
            + "      scene.remove(currentModel);\n"
            + "      currentModel = null;\n"
            + "    }\n"
            + "  }\n"
            + "\n"
            + "  window.setModelColor = setModelColor;\n"
            + "  window.setWireframe = setWireframe;\n"
            + "  window.setBgColor = setBgColor;\n"
            + "  window.fitCamera = fitCamera;\n"
            + "  window.clearModel = clearModel;\n"
            + "  window.getModel = function() { return currentModel; };\n"
            + "\n"
            + "  window.loadGltf = function(base64, filename) {\n"
            + "    clearModel();\n"
            + "    const binary = atob(base64);\n"
            + "    const array = new Uint8Array(binary.length);\n"
            + "    for (let i = 0; i < binary.length; i++) array[i] = binary.charCodeAt(i);\n"
            + "    const blob = new Blob([array], { type: 'model/gltf-binary' });\n"
            + "    const url = URL.createObjectURL(blob);\n"
            + "    const loader = new GLTFLoader();\n"
            + "    loader.load(url, gltf => {\n"
            + "      currentModel = gltf.scene;\n"
            + "      currentModel.traverse(c => { if (c.isMesh) { c.castShadow = true; c.receiveShadow = true; c.material.color.setHex(modelColor); } });\n"
            + "      scene.add(currentModel);\n"
            + "      fitCamera(currentModel);\n"
            + "      URL.revokeObjectURL(url);\n"
            + "    }, undefined, err => { console.error(err); });\n"
            + "  };\n"
            + "\n"
            + "  window.loadObj = function(base64) {\n"
            + "    clearModel();\n"
            + "    const binary = atob(base64);\n"
            + "    const bytes = new Uint8Array(binary.length);\n"
            + "    for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);\n"
            + "    const text = new TextDecoder('utf-8').decode(bytes);\n"
            + "    const loader = new OBJLoader();\n"
            + "    const obj = loader.parse(text);\n"
            + "    currentModel = obj;\n"
            + "    currentModel.traverse(c => { if (c.isMesh) { c.castShadow = true; c.receiveShadow = true; c.material.color.setHex(modelColor); } });\n"
            + "    scene.add(currentModel);\n"
            + "    fitCamera(currentModel);\n"
            + "  };\n"
            + "\n"
            + "  window.loadStl = function(base64) {\n"
            + "    clearModel();\n"
            + "    const binary = atob(base64);\n"
            + "    const array = new Uint8Array(binary.length);\n"
            + "    for (let i = 0; i < binary.length; i++) array[i] = binary.charCodeAt(i);\n"
            + "    const loader = new STLLoader();\n"
            + "    const geom = loader.parse(array.buffer);\n"
            + "    const mat = new THREE.MeshStandardMaterial({ color: modelColor, roughness: 0.5, metalness: 0.3 });\n"
            + "    const mesh = new THREE.Mesh(geom, mat);\n"
            + "    mesh.castShadow = true; mesh.receiveShadow = true;\n"
            + "    currentModel = new THREE.Group();\n"
            + "    currentModel.add(mesh);\n"
            + "    scene.add(currentModel);\n"
            + "    fitCamera(currentModel);\n"
            + "  };\n"
            + "</script>\n</body>\n</html>";

        engine.loadContent(html, "text/html");
    }

    @FXML
    private void onBrowse() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Open 3D Model");
        String fmt = modelFormatChoice.getValue();
        if (fmt.contains("glTF")) {
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("glTF/GLB", "*.glb", "*.gltf"));
        } else if (fmt.contains("OBJ")) {
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("OBJ", "*.obj"));
        } else {
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("STL", "*.stl"));
        }
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("All 3D Models", "*.glb", "*.gltf", "*.obj", "*.stl"));
        File f = fc.showOpenDialog(stage);
        if (f != null) {
            filePathField.setText(f.getAbsolutePath());
        }
    }

    @FXML
    private void onLoad() {
        String path = filePathField.getText().trim();
        if (path.isEmpty()) { statusLabel.setText("Select a model file first."); return; }
        File f = new File(path);
        if (!f.exists()) { statusLabel.setText("File not found."); return; }
        try {
            byte[] data = Files.readAllBytes(f.toPath());
            String base64 = java.util.Base64.getEncoder().encodeToString(data);
            String ext = path.substring(path.lastIndexOf('.') + 1).toLowerCase();
            String js;

            if (ext.equals("glb") || ext.equals("gltf")) {
                engine.executeScript("window.loadGltf('" + base64 + "', '" + f.getName() + "')");
            } else if (ext.equals("obj")) {
                engine.executeScript("window.loadObj('" + base64 + "')");
            } else if (ext.equals("stl")) {
                engine.executeScript("window.loadStl('" + base64 + "')");
            } else {
                statusLabel.setText("Unsupported format: ." + ext);
                return;
            }

            statusLabel.setText("Loaded: " + f.getName() + " (" + data.length + " bytes)");
        } catch (Exception ex) {
            statusLabel.setText("Load error: " + ex.getMessage());
        }
    }

    @FXML
    private void onResetCamera() {
        engine.executeScript("var m = window.getModel(); if (m) window.fitCamera(m);");
        statusLabel.setText("Camera reset");
    }

    @FXML
    private void onWireframeToggle() {
        boolean wf = wireframeCheck.isSelected();
        engine.executeScript("window.setWireframe(" + wf + ")");
        statusLabel.setText(wf ? "Wireframe on" : "Wireframe off");
    }

    @FXML
    private void onColorChange() {
        String hex = colorPicker.getValue().toString().substring(2, 8);
        engine.executeScript("window.setModelColor(0x" + hex + ")");
        statusLabel.setText("Color updated");
    }

    @FXML
    private void onBgColorChange() {
        String hex = bgColorPicker.getValue().toString().substring(2, 8);
        engine.executeScript("window.setBgColor(0x" + hex + ")");
        statusLabel.setText("Background color updated");
    }

    @FXML
    private void onClose() {
        if (stage != null) stage.close();
    }
}
