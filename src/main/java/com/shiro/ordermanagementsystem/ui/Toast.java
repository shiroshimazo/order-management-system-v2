package com.shiro.ordermanagementsystem.ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

public final class Toast {

    public enum Kind { SUCCESS, ERROR, INFO }

    private Toast() {}

    public static void success(Node anchor, String message) { show(anchor, message, Kind.SUCCESS); }
    public static void error(Node anchor, String message)   { show(anchor, message, Kind.ERROR); }
    public static void info(Node anchor, String message)    { show(anchor, message, Kind.INFO); }

    public static void show(Node anchor, String message, Kind kind) {
        if (anchor == null || anchor.getScene() == null) return;
        Scene scene = anchor.getScene();
        Window owner = scene.getWindow();
        if (owner == null) return;

        String bg = switch (kind) {
            case SUCCESS -> "#167a30";
            case ERROR   -> "#c5241a";
            case INFO    -> "#1f2228";
        };

        Label label = new Label(message);
        label.setStyle(
                "-fx-text-fill: #ffffff;" +
                "-fx-font-size: 12.5px;" +
                "-fx-font-weight: bold;"
        );

        HBox root = new HBox(label);
        root.setAlignment(Pos.CENTER);
        root.setStyle(
                "-fx-background-color: " + bg + ";" +
                "-fx-background-radius: 10;" +
                "-fx-padding: 12 18 12 18;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 18, 0, 0, 6);"
        );

        Popup popup = new Popup();
        popup.getContent().add(root);
        popup.setAutoFix(true);
        popup.show(owner);

        Platform.runLater(() -> {
            double margin = 24;
            popup.setX(owner.getX() + owner.getWidth() - root.getWidth() - margin);
            popup.setY(owner.getY() + margin + 48);
        });

        FadeTransition fadeIn = new FadeTransition(Duration.millis(180), root);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        PauseTransition hold = new PauseTransition(Duration.seconds(2.6));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(220), root);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        SequentialTransition seq = new SequentialTransition(fadeIn, hold, fadeOut);
        seq.setOnFinished(e -> popup.hide());
        seq.play();
    }
}
