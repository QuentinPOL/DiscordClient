package com.example.discordlike_client.controller;

import com.example.discordlike_client.model.Friend;
import com.example.discordlike_client.model.FriendStatus;
import com.example.discordlike_client.model.ServerItem;
import com.example.discordlike_client.model.Utilisateur;
import com.example.discordlike_client.websocket.GlobalWebSocketClient;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import okhttp3.*;

import java.io.IOException;

public class PrivateMessagesController implements GlobalWebSocketClient.MessageListener {

    @FXML
    private Label friendNameLabel;
    @FXML
    private VBox messagesContainer;    // Contiendra les messages
    @FXML
    private TextField messageInputField;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private Button amisButton;

    @FXML private HBox rootPane;
    @FXML private Label usernameLabel;
    @FXML private ListView<Friend> privateMessagesList;
    @FXML private ListView<ServerItem> serversListView;

    @FXML private StackPane discordLogoContainer;
    @FXML private ImageView discordLogo;
    @FXML private ImageView userAvatar;
    @FXML private Label statusText;
    @FXML private Circle statusIndicator;

    private ObservableList<Friend> friendsList = FXCollections.observableArrayList();

    // URL de l'API et client HTTP
    private static final String API_URL = "http://163.172.34.212:8080/api";
    private final OkHttpClient client = new OkHttpClient();

    // Classe interne pour le mapping JSON des messages privés
    private static class PrivateMessage {
        int id;
        String sender;
        String content;
        String timestamp;
        String serverId;
        String recipientUsername;
        String type;
    }

    // Classe interne pour le mapping JSON de l'API
    private static class FriendApiResponse {
        String friendUsername;
        int friendshipId;
        String friendshipStatus;
        String friendOnlineStatus;
        String avatar;
        String requestType;
    }

    private static class OutgoingPrivateMessage {
        String recipientUsername;
        String content;
        String timestamp;

        public OutgoingPrivateMessage(String recipientUsername, String content, String timestamp) {
            this.recipientUsername = recipientUsername;
            this.content = content;
            this.timestamp = timestamp;
        }
    }

    // Classe interne pour le mapping de la mise à jour de statut d'ami
    private static class FriendStatusUpdate {
        String friendUsername;
        String friendOnlineStatus;
        String avatar;
    }

    // Méthode pour convertir une chaîne en FriendStatus
    private FriendStatus mapStatus(String status) {
        switch (status) {
            case "ONLINE": return FriendStatus.ONLINE;
            case "OFFLINE": return FriendStatus.OFFLINE;
            case "BLOCKED": return FriendStatus.BLOCKED;
            case "DO_NOT_DISTURB": return FriendStatus.DND;
            case "IDLE": return FriendStatus.BUSY;
            default: return FriendStatus.OFFLINE;
        }
    }

    @FXML
    public void initialize() {
        GlobalWebSocketClient.getInstance().addMessageListener(this);

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
        userAvatar.setImage(new Image(utilisateur.getImagePath(), true));
        applyCircularClip(userAvatar);

        // Mettre à jour le statut utilisateur (pastille et texte)
        updateStatusColor(1);

        // Créer et appliquer le menu contextuel pour le statut
        ContextMenu statusMenu = createStatusMenu();
        userAvatar.setOnMouseClicked((MouseEvent event) -> {
            statusMenu.show(statusIndicator, event.getScreenX(), event.getScreenY());
        });

        // Configuration de la ListView des messages privés
        privateMessagesList.setItems(friendsList);
        privateMessagesCellFactory();

        // Liste de serveurs (icônes)
        serversListView.getItems().addAll(
                new ServerItem("/Image/6537937.jpg")
        );
        serversListView.setFocusTraversable(false);
        privateMessagesList.setFocusTraversable(false);

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
                setOnMouseEntered(event -> setStyle("-fx-background-color: #40444B; -fx-background-radius: 5;"));
                setOnMouseExited(event -> setStyle("-fx-background-color: transparent;"));

                // Clic : ouvrir la vue server-view.fxml
                setOnMouseClicked(event -> {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/discordlike_client/server-view.fxml"));
                        Parent root = loader.load();

                        // Récupération du contrôleur et passage du serverItem
                        ServersController controller = loader.getController();
                        controller.setServerItem(item);

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
                        e.printStackTrace();
                    }
                });
            }
        });

        // On désactive les données fictives et on récupère les listes depuis l'API
        fetchAcceptedFriends();

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

        // 1) Créer le HBox + ImageView + Label
        HBox hbox = new HBox(10);
        hbox.setAlignment(Pos.CENTER_LEFT);

        ImageView imageView = new ImageView(
                getClass().getResource("/Image/friends.png").toExternalForm()
        );
        imageView.setFitWidth(28);
        imageView.setFitHeight(28);

        Label label = new Label("Amis");
        label.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        hbox.getChildren().addAll(imageView, label);

        // 2) Affecter le HBox comme "graphic" du bouton
        amisButton.setGraphic(hbox);
        amisButton.setContentDisplay(ContentDisplay.LEFT);

        // Rendre l’image elle-même circulaire
        applyCircularClip(discordLogo);

        // Gérer le hover sur le conteneur pour changer la bordure en blanc
        discordLogoContainer.setOnMouseEntered(e -> {
            discordLogoContainer.setStyle(
                    "-fx-border-color: cyan; " +
                            "-fx-border-width: 2; " +
                            "-fx-border-radius: 30; " +
                            "-fx-background-radius: 30; " +
                            "-fx-padding: 4;"
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
    public void setFriendName(String friendName) {
        // Le label affiché en haut de la colonne principale
        friendNameLabel.setText(friendName);

        // Dès que l'on change d'ami, on récupère la conversation depuis l'API
        fetchPrivateMessages(friendName);
    }

    // Récupère les messages privés entre l'utilisateur courant et l'ami sélectionné
    private void fetchPrivateMessages(String friendName) {
        String currentUser = Utilisateur.getInstance().getPseudo();
        String url = API_URL + "/messages/private?user1=" + currentUser + "&user2=" + friendName;
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + Utilisateur.getInstance().getToken())
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Platform.runLater(() -> showAlert("Erreur", "Impossible de récupérer les messages privés."));
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String responseBodyString = responseBody.string();
                    if (response.isSuccessful()) {
                        PrivateMessage[] messages = new Gson().fromJson(responseBodyString, PrivateMessage[].class);
                        Platform.runLater(() -> {
                            messagesContainer.getChildren().clear();
                            for (PrivateMessage msg : messages) {
                                // Déterminer l'auteur et l'avatar à afficher
                                String author = msg.sender.equalsIgnoreCase(Utilisateur.getInstance().getPseudo())
                                        ? Utilisateur.getInstance().getPseudo()
                                        : friendName;

                                addMessage(author, msg.timestamp, msg.content, Utilisateur.getInstance().getImagePath());
                            }
                            scrollPane.setVvalue(1.0);
                        });
                    } else {
                        Platform.runLater(() -> showAlert("Erreur", "Erreur lors de la récupération des messages. Code: " + response.code()));
                    }
                }
            }
        });
    }

    // Méthode d'affichage d'un message dans la VBox messagesContainer
    private void addMessage(String author, String time, String content, String avatarPath) {
        HBox messageHBox = new HBox(10);
        messageHBox.setStyle("-fx-background-color: transparent;");
        messageHBox.setOnMouseEntered(e -> messageHBox.setStyle("-fx-background-color: #40444B;"));
        messageHBox.setOnMouseExited(e -> messageHBox.setStyle("-fx-background-color: transparent;"));

        ImageView avatar = new ImageView();
        avatar.setImage(new Image(avatarPath, true));

        avatar.setFitWidth(40);
        avatar.setFitHeight(40);
        Circle clip = new Circle(20, 20, 20);
        avatar.setClip(clip);

        VBox textVBox = new VBox(3);
        HBox authorLine = new HBox(5);
        Label authorLabel = new Label(author);
        authorLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold;");
        Label timeLabel = new Label(time);
        timeLabel.setStyle("-fx-text-fill: #72767D; -fx-font-size: 12px;");
        authorLine.getChildren().addAll(authorLabel, timeLabel);

        Label messageLabel = new Label(content);
        messageLabel.setStyle("-fx-text-fill: #DCDDDE; -fx-wrap-text: true; -fx-font-size: 14px;");
        messageLabel.setMaxWidth(500);

        textVBox.getChildren().addAll(authorLine, messageLabel);
        messageHBox.getChildren().addAll(avatar, textVBox);
        messagesContainer.getChildren().add(messageHBox);
    }

    @FXML
    private void handleSendMessage() {
        String message = messageInputField.getText().trim();
        if (!message.isEmpty()) {
            // Le destinataire correspond au nom affiché dans le header de la conversation
            String recipient = friendNameLabel.getText();
            String timestamp = getCurrentTimestamp();

            OutgoingPrivateMessage outgoing = new OutgoingPrivateMessage(recipient, message, timestamp);
            String json = new Gson().toJson(outgoing);

            // Envoyer le JSON via le WebSocket (l'URL utilisée est celle initialisée dans GlobalWebSocketClient)
            GlobalWebSocketClient.getInstance().sendGlobalMessage(json);

            // Ajout du message dans l'interface locale (vous pouvez afficher votre pseudo, ici extrait de Utilisateur)
            addMessage(Utilisateur.getInstance().getPseudo(), timestamp, message, Utilisateur.getInstance().getImagePath());
            messageInputField.clear();
            Platform.runLater(() -> scrollPane.setVvalue(1.0));
        }
    }

    private String getCurrentTimestamp() {
        return java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
    }

    // Cell factory pour la ListView des messages privés (liste d'amis)
    private void privateMessagesCellFactory() {
        privateMessagesList.setCellFactory(list -> new ListCell<Friend>() {
            private HBox container = new HBox(10);
            private StackPane avatarStack = new StackPane();
            private ImageView avatar = new ImageView();
            private Circle statusIndicator = new Circle(6);
            private VBox textContainer = new VBox(2);
            private Label pseudoLabel = new Label();

            {
                avatar.setFitWidth(40);
                avatar.setFitHeight(40);
                Circle clip = new Circle(20, 20, 20);
                avatar.setClip(clip);
                statusIndicator.setStroke(Color.WHITE);
                statusIndicator.setStrokeWidth(2);
                StackPane.setAlignment(statusIndicator, Pos.BOTTOM_RIGHT);
                avatarStack.getChildren().addAll(avatar, statusIndicator);
                pseudoLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
                textContainer.getChildren().add(pseudoLabel);
                container.setAlignment(Pos.CENTER_LEFT);
                container.getChildren().addAll(avatarStack, textContainer);
                this.setOnMouseEntered(e -> {
                    if (!isEmpty()) {
                        setStyle("-fx-background-color: #40444B; -fx-cursor: hand;");
                    }
                });
                this.setOnMouseExited(e -> {
                    if (!isEmpty()) {
                        setStyle("-fx-background-color: transparent;");
                    }
                });
            }

            @Override
            protected void updateItem(Friend friend, boolean empty) {
                super.updateItem(friend, empty);
                if (empty || friend == null) {
                    setGraphic(null);
                } else {
                    pseudoLabel.setText(friend.getPseudo());

                    // Chargement de l’avatar depuis le chemin dynamique
                    avatar.setImage(new Image(friend.getAvatarPath(), true));

                    switch (friend.getOnlineStatus()) {
                        case ONLINE:
                            statusIndicator.setFill(Color.web("#43B581"));
                            break;
                        case OFFLINE:
                            statusIndicator.setFill(Color.web("#747F8D"));
                            break;
                        case BLOCKED:
                            statusIndicator.setFill(Color.web("#F04747"));
                            break;
                        case DND:
                            statusIndicator.setFill(Color.web("#F04747"));
                            break;
                        case BUSY:
                            statusIndicator.setFill(Color.web("#FAA61A"));
                            break;
                        default:
                            statusIndicator.setFill(Color.web("#747F8D"));
                            break;
                    }
                    setGraphic(container);
                    container.setOnMouseClicked(e -> {
                        // Lorsque l'on clique sur un ami, on affiche sa conversation
                        setFriendName(friend.getPseudo());
                    });
                }
            }
        });
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

    // Récupérer la liste des amis acceptés via l'API
    private void fetchAcceptedFriends() {
        String url = API_URL + "/friends/list";
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + Utilisateur.getInstance().getToken())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Platform.runLater(() -> showAlert("Erreur", "Impossible de récupérer la liste des amis."));
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        String json = responseBody.string();
                        Gson gson = new Gson();
                        MainViewController.FriendApiResponse[] friendsResponse = gson.fromJson(json, MainViewController.FriendApiResponse[].class);

                        Platform.runLater(() -> {
                            friendsList.clear();
                            for (MainViewController.FriendApiResponse fr : friendsResponse) {
                                FriendStatus status;
                                switch (fr.friendOnlineStatus) {
                                    case "ONLINE":
                                        status = FriendStatus.ONLINE;
                                        break;
                                    case "OFFLINE":
                                        status = FriendStatus.OFFLINE;
                                        break;
                                    case "BLOCKED":
                                        status = FriendStatus.BLOCKED;
                                        break;
                                    case "DO_NOT_DISTURB":
                                        status = FriendStatus.DND;
                                        break;
                                    case "IDLE":
                                        status = FriendStatus.BUSY;
                                        break;
                                    default:
                                        status = FriendStatus.OFFLINE;
                                        break;
                                }

                                friendsList.add(new Friend(fr.friendUsername, fr.avatar, FriendStatus.OFFLINE, status));
                            }
                        });
                    } else {
                        Platform.runLater(() -> showAlert("Erreur", "Erreur lors de la récupération des amis acceptés. Code: " + response.code()));
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

            GlobalWebSocketClient.getInstance().removeMessageListener(this);
            GlobalWebSocketClient.getInstance().close();

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
    private void handleFriendListClick() {
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

    // Réception d'un message via WebSocket
    @Override
    public void onMessageReceived(String message) {
        Platform.runLater(() -> {
            // Si le JSON contient "friendUsername" et "friendOnlineStatus", c'est une mise à jour de statut
            if (message.contains("friendUsername") && message.contains("friendOnlineStatus")) {
                try {
                    // Parser le JSON en FriendStatusUpdate
                    FriendStatusUpdate update = new Gson().fromJson(message, FriendStatusUpdate.class);
                    if (update.friendUsername != null && update.friendOnlineStatus != null) {
                        for (Friend f : friendsList) {
                            if (f.getPseudo().equalsIgnoreCase(update.friendUsername)) {
                                f.setOnlineStatus(mapStatus(update.friendOnlineStatus));
                                break;
                            }
                        }
                        // Rafraîchir la ListView pour mettre à jour l'affichage
                        privateMessagesList.refresh();
                    }
                } catch (Exception e) {
                    System.out.println("Erreur lors de la mise à jour du statut : " + e.getMessage());
                }
            } else {
                // Sinon, traiter comme un message privé entrant
                try {
                    PrivateMessage incoming = new Gson().fromJson(message, PrivateMessage.class);
                    if (incoming.recipientUsername.equals(Utilisateur.getInstance().getPseudo()) &&
                            incoming.sender.equals(friendNameLabel.getText())) {
                        String avatarPath = getFriendAvatar(incoming.sender);
                        addMessage(incoming.sender, incoming.timestamp, incoming.content, avatarPath);
                        scrollPane.setVvalue(1.0);
                    }
                } catch (Exception e) {
                    showAlert("Erreur", "Erreur lors de la réception d'un message");
                }
            }
        });
    }

    private String getFriendAvatar(String friendName) {
        // Parcourt la liste des amis pour retrouver l'ami correspondant
        for (Friend f : friendsList) {
            if (f.getPseudo().equalsIgnoreCase(friendName)) {
                return f.getAvatarPath();
            }
        }
        // Si l'ami n'est pas trouvé, vous pouvez renvoyer un avatar par défaut (optionnel)
        return "https://www.pngmart.com/files/22/User-Avatar-Profile-Download-PNG-Isolated-Image.png";
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}