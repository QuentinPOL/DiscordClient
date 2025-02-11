package com.example.discordlike_client.controller;

import com.example.discordlike_client.model.ServerItem;
import com.example.discordlike_client.model.Utilisateur;
import com.example.discordlike_client.websocket.GlobalWebSocketClient;
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
    @FXML private ListView<String> privateMessagesList;
    @FXML private ListView<ServerItem> serversListView;

    @FXML private StackPane discordLogoContainer;
    @FXML private ImageView discordLogo;
    @FXML private ImageView userAvatar;
    @FXML private Label statusText;
    @FXML private Circle statusIndicator;

    // URL de l'API et client HTTP
    private static final String API_URL = "http://163.172.34.212:8080/api";
    private final OkHttpClient client = new OkHttpClient();

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
        String imagePath = getClass().getResource("/Image/pp.jpg").toExternalForm();
        userAvatar.setImage(new Image(imagePath));
        applyCircularClip(userAvatar);

        // Mettre à jour le statut utilisateur (pastille et texte)
        updateStatusColor(1);

        // Créer et appliquer le menu contextuel pour le statut
        ContextMenu statusMenu = createStatusMenu();
        userAvatar.setOnMouseClicked((MouseEvent event) -> {
            statusMenu.show(statusIndicator, event.getScreenX(), event.getScreenY());
        });

        // Configuration de la ListView des messages privés
        privateMessagesList.getItems().addAll("alexpel", "b-KOS", "cloudbray", "Joris HURTEL");
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

        // Charger/construire la conversation
        loadConversationForFriend(friendName);
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
            GlobalWebSocketClient.getInstance().sendGlobalMessage("CHAT_MESSAGE:" + message);

            // Ajout du message dans l’interface utilisateur locale
            addMessage("Moi", "maintenant", message, "/Image/pp.jpg");
            messageInputField.clear();
            Platform.runLater(() -> scrollPane.setVvalue(1.0));
        }
    }

    // Cell factory pour la ListView des messages privés
    private void privateMessagesCellFactory() {
        privateMessagesList.setCellFactory(list -> new ListCell<String>() {
            // Déclaration des composants pour la cellule
            private HBox container = new HBox(10);
            private StackPane avatarStack = new StackPane();
            private ImageView avatar = new ImageView();
            private Circle statusIndicator = new Circle(6);
            private VBox textContainer = new VBox(2);
            private Label pseudoLabel = new Label();
            private Label statusLabel = new Label();

            {
                // Configuration de l'avatar : taille et clip circulaire
                avatar.setFitWidth(40);
                avatar.setFitHeight(40);
                Circle clip = new Circle(20, 20, 20);
                avatar.setClip(clip);

                // Configuration de l'indicateur de statut (pastille)
                statusIndicator.setStroke(Color.WHITE);
                statusIndicator.setStrokeWidth(2);
                statusIndicator.setFill(Color.web("#43B581"));

                // On superpose l'indicateur sur l'avatar
                avatarStack.getChildren().addAll(avatar, statusIndicator);
                StackPane.setAlignment(statusIndicator, Pos.BOTTOM_RIGHT);

                // Configuration des labels (pseudo et libellé du statut)
                pseudoLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
                statusLabel.setStyle("-fx-text-fill: #B9BBBE; -fx-font-size: 12px;");
                textContainer.getChildren().addAll(pseudoLabel, statusLabel);

                container.setAlignment(Pos.CENTER_LEFT);
                container.getChildren().addAll(avatarStack, textContainer);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    pseudoLabel.setText(item);
                    statusLabel.setText("En ligne");
                    statusIndicator.setFill(Color.web("#43B581"));  // Par exemple : vert pour "en ligne"

                    // Charger l'image de l'avatar (à remplacer par le chemin dynamique de l’avatar de l’ami)
                    avatar.setImage(new Image(getClass().getResource("/Image/pp.jpg").toExternalForm()));

                    this.setOnMouseEntered(e -> {
                        setStyle("-fx-background-color: #40444B;");
                    });
                    this.setOnMouseExited(e -> {
                        // Couleur de fond quand la cellule n'est pas sélectionnée
                        setStyle("-fx-background-color: transparent;");
                    });

                    setGraphic(container);

                    // Gestion du clic sur la cellule : ouverture de la conversation
                    //container.setOnMouseClicked(e -> onMessagePrivateClicked(item));
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

    @Override
    public void onMessageReceived(String message) {
        Platform.runLater(() -> {
            System.out.println("Message reçu dans PrivateMessagesController : " + message);
            // Mettez à jour la conversation ici
            addMessage("Serveur", "maintenant", message, "/Image/pp.jpg");
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}