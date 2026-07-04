package com.eagle.tools;

import com.eagle.util.ThemeManager;
import com.google.gson.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.*;
import javafx.util.Duration;
import javax.sound.sampled.*;

public class VoiceToCode {

    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.webide";
    private static final Gson GSON = new Gson();

    private static volatile boolean recording = false;
    private static Thread recordThread;
    private static TargetDataLine micLine;
    private static ByteArrayOutputStream audioBuffer;
    private static final List<Double> waveformBuffer = new ArrayList<>();
    private static Timeline waveformAnim;

    // AI Provider config from ai.properties
    private static String providerType = "gemini";
    private static String geminiKey = "";
    private static String geminiModel = "gemini-2.0-flash";
    private static String openaiEndpoint = "https://api.openai.com/v1/chat/completions";
    private static String openaiKey = "";
    private static String openaiModel = "gpt-4o-audio-preview";
    private static String ollamaEndpoint = "http://localhost:11434/api/generate";
    private static String ollamaModel = "llama3.2-vision";

    public static void show(Window owner) {
        loadConfig();

        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.setTitle("Voice to Code");
        stage.setMinWidth(550);
        stage.setMinHeight(600);

        Label status = new Label("Record audio or drop an audio file to transcribe via AI.");
        status.setWrapText(true);
        status.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-primary;");

        Canvas waveform = new Canvas(510, 60);
        GraphicsContext gc = waveform.getGraphicsContext2D();
        gc.setFill(Color.rgb(30, 30, 50));
        gc.fillRect(0, 0, 510, 60);

        Button recordBtn = new Button();
        recordBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 10 20; -fx-cursor: hand; -fx-background-radius: 8;");
        recordBtn.setText("Start Recording");

        Label timerLabel = new Label("0:00");
        timerLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: -text-muted;");

        TextArea result = new TextArea();
        result.setPromptText("Transcribed text and generated code will appear here...");
        result.setPrefRowCount(8);
        result.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12px;");

        // Drop zone for audio files
        Label dropZone = new Label("Drop audio file here (.wav, .mp3, .m4a, .ogg)");
        dropZone.setStyle("-fx-background-color: -bg-tertiary; -fx-border-color: -accent; -fx-border-style: dashed; -fx-border-radius: 6; -fx-padding: 8; -fx-alignment: center; -fx-font-size: 11px;");
        dropZone.setAlignment(Pos.CENTER);
        dropZone.setMaxWidth(Double.MAX_VALUE);
        dropZone.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) e.acceptTransferModes(TransferMode.COPY);
        });
        dropZone.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasFiles()) {
                File audioFile = db.getFiles().get(0);
                processAudioFile(audioFile, result, status);
            }
        });

        Button loadFileBtn = new Button("Load Audio File");
        loadFileBtn.setTooltip(new Tooltip("Select an audio file from disk"));
        loadFileBtn.setOnAction(ev -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Audio File");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Audio Files", "*.wav", "*.mp3", "*.m4a", "*.ogg", "*.flac", "*.webm"));
            File f = fc.showOpenDialog(stage);
            if (f != null) processAudioFile(f, result, status);
        });

        ComboBox<String> aiProviderBox = new ComboBox<>();
        aiProviderBox.getItems().addAll("gemini", "openai", "ollama");
        aiProviderBox.setValue(providerType);
        aiProviderBox.setOnAction(ev -> {
            providerType = aiProviderBox.getValue();
            saveConfig();
        });

        HBox actionBar = new HBox(6);
        actionBar.setAlignment(Pos.CENTER);

        Button copyBtn = new Button("Copy");
        copyBtn.setStyle("-fx-background-color: -bg-tertiary; -fx-text-fill: -text-primary; -fx-cursor: hand; -fx-font-size: 11px;");
        copyBtn.setOnAction(ev -> {
            String text = result.getText();
            if (text != null && !text.isEmpty()) {
                ClipboardContent cc = new ClipboardContent();
                cc.putString(text);
                Clipboard.getSystemClipboard().setContent(cc);
                status.setText("Copied");
            }
        });

        Button clearBtn = new Button("Clear");
        clearBtn.setStyle("-fx-background-color: -bg-tertiary; -fx-text-fill: -text-primary; -fx-cursor: hand; -fx-font-size: 11px;");
        clearBtn.setOnAction(ev -> result.clear());

        Button sendToEditorBtn = new Button("Send to Editor");
        sendToEditorBtn.setStyle("-fx-background-color: -accent; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 11px;");
        sendToEditorBtn.setOnAction(ev -> {
            String text = result.getText();
            if (text != null && !text.isEmpty()) {
                ClipboardContent cc = new ClipboardContent();
                cc.putString(text);
                Clipboard.getSystemClipboard().setContent(cc);
                status.setText("Copied to clipboard — paste in editor");
            }
        });

        actionBar.getChildren().addAll(copyBtn, clearBtn, sendToEditorBtn);

        recordBtn.setOnAction(e -> {
            if (!recording) {
                startRecording(status, recordBtn, timerLabel, waveform, gc);
            } else {
                stopRecording(status, recordBtn, timerLabel, result, waveform, gc);
            }
        });

        HBox recRow = new HBox(10, recordBtn, timerLabel, new Label("AI:"), aiProviderBox);
        recRow.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(8, status, recRow, waveform, dropZone, new HBox(6, loadFileBtn), result, actionBar);
        root.setPadding(new Insets(12));

        Scene scene = new Scene(root, 560, 520);
        ThemeManager.getInstance().applyTheme(scene);
        stage.setScene(scene);
        stage.show();
    }

    private static void processAudioFile(File audioFile, TextArea result, Label status) {
        if (audioFile == null || !audioFile.exists()) {
            status.setText("File not found");
            return;
        }
        String name = audioFile.getName().toLowerCase();
        if (!name.endsWith(".wav") && !name.endsWith(".mp3") && !name.endsWith(".m4a")
            && !name.endsWith(".ogg") && !name.endsWith(".flac") && !name.endsWith(".webm")) {
            status.setText("Unsupported audio format: " + name.substring(name.lastIndexOf('.')));
            return;
        }
        status.setText("Processing audio file: " + audioFile.getName());
        result.setText("Sending to AI for transcription...\n");
        sendAudioToAI(audioFile, result, status);
    }

    private static void startRecording(Label status, Button recordBtn, Label timer, Canvas waveform, GraphicsContext gc) {
        recording = true;
        audioBuffer = new ByteArrayOutputStream();
        waveformBuffer.clear();
        recordBtn.setText("Stop Recording");
        recordBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 10 20; -fx-cursor: hand; -fx-background-radius: 8;");
        status.setText("Listening... Speak your code description.");
        status.setStyle("-fx-font-size: 12px; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");

        final long[] startTime = {System.currentTimeMillis()};

        Timeline timerAnim = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
            long elapsed = System.currentTimeMillis() - startTime[0];
            long secs = elapsed / 1000;
            timer.setText(String.format("%d:%02d", secs / 60, secs % 60));
        }));
        timerAnim.setCycleCount(Timeline.INDEFINITE);
        timerAnim.play();

        waveformAnim = new Timeline(new KeyFrame(Duration.millis(50), ev -> {
            gc.setFill(Color.rgb(30, 30, 50));
            gc.fillRect(0, 0, waveform.getWidth(), waveform.getHeight());
            gc.setStroke(Color.rgb(100, 200, 255));
            gc.setLineWidth(2);
            double w = waveform.getWidth();
            double h = waveform.getHeight();
            synchronized (waveformBuffer) {
                int n = waveformBuffer.size();
                if (n > 0) {
                    double step = w / Math.min(n, 200);
                    double mid = h / 2;
                    gc.beginPath();
                    gc.moveTo(0, mid);
                    int start = Math.max(0, n - 200);
                    for (int i = start; i < n; i++) {
                        double x = (i - start) * step;
                        double val = waveformBuffer.get(i) * mid * 0.8;
                        gc.lineTo(x, mid - val);
                    }
                    gc.stroke();
                    gc.beginPath();
                    gc.moveTo(0, mid);
                    for (int i = start; i < n; i++) {
                        double x = (i - start) * step;
                        double val = waveformBuffer.get(i) * mid * 0.8;
                        gc.lineTo(x, mid + val);
                    }
                    gc.stroke();
                }
            }
            long elapsed = System.currentTimeMillis() - startTime[0];
            if ((elapsed / 500) % 2 == 0) {
                gc.setFill(Color.RED);
                gc.fillOval(5, 5, 10, 10);
            }
        }));
        waveformAnim.setCycleCount(Timeline.INDEFINITE);
        waveformAnim.play();

        recordThread = new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(16000, 16, 1, true, true);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                if (!AudioSystem.isLineSupported(info)) {
                    Platform.runLater(() -> status.setText("Microphone not available."));
                    recording = false;
                    return;
                }
                micLine = (TargetDataLine) AudioSystem.getLine(info);
                micLine.open(format);
                micLine.start();

                byte[] buffer = new byte[2048];
                while (recording) {
                    int bytesRead = micLine.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        audioBuffer.write(buffer, 0, bytesRead);
                        double rms = 0;
                        for (int i = 0; i < bytesRead - 1; i += 2) {
                            short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
                            rms += sample * sample;
                        }
                        rms = Math.sqrt(rms / (bytesRead / 2));
                        double normalized = Math.min(1.0, rms / 32768.0);
                        synchronized (waveformBuffer) {
                            waveformBuffer.add(normalized);
                            if (waveformBuffer.size() > 500) waveformBuffer.remove(0);
                        }
                    }
                }
            } catch (Exception ex) {
                Platform.runLater(() -> status.setText("Mic error: " + ex.getMessage()));
            } finally {
                recording = false;
                if (micLine != null) {
                    micLine.stop();
                    micLine.close();
                }
            }
        });
        recordThread.setDaemon(true);
        recordThread.start();
    }

    private static void stopRecording(Label status, Button recordBtn, Label timer, TextArea result, Canvas waveform, GraphicsContext gc) {
        recording = false;
        if (waveformAnim != null) {
            waveformAnim.stop();
            waveformAnim = null;
        }
        recordBtn.setText("Start Recording");
        recordBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 10 20; -fx-cursor: hand; -fx-background-radius: 8;");
        timer.setText("0:00");

        if (audioBuffer == null || audioBuffer.size() < 1600) {
            status.setText("Recording too short. Try again.");
            status.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-primary;");
            gc.setFill(Color.rgb(30, 30, 50));
            gc.fillRect(0, 0, waveform.getWidth(), waveform.getHeight());
            return;
        }

        try {
            File wavDir = new File(CONFIG_DIR, "voice");
            wavDir.mkdirs();
            File wavFile = new File(wavDir, "recording_" + System.currentTimeMillis() + ".wav");

            byte[] audioData = audioBuffer.toByteArray();
            AudioFormat format = new AudioFormat(16000, 16, 1, true, true);
            try (ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
                 AudioInputStream ais = new AudioInputStream(bais, format, audioData.length / format.getFrameSize())) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, wavFile);
            }

            status.setText("Audio saved (" + (audioData.length / 16000) + "s). Sending to AI...");
            status.setStyle("-fx-font-size: 12px; -fx-text-fill: #22c55e; -fx-font-weight: bold;");

            result.setText("Sending audio to AI...\n");
            sendAudioToAI(wavFile, result, status);
        } catch (Exception ex) {
            status.setText("Error: " + ex.getMessage());
            result.setText("// Error saving audio: " + ex.getMessage());
        }

        gc.setFill(Color.rgb(30, 30, 50));
        gc.fillRect(0, 0, waveform.getWidth(), waveform.getHeight());
        audioBuffer = null;
    }

    private static void sendAudioToAI(File audioFile, TextArea result, Label status) {
        new Thread(() -> {
            try {
                byte[] audioBytes = Files.readAllBytes(audioFile.toPath());
                String base64Audio = Base64.getEncoder().encodeToString(audioBytes);
                String mimeType = getMimeType(audioFile.getName());

                String aiResponse;
                switch (providerType) {
                    case "openai":
                        aiResponse = callOpenaiAudio(base64Audio, mimeType);
                        break;
                    case "ollama":
                        aiResponse = callOllamaAudio(base64Audio);
                        break;
                    default:
                        aiResponse = callGeminiAudio(base64Audio, mimeType);
                }

                final String response = aiResponse;
                Platform.runLater(() -> {
                    if (response != null && !response.isEmpty()) {
                        result.setText(response);
                        status.setText("Transcription complete");
                    } else {
                        result.setText("// AI returned empty response");
                        status.setText("Empty response from AI");
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    result.setText("// Error: " + ex.getMessage());
                    status.setText("AI request failed");
                });
            }
        }).start();
    }

    private static String callGeminiAudio(String base64Audio, String mimeType) throws Exception {
        JsonObject body = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();

        JsonObject audioPart = new JsonObject();
        JsonObject inlineData = new JsonObject();
        inlineData.addProperty("mimeType", mimeType);
        inlineData.addProperty("data", base64Audio);
        audioPart.add("inlineData", inlineData);
        parts.add(audioPart);

        JsonObject promptPart = new JsonObject();
        promptPart.addProperty("text", "Transcribe this audio accurately. Then generate complete code based on what was described. "
            + "Output in this format:\nTRANSCRIPT: <transcribed text>\n\n```language\n// code here\n```");
        parts.add(promptPart);

        content.add("parts", parts);
        content.addProperty("role", "user");
        contents.add(content);
        body.add("contents", contents);

        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/"
            + geminiModel + ":generateContent?key=" + geminiKey;

        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(120000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(GSON.toJson(body).getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(code == 200 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) response.append(line);
        }

        if (code == 200) {
            JsonObject json = GSON.fromJson(response.toString(), JsonObject.class);
            if (json.has("candidates")) {
                JsonArray candidates = json.getAsJsonArray("candidates");
                if (candidates.size() > 0) {
                    JsonObject candidate = candidates.get(0).getAsJsonObject();
                    if (candidate.has("content")) {
                        JsonArray partss = candidate.getAsJsonObject("content").getAsJsonArray("parts");
                        if (partss.size() > 0) {
                            return partss.get(0).getAsJsonObject().get("text").getAsString();
                        }
                    }
                }
            }
            return "// Gemini returned empty response\n" + response.toString();
        }
        return "// Gemini API error (" + code + "):\n" + response.toString();
    }

    private static String callOpenaiAudio(String base64Audio, String mimeType) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("model", openaiModel);

        JsonArray messages = new JsonArray();
        JsonObject msg = new JsonObject();
        msg.addProperty("role", "user");
        JsonArray content = new JsonArray();

        JsonObject audioContent = new JsonObject();
        audioContent.addProperty("type", "input_audio");
        audioContent.addProperty("data", base64Audio);
        audioContent.addProperty("format", mimeType.endsWith("mp3") ? "mp3" : "wav");
        content.add(audioContent);

        JsonObject textContent = new JsonObject();
        textContent.addProperty("type", "text");
        textContent.addProperty("text", "Transcribe this audio accurately. Then generate complete code based on what was described. "
            + "Output in format:\nTRANSCRIPT: <text>\n\n```language\n// code\n```");
        content.add(textContent);

        msg.add("content", content);
        messages.add(msg);
        body.add("messages", messages);
        body.addProperty("max_tokens", 4096);

        HttpURLConnection conn = (HttpURLConnection) new URL(openaiEndpoint).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + openaiKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(120000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(GSON.toJson(body).getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(code == 200 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) response.append(line);
        }

        if (code == 200) {
            JsonObject json = GSON.fromJson(response.toString(), JsonObject.class);
            if (json.has("choices")) {
                JsonArray choices = json.getAsJsonArray("choices");
                if (choices.size() > 0) {
                    JsonObject choice = choices.get(0).getAsJsonObject();
                    if (choice.has("message")) {
                        return choice.getAsJsonObject("message").get("content").getAsString();
                    }
                }
            }
            return "// OpenAI returned empty response";
        }
        return "// OpenAI API error (" + code + "):\n" + response.toString();
    }

    private static String callOllamaAudio(String base64Audio) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("model", ollamaModel);
        body.addProperty("prompt", "Transcribe this audio accurately. Generate complete code based on what was described. "
            + "Output TRANSCRIPT: <text> then code in ``` blocks.");
        body.addProperty("stream", false);

        JsonArray images = new JsonArray();
        // Ollama vision models use base64 images; for audio we send text only
        // Audio transcription via Ollama is limited; fallback to text-based
        body.addProperty("prompt", "The user attempted to send audio. Respond: 'Audio transcription via Ollama is limited. "
            + "Please use Gemini or OpenAI for audio processing, or type your request as text.'");

        HttpURLConnection conn = (HttpURLConnection) new URL(ollamaEndpoint).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(GSON.toJson(body).getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(code == 200 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) response.append(line);
        }

        if (code == 200) {
            JsonObject json = GSON.fromJson(response.toString(), JsonObject.class);
            return json.has("response") ? json.get("response").getAsString() : "// Empty response";
        }
        return "// Ollama API error (" + code + "):\n" + response.toString();
    }

    public static void showTextFallback(Window owner) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.setTitle("Text to Code");

        VBox root = new VBox(8);
        root.setPadding(new Insets(12));

        Label instr = new Label("Describe what code you want, then click Generate.");
        instr.setWrapText(true);

        TextArea input = new TextArea();
        input.setPromptText("e.g. Create a Java class that reads CSV and returns a list of objects");
        input.setPrefRowCount(6);

        ComboBox<String> langBox = new ComboBox<>();
        langBox.getItems().addAll("Java", "JavaScript", "Python", "TypeScript", "HTML", "CSS", "SQL", "Kotlin", "Go", "Rust", "C++", "PHP");
        langBox.setValue("Java");

        TextArea output = new TextArea();
        output.setPromptText("Generated code will appear here...");
        output.setPrefRowCount(10);
        output.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px;");
        output.setEditable(false);

        Button genBtn = new Button("Generate Code");
        genBtn.setStyle("-fx-background-color: -accent; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 6 14;");

        ComboBox<String> providerBox = new ComboBox<>();
        providerBox.getItems().addAll("gemini", "openai", "ollama");
        providerBox.setValue(providerType);

        genBtn.setOnAction(ev -> {
            String desc = input.getText().trim();
            if (desc.isEmpty()) { output.setText("// Please describe what you want"); return; }
            output.setText("Generating...\n");
            generateFromText(desc, langBox.getValue(), output, providerBox.getValue());
        });

        HBox controls = new HBox(6, genBtn, new Label("AI:"), providerBox);

        Button copyBtn = new Button("Copy");
        copyBtn.setStyle("-fx-cursor: hand;");
        copyBtn.setOnAction(ev -> {
            String text = output.getText();
            if (!text.isEmpty()) {
                ClipboardContent cc = new ClipboardContent();
                cc.putString(text);
                Clipboard.getSystemClipboard().setContent(cc);
            }
        });

        root.getChildren().addAll(instr, input, langBox, controls, new HBox(6, copyBtn), output);

        Scene scene = new Scene(root, 540, 520);
        ThemeManager.getInstance().applyTheme(scene);
        stage.setScene(scene);
        stage.show();
    }

    private static void generateFromText(String description, String lang, TextArea output, String provider) {
        new Thread(() -> {
            try {
                String prompt = "Generate " + lang + " code for: " + description
                    + "\nOutput ONLY the code in ``` blocks with minimal explanation.";

                String result;
                switch (provider) {
                    case "openai":
                        result = callOpenaiText(prompt);
                        break;
                    case "ollama":
                        result = callOllamaText(prompt);
                        break;
                    default:
                        result = callGeminiText(prompt);
                }

                final String finalResult = result;
                Platform.runLater(() -> output.setText(finalResult));
            } catch (Exception ex) {
                Platform.runLater(() -> output.setText("// Error: " + ex.getMessage()));
            }
        }).start();
    }

    private static String callGeminiText(String prompt) throws Exception {
        JsonObject body = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", prompt);
        parts.add(textPart);
        content.add("parts", parts);
        content.addProperty("role", "user");
        contents.add(content);
        body.add("contents", contents);

        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/"
            + geminiModel + ":generateContent?key=" + geminiKey;

        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(GSON.toJson(body).getBytes(StandardCharsets.UTF_8));
        }

        StringBuilder responseBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getResponseCode() == 200 ? conn.getInputStream() : conn.getErrorStream()))) {
            String line;
            while ((line = br.readLine()) != null) responseBuilder.append(line);
        }

        if (conn.getResponseCode() == 200) {
            JsonObject json = GSON.fromJson(responseBuilder.toString(), JsonObject.class);
            String text = "";
            if (json.has("candidates")) {
                JsonArray candidates = json.getAsJsonArray("candidates");
                if (candidates.size() > 0) {
                    JsonObject candidate = candidates.get(0).getAsJsonObject();
                    if (candidate.has("content")) {
                        JsonArray partss = candidate.getAsJsonObject("content").getAsJsonArray("parts");
                        if (partss.size() > 0) {
                            text = partss.get(0).getAsJsonObject().get("text").getAsString();
                        }
                    }
                }
            }
            return text.isEmpty() ? "// Empty response" : text;
        }
        return "// API Error (" + conn.getResponseCode() + "):\n" + responseBuilder.toString();
    }

    private static String callOpenaiText(String prompt) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("model", openaiModel);
        JsonArray messages = new JsonArray();
        JsonObject msg = new JsonObject();
        msg.addProperty("role", "user");
        msg.addProperty("content", prompt);
        messages.add(msg);
        body.add("messages", messages);
        body.addProperty("max_tokens", 2048);

        HttpURLConnection conn = (HttpURLConnection) new URL(openaiEndpoint).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + openaiKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(GSON.toJson(body).getBytes(StandardCharsets.UTF_8));
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getResponseCode() == 200 ? conn.getInputStream() : conn.getErrorStream()))) {
            String line;
            while ((line = br.readLine()) != null) response.append(line);
        }

        if (conn.getResponseCode() == 200) {
            JsonObject json = GSON.fromJson(response.toString(), JsonObject.class);
            if (json.has("choices")) {
                JsonArray choices = json.getAsJsonArray("choices");
                if (choices.size() > 0 && choices.get(0).getAsJsonObject().has("message")) {
                    return choices.get(0).getAsJsonObject().getAsJsonObject("message").get("content").getAsString();
                }
            }
            return "// Empty response";
        }
        return "// API Error (" + conn.getResponseCode() + "):\n" + response.toString();
    }

    private static String callOllamaText(String prompt) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("model", ollamaModel);
        body.addProperty("prompt", prompt);
        body.addProperty("stream", false);

        HttpURLConnection conn = (HttpURLConnection) new URL(ollamaEndpoint).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(GSON.toJson(body).getBytes(StandardCharsets.UTF_8));
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getResponseCode() == 200 ? conn.getInputStream() : conn.getErrorStream()))) {
            String line;
            while ((line = br.readLine()) != null) response.append(line);
        }

        if (conn.getResponseCode() == 200) {
            JsonObject json = GSON.fromJson(response.toString(), JsonObject.class);
            return json.has("response") ? json.get("response").getAsString() : "// Empty response";
        }
        return "// API Error (" + conn.getResponseCode() + "):\n" + response.toString();
    }

    private static String getMimeType(String filename) {
        String ext = filename.contains(".") ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase() : "";
        switch (ext) {
            case "mp3": return "audio/mpeg";
            case "m4a": return "audio/mp4";
            case "ogg": return "audio/ogg";
            case "flac": return "audio/flac";
            case "webm": return "audio/webm";
            default: return "audio/wav";
        }
    }

    private static void loadConfig() {
        try {
            File f = new File(CONFIG_DIR, "ai.properties");
            if (f.exists()) {
                Properties p = new Properties();
                try (FileInputStream in = new FileInputStream(f)) {
                    p.load(in);
                }
                providerType = p.getProperty("provider", "gemini");
                geminiKey = p.getProperty("gemini.key", "");
                geminiModel = p.getProperty("gemini.model", "gemini-2.0-flash");
                openaiEndpoint = p.getProperty("openai.endpoint", "https://api.openai.com/v1/chat/completions");
                openaiKey = p.getProperty("openai.key", "");
                openaiModel = p.getProperty("openai.model", "gpt-4o-audio-preview");
                ollamaEndpoint = p.getProperty("ollama.endpoint", "http://localhost:11434/api/generate");
                ollamaModel = p.getProperty("ollama.model", "llama3.2-vision");
            }
        } catch (Exception ignored) {}
    }

    private static void saveConfig() {
        try {
            new File(CONFIG_DIR).mkdirs();
            Properties p = new Properties();
            p.setProperty("provider", providerType == null ? "gemini" : providerType);
            p.setProperty("gemini.key", geminiKey == null ? "" : geminiKey);
            p.setProperty("gemini.model", geminiModel);
            p.setProperty("openai.endpoint", openaiEndpoint);
            p.setProperty("openai.key", openaiKey == null ? "" : openaiKey);
            p.setProperty("openai.model", openaiModel);
            p.setProperty("ollama.endpoint", ollamaEndpoint);
            p.setProperty("ollama.model", ollamaModel);
            try (FileOutputStream out = new FileOutputStream(new File(CONFIG_DIR, "ai.properties"))) {
                p.store(out, "Webide AI Config");
            }
        } catch (Exception ignored) {}
    }
}
