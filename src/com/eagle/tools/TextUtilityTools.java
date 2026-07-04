package com.eagle.tools;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

public class TextUtilityTools {

    public static void showBase64Encoder() {
        DialogUtil.textInput("Base64 Encoder", "Enter text to encode", "").showAndWait().ifPresent(input -> {
            String result = Base64.getEncoder().encodeToString(input.getBytes());
            DialogUtil.showResult("Base64 Encoded", result);
        });
    }

    public static void showBase64Decoder() {
        DialogUtil.textInput("Base64 Decoder", "Enter Base64 to decode", "").showAndWait().ifPresent(input -> {
            try {
                String result = new String(Base64.getDecoder().decode(input));
                DialogUtil.showResult("Base64 Decoded", result);
            } catch (Exception e) {
                DialogUtil.showError("Error", "Invalid Base64 input");
            }
        });
    }

    public static void showUrlEncoder() {
        DialogUtil.textInput("URL Encoder", "Enter text to URL-encode", "").showAndWait().ifPresent(input -> {
            String result = java.net.URLEncoder.encode(input);
            DialogUtil.showResult("URL Encoded", result);
        });
    }

    public static void showUrlDecoder() {
        DialogUtil.textInput("URL Decoder", "Enter URL-encoded text to decode", "").showAndWait().ifPresent(input -> {
            try {
                String result = java.net.URLDecoder.decode(input);
                DialogUtil.showResult("URL Decoded", result);
            } catch (Exception e) {
                DialogUtil.showError("Error", "Invalid URL-encoded input");
            }
        });
    }

    public static void showUuidGenerator() {
        String uuid = UUID.randomUUID().toString();
        DialogUtil.showResult("UUID Generated", uuid);
    }

    public static void showHashGenerator() {
        DialogUtil.textInput("Hash Generator", "Enter text to hash", "").showAndWait().ifPresent(input -> {
            String[] algos = {"MD5", "SHA-1", "SHA-256", "SHA-512"};
            ChoiceDialog<String> dlg = new ChoiceDialog<>("MD5", algos);
            dlg.setTitle("Hash Generator");
            dlg.setHeaderText("Select hash algorithm");
            if (DialogUtil.getOwnerWindow() != null) dlg.initOwner(DialogUtil.getOwnerWindow());
            dlg.showAndWait().ifPresent(algo -> {
                try {
                    MessageDigest md = MessageDigest.getInstance(algo);
                    byte[] digest = md.digest(input.getBytes());
                    StringBuilder sb = new StringBuilder();
                    for (byte b : digest) sb.append(String.format("%02x", b & 0xff));
                    DialogUtil.showResult(algo + " Hash", sb.toString());
                } catch (Exception e) {
                    DialogUtil.showError("Error", "Hash failed: " + e.getMessage());
                }
            });
        });
    }

    public static void showPasswordGenerator() {
        DialogUtil.textInput("Password Generator", "Enter password length (4-128):", "16").showAndWait().ifPresent(lenStr -> {
            try {
                int len = Integer.parseInt(lenStr);
                if (len < 4 || len > 128) { DialogUtil.showError("Error", "Length must be 4-128"); return; }
                String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+-=[]{}|;:,.<>?";
                SecureRandom rnd = new SecureRandom();
                StringBuilder sb = new StringBuilder(len);
                for (int i = 0; i < len; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
                DialogUtil.showResult("Generated Password", sb.toString());
            } catch (NumberFormatException e) {
                DialogUtil.showError("Error", "Invalid number");
            }
        });
    }

    public static void showTimestampConverter() {
        DialogUtil.textInput("Timestamp Converter", "Enter Unix timestamp (seconds) or leave empty for current time", "").showAndWait().ifPresent(input -> {

            long epoch;
            if (input == null || input.trim().isEmpty()) {
                epoch = System.currentTimeMillis() / 1000;
            } else {
                try { epoch = Long.parseLong(input.trim()); }
                catch (NumberFormatException e) { DialogUtil.showError("Error", "Invalid timestamp"); return; }
            }
            Instant instant = Instant.ofEpochSecond(epoch);
            ZonedDateTime utc = instant.atZone(ZoneOffset.UTC);
            ZonedDateTime local = instant.atZone(ZoneId.systemDefault());
            StringBuilder sb = new StringBuilder();
            sb.append("Unix Timestamp: ").append(epoch).append("\n");
            sb.append("UTC: ").append(utc.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
            sb.append("Local: ").append(local.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
            sb.append("ISO 8601: ").append(instant.toString()).append("\n");
            sb.append("Day of Year: ").append(local.getDayOfYear()).append("\n");
            sb.append("Week of Year: ").append(local.format(DateTimeFormatter.ofPattern("w"))).append("\n");
            DialogUtil.showResult("Timestamp Converter", sb.toString());
        });
    }

    public static void showLoremIpsumGenerator() {
        DialogUtil.textInput("Lorem Ipsum Generator", "Number of paragraphs (1-20):", "3").showAndWait().ifPresent(input -> {
            try {
                int count = Math.min(Math.max(Integer.parseInt(input.trim()), 1), 20);
                StringBuilder result = new StringBuilder();
                String[] sentences = {
                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
                    "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
                    "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris.",
                    "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore.",
                    "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia."
                };
                java.util.Random rnd = new java.util.Random();
                for (int p = 0; p < count; p++) {
                    int sCount = 3 + rnd.nextInt(5);
                    for (int s = 0; s < sCount; s++) {
                        result.append(sentences[rnd.nextInt(sentences.length)]).append(" ");
                    }
                    result.append("\n\n");
                }
                DialogUtil.showResult("Lorem Ipsum", result.toString().trim());
            } catch (NumberFormatException e) {
                DialogUtil.showError("Error", "Invalid number");
            }
        });
    }

    public static void showJwtDecoder() {
        DialogUtil.textInput("JWT Decoder", "Enter JWT token:", "").showAndWait().ifPresent(input -> {
            try {
                String[] parts = input.trim().split("\\.");
                if (parts.length != 3) { DialogUtil.showError("Error", "Invalid JWT format (expected 3 parts)"); return; }
                StringBuilder sb = new StringBuilder();
                sb.append("--- Header ---\n");
                sb.append(formatJson(new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8)));
                sb.append("\n\n--- Payload ---\n");
                sb.append(formatJson(new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)));
                sb.append("\n\n--- Signature ---\n");
                sb.append(parts[2]);
                DialogUtil.showResult("JWT Decoded", sb.toString());
            } catch (Exception e) {
                DialogUtil.showError("Error", "Failed to decode JWT: " + e.getMessage());
            }
        });
    }

    public static void showColorPaletteGenerator() {
        DialogUtil.textInput("Color Palette Generator", "Enter a hex color (e.g. #ff5555):", "#ff5555").showAndWait().ifPresent(input -> {
            try {
                String hex = input.trim().startsWith("#") ? input.trim().substring(1) : input.trim();
                if (hex.length() != 6 && hex.length() != 3) { DialogUtil.showError("Error", "Invalid hex color"); return; }
                if (hex.length() == 3) {
                    hex = "" + hex.charAt(0) + hex.charAt(0) + hex.charAt(1) + hex.charAt(1) + hex.charAt(2) + hex.charAt(2);
                }
                int r = Integer.parseInt(hex.substring(0, 2), 16);
                int g = Integer.parseInt(hex.substring(2, 4), 16);
                int b = Integer.parseInt(hex.substring(4, 6), 16);

                float[] hsl = rgbToHsl(r, g, b);
                StringBuilder sb = new StringBuilder();
                sb.append("Original: #").append(hex.toUpperCase()).append("\n\n");
                sb.append("-- Monochromatic --\n");
                for (int i = 0; i < 5; i++) {
                    float l = 0.2f + i * 0.15f;
                    int[] c = hslToRgb(hsl[0], hsl[1], Math.min(l, 0.9f));
                    sb.append(String.format("  #%02X%02X%02X\n", c[0], c[1], c[2]));
                }
                sb.append("\n-- Complementary --\n");
                int[] comp = hslToRgb((hsl[0] + 0.5f) % 1.0f, hsl[1], hsl[2]);
                sb.append(String.format("  #%02X%02X%02X\n", comp[0], comp[1], comp[2]));
                sb.append("\n-- Analogous --\n");
                for (int i = -2; i <= 2; i++) {
                    if (i == 0) continue;
                    int[] c = hslToRgb((hsl[0] + i * 0.083f + 1.0f) % 1.0f, hsl[1], hsl[2]);
                    sb.append(String.format("  #%02X%02X%02X\n", c[0], c[1], c[2]));
                }
                sb.append("\n-- Triadic --\n");
                for (int i = 0; i < 3; i++) {
                    int[] c = hslToRgb((hsl[0] + i * 0.333f) % 1.0f, hsl[1], hsl[2]);
                    sb.append(String.format("  #%02X%02X%02X\n", c[0], c[1], c[2]));
                }
                sb.append("\n-- Square --\n");
                for (int i = 0; i < 4; i++) {
                    int[] c = hslToRgb((hsl[0] + i * 0.25f) % 1.0f, hsl[1], hsl[2]);
                    sb.append(String.format("  #%02X%02X%02X\n", c[0], c[1], c[2]));
                }
                DialogUtil.showResult("Color Palette", sb.toString());
            } catch (Exception e) {
                DialogUtil.showError("Error", "Failed to generate palette: " + e.getMessage());
            }
        });
    }

    private static float[] rgbToHsl(int r, int g, int b) {
        float rf = r / 255f, gf = g / 255f, bf = b / 255f;
        float max = Math.max(rf, Math.max(gf, bf)), min = Math.min(rf, Math.min(gf, bf));
        float h = 0, s, l = (max + min) / 2f;
        if (max == min) { h = s = 0; }
        else {
            float d = max - min;
            s = l > 0.5f ? d / (2f - max - min) : d / (max + min);
            if (max == rf) h = (gf - bf) / d + (gf < bf ? 6f : 0f);
            else if (max == gf) h = (bf - rf) / d + 2f;
            else h = (rf - gf) / d + 4f;
            h /= 6f;
        }
        return new float[]{h, s, l};
    }

    private static int[] hslToRgb(float h, float s, float l) {
        if (s == 0) { int v = Math.round(l * 255); return new int[]{v, v, v}; }
        float q = l < 0.5f ? l * (1f + s) : l + s - l * s;
        float p = 2f * l - q;
        float[] rgb = {h + 1f / 3f, h, h - 1f / 3f};
        for (int i = 0; i < 3; i++) {
            if (rgb[i] < 0) rgb[i] += 1f;
            if (rgb[i] > 1) rgb[i] -= 1f;
            if (rgb[i] < 1f / 6f) rgb[i] = p + (q - p) * 6f * rgb[i];
            else if (rgb[i] < 0.5f) rgb[i] = q;
            else if (rgb[i] < 2f / 3f) rgb[i] = p + (q - p) * (2f / 3f - rgb[i]) * 6f;
            else rgb[i] = p;
        }
        return new int[]{Math.round(rgb[0] * 255), Math.round(rgb[1] * 255), Math.round(rgb[2] * 255)};
    }

    private static String formatJson(String json) {
        StringBuilder sb = new StringBuilder();
        int indent = 0;
        boolean inString = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') inString = !inString;
            if (!inString) {
                if (c == '{' || c == '[') {
                    sb.append(c).append("\n"); indent++;
                    for (int j = 0; j < indent; j++) sb.append("  "); continue;
                }
                if (c == '}' || c == ']') {
                    sb.append("\n"); indent--;
                    for (int j = 0; j < indent; j++) sb.append("  ");
                    sb.append(c); continue;
                }
                if (c == ',') { sb.append(c).append("\n"); for (int j = 0; j < indent; j++) sb.append("  "); continue; }
                if (c == ':') { sb.append(": "); continue; }
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
