package com.example.discordlike_client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.util.regex.Pattern;
import javafx.application.Platform; // Import nécessaire pour Platform.runLater()
import okhttp3.*;

import java.io.IOException;

public class Inscription {

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField passwordFieldConfirm;

    // Ajout de contrôle de saisie
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
    private static final String PASSWORD_REGEX = "^(?=.*[A-Z])(?=.*[!@#$%^&*])(?=.*[0-9])(?=.*[a-zA-Z]).{10,}$";

    // URL de l'API pour l'inscription
    private static final String API_URL = "http://163.172.34.212:8080/api/users/register";
    private final OkHttpClient client = new OkHttpClient();

    @FXML
    protected void onSignUpButtonClick(ActionEvent event) {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();
        String confirmPassword = passwordFieldConfirm.getText().trim();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showAlert("Erreur", "Tous les champs doivent être remplis.");
            return;
        }

        if (!Pattern.matches(EMAIL_REGEX, email)) {
            showAlert("Erreur", "Veuillez entrer une adresse e-mail valide.");
            return;
        }

        if (!Pattern.matches(PASSWORD_REGEX, password)) {
            showAlert("Erreur", "Le mot de passe doit contenir au moins 10 caractères, une majuscule, un caractère spécial et un chiffre.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showAlert("Erreur", "Les mots de passe ne correspondent pas.");
            return;
        }

        // Envoyer la requête HTTP d'inscription
        sendSignUpRequest(username, email, password);
    }

    private void sendSignUpRequest(String username, String email, String password) {
        // Construction du JSON
        String json = "{"
                + "\"email\":\"" + email + "\","
                + "\"passwordHash\":\"" + password + "\","
                + "\"username\":\"" + username + "\""
                + "}";

        // Création du corps de requête
        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));

        // Création de la requête HTTP POST
        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .build();

        // Exécution asynchrone pour éviter de bloquer l'UI
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Platform.runLater(() -> showAlert("Erreur", "Impossible de se connecter au serveur."));
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                System.out.println(response.body().string());

                if (response.isSuccessful()) {
                    Platform.runLater(() -> showAlert("Succès", "Inscription réussie !"));
                } else {
                    Platform.runLater(() -> showAlert("Erreur", "Inscription échouée. Vérifiez vos informations."));
                }
            }
        });
    }

    @FXML
    protected void onBackToLoginClick(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/example/discordlike_client/hello-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
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