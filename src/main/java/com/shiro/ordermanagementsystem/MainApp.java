package com.shiro.ordermanagementsystem;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.text.Font;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Load SF Pro Display fonts (must come BEFORE FXMLLoader)
        Font.loadFont(getClass().getResourceAsStream(
                "/ordermanagementsystem/fonts/SF-Pro-Display-Regular.otf"), 13);
        Font.loadFont(getClass().getResourceAsStream(
                "/ordermanagementsystem/fonts/SF-Pro-Display-Medium.otf"), 13);
        Font.loadFont(getClass().getResourceAsStream(
                "/ordermanagementsystem/fonts/SF-Pro-Display-Bold.otf"), 13);

        // ... rest of your existing code (FXMLLoader, Scene, stage.show, etc.)
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/ordermanagementsystem/fxml/LoginDashBoard.fxml")
        );
        Scene scene = new Scene(loader.load());

        // Force Ikonli icons to render
        scene.getStylesheets().add(
                "https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined"
        );

        stage.setTitle("Order Management System");
        stage.setWidth(420);
        stage.setHeight(660);
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}