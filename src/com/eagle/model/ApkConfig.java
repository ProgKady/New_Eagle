package com.eagle.model;

import java.io.File;

public class ApkConfig {

    // App identity
    private String appName = "MyApp";
    private String packageName = "com.kadysoft.myapp";
    private File iconFile;
    private String versionName = "1.0";
    private int versionCode = 1;

    // Splash screen
    private boolean enableSplash = true;
    private String splashTitle = "MyApp";
    private String splashSubtitle = "Loading...";
    private String splashBgColor = "#6C5CE7";
    private String splashTextColor = "#FFFFFF";
    private String splashTextColorSecondary = "#CCCCCC";

    // Theme colors
    private String primaryColor = "#6C5CE7";
    private String secondaryColor = "#00B894";
    private String statusBarColor = "#5A4BD1";
    private boolean darkTheme = false;
    private boolean enableNavBar = true;

    // Push notifications (Firebase)
    private boolean enablePush = false;
    private String fcmServerKey = "";
    private String fcmSenderId = "";

    // Ads (AdMob)
    private boolean enableAds = false;
    private String admobAppId = "";
    private String admobBannerId = "ca-app-pub-3940256099942544/6300978111";
    private String admobInterstitialId = "ca-app-pub-3940256099942544/1033173712";
    private boolean enableBannerAd = true;
    private boolean enableInterstitialAd = false;

    // Build options
    private boolean generateAab = false;
    private String keystoreAlias = "eagle";
    private String keystorePass = "eagle710";
    private String keystoreDname = "CN=WebIDE, OU=Development, O=EagleSoft, L=Cairo, C=EG";

    // Source info (set by each controller)
    private String sourceType; // "html", "zip", "url", "js", "pdf", "music", "quiz", "webapp", "website", "javafx"
    private String sourceInput;
    private String extraInput;

    // Permissions
    private boolean internetPermission = true;
    private boolean storagePermission = true;
    private boolean cameraPermission = false;
    private boolean locationPermission = false;
    private boolean microphonePermission = false;
    private boolean contactsPermission = false;
    private boolean smsPermission = false;
    private boolean bluetoothPermission = false;

    // Getters & Setters
    public String getAppName() { return appName; }
    public void setAppName(String v) { this.appName = v; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String v) { this.packageName = v; }

    public File getIconFile() { return iconFile; }
    public void setIconFile(File v) { this.iconFile = v; }

    public String getVersionName() { return versionName; }
    public void setVersionName(String v) { this.versionName = v; }

    public int getVersionCode() { return versionCode; }
    public void setVersionCode(int v) { this.versionCode = v; }

    public boolean isEnableSplash() { return enableSplash; }
    public void setEnableSplash(boolean v) { this.enableSplash = v; }

    public String getSplashTitle() { return splashTitle; }
    public void setSplashTitle(String v) { this.splashTitle = v; }

    public String getSplashSubtitle() { return splashSubtitle; }
    public void setSplashSubtitle(String v) { this.splashSubtitle = v; }

    public String getSplashBgColor() { return splashBgColor; }
    public void setSplashBgColor(String v) { this.splashBgColor = v; }

    public String getSplashTextColor() { return splashTextColor; }
    public void setSplashTextColor(String v) { this.splashTextColor = v; }

    public String getSplashTextColorSecondary() { return splashTextColorSecondary; }
    public void setSplashTextColorSecondary(String v) { this.splashTextColorSecondary = v; }

    public String getPrimaryColor() { return primaryColor; }
    public void setPrimaryColor(String v) { this.primaryColor = v; }

    public String getSecondaryColor() { return secondaryColor; }
    public void setSecondaryColor(String v) { this.secondaryColor = v; }

    public String getStatusBarColor() { return statusBarColor; }
    public void setStatusBarColor(String v) { this.statusBarColor = v; }

    public boolean isDarkTheme() { return darkTheme; }
    public void setDarkTheme(boolean v) { this.darkTheme = v; }

    public boolean isEnableNavBar() { return enableNavBar; }
    public void setEnableNavBar(boolean v) { this.enableNavBar = v; }

    public boolean isEnablePush() { return enablePush; }
    public void setEnablePush(boolean v) { this.enablePush = v; }

    public String getFcmServerKey() { return fcmServerKey; }
    public void setFcmServerKey(String v) { this.fcmServerKey = v; }

    public String getFcmSenderId() { return fcmSenderId; }
    public void setFcmSenderId(String v) { this.fcmSenderId = v; }

    public boolean isEnableAds() { return enableAds; }
    public void setEnableAds(boolean v) { this.enableAds = v; }

    public String getAdmobAppId() { return admobAppId; }
    public void setAdmobAppId(String v) { this.admobAppId = v; }

    public String getAdmobBannerId() { return admobBannerId; }
    public void setAdmobBannerId(String v) { this.admobBannerId = v; }

    public String getAdmobInterstitialId() { return admobInterstitialId; }
    public void setAdmobInterstitialId(String v) { this.admobInterstitialId = v; }

    public boolean isEnableBannerAd() { return enableBannerAd; }
    public void setEnableBannerAd(boolean v) { this.enableBannerAd = v; }

    public boolean isEnableInterstitialAd() { return enableInterstitialAd; }
    public void setEnableInterstitialAd(boolean v) { this.enableInterstitialAd = v; }

    public boolean isGenerateAab() { return generateAab; }
    public void setGenerateAab(boolean v) { this.generateAab = v; }

    public String getKeystoreAlias() { return keystoreAlias; }
    public void setKeystoreAlias(String v) { this.keystoreAlias = v; }

    public String getKeystorePass() { return keystorePass; }
    public void setKeystorePass(String v) { this.keystorePass = v; }

    public String getKeystoreDname() { return keystoreDname; }
    public void setKeystoreDname(String v) { this.keystoreDname = v; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String v) { this.sourceType = v; }

    public String getSourceInput() { return sourceInput; }
    public void setSourceInput(String v) { this.sourceInput = v; }

    public String getExtraInput() { return extraInput; }
    public void setExtraInput(String v) { this.extraInput = v; }

    public boolean isInternetPermission() { return internetPermission; }
    public void setInternetPermission(boolean v) { this.internetPermission = v; }

    public boolean isStoragePermission() { return storagePermission; }
    public void setStoragePermission(boolean v) { this.storagePermission = v; }

    public boolean isCameraPermission() { return cameraPermission; }
    public void setCameraPermission(boolean v) { this.cameraPermission = v; }

    public boolean isLocationPermission() { return locationPermission; }
    public void setLocationPermission(boolean v) { this.locationPermission = v; }

    public boolean isMicrophonePermission() { return microphonePermission; }
    public void setMicrophonePermission(boolean v) { this.microphonePermission = v; }

    public boolean isContactsPermission() { return contactsPermission; }
    public void setContactsPermission(boolean v) { this.contactsPermission = v; }

    public boolean isSmsPermission() { return smsPermission; }
    public void setSmsPermission(boolean v) { this.smsPermission = v; }

    public boolean isBluetoothPermission() { return bluetoothPermission; }
    public void setBluetoothPermission(boolean v) { this.bluetoothPermission = v; }
}
