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
import okhttp3.*;

import java.io.IOException;

public class Inscription {

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    // Ajout de contrôle de saisie
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
    private static final String PASSWORD_REGEX = "^(?=.*[A-Z])(?=.*[!@#$%^&*])(?=.*[0-9])(?=.*[a-zA-Z]).{10,}$";

    // Liens vers l'api d'inscription ici

    @FXML
    protected void onSignUpButtonClick(ActionEvent event) {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
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

        showAlert("Succès", "Inscription réussie !");
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