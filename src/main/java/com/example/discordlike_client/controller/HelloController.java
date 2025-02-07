package com.example.discordlike_client.controller;

import com.example.discordlike_client.model.Utilisateur;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import java.security.PrivateKey;
import java.util.Random;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javafx.stage.Stage;

import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class HelloController {

    @FXML
    private TextField emailOrPhoneField;
    private int isOnSending = 0;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    private static final String LOGIN_URL = "http://163.172.34.212:8080/api/users/login"; // URL du serveur

    @FXML
    protected void onLoginButtonClick(ActionEvent event) {
        if (isOnSending == 0) // Si on est en envoie
        {
            String emailUsername = emailOrPhoneField.getText();
            String password = passwordField.getText();

            if (emailUsername.isEmpty() || password.isEmpty()) {
                showAlert("Erreur", "Tous les champs doivent être remplis.");
                return;
            }

            // On désactive le bouton pour empêcher tout nouveau clic
            loginButton.setDisable(true);

            // Hachage du mot de passe avec SHA-256
            String hashedPassword = hashPassword(password);

            // Construire la requête HTTP
            OkHttpClient client = new OkHttpClient();

            // Construction du JSON
            String json = "{"
                    + "\"usernameOrEmail\":\"" + emailUsername + "\","
                    + "\"password\":\"" + hashedPassword + "\""
                    + "}";

            // Création du corps de requête
            RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));

            // Création de la requête HTTP POST
            Request request = new Request.Builder()
                    .url(LOGIN_URL)
                    .post(body)
                    .build();

            // Envoyer la requête
            isOnSending = 1;
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    isOnSending = 0;
                    loginButton.setDisable(false);
                    Platform.runLater(() -> showAlert("Erreur", "Échec de la communication avec le serveur."));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    isOnSending = 0;

                    if (response.isSuccessful()) {
                        Platform.runLater(() -> {
                            isOnSending = 0;
                            // Réactiver le bouton
                            loginButton.setDisable(false);

                            showAlert("Succès", "Connexion réussie !");

                            // Stocker les infos de l'utilisateur
                            Utilisateur utilisateur = Utilisateur.getInstance();

                            // Récupérer le pseudo et l'adresse mail
                            utilisateur.setAdresseMail(emailUsername);
                            utilisateur.setPseudo(emailUsername);

                            // Extraire le token du JSON de la réponse
                            try {
                                JSONObject jsonResponse = new JSONObject(responseBody);

                                String email = jsonResponse.getString("email");
                                String username = jsonResponse.getString("username");
                                String token = jsonResponse.getString("token");
                                String avatar = jsonResponse.getString("avatar");
                                String status = jsonResponse.getString("status");

                                utilisateur.setAdresseMail(email);
                                utilisateur.setPseudo(username);
                                utilisateur.setStatutString(status);
                                utilisateur.setToken(token);
                                utilisateur.setImagePath(avatar);
                            } catch (JSONException e) {
                                e.printStackTrace();
                                showAlert("Erreur", "Impossible de récupérer le token.");
                            }


                            redirectToMainView();
                        });
                    } else {
                        Platform.runLater(() -> {
                            isOnSending = 0;
                            loginButton.setDisable(false);
                            showAlert("Erreur", "Connexion échouée. Vérifiez vos informations.");
                        });
                    }
                }
            });
        }
        else {
            Platform.runLater(() -> showAlert("Erreur", "Envoie déjà en cours !"));
        }
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
    protected void onSignUpLinkClick(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/example/discordlike_client/inscription.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void onGuestLoginClick(ActionEvent event) {
        showAlert("Mode Invité", "Vous êtes connecté en tant qu'invité.");

        // Stocker les infos de l'utilisateur
        Utilisateur utilisateur = Utilisateur.getInstance();

        // Récupérer le pseudo et l'adresse mail
        utilisateur.setAdresseMail(null);

        // Générer 5 chiffres aléatoires
        Random random = new Random();
        int randomNumber = 10000 + random.nextInt(90000); // Génère un nombre entre 10000 et 99999

        utilisateur.setPseudo("Guest" + randomNumber);
        redirectToMainView();
    }

    private void redirectToMainView() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/example/discordlike_client/main-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());

            Stage stage = (Stage) emailOrPhoneField.getScene().getWindow();
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