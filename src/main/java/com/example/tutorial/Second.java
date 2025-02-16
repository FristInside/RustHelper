package com.example.tutorial;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class Second {

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;



    @FXML
    private Button gaBack;

    @FXML
    void goBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("hello-view.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) gaBack.getScene().getWindow(); // Получаем текущее окно
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void initialize() {
        gaBack.setOnAction(this::goBack);
    }

}
