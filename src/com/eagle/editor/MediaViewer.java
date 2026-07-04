package com.eagle.editor;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

import com.eagle.icons.IconManager;
import java.io.File;

/**
 * Renders a read-only viewer for binary asset files: images get an ImageView
 * with zoom-to-fit, audio/video get a MediaPlayer with basic transport controls.
 */
public class MediaViewer extends VBox {

    private MediaPlayer mediaPlayer;

    public MediaViewer(File file) {
        setAlignment(Pos.CENTER);
        setSpacing(12);
        setStyle("-fx-padding: 20;");

        String name = file.getName().toLowerCase();
        if (isImage(name)) {
            buildImageViewer(file);
        } else if (isAudio(name) || isVideo(name)) {
            buildMediaPlayer(file, isVideo(name));
        } else {
            getChildren().add(new Label("Preview not available for this file type."));
        }
    }

    private void buildImageViewer(File file) {
        try {
            Image image = new Image(file.toURI().toString());
            ImageView view = new ImageView(image);
            view.setPreserveRatio(true);
            view.setSmooth(true);
            view.setFitWidth(800);
            VBox.setVgrow(view, javafx.scene.layout.Priority.ALWAYS);

            Label info = new Label(file.getName() + "   •   " +
                    (int) image.getWidth() + " × " + (int) image.getHeight() + " px");
            info.getStyleClass().add("muted");

            getChildren().addAll(view, info);
        } catch (Exception e) {
            getChildren().add(new Label("Could not load image: " + e.getMessage()));
        }
    }

    private void buildMediaPlayer(File file, boolean isVideo) {
        try {
            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);

            Label title = new Label(file.getName());
            title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            Button playBtn = new Button("Play");
            Button pauseBtn = new Button("Pause");
            Button stopBtn = new Button("Stop");
            Slider seekSlider = new Slider();
            Label timeLabel = new Label("00:00 / 00:00");

            playBtn.setOnAction(e -> mediaPlayer.play());
            pauseBtn.setOnAction(e -> mediaPlayer.pause());
            stopBtn.setOnAction(e -> mediaPlayer.stop());

            mediaPlayer.currentTimeProperty().addListener((obs, old, val) -> {
                if (!seekSlider.isValueChanging()) {
                    Duration total = mediaPlayer.getTotalDuration();
                    if (total != null && total.toMillis() > 0) {
                        seekSlider.setValue(val.toMillis() / total.toMillis() * 100);
                    }
                    timeLabel.setText(formatDuration(val) + " / " + formatDuration(total));
                }
            });

            seekSlider.valueChangingProperty().addListener((obs, was, isNow) -> {
                if (!isNow) {
                    Duration total = mediaPlayer.getTotalDuration();
                    if (total != null) {
                        mediaPlayer.seek(total.multiply(seekSlider.getValue() / 100.0));
                    }
                }
            });

            HBox controls = new HBox(10, playBtn, pauseBtn, stopBtn);
            controls.setAlignment(Pos.CENTER);

            VBox.setMargin(title, new javafx.geometry.Insets(0, 0, 10, 0));
            getChildren().add(title);

            if (isVideo) {
                MediaView mediaView = new MediaView(mediaPlayer);
                mediaView.setFitWidth(760);
                mediaView.setPreserveRatio(true);
                getChildren().add(mediaView);
            } else {
                Label waveformPlaceholder = new Label("Audio file");
                waveformPlaceholder.setStyle("-fx-font-size: 40px;");
                getChildren().add(waveformPlaceholder);
            }

            getChildren().addAll(controls, seekSlider, timeLabel);

        } catch (Exception e) {
            getChildren().add(new Label("Could not load media: " + e.getMessage()));
        }
    }

    private String formatDuration(Duration d) {
        if (d == null) return "00:00";
        int totalSeconds = (int) d.toSeconds();
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public void dispose() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }
    }

    public static boolean isImage(String lowerName) {
        return lowerName.endsWith(".png") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")
                || lowerName.endsWith(".gif") || lowerName.endsWith(".bmp") || lowerName.endsWith(".webp");
    }

    public static boolean isAudio(String lowerName) {
        return lowerName.endsWith(".mp3") || lowerName.endsWith(".wav") || lowerName.endsWith(".aac")
                || lowerName.endsWith(".m4a");
    }

    public static boolean isVideo(String lowerName) {
        return lowerName.endsWith(".mp4") || lowerName.endsWith(".m4v") || lowerName.endsWith(".mov")
                || lowerName.endsWith(".avi") || lowerName.endsWith(".webm");
    }

    public static boolean isMediaFile(File file) {
        String n = file.getName().toLowerCase();
        return isImage(n) || isAudio(n) || isVideo(n);
    }
}
