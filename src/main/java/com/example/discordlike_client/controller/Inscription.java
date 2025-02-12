package com.example.discordlike_client.controller;

import com.example.discordlike_client.model.Utilisateur;
import com.example.discordlike_client.websocket.GlobalWebSocketClient;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.json.JSONException;
import org.json.JSONObject;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;
import javafx.application.Platform;
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

        if (username.length() > 10) {
            showAlert("Erreur", "Le pseudo ne peut pas dépasser 10 caractères.");
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

        // Hachage du mot de passe avec SHA-256
        String hashedPassword = hashPassword(password);

        // Envoyer la requête HTTP d'inscription
        sendSignUpRequest(username, email, hashedPassword);
    }

    private void sendSignUpRequest(String username, String email, String password) {
        // Construction du JSON
        String json = "{"
                + "\"email\":\"" + email + "\","
                + "\"password\":\"" + password + "\","
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
                String responseBody = response.body().string();

                if (response.isSuccessful()) {
                    Platform.runLater(() -> {
                        showAlert("Succès", "Inscription réussie !");

                        // Stocker les infos de l'utilisateur
                        Utilisateur utilisateur = Utilisateur.getInstance();
                        utilisateur.setAdresseMail(email);
                        utilisateur.setPseudo(username);

                        // Extraire le token du JSON de la réponse
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String token = jsonResponse.getString("token");
                            String avatar = jsonResponse.getString("avatar");

                            // Connexion WebSocket
                            GlobalWebSocketClient.getInstance().connect("ws://163.172.34.212:8090?token=" + token);

                            utilisateur.setToken(token);
                            utilisateur.setImagePath(avatar);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            showAlert("Erreur", "Impossible de récupérer le token.");
                        }

                        redirectToMainView();
                    });
                } else if (response.code() == 403) {
                    Platform.runLater(() -> showAlert("Erreur", "Inscription échouée. Adresse-Mail/Pseudo déjà existant."));
                }
                else {
                    Platform.runLater(() -> showAlert("Erreur", "Inscription échouée. Vérifiez vos informations."));
                }
            }
        });
    }

    // Méthode pour hacher un mot de passe avec SHA-256
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
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

    private void redirectToMainView() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/example/discordlike_client/main-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'accéder au tableau de bord.");
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