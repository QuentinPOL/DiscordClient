package com.example.discordlike_client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import okhttp3.*;

import java.io.IOException;

public class HelloController {

    @FXML
    private TextField emailOrPhoneField;

    @FXML
    private PasswordField passwordField;

    private static final String LOGIN_URL = "http://localhost:8080/api/login"; // URL du serveur

    @FXML
    protected void onLoginButtonClick(ActionEvent event) {
        String emailOrPhone = emailOrPhoneField.getText();
        String password = passwordField.getText();

        if (emailOrPhone.isEmpty() || password.isEmpty()) {
            showAlert("Erreur", "Tous les champs doivent être remplis.");
            return;
        }

        // Construire la requête HTTP
        OkHttpClient client = new OkHttpClient();

        RequestBody body = new FormBody.Builder()
                .add("email", emailOrPhone)
                .add("password", password)
                .build();

        Request request = new Request.Builder()
                .url(LOGIN_URL)
                .post(body)
                .build();

        // Envoyer la requête
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                showAlert("Erreur", "Échec de la communication avec le serveur.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    showAlert("Succès", "Connexion réussie !");
                } else {
                    showAlert("Erreur", "Connexion échouée. Vérifiez vos informations.");
                }
            }
        });
    }

    @FXML
    protected void onSignUpLinkClick(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/example/discordlike_client/inscription.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setTitle("Inscription");
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}