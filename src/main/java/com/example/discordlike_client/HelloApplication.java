package com.example.discordlike_client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage primaryStage) throws IOException {
        // Charger le fichier FXML
        Parent root = FXMLLoader.load(getClass().getResource("hello-view.fxml"));

        // Créer une scène avec le fichier FXML
        Scene scene = new Scene(root);

        // Configurer la fenêtre
        primaryStage.setTitle("Discord Client");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false); // Permet d'agrandir et réduire la fenêtre
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}