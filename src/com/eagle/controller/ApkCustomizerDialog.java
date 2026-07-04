package com.eagle.controller;

import com.eagle.model.ApkConfig;
import com.eagle.util.ThemeManager;
import java.io.File;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ApkCustomizerDialog {

    private final ApkConfig config;
    private final Stage owner;

    public ApkCustomizerDialog(Stage owner, ApkConfig config) {
        this.owner = owner;
        this.config = config;
    }

    public void show() {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Customize App \u2014 " + config.getAppName());

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("customizer-tabs");

        tabs.getTabs().add(createAppInfoTab());
        tabs.getTabs().add(createAppearanceTab());
        tabs.getTabs().add(createPermissionsTab());
        //tabs.getTabs().add(createFeaturesTab());
        tabs.getTabs().add(createBuildTab());

        Button okBtn = new Button("OK");
        okBtn.setDefaultButton(true);
        okBtn.getStyleClass().add("btn-primary");
        okBtn.setOnAction(e -> dialog.close());

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> dialog.close());

        HBox buttons = new HBox(10, cancelBtn, okBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(14, 20, 16, 20));
        buttons.getStyleClass().add("customizer-footer");

        VBox root = new VBox(tabs, buttons);
        root.setPrefSize(1100, 800);
        root.getStyleClass().add("customizer-root");

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/com/eagle/css/base.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/com/eagle/css/apk-dialog.css").toExternalForm());
        ThemeManager.getInstance().applyTheme(scene);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // ----------------------------------------------------------------
    // UTILITY: build a labeled field card
    // ----------------------------------------------------------------
    private VBox groupCard(String title) {
        VBox card = new VBox(10);
        card.getStyleClass().add("customizer-card");
        if (title != null) {
            Label lbl = new Label(title);
            lbl.getStyleClass().add("customizer-card-title");
            card.getChildren().add(lbl);
        }
        return card;
    }

    private VBox fieldRow(String labelText, javafx.scene.Node control) {
        VBox row = new VBox(4);
        row.getStyleClass().add("customizer-field");
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("customizer-field-label");
        row.getChildren().addAll(lbl, control);
        return row;
    }

    private HBox inlineRow(javafx.scene.Node... nodes) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(nodes);
        return row;
    }

    // ----------------------------------------------------------------
    // TAB 1 : App Info
    // ----------------------------------------------------------------
    private Tab createAppInfoTab() {
        ScrollPane sp = newScrollPane();
        VBox box = new VBox(16);
        box.setPadding(new Insets(20));

        VBox idCard = groupCard("IDENTITY");
        TextField nameField = new TextField(config.getAppName());
        nameField.setPromptText("MyApp");
        nameField.textProperty().addListener((o, a, b) -> config.setAppName(b));
        idCard.getChildren().add(fieldRow("App Name", nameField));

        TextField pkgField = new TextField(config.getPackageName());
        pkgField.setPromptText("com.kadysoft.myapp");
        pkgField.textProperty().addListener((o, a, b) -> config.setPackageName(b));
        idCard.getChildren().add(fieldRow("Package Name", pkgField));

        HBox verRow = new HBox(16);
        TextField verField = new TextField(config.getVersionName());
        verField.setPromptText("1.0");
        verField.setPrefColumnCount(8);
        verField.textProperty().addListener((o, a, b) -> config.setVersionName(b));
        VBox verCol = fieldRow("Version", verField);

        Spinner<Integer> codeSpin = new Spinner<>(1, 9999, config.getVersionCode());
        codeSpin.setEditable(true);
        codeSpin.setPrefWidth(110);
        VBox codeCol = fieldRow("Version Code", codeSpin);

        verRow.getChildren().addAll(verCol, codeCol);
        idCard.getChildren().add(verRow);

        box.getChildren().add(idCard);

        // Icon preview (read-only, no picker)
        VBox iconCard = groupCard("APP ICON");
        HBox iconRow = new HBox(16);
        iconRow.setAlignment(Pos.CENTER_LEFT);
        ImageView iconPreview = new ImageView();
        iconPreview.setFitWidth(52);
        iconPreview.setFitHeight(52);
        if (config.getIconFile() != null) {
            try { iconPreview.setImage(new Image(config.getIconFile().toURI().toString())); } catch (Exception ignored) { }
        }
        Label iconLabel = new Label("Icon selected from main screen");
        iconLabel.getStyleClass().add("customizer-hint");
        iconRow.getChildren().addAll(iconPreview, iconLabel);
        iconCard.getChildren().add(iconRow);
        box.getChildren().add(iconCard);

        sp.setContent(box);
        return new Tab("App Info", sp);
    }

    // ----------------------------------------------------------------
    // TAB 2 : Appearance (Splash + Colors)
    // ----------------------------------------------------------------
    private Tab createAppearanceTab() {
        ScrollPane sp = newScrollPane();
        VBox box = new VBox(16);
        box.setPadding(new Insets(20));

        // ---- Splash section ----
        VBox splashCard = groupCard("SPLASH SCREEN");

        ToggleGroup splashGroup = new ToggleGroup();
        RadioButton splashNone = new RadioButton("No Splash");
        splashNone.setToggleGroup(splashGroup);
        RadioButton splashColor = new RadioButton("Solid Color");
        splashColor.setToggleGroup(splashGroup);
        RadioButton splashImg = new RadioButton("Image");
        splashImg.setToggleGroup(splashGroup);

        splashNone.getStyleClass().add("customizer-radio");
        splashColor.getStyleClass().add("customizer-radio");
        splashImg.getStyleClass().add("customizer-radio");

        HBox radioRow = new HBox(16);
        radioRow.setAlignment(Pos.CENTER_LEFT);
        radioRow.getChildren().addAll(splashNone, splashColor, splashImg);
        splashCard.getChildren().add(radioRow);

        // Restore state
        if (!config.isEnableSplash()) splashNone.setSelected(true);
        else splashColor.setSelected(true);

        // --- Splash controls (will toggle visibility) ---
        TextField splashTitleField = new TextField(config.getSplashTitle());
        splashTitleField.setPromptText("App Title shown on splash");
        splashTitleField.textProperty().addListener((o, a, b) -> config.setSplashTitle(b));
        VBox titleRow = fieldRow("Title", splashTitleField);

        Spinner<Integer> timerSpin = new Spinner<>(1, 10, 2);
        timerSpin.setEditable(true);
        timerSpin.setPrefWidth(100);
        VBox timerRow = fieldRow("Duration (seconds)", timerSpin);

        // Color bg section (visible only when Solid Color selected)
        ColorPicker splashBgPicker = new ColorPicker();
        try { splashBgPicker.setValue(javafx.scene.paint.Color.valueOf(config.getSplashBgColor())); } catch (Exception ignored) { }
        splashBgPicker.valueProperty().addListener((o, a, b) -> config.setSplashBgColor(toHex(b)));
        VBox colorBgRow = fieldRow("Background Color", splashBgPicker);

        // Image bg section (visible only when Image selected)
        HBox imgRow = new HBox(12);
        imgRow.setAlignment(Pos.CENTER_LEFT);
        ImageView splashPreview = new ImageView();
        splashPreview.setFitWidth(64);
        splashPreview.setFitHeight(64);
        Button splashImgBtn = new Button("Choose Image\u2026");
        splashImgBtn.setOnAction(e -> {
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setTitle("Select Splash Background Image");
            fc.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"),
                new javafx.stage.FileChooser.ExtensionFilter("PNG", "*.png"),
                new javafx.stage.FileChooser.ExtensionFilter("JPEG", "*.jpg", "*.jpeg")
            );
            File f = fc.showOpenDialog(owner);
            if (f != null) {
                config.setSplashBgColor("IMAGE:" + f.getAbsolutePath());
                try { splashPreview.setImage(new Image(f.toURI().toString())); } catch (Exception ignored) { }
            }
        });
        imgRow.getChildren().addAll(splashImgBtn, splashPreview);
        VBox imgField = fieldRow("Background Image", imgRow);

        // Text colors (always visible when splash enabled)
        ColorPicker splashTextPicker = new ColorPicker();
        try { splashTextPicker.setValue(javafx.scene.paint.Color.valueOf(config.getSplashTextColor())); } catch (Exception ignored) { }
        splashTextPicker.valueProperty().addListener((o, a, b) -> config.setSplashTextColor(toHex(b)));
        VBox textColorRow = fieldRow("Title Color", splashTextPicker);

        ColorPicker splashSecPicker = new ColorPicker();
        try { splashSecPicker.setValue(javafx.scene.paint.Color.valueOf(config.getSplashTextColorSecondary())); } catch (Exception ignored) { }
        splashSecPicker.valueProperty().addListener((o, a, b) -> config.setSplashTextColorSecondary(toHex(b)));
        VBox secColorRow = fieldRow("Subtitle Color", splashSecPicker);

        // Group all splash controls so we can toggle them together
        VBox splashControls = new VBox(10);
        splashControls.getChildren().addAll(
            titleRow, timerRow, colorBgRow, imgField, textColorRow, secColorRow
        );
        splashCard.getChildren().add(splashControls);

        // --- Toggle visibility based on selected radio ---
        splashGroup.selectedToggleProperty().addListener((o, a, b) -> {
            boolean none = (b == splashNone);
            boolean solid = (b == splashColor);
            boolean image = (b == splashImg);

            config.setEnableSplash(!none);

            splashControls.setVisible(!none);
            splashControls.setManaged(!none);

            colorBgRow.setVisible(solid);
            colorBgRow.setManaged(solid);

            imgField.setVisible(image);
            imgField.setManaged(image);
        });
        // Apply initial state
        javafx.application.Platform.runLater(() -> {
            Toggle sel = splashGroup.getSelectedToggle();
            if (sel != null) {
                boolean none = (sel == splashNone);
                boolean solid = (sel == splashColor);
                boolean image = (sel == splashImg);
                splashControls.setVisible(!none);
                splashControls.setManaged(!none);
                colorBgRow.setVisible(solid);
                colorBgRow.setManaged(solid);
                imgField.setVisible(image);
                imgField.setManaged(image);
            }
        });

        box.getChildren().add(splashCard);

        // ---- Colors section ----
        VBox colorsCard = groupCard("APP THEME COLORS");

        ColorPicker primaryPicker = new ColorPicker();
        try { primaryPicker.setValue(javafx.scene.paint.Color.valueOf(config.getPrimaryColor())); } catch (Exception ignored) { }
        primaryPicker.valueProperty().addListener((o, a, b) -> config.setPrimaryColor(toHex(b)));
        colorsCard.getChildren().add(fieldRow("Primary Color", primaryPicker));

        ColorPicker secondaryPicker = new ColorPicker();
        try { secondaryPicker.setValue(javafx.scene.paint.Color.valueOf(config.getSecondaryColor())); } catch (Exception ignored) { }
        secondaryPicker.valueProperty().addListener((o, a, b) -> config.setSecondaryColor(toHex(b)));
        colorsCard.getChildren().add(fieldRow("Secondary Color", secondaryPicker));

        ColorPicker statusPicker = new ColorPicker();
        try { statusPicker.setValue(javafx.scene.paint.Color.valueOf(config.getStatusBarColor())); } catch (Exception ignored) { }
        statusPicker.valueProperty().addListener((o, a, b) -> config.setStatusBarColor(toHex(b)));
        colorsCard.getChildren().add(fieldRow("Status Bar Color", statusPicker));

        CheckBox darkCb = new CheckBox("Use dark status bar icons (light background)");
        darkCb.setSelected(config.isDarkTheme());
        darkCb.selectedProperty().addListener((o, a, b) -> config.setDarkTheme(b));
        VBox cbRow = new VBox(4);
        cbRow.getChildren().add(darkCb);
        colorsCard.getChildren().add(cbRow);

        box.getChildren().add(colorsCard);

        sp.setContent(box);
        return new Tab("Appearance", sp);
    }

    // ----------------------------------------------------------------
    // TAB 3 : Permissions
    // ----------------------------------------------------------------
    private Tab createPermissionsTab() {
        ScrollPane sp = newScrollPane();
        VBox box = new VBox(16);
        box.setPadding(new Insets(20));

        VBox permCard = groupCard("ANDROID PERMISSIONS");

        Label hint = new Label("Choose which permissions your app needs:");
        hint.getStyleClass().add("customizer-hint");
        permCard.getChildren().add(hint);

        CheckBox internetCb = new CheckBox("Internet");
        internetCb.setSelected(config.isInternetPermission());
        internetCb.selectedProperty().addListener((o, a, b) -> config.setInternetPermission(b));

        CheckBox storageCb = new CheckBox("Storage (Read/Write files)");
        storageCb.setSelected(config.isStoragePermission());
        storageCb.selectedProperty().addListener((o, a, b) -> config.setStoragePermission(b));

        CheckBox cameraCb = new CheckBox("Camera");
        cameraCb.setSelected(config.isCameraPermission());
        cameraCb.selectedProperty().addListener((o, a, b) -> config.setCameraPermission(b));

        CheckBox locationCb = new CheckBox("Location (GPS)");
        locationCb.setSelected(config.isLocationPermission());
        locationCb.selectedProperty().addListener((o, a, b) -> config.setLocationPermission(b));

        CheckBox microphoneCb = new CheckBox("Microphone / Audio Record");
        microphoneCb.setSelected(config.isMicrophonePermission());
        microphoneCb.selectedProperty().addListener((o, a, b) -> config.setMicrophonePermission(b));

        CheckBox contactsCb = new CheckBox("Read Contacts");
        contactsCb.setSelected(config.isContactsPermission());
        contactsCb.selectedProperty().addListener((o, a, b) -> config.setContactsPermission(b));

        CheckBox smsCb = new CheckBox("Send SMS");
        smsCb.setSelected(config.isSmsPermission());
        smsCb.selectedProperty().addListener((o, a, b) -> config.setSmsPermission(b));
        CheckBox notificationsCb = new CheckBox("Notifications (Android 13+)");
        notificationsCb.setSelected(config.isEnablePush());
        CheckBox bluetoothCb = new CheckBox("Bluetooth");
        bluetoothCb.setSelected(config.isBluetoothPermission());
        bluetoothCb.selectedProperty().addListener((o, a, b) -> config.setBluetoothPermission(b));

        CheckBox vibrateCb = new CheckBox("Vibrate");
        vibrateCb.setSelected(true);

        VBox permList = new VBox(8);
        permList.setPadding(new Insets(4, 0, 0, 0));
        permList.getChildren().addAll(
            internetCb, storageCb, cameraCb, locationCb, microphoneCb,
            contactsCb, smsCb, notificationsCb, bluetoothCb, vibrateCb
        );
        permCard.getChildren().add(permList);

        box.getChildren().add(permCard);
        sp.setContent(box);
        return new Tab("Permissions", sp);
    }

    // ----------------------------------------------------------------
    // TAB 4 : Features (Push + Ads)
    // ----------------------------------------------------------------
    private Tab createFeaturesTab() {
        ScrollPane sp = newScrollPane();
        VBox box = new VBox(16);
        box.setPadding(new Insets(20));

        // ---- Push ----
        VBox pushCard = groupCard("PUSH NOTIFICATIONS (FIREBASE)");

        CheckBox pushCb = new CheckBox("Enable Push Notifications");
        pushCb.setSelected(config.isEnablePush());
        pushCb.selectedProperty().addListener((o, a, b) -> config.setEnablePush(b));
        pushCard.getChildren().add(pushCb);

        TextField senderField = new TextField(config.getFcmSenderId());
        senderField.setPromptText("Firebase Sender ID");
        senderField.textProperty().addListener((o, a, b) -> config.setFcmSenderId(b));
        senderField.disableProperty().bind(pushCb.selectedProperty().not());
        pushCard.getChildren().add(fieldRow("Sender ID", senderField));

        TextField serverKeyField = new TextField(config.getFcmServerKey());
        serverKeyField.setPromptText("Firebase Server Key (for sending from server)");
        serverKeyField.textProperty().addListener((o, a, b) -> config.setFcmServerKey(b));
        serverKeyField.disableProperty().bind(pushCb.selectedProperty().not());
        pushCard.getChildren().add(fieldRow("Server Key", serverKeyField));

        Label pushNote = new Label("Requires google-services.json in the project.\n"
            + "Add FCM dependency to build.gradle for full functionality.\n"
            + "The generated service handles basic notification display.");
        pushNote.setWrapText(true);
        pushNote.getStyleClass().add("customizer-note");
        pushCard.getChildren().add(pushNote);

        box.getChildren().add(pushCard);

        // ---- Ads ----
        VBox adsCard = groupCard("ADVERTISEMENTS (ADMOB)");

        CheckBox adsCb = new CheckBox("Enable Ads");
        adsCb.setSelected(config.isEnableAds());
        adsCb.selectedProperty().addListener((o, a, b) -> config.setEnableAds(b));
        adsCard.getChildren().add(adsCb);

        TextField appIdField = new TextField(config.getAdmobAppId());
        appIdField.setPromptText("AdMob App ID (ca-app-pub-xxx~xxx)");
        appIdField.textProperty().addListener((o, a, b) -> config.setAdmobAppId(b));
        appIdField.disableProperty().bind(adsCb.selectedProperty().not());
        adsCard.getChildren().add(fieldRow("App ID", appIdField));

        TextField bannerField = new TextField(config.getAdmobBannerId());
        bannerField.setPromptText("Banner Ad Unit ID");
        bannerField.textProperty().addListener((o, a, b) -> config.setAdmobBannerId(b));
        bannerField.disableProperty().bind(adsCb.selectedProperty().not());
        adsCard.getChildren().add(fieldRow("Banner ID", bannerField));

        TextField interField = new TextField(config.getAdmobInterstitialId());
        interField.setPromptText("Interstitial Ad Unit ID");
        interField.textProperty().addListener((o, a, b) -> config.setAdmobInterstitialId(b));
        interField.disableProperty().bind(adsCb.selectedProperty().not());
        adsCard.getChildren().add(fieldRow("Interstitial ID", interField));

        CheckBox bannerCb = new CheckBox("Show Banner Ad (bottom of screen)");
        bannerCb.setSelected(config.isEnableBannerAd());
        bannerCb.selectedProperty().addListener((o, a, b) -> config.setEnableBannerAd(b));
        bannerCb.disableProperty().bind(adsCb.selectedProperty().not());
        adsCard.getChildren().add(bannerCb);

        CheckBox interCb = new CheckBox("Show Interstitial Ad (between pages)");
        interCb.setSelected(config.isEnableInterstitialAd());
        interCb.selectedProperty().addListener((o, a, b) -> config.setEnableInterstitialAd(b));
        interCb.disableProperty().bind(adsCb.selectedProperty().not());
        adsCard.getChildren().add(interCb);

        Label adsNote = new Label("Ads require Google Play Services (Google Mobile Ads SDK).\n"
            + "Test IDs are used by default. Replace with your real AdMob IDs before publishing.");
        adsNote.setWrapText(true);
        adsNote.getStyleClass().add("customizer-note");
        adsCard.getChildren().add(adsNote);

        box.getChildren().add(adsCard);
        sp.setContent(box);
        return new Tab("Features", sp);
        //return new Tab("", null);
        //return null;
        
    }

    // ----------------------------------------------------------------
    // TAB 5 : Build Options
    // ----------------------------------------------------------------
    private Tab createBuildTab() {
        ScrollPane sp = newScrollPane();
        VBox box = new VBox(16);
        box.setPadding(new Insets(20));

        VBox buildCard = groupCard("BUILD OUTPUT");
        
        CheckBox aabCb = new CheckBox("Generate AAB (Android App Bundle)");
        aabCb.setSelected(config.isGenerateAab());
        aabCb.selectedProperty().addListener((o, a, b) -> config.setGenerateAab(b));
        buildCard.getChildren().add(aabCb);

        Label aabNote = new Label("AAB is required for Google Play Store. APK works everywhere else.\n"
            + "AAB generation requires bundletool.jar in the tools directory.");
        aabNote.setWrapText(true);
        aabNote.getStyleClass().add("customizer-note");
        buildCard.getChildren().add(aabNote);

        box.getChildren().add(buildCard);

        VBox ksCard = groupCard("CODE SIGNING");

        TextField ksPathField = new TextField(config.getKeystorePass().isEmpty() ? "tools/eagle.keystore" : "");
        ksPathField.setPromptText("Keystore path (leave empty for default)");
        ksPathField.textProperty().addListener((o, a, b) -> {
            if (!b.isEmpty()) config.setKeystorePass(b);
        });
        ksCard.getChildren().add(fieldRow("Keystore Path", ksPathField));

        TextField ksAliasField = new TextField(config.getKeystoreAlias());
        ksAliasField.setPromptText("eagle");
        ksAliasField.textProperty().addListener((o, a, b) -> config.setKeystoreAlias(b));
        ksCard.getChildren().add(fieldRow("Alias", ksAliasField));

        PasswordField ksPassField = new PasswordField();
        ksPassField.setPromptText("Keystore Password");
        ksPassField.textProperty().addListener((o, a, b) -> config.setKeystorePass(b));
        ksCard.getChildren().add(fieldRow("Password", ksPassField));

        box.getChildren().add(ksCard);

        VBox advCard = groupCard("ADVANCED");

        CheckBox debuggableCb = new CheckBox("Debuggable APK");
        debuggableCb.setSelected(false);
        advCard.getChildren().add(debuggableCb);
        Label debNote = new Label("Disable for release builds on Google Play.");
        debNote.getStyleClass().add("customizer-note");
        advCard.getChildren().add(debNote);

        CheckBox fullscreenCb = new CheckBox("Fullscreen (hide status bar)");
        fullscreenCb.setSelected(true);
        advCard.getChildren().add(fullscreenCb);

        CheckBox hardwareAccelCb = new CheckBox("Hardware Acceleration");
        hardwareAccelCb.setSelected(true);
        advCard.getChildren().add(hardwareAccelCb);

        CheckBox cleartextCb = new CheckBox("Allow HTTP (cleartext traffic)");
        cleartextCb.setSelected(true);
        advCard.getChildren().add(cleartextCb);

        if (config.getSourceType() != null) {
            Separator sep = new Separator();
            advCard.getChildren().add(sep);
            Label srcType = new Label("Source: " + config.getSourceType());
            srcType.getStyleClass().add("customizer-source-label");
            advCard.getChildren().add(srcType);
        }

        box.getChildren().add(advCard);
        sp.setContent(box);
        return new Tab("Build", sp);
    }

    // ----------------------------------------------------------------
    // UTILITY
    // ----------------------------------------------------------------
    private static ScrollPane newScrollPane() {
        ScrollPane sp = new ScrollPane();
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        sp.getStyleClass().add("customizer-scroll");
        return sp;
    }

    private static String toHex(javafx.scene.paint.Color c) {
        int r = (int) Math.round(c.getRed() * 255);
        int g = (int) Math.round(c.getGreen() * 255);
        int b = (int) Math.round(c.getBlue() * 255);
        return String.format("#%02X%02X%02X", r, g, b);
    }
}
