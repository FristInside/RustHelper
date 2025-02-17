package com.example.tutorial;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 730, 700);
        stage.setTitle("Rust Search");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
        //scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/stayle.css")).toExternalForm());
    }

    public static void main(String[] args) {
        launch();
    }
}