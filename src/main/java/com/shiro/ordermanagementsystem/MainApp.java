package com.shiro.ordermanagementsystem;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
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