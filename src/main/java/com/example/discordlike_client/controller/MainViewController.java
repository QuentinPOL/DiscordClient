package com.example.discordlike_client.controller;

import com.example.discordlike_client.model.Utilisateur;
import com.example.discordlike_client.model.Friend;
import com.example.discordlike_client.model.FriendStatus;
import com.google.gson.Gson;  // Assurez-vous d'ajouter Gson à vos dépendances
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import okhttp3.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

public class MainViewController {

    @FXML private HBox rootPane;
    @FXML private Label usernameLabel;
    @FXML private ListView<Friend> friendsList;
    @FXML private ListView<String> privateMessagesList;
    @FXML private ListView<ServerItem> serversListView;

    @FXML private ImageView userAvatar;
    @FXML private Circle statusIndicator;
    @FXML private Label statusText;
    @FXML private Label friendsLabel;

    // Boutons de filtrage
    @FXML private Button btnOnline;
    @FXML private Button btnAll;
    @FXML private Button btnPending;
    @FXML private Button btnBlocked;
    @FXML private Button btnAdd;

    // Ajout d'ami
    @FXML private HBox addFriendContainer;
    @FXML private TextField friendNameField;

    // Listes pour gérer les amis
    private ObservableList<Friend> acceptedFriends = FXCollections.observableArrayList();
    private ObservableList<Friend> pendingFriends = FXCollections.observableArrayList();
    private ObservableList<Friend> filteredFriends = FXCollections.observableArrayList();
    private String currentFilter = "ONLINE"; // pour mémoriser le filtre actif

    // URL de l'API et client HTTP
    private static final String API_URL = "http://163.172.34.212:8080/api";
    private final OkHttpClient client = new OkHttpClient();

    // Classe interne pour le mapping JSON de l'API
    private static class FriendApiResponse {
        String friendUsername;
        int friendshipId;
        String friendshipStatus;
        String friendOnlineStatus;
        String avatar;
    }

    // Pour gérer les errur
    private static class ErrorResponse {
        int status;
        String error;
        String message;
        String timestamp;
    }

    @FXML
    public void initialize() {
        // Forcer la fenêtre en mode maximisé et redimensionnable
        Platform.runLater(() -> {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setMaximized(true);
            stage.setResizable(true);
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
                new ServerItem("/Image/testDiscord.png"),
                new ServerItem("/Image/testDiscord.png"),
                new ServerItem("/Image/testDiscord.png")
        );
        serversListView.setFocusTraversable(false);
        privateMessagesList.setFocusTraversable(false);
        friendsList.setFocusTraversable(false);

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
            }
        });

        // On désactive les données fictives et on récupère les listes depuis l'API
        fetchAcceptedFriends();
        fetchPendingFriends();

        // Configuration de la ListView des amis
        friendsList.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent;");
        friendsList.setCellFactory(list -> new ListCell<Friend>() {
            private HBox container = new HBox(10);
            private StackPane avatarPane = new StackPane();
            private ImageView avatar = new ImageView();
            private Circle friendStatusIndicator = new Circle(6);
            private VBox friendInfo = new VBox(2);
            private Label pseudoLabel = new Label();
            private Label statusLabel = new Label();

            {
                // Configuration de l'avatar
                avatar.setFitWidth(40);
                avatar.setFitHeight(40);
                Circle clip = new Circle(20, 20, 20);
                avatar.setClip(clip);

                // Configuration de la pastille de statut
                friendStatusIndicator.setStroke(Color.WHITE);
                friendStatusIndicator.setStrokeWidth(2);
                StackPane.setAlignment(friendStatusIndicator, Pos.BOTTOM_RIGHT);
                avatarPane.getChildren().addAll(avatar, friendStatusIndicator);

                // Configuration des labels
                pseudoLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
                statusLabel.setStyle("-fx-text-fill: #B9BBBE; -fx-font-size: 12px;");
                friendInfo.getChildren().addAll(pseudoLabel, statusLabel);

                // Assemblage dans le conteneur principal
                container.getChildren().addAll(avatarPane, friendInfo);
                container.setAlignment(Pos.CENTER_LEFT);
                container.setStyle(
                        "-fx-background-color: transparent; " +
                                "-fx-border-color: #40444B; " +
                                "-fx-border-width: 0 0 1 0; " +
                                "-fx-padding: 5 10 5 10;"
                );

                // Effet hover
                this.setOnMouseEntered(e -> {
                    container.setStyle(
                            "-fx-background-color: rgba(255,255,255,0.1); " +
                                    "-fx-border-color: #40444B; " +
                                    "-fx-border-width: 0 0 1 0; " +
                                    "-fx-padding: 5 10 5 10;"
                    );
                });
                this.setOnMouseExited(e -> {
                    container.setStyle(
                            "-fx-background-color: transparent; " +
                                    "-fx-border-color: #40444B; " +
                                    "-fx-border-width: 0 0 1 0; " +
                                    "-fx-padding: 5 10 5 10;"
                    );
                });
            }

            @Override
            protected void updateItem(Friend friend, boolean empty) {
                super.updateItem(friend, empty);
                if (empty || friend == null) {
                    setGraphic(null);
                } else {
                    Image img = new Image(getClass().getResource(friend.getAvatarPath()).toExternalForm());
                    avatar.setImage(img);
                    pseudoLabel.setText(friend.getPseudo());

                    // Si l'ami est en attente, on affiche "En attente" mais la couleur dépend du statut en ligne réel
                    if(friend.getFriendshipStatus() == FriendStatus.PENDING) {
                        statusLabel.setText("En attente");
                        switch(friend.getOnlineStatus()){
                            case ONLINE:
                                friendStatusIndicator.setFill(Color.web("#43B581"));
                                break;
                            case OFFLINE:
                                friendStatusIndicator.setFill(Color.web("#747F8D"));
                                break;
                            case DND:
                                friendStatusIndicator.setFill(Color.web("#F04747"));
                                break;
                            case BUSY:
                                friendStatusIndicator.setFill(Color.web("#FAA61A"));
                                break;
                            default:
                                friendStatusIndicator.setFill(Color.web("#747F8D"));
                                break;
                        }
                    } else {
                        // Pour un ami accepté ou bloqué, on utilise directement le statut en ligne réel
                        switch(friend.getOnlineStatus()){
                            case ONLINE:
                                friendStatusIndicator.setFill(Color.web("#43B581"));
                                statusLabel.setText("En ligne");
                                break;
                            case OFFLINE:
                                friendStatusIndicator.setFill(Color.web("#747F8D"));
                                statusLabel.setText("Hors ligne");
                                break;
                            case BLOCKED:
                                friendStatusIndicator.setFill(Color.web("#F04747"));
                                statusLabel.setText("Bloqué");
                                break;
                            case DND:
                                friendStatusIndicator.setFill(Color.web("#F04747"));
                                statusLabel.setText("Occupé");
                                break;
                            case BUSY:
                                friendStatusIndicator.setFill(Color.web("#FAA61A"));
                                statusLabel.setText("Absent");
                                break;
                            default:
                                friendStatusIndicator.setFill(Color.web("#747F8D"));
                                statusLabel.setText("Hors ligne");
                                break;
                        }
                    }
                    setGraphic(container);
                }
            }
        });

        // Lier la ListView à la liste filtrée
        friendsList.setItems(filteredFriends);

        // Appliquer l'effet hover sur les boutons de filtrage
        applyHoverEffect(btnOnline, "-fx-background-color: #5865F2; -fx-text-fill: white;");
        applyHoverEffect(btnAll, "-fx-background-color: transparent; -fx-text-fill: white;");
        applyHoverEffect(btnPending, "-fx-background-color: transparent; -fx-text-fill: white;");
        applyHoverEffect(btnBlocked, "-fx-background-color: transparent; -fx-text-fill: white;");
        applyHoverEffect(btnAdd, "-fx-background-color: #3BA55D; -fx-text-fill: white;");

        // Par défaut, appliquer le filtre sur les amis en ligne
        filterFriends("ONLINE");
    }

    // Méthode de filtrage utilisant les listes récupérées via l'API
    private void filterFriends(String filter) {
        currentFilter = filter;
        filteredFriends.clear();
        switch(filter) {
            case "ONLINE":
                for (Friend f : acceptedFriends) {
                    if (f.getOnlineStatus() == FriendStatus.ONLINE)
                        filteredFriends.add(f);
                }
                updateFriendsLabel("En ligne", filteredFriends.size());
                break;
            case "ALL":
                for (Friend f : acceptedFriends) {
                    // Ici, on affiche par exemple les amis en ligne ou hors ligne
                    if (f.getOnlineStatus() == FriendStatus.ONLINE || f.getOnlineStatus() == FriendStatus.OFFLINE)
                        filteredFriends.add(f);
                }
                updateFriendsLabel("Tous", filteredFriends.size());
                break;
            case "PENDING":
                filteredFriends.addAll(pendingFriends);
                updateFriendsLabel("En Attente", filteredFriends.size());
                break;
            case "BLOCKED":
                for (Friend f : acceptedFriends) {
                    if (f.getOnlineStatus() == FriendStatus.BLOCKED)
                        filteredFriends.add(f);
                }
                updateFriendsLabel("Bloqués", filteredFriends.size());
                break;
        }
    }

    private void updateFriendsLabel(String category, int count) {
        friendsLabel.setText(category + " — " + count);
    }

    // Gestionnaires d'événements pour les boutons de filtre
    @FXML private void handleOnlineFriends() {
        filterFriends("ONLINE");
        addFriendContainer.setVisible(false);
    }
    @FXML private void handleAllFriends() {
        filterFriends("ALL");
        addFriendContainer.setVisible(false);
    }
    @FXML private void handlePendingFriends() {
        filterFriends("PENDING");
        addFriendContainer.setVisible(false);
    }
    @FXML private void handleBlockedFriends() {
        filterFriends("BLOCKED");
        addFriendContainer.setVisible(false);
    }

    // Ajout d'ami
    @FXML private void handleAddFriend() {
        // Afficher le formulaire d'ajout inline
        addFriendContainer.setVisible(true);
        friendNameField.requestFocus();
    }

    @FXML private void handleConfirmAddFriend() {
        String pseudo = friendNameField.getText().trim();
        if (!pseudo.isEmpty()) {
            // Envoyer la demande d'ami vers le serveur
            sendFriendRequest(pseudo);
        }
        // Réinitialiser et masquer le formulaire
        friendNameField.clear();
        addFriendContainer.setVisible(false);
    }

    @FXML private void handleCancelAddFriend() {
        // Annuler l'ajout et masquer le formulaire
        friendNameField.clear();
        addFriendContainer.setVisible(false);
    }

    private void sendFriendRequest(String friendUsername) {
        // Construire l'URL complète
        String url = API_URL + "/friends/send-request";
        // Créer le JSON à envoyer
        String json = "{\"friendUsername\":\"" + friendUsername + "\"}";

        // Création du corps de la requête en indiquant le type JSON
        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));

        // Construction de la requête POST en ajoutant le token d'authentification
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Bearer " + Utilisateur.getInstance().getToken())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Platform.runLater(() -> showAlert("Erreur", "Impossible d'envoyer la demande d'ami."));
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Gson gson = new Gson();
                if (response.isSuccessful()) {
                    // En cas de succès, on récupère les informations de l'ami ajouté
                    FriendApiResponse friendResponse = gson.fromJson(responseBody, FriendApiResponse.class);
                    System.out.println(friendResponse.friendshipId);

                    // On convertit le status online reçu en type FriendStatus
                    FriendStatus onlineStatus;
                    switch(friendResponse.friendOnlineStatus) {
                        case "ONLINE":
                            onlineStatus = FriendStatus.ONLINE;
                            break;
                        case "OFFLINE":
                            onlineStatus = FriendStatus.OFFLINE;
                            break;
                        case "BLOCKED":
                            onlineStatus = FriendStatus.BLOCKED;
                            break;
                        case "DO_NOT_DISTURB":
                            onlineStatus = FriendStatus.DND;
                            break;
                        case "IDLE":
                            onlineStatus = FriendStatus.BUSY;
                            break;
                        default:
                            onlineStatus = FriendStatus.OFFLINE;
                            break;
                    }

                    // On crée un objet Friend à partir des informations reçues
                    Friend newFriend = new Friend(friendResponse.friendUsername, "/Image/pp.jpg", FriendStatus.PENDING, onlineStatus);

                    // Assurez-vous que setIDFriendship prend un paramètre et modifie l'objet correctement
                    newFriend.setIDFriendship(friendResponse.friendshipId);

                    Platform.runLater(() -> {
                        // Ajout de l'ami à la liste pending et mise à jour de l'affichage
                        pendingFriends.add(newFriend);
                        filterFriends(currentFilter);
                        showAlert("Succès", "Demande d'ami envoyée avec succès !");
                    });
                } else {
                    // En cas d'erreur, on parse le JSON d'erreur et on affiche le message approprié
                    ErrorResponse errorResponse = gson.fromJson(responseBody, ErrorResponse.class);
                    String errorMsg = errorResponse.message;  // par exemple : "Ami non trouvé", "Vous ne pouvez pas vous ajouter vous-même en ami.", etc.

                    Platform.runLater(() -> showAlert("Erreur", errorMsg));
                }
            }
        });
    }
    // Fin de l'ajout d'ami

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
                e.printStackTrace();
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    Gson gson = new Gson();
                    FriendApiResponse[] friendsResponse = gson.fromJson(json, FriendApiResponse[].class);

                    Platform.runLater(() -> {
                        acceptedFriends.clear();
                        for (FriendApiResponse fr : friendsResponse) {
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
                            acceptedFriends.add(new Friend(fr.friendUsername, "/Image/pp.jpg", FriendStatus.ONLINE, "ONLINE".equals(fr.friendOnlineStatus) ? FriendStatus.ONLINE : FriendStatus.OFFLINE));
                        }
                        // Réappliquer le filtre si nécessaire
                        if ("ONLINE".equals(currentFilter) || "ALL".equals(currentFilter) || "BLOCKED".equals(currentFilter)) {
                            filterFriends(currentFilter);
                        }
                    });
                } else {
                    Platform.runLater(() -> showAlert("Erreur", "Erreur lors de la récupération des amis acceptés. Code: " + response.code()));
                }
            }
        });
    }

    // Récupérer la liste des amis en attente via l'API
    private void fetchPendingFriends() {
        String url = API_URL + "/friends/pending";
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + Utilisateur.getInstance().getToken())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Platform.runLater(() -> showAlert("Erreur", "Impossible de récupérer les amis en attente."));
                e.printStackTrace();
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    Gson gson = new Gson();
                    FriendApiResponse[] friendsResponse = gson.fromJson(json, FriendApiResponse[].class);

                    Platform.runLater(() -> {
                        pendingFriends.clear();
                        for (FriendApiResponse fr : friendsResponse) {
                            System.out.println(fr.friendshipId);
                            FriendStatus onlineStatus;
                            switch(fr.friendOnlineStatus){
                                case "ONLINE":
                                    onlineStatus = FriendStatus.ONLINE;
                                    break;
                                case "OFFLINE":
                                    onlineStatus = FriendStatus.OFFLINE;
                                    break;
                                case "BLOCKED":
                                    onlineStatus = FriendStatus.BLOCKED;
                                    break;
                                case "DO_NOT_DISTURB":
                                    onlineStatus = FriendStatus.DND;
                                    break;
                                case "IDLE":
                                    onlineStatus = FriendStatus.BUSY;
                                    break;
                                default:
                                    onlineStatus = FriendStatus.OFFLINE;
                                    break;
                            }
                            // Création de l'objet Friend
                            Friend newFriend = new Friend(fr.friendUsername, "/Image/pp.jpg", FriendStatus.PENDING, onlineStatus);

                            // Assurez-vous que setIDFriendship prend un paramètre et modifie l'objet correctement
                            newFriend.setIDFriendship(fr.friendshipId);

                            // Ajout à la liste des amis en attente
                            pendingFriends.add(newFriend);
                        }
                        if ("PENDING".equals(currentFilter)) {
                            filterFriends(currentFilter);
                        }
                    });
                } else {
                    Platform.runLater(() -> showAlert("Erreur", "Erreur lors de la récupération des amis en attente. Code: " + response.code()));
                }
            }
        });
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
                // Par défaut, couleur pour "En ligne" (à adapter selon vos données)
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
                    // Ici, "item" représente le pseudo de l'ami.
                    // Si vous avez un modèle plus complet (par exemple, un objet PrivateMessageItem ou Friend),
                    // vous pourrez récupérer directement avatarPath et statut réel.
                    pseudoLabel.setText(item);

                    // Exemple statique : on affiche "En ligne" et on charge un avatar par défaut.
                    // Remplacez ces valeurs par celles de votre source de données.
                    statusLabel.setText("En ligne");
                    statusIndicator.setFill(Color.web("#43B581"));  // Par exemple : vert pour "en ligne"

                    // Charger l'image de l'avatar (à remplacer par le chemin dynamique de l’avatar de l’ami)
                    avatar.setImage(new Image(getClass().getResource("/Image/pp.jpg").toExternalForm()));

                    setGraphic(container);

                    // Gestion du clic sur la cellule : ouverture de la conversation
                    //container.setOnMouseClicked(e -> openConversation(item));
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

    // Appliquer un effet hover sur un bouton
    private void applyHoverEffect(Button btn, String originalStyle) {
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white;"));
        btn.setOnMouseExited(e -> btn.setStyle(originalStyle));
    }

    @FXML
    private void handleLogoutClick() {
        try {
            Utilisateur.reset();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/discordlike_client/hello-view.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                e.printStackTrace();
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Platform.runLater(() -> showAlert("Succès", "Changement de status réussi !"));
                } else {
                    Platform.runLater(() -> showAlert("Erreur", "Échec du changement de status. Code: " + response.code()));
                }
            }
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
