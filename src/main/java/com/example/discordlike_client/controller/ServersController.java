package com.example.discordlike_client.controller;

import com.example.discordlike_client.model.ServerItem;
import com.example.discordlike_client.model.Utilisateur;
import com.example.discordlike_client.websocket.GlobalWebSocketClient;
import com.google.gson.Gson;
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
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import okhttp3.*;

import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServersController implements GlobalWebSocketClient.MessageListener {

    private ServerItem serverItem;
    private final int serverId = 1;

    // Ajoutez cette ligne parmi vos autres @FXML
    @FXML private VBox channelsContainer;
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

    @FXML private VBox onlineUsersContainer;
    @FXML private VBox offlineUsersContainer;

    private Map<String, Circle> friendStatusCircles = new HashMap<>();
    private Map<String, HBox> friendItemNodes = new HashMap<>();

    // URL de l'API et client HTTP
    private static final String API_URL = "http://163.172.34.212:8080/api";
    private final OkHttpClient client = new OkHttpClient();

    // Classe pour représenter un message de serveur reçu
    private static class ServerMessage {
        String sender;
        String content;
        String timestamp;
        int serverId;
    }

    // Classe pour représenter le message à envoyer
    private static class OutgoingServerMessage {
        String content;
        String timestamp;
        int serverId;

        public OutgoingServerMessage(String content, String timestamp, int serverId) {
            this.content = content;
            this.timestamp = timestamp;
            this.serverId = serverId;
        }
    }

    // Classe pour représenter un utilisateur du serveur (déjà présente)
    private static class ServerUser {
        String username;
        String avatar;
        String status; // "ONLINE", "DND", "BUSY" pour les utilisateurs en ligne ; "OFFLINE", "INVISIBLE" pour hors ligne

        public ServerUser(String username, String avatar, String status) {
            this.username = username;
            this.avatar = avatar;
            this.status = status;
        }

        public String getOnlineStatus() {
            return status;
        }
    }

    private static class FriendStatusMessage {
        String friendUsername;
        String friendOnlineStatus;
        String avatar;
    }

    public void setServerItem(ServerItem serverItem) {
        this.serverItem = serverItem;
        // Par exemple, pour afficher le nom du canal général
        channelNameLabel.setText("# général");
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

        userAvatar.setImage(new Image(utilisateur.getImagePath(), true));
        applyCircularClip(userAvatar);

        // Mettre à jour le statut utilisateur (pastille et texte)
        updateStatusColor(1);

        // Créer et appliquer le menu contextuel pour le statut
        ContextMenu statusMenu = createStatusMenu();
        userAvatar.setOnMouseClicked((MouseEvent event) -> {
            statusMenu.show(statusIndicator, event.getScreenX(), event.getScreenY());
            userAvatar.setStyle("-fx-cursor: hand;");
        });

        loadTextChannels();

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

        // Au démarrage, on peut lancer le fetch des messages du serveur
        fetchServerMessages();
        fetchServerUsers();
    }

    private void loadTextChannels() {
        // Exemple de liste de salons de texte (à remplacer par une récupération via API si nécessaire)
        String[] channels = {"général", "test", "annonces", "discussions"};

        channelsContainer.getChildren().clear();

        // En-tête de la catégorie
        HBox header = new HBox();
        header.setSpacing(3);
        header.setAlignment(Pos.CENTER_LEFT);
        Label headerLabel = new Label("SALONS TEXTUELS");
        headerLabel.setStyle("-fx-text-fill: #8E9297; -fx-font-size: 12px; -fx-font-weight: bold;");
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label plusLabel = new Label("+");
        plusLabel.setStyle("-fx-text-fill: #8E9297; -fx-cursor: hand; -fx-font-size: 16px; -fx-font-weight: bold;");
        header.getChildren().addAll(headerLabel, spacer, plusLabel);
        channelsContainer.getChildren().add(header);

        // Pour chaque salon de texte, création d'un HBox avec icône et label
        for (String channelName : channels) {
            HBox channelBox = new HBox(5);
            channelBox.setAlignment(Pos.CENTER_LEFT);
            channelBox.setStyle("-fx-cursor: hand;");
            ImageView channelIcon = new ImageView(new Image(getClass().getResource("/Image/chanel-texte.png").toExternalForm()));
            channelIcon.setFitWidth(18);
            channelIcon.setFitHeight(18);
            Label channelLabel = new Label(channelName);
            channelLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 16px;");
            channelBox.getChildren().addAll(channelIcon, channelLabel);
            channelBox.setOnMouseClicked(e -> {
                channelNameLabel.setText("# " + channelName);
                // Chargez la conversation correspondante si besoin
            });
            channelsContainer.getChildren().add(channelBox);
        }
    }

    private void fetchServerUsers() {
        String token = Utilisateur.getInstance().getToken();

        // Requête pour récupérer les utilisateurs en ligne (/api/users/connected)
        Request onlineRequest = new Request.Builder()
                .url(API_URL + "/users/connected")
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();
        client.newCall(onlineRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Platform.runLater(() -> showAlert("Erreur", "Impossible de récupérer les utilisateurs en ligne."));
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    String json = body.string();
                    if (response.isSuccessful()) {
                        ServerUser[] onlineUsers = new Gson().fromJson(json, ServerUser[].class);
                        Platform.runLater(() -> {
                            onlineUsersContainer.getChildren().clear();
                            Label onlineTitle = new Label("En ligne");
                            onlineTitle.setStyle("-fx-text-fill: #8E9297; -fx-font-size: 12px; -fx-font-weight: bold;");
                            onlineUsersContainer.getChildren().add(onlineTitle);
                            for (ServerUser user : onlineUsers) {
                                HBox userItem = createUserItem(user);
                                onlineUsersContainer.getChildren().add(userItem);
                            }
                        });
                    } else {
                        Platform.runLater(() -> showAlert("Erreur", "Erreur lors de la récupération des utilisateurs en ligne. Code: " + response.code()));
                    }
                }
            }
        });

        // Requête pour récupérer les utilisateurs hors ligne (/api/users/unavailable)
        Request offlineRequest = new Request.Builder()
                .url(API_URL + "/users/unavailable")
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();
        client.newCall(offlineRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Platform.runLater(() -> showAlert("Erreur", "Impossible de récupérer les utilisateurs hors ligne."));
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    String json = body.string();
                    if (response.isSuccessful()) {
                        ServerUser[] offlineUsers = new Gson().fromJson(json, ServerUser[].class);
                        Platform.runLater(() -> {
                            offlineUsersContainer.getChildren().clear();
                            Label offlineTitle = new Label("Hors ligne");
                            offlineTitle.setStyle("-fx-text-fill: #8E9297; -fx-font-size: 12px; -fx-font-weight: bold;");
                            offlineUsersContainer.getChildren().add(offlineTitle);
                            for (ServerUser user : offlineUsers) {
                                HBox userItem = createUserItem(user);
                                offlineUsersContainer.getChildren().add(userItem);
                            }
                        });
                    } else {
                        Platform.runLater(() -> showAlert("Erreur", "Erreur lors de la récupération des utilisateurs hors ligne. Code: " + response.code()));
                    }
                }
            }
        });
    }

    // Récupère la conversation du serveur via une requête GET
    private void fetchServerMessages() {
        String url = API_URL + "/messages/server/" + serverId;

        String token = Utilisateur.getInstance().getToken();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Platform.runLater(() -> showAlert("Erreur", "Impossible de récupérer les messages du serveur."));
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String json = responseBody.string();
                    if (response.isSuccessful()) {
                        ServerMessage[] messages = new Gson().fromJson(json, ServerMessage[].class);
                        Platform.runLater(() -> {
                            messagesContainer.getChildren().clear();
                            for (ServerMessage msg : messages) {
                                // Ajoute le message à l'interface
                                addMessage(msg.sender, msg.timestamp, msg.content, Utilisateur.getInstance().getImagePath());
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

    // Crée un item (HBox) pour représenter un utilisateur
    private HBox createUserItem(ServerUser user) {
        HBox container = new HBox(10);
        container.setAlignment(Pos.CENTER_LEFT);

        // Récupération de l'avatar (avec avatar par défaut si nécessaire)
        String avatarUrl = (user.avatar != null && !user.avatar.isEmpty())
                ? user.avatar
                : "https://www.pngmart.com/files/22/User-Avatar-Profile-Download-PNG-Isolated-Image.png";
        ImageView avatarView = new ImageView(new Image(avatarUrl, true));
        avatarView.setFitWidth(30);
        avatarView.setFitHeight(30);
        Circle clip = new Circle(15, 15, 15);
        avatarView.setClip(clip);

        // Création de la pastille de statut
        Circle statusCircle = new Circle(5);
        String status = (user.status != null) ? user.status : "OFFLINE";
        switch (status) {
            case "ONLINE":
                statusCircle.setFill(Color.web("#43B581"));
                break;
            case "DO_NOT_DISTURB":
                statusCircle.setFill(Color.web("#F04747"));
                break;
            case "IDLE":
                statusCircle.setFill(Color.web("#FAA61A"));
                break;
            default:
                statusCircle.setFill(Color.web("#747F8D"));
                break;
        }

        // Empilement de l'avatar et de la pastille
        StackPane avatarStack = new StackPane();
        avatarStack.getChildren().addAll(avatarView, statusCircle);
        StackPane.setAlignment(statusCircle, Pos.BOTTOM_RIGHT);

        // Création du label du nom d'utilisateur
        String username = (user.username != null && !user.username.isEmpty())
                ? user.username
                : "Inconnu";
        Label usernameLabel = new Label(username);
        usernameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        container.getChildren().addAll(avatarStack, usernameLabel);

        // Stocker la référence à la pastille et à l'item dans les maps
        friendStatusCircles.put(username, statusCircle);
        friendItemNodes.put(username, container);

        return container;
    }

    private boolean isOnline(String status) {
        return "ONLINE".equals(status) || "DO_NOT_DISTURB".equals(status) || "IDLE".equals(status);
    }

    // Méthode appelée depuis le MainViewController
    public void setChannelName(String channelName) {
        // Le label affiché en haut de la colonne principale
        channelNameLabel.setText(channelName);
    }

    // Méthode utilitaire pour obtenir le timestamp courant au format yyyy-MM-dd'T'HH:mm:ss
    private String getCurrentTimestamp() {
        return LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
    }

    // Ajoute un message dans la VBox messagesContainer
    private void addMessage(String author, String time, String content, String avatarPath) {
        HBox messageHBox = new HBox(10);
        messageHBox.setStyle("-fx-background-color: transparent;");
        messageHBox.setOnMouseEntered(e -> messageHBox.setStyle("-fx-background-color: #40444B;"));
        messageHBox.setOnMouseExited(e -> messageHBox.setStyle("-fx-background-color: transparent;"));

        // Avatar
        ImageView avatar = new ImageView();
        avatar.setImage(new Image(Utilisateur.getInstance().getImagePath(), true));
        avatar.setFitWidth(40);
        avatar.setFitHeight(40);
        Circle clip = new Circle(20, 20, 20);
        avatar.setClip(clip);

        // Texte
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
        String content = messageInputField.getText().trim();
        if (!content.isEmpty()) {
            String timestamp = getCurrentTimestamp();
            OutgoingServerMessage outgoing = new OutgoingServerMessage(content, timestamp, serverId);
            String json = new Gson().toJson(outgoing);

            // Envoi du message via WebSocket
            GlobalWebSocketClient.getInstance().sendGlobalMessage(json);

            // Ajout du message dans l'interface locale
            addMessage(Utilisateur.getInstance().getPseudo(), timestamp, content, Utilisateur.getInstance().getImagePath());
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

    @Override
    public void onMessageReceived(String message) {
        Platform.runLater(() -> {
            try {
                if (message.contains("friendUsername")) {
                    // Le message concerne une mise à jour de statut d'un ami
                    FriendStatusMessage friendStatus = new Gson().fromJson(message, FriendStatusMessage.class);
                    updateFriendStatus(friendStatus);
                } else {
                    // Message de serveur habituel
                    ServerMessage incoming = new Gson().fromJson(message, ServerMessage.class);
                    if (incoming.serverId == serverId) {
                        addMessage(incoming.sender, incoming.timestamp, incoming.content, Utilisateur.getInstance().getImagePath());
                        scrollPane.setVvalue(1.0);
                    }
                }
            } catch (Exception e) {
                showAlert("Erreur", "Erreur lors de la réception d'un message");
            }
        });
    }

    private void updateFriendStatus(FriendStatusMessage friendStatus) {
        // Mise à jour de la pastille (couleur)
        Circle statusCircle = friendStatusCircles.get(friendStatus.friendUsername);
        if (statusCircle != null) {
            switch (friendStatus.friendOnlineStatus) {
                case "ONLINE":
                    statusCircle.setFill(Color.web("#43B581"));
                    break;
                case "DO_NOT_DISTURB":
                    statusCircle.setFill(Color.web("#F04747"));
                    break;
                case "IDLE":
                    statusCircle.setFill(Color.web("#FAA61A"));
                    break;
                default:
                    statusCircle.setFill(Color.web("#747F8D"));
                    break;
            }
        }

        // Récupération de l'item utilisateur
        HBox friendItem = friendItemNodes.get(friendStatus.friendUsername);
        if (friendItem != null) {
            // On détermine dans quel conteneur l'item doit se trouver
            boolean shouldBeOnline = isOnline(friendStatus.friendOnlineStatus);
            // On récupère le conteneur actuel (online ou offline)
            VBox currentContainer = (VBox) friendItem.getParent();
            // Si l'item n'est pas dans le bon conteneur, on le déplace
            if (shouldBeOnline && currentContainer != onlineUsersContainer) {
                if (currentContainer != null) {
                    currentContainer.getChildren().remove(friendItem);
                }
                onlineUsersContainer.getChildren().add(friendItem);
            } else if (!shouldBeOnline && currentContainer != offlineUsersContainer) {
                if (currentContainer != null) {
                    currentContainer.getChildren().remove(friendItem);
                }
                offlineUsersContainer.getChildren().add(friendItem);
            }
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
