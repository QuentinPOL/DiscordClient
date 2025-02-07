package com.example.discordlike_client.controller;

import com.example.discordlike_client.model.ServerItem;
import com.example.discordlike_client.model.Utilisateur;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import okhttp3.*;

import java.io.IOException;

public class ServersController {

    private ServerItem serverItem;

    @FXML
    private Label channelNameLabel;
    @FXML
    private VBox messagesContainer;    // Contiendra les messages
    @FXML
    private TextField messageInputField;
    @FXML
    private ScrollPane scrollPane;

    @FXML private HBox rootPane;
    @FXML private Label usernameLabel;
    @FXML private ListView<ServerItem> serversListView;

    @FXML private StackPane discordLogoContainer;
    @FXML private ImageView discordLogo;
    @FXML private ImageView userAvatar;
    @FXML private Label statusText;
    @FXML private Circle statusIndicator;


    // URL de l'API et client HTTP
    private static final String API_URL = "http://163.172.34.212:8080/api";
    private final OkHttpClient client = new OkHttpClient();

    public void setServerItem(ServerItem serverItem) {
        this.serverItem = serverItem;
        channelNameLabel.setText("#général");
    }

    @FXML
    public void initialize() {
        // Forcer la fenêtre en mode maximisé et redimensionnable
        Platform.runLater(() -> {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setResizable(true);
            stage.setMaximized(true);
        });

        // Récupérer les infos de l'utilisateur
        Utilisateur utilisateur = Utilisateur.getInstance();
        usernameLabel.setText(utilisateur.getPseudo());

        // Charger et afficher l'avatar utilisateur
        String imagePath = getClass().getResource("/Image/pp.jpg").toExternalForm();
        userAvatar.setImage(new Image(imagePath));
        applyCircularClip(userAvatar);

        // Mettre à jour le statut utilisateur (pastille et texte)
        updateStatusColor(1);

        // Créer et appliquer le menu contextuel pour le statut
        ContextMenu statusMenu = createStatusMenu();
        userAvatar.setOnMouseClicked((MouseEvent event) -> {
            statusMenu.show(statusIndicator, event.getScreenX(), event.getScreenY());
            userAvatar.setStyle("-fx-cursor: hand;");
        });

        // Liste de serveurs (icônes)
        serversListView.getItems().addAll(
                new ServerItem("/Image/6537937.jpg")
        );
        serversListView.setFocusTraversable(false);

        // Configuration de la ListView des serveurs (icône rond)
        serversListView.setCellFactory(list -> new ListCell<ServerItem>() {
            private final ImageView imageView = new ImageView();
            @Override
            protected void updateItem(ServerItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }
                Image img = new Image(getClass().getResource(item.getImagePath()).toExternalForm());
                imageView.setImage(img);
                imageView.setFitWidth(64);
                imageView.setFitHeight(64);
                imageView.setPreserveRatio(true);
                Circle clip = new Circle(32, 32, 32);
                imageView.setClip(clip);
                setGraphic(imageView);
                this.setStyle("-fx-background-color: #40444B; -fx-background-radius: 5;");

                setOnMouseEntered(event -> setStyle("-fx-background-color: #40444B; -fx-background-radius: 5; -fx-cursor: hand;"));
                setOnMouseExited(event -> setStyle("-fx-background-color: #40444B; -fx-background-radius: 5;"));
            }
        });

        scrollPane.setStyle(
                "-fx-background: transparent;" +
                        "-fx-background-color: transparent;" +
                        "-fx-control-inner-background: transparent;" +
                        "-fx-focus-color: transparent;" +
                        "-fx-faint-focus-color: transparent;"
        );
        messagesContainer.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-focus-color: transparent;" +
                        "-fx-faint-focus-color: transparent;"
        );

        // Rendre l’image elle-même circulaire
        applyCircularClip(discordLogo);

        // Gérer le hover sur le conteneur pour changer la bordure en blanc
        discordLogoContainer.setOnMouseEntered(e -> {
            discordLogoContainer.setStyle(
                    "-fx-border-color: cyan; " +
                            "-fx-border-width: 2; " +
                            "-fx-border-radius: 30; " +
                            "-fx-background-radius: 30; " +
                            "-fx-padding: 4;" +
                            "-fx-cursor: hand;"
            );
        });
        discordLogoContainer.setOnMouseExited(e -> {
            discordLogoContainer.setStyle(
                    "-fx-border-color: grey; " +
                            "-fx-border-width: 2; " +
                            "-fx-border-radius: 30; " +
                            "-fx-background-radius: 30; " +
                            "-fx-padding: 4;"
            );
        });
    }

    // Méthode appelée depuis le MainViewController
    public void setChannelName(String channelName) {
        // Le label affiché en haut de la colonne principale
        channelNameLabel.setText(channelName);

        // Charger/construire la conversation
        loadConversationForFriend(channelName);
    }

    private void loadConversationForFriend(String friendName) {
        messagesContainer.getChildren().clear();
        // Ex: on simule des données
        addMessage("b-KOS", "Hier à 16:09", "En vrai le truc le plus \"chiant\" c'est le problème 2 pour moi", "/Image/pp.jpg");
        addMessage("DJquinquin", "Hier à 16:09", "juste nous on c'est arrêté à 30k", "/Image/pp.jpg");
        addMessage("b-KOS", "Hier à 16:09", "En vrai le truc le plus \"chiant\" c'est le problème 2 pour moi", "/Image/pp.jpg");
        addMessage("DJquinquin", "Hier à 16:09", "juste nous on c'est arrêté à 30k", "/Image/pp.jpg");
        addMessage("b-KOS", "Hier à 16:09", "En vrai le truc le plus \"chiant\" c'est le problème 2 pour moi", "/Image/pp.jpg");
        addMessage("DJquinquin", "Hier à 16:09", "juste nous on c'est arrêté à 30k", "/Image/pp.jpg");
        addMessage("b-KOS", "Hier à 16:09", "En vrai le truc le plus \"chiant\" c'est le problème 2 pour moi", "/Image/pp.jpg");
        addMessage("DJquinquin", "Hier à 16:09", "juste nous on c'est arrêté à 30k", "/Image/pp.jpg");
        addMessage("b-KOS", "Hier à 16:09", "En vrai le truc le plus \"chiant\" c'est le problème 2 pour moi", "/Image/pp.jpg");
        addMessage("DJquinquin", "Hier à 16:09", "juste nous on c'est arrêté à 30k", "/Image/pp.jpg");
        addMessage("b-KOS", "Hier à 16:09", "En vrai le truc le plus \"chiant\" c'est le problème 2 pour moi", "/Image/pp.jpg");
        addMessage("DJquinquin", "Hier à 16:09", "juste nous on c'est arrêté à 30k", "/Image/pp.jpg");
        addMessage("b-KOS", "Hier à 16:09", "En vrai le truc le plus \"chiant\" c'est le problème 2 pour moi", "/Image/pp.jpg");
        addMessage("DJquinquin", "Hier à 16:09", "juste nous on c'est arrêté à 30k", "/Image/pp.jpg");

        // Scroll en bas
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    private void addMessage(String author, String time, String content, String avatarPath) {
        // HBox global
        HBox messageHBox = new HBox(10);

        // Style par défaut = transparent
        messageHBox.setStyle("-fx-background-color: transparent;");

        // Effet de survol
        messageHBox.setOnMouseEntered(e -> {
            messageHBox.setStyle("-fx-background-color: #40444B;");
        });
        messageHBox.setOnMouseExited(e -> {
            messageHBox.setStyle("-fx-background-color: transparent;");
        });


        // Avatar
        ImageView avatar = new ImageView(new Image(getClass().getResource(avatarPath).toExternalForm()));
        avatar.setFitWidth(40);
        avatar.setFitHeight(40);
        // Clip circulaire
        Circle clip = new Circle(20, 20, 20);
        avatar.setClip(clip);

        // Partie texte
        VBox textVBox = new VBox(3);

        // Ligne auteur + heure
        HBox authorLine = new HBox(5);
        Label authorLabel = new Label(author);
        authorLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold;");
        Label timeLabel = new Label(time);
        timeLabel.setStyle("-fx-text-fill: #72767D; -fx-font-size: 12px;");
        authorLine.getChildren().addAll(authorLabel, timeLabel);

        // Contenu du message
        Label messageLabel = new Label(content);
        messageLabel.setStyle("-fx-text-fill: #DCDDDE; -fx-wrap-text: true; -fx-font-size: 14px;");
        messageLabel.setMaxWidth(500); // Pour forcer le wrap si c'est trop long

        textVBox.getChildren().addAll(authorLine, messageLabel);

        messageHBox.getChildren().addAll(avatar, textVBox);
        messagesContainer.getChildren().add(messageHBox);
    }

    private HBox buildMessageBubble(String author, String content, String side) {
        HBox msgBox = new HBox();
        msgBox.setSpacing(8);

        // Label pour l'auteur
        Label authorLabel = new Label(author + " :");
        authorLabel.setStyle("-fx-text-fill: #B9BBBE; -fx-font-weight: bold;");

        // Label pour le contenu
        Label contentLabel = new Label(content);
        contentLabel.setStyle("-fx-text-fill: white; -fx-wrap-text: true;");

        msgBox.getChildren().addAll(authorLabel, contentLabel);

        // Alignement : si c’est "me", on aligne à droite
        if ("me".equals(side)) {
            msgBox.setAlignment(Pos.CENTER_RIGHT);
        } else {
            msgBox.setAlignment(Pos.CENTER_LEFT);
        }

        return msgBox;
    }

    @FXML
    private void handleSendMessage() {
        String message = messageInputField.getText().trim();
        if (!message.isEmpty()) {
            // 1) Afficher localement le message (ajouter dans messagesContainer)
            messagesContainer.getChildren().add(buildMessageBubble("Moi", message, "me"));

            // 2) (Facultatif) Envoyer le message vers l’API
            //   sendMessageToAPI(friendNameLabel.getText(), message);

            // 3) Vider le champ et scroller en bas
            messageInputField.clear();
            Platform.runLater(() -> scrollPane.setVvalue(1.0));
        }
    }

    // Appliquer un masque circulaire à une ImageView
    private void applyCircularClip(ImageView imageView) {
        double radius = Math.min(imageView.getFitWidth(), imageView.getFitHeight()) / 2;
        Circle clip = new Circle(radius);
        clip.setCenterX(imageView.getFitWidth() / 2);
        clip.setCenterY(imageView.getFitHeight() / 2);
        imageView.setClip(clip);
    }

    // Envoi d'une requête de changement de statut à l'API
    private void sendRequest(int TypeRequeqst, String informations) {
        String url = API_URL;
        String json = "";
        String token;

        switch (TypeRequeqst) {
            case 1:
                url += "/users/status";
                json = "{\"status\":\"" + informations + "\"}";
                break;
        }

        token = Utilisateur.getInstance().getToken();
        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(url)
                .patch(body)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Platform.runLater(() -> showAlert("Erreur", "Impossible de se connecter au serveur."));
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        Platform.runLater(() -> showAlert("Succès", "Changement de status réussi !"));
                    } else {
                        Platform.runLater(() -> showAlert("Erreur", "Échec du changement de status. Code: " + response.code()));
                    }
                }
            }
        });
    }

    // Création du menu contextuel pour le statut
    private ContextMenu createStatusMenu() {
        ContextMenu menu = new ContextMenu();
        menu.setStyle("-fx-background-color: #2F3136; -fx-background-radius: 5;");
        MenuItem online = createStyledMenuItem("En ligne", "#43B581");
        MenuItem busy = createStyledMenuItem("Occupé", "#F04747");
        MenuItem dnd = createStyledMenuItem("Absent", "#FAA61A");
        MenuItem invisible = createStyledMenuItem("Invisible", "#747F8D");
        online.setOnAction(e -> changeStatus(Utilisateur.Status.ONLINE));
        busy.setOnAction(e -> changeStatus(Utilisateur.Status.BUSY));
        dnd.setOnAction(e -> changeStatus(Utilisateur.Status.DND));
        invisible.setOnAction(e -> changeStatus(Utilisateur.Status.INVISIBLE));
        menu.getItems().addAll(online, busy, dnd, invisible);
        return menu;
    }

    // Création d'un item du menu stylisé
    private MenuItem createStyledMenuItem(String text, String color) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 2;");
        HBox container = new HBox(label);
        container.setStyle("-fx-background-color: transparent; -fx-padding: 2;");
        container.setOnMouseExited(e -> container.setStyle("-fx-background-color: transparent; -fx-padding: 2;"));
        MenuItem menuItem = new MenuItem();
        menuItem.setGraphic(container);
        return menuItem;
    }

    // Changer le statut utilisateur
    private void changeStatus(Utilisateur.Status newStatus) {
        Utilisateur.getInstance().setStatutEnum(newStatus);
        updateStatusColor(2);
    }

    // Met à jour la pastille et le texte du statut utilisateur
    private void updateStatusColor(int type) {
        Utilisateur.Status currentStatus = Utilisateur.getInstance().getStatut();
        switch (currentStatus) {
            case ONLINE:
                statusIndicator.setFill(Color.web("#43B581"));
                statusText.setText("En ligne");
                if (type == 2) sendRequest(1, "ONLINE");
                break;
            case DND:
                statusIndicator.setFill(Color.web("#F04747"));
                statusText.setText("Occupé");
                if (type == 2) sendRequest(1, "DO_NOT_DISTURB");
                break;
            case BUSY:
                statusIndicator.setFill(Color.web("#FAA61A"));
                statusText.setText("Absent");
                if (type == 2) sendRequest(1, "IDLE");
                break;
            case INVISIBLE:
                statusIndicator.setFill(Color.web("#747F8D"));
                statusText.setText("Invisible");
                if (type == 2) sendRequest(1, "INVISIBLE");
                break;
        }
    }

    @FXML
    private void handleLogoutClick() {
        try {
            sendRequest(1, "OFFLINE");

            Utilisateur.reset();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/discordlike_client/hello-view.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(false);
        } catch (IOException e) {
            showAlert("Erreur", "Erreur lors du clique sur le bouton de déconnexion");
        }
    }

    @FXML
    private void handlePrivateMessagetClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/discordlike_client/main-view.fxml"));
            Parent root = loader.load();

            // Affichage dans la même fenêtre
            Stage stage = (Stage) rootPane.getScene().getWindow();
            // 1) Mémoriser l'état avant de changer la scène
            boolean wasMaximized = stage.isMaximized();
            double oldWidth = stage.getWidth();
            double oldHeight = stage.getHeight();

            // 2) Charger votre nouveau root FXML
            stage.setScene(new Scene(root));

            // 3) Restaurer l'état (maximisé) ou la taille
            stage.setMaximized(wasMaximized);

            stage.setWidth(oldWidth);
            stage.setHeight(oldHeight);
        } catch (IOException e) {
            showAlert("Erreur", "Erreur lors du clique sur la liste d'amis");
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
