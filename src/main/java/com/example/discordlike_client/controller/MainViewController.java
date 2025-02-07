package com.example.discordlike_client.controller;

import com.example.discordlike_client.model.ServerItem;
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
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import okhttp3.*;

import java.io.IOException;

public class MainViewController {

    @FXML private HBox rootPane;
    @FXML private Label usernameLabel;
    @FXML private ListView<Friend> friendsList;
    @FXML private ListView<String> privateMessagesList;
    @FXML private ListView<ServerItem> serversListView;

    @FXML private StackPane discordLogoContainer;
    @FXML private ImageView discordLogo;
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
        String requestType;
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
                new ServerItem("/Image/6537937.jpg")
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
        fetchPendingFriends();

        // Configuration de la ListView des amis
        friendsList.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent;");
        friendsList.setCellFactory(list -> new ListCell<Friend>() {
            // Conteneurs et nœuds réutilisés
            private final HBox container = new HBox(10);
            private final StackPane avatarPane = new StackPane();
            private final ImageView avatar = new ImageView();
            private final Circle friendStatusIndicator = new Circle(6);
            private final VBox friendInfo = new VBox(2);
            private final Label pseudoLabel = new Label();
            private final Label statusLabel = new Label();

            {
                // Configuration initiale (appelée 1 seule fois par cellule)

                // 1) Avatar
                avatar.setFitWidth(40);
                avatar.setFitHeight(40);
                Circle clip = new Circle(20, 20, 20);
                avatar.setClip(clip);

                // 2) Pastille de statut
                friendStatusIndicator.setStroke(Color.WHITE);
                friendStatusIndicator.setStrokeWidth(2);
                StackPane.setAlignment(friendStatusIndicator, Pos.BOTTOM_RIGHT);
                avatarPane.getChildren().addAll(avatar, friendStatusIndicator);

                // 3) Labels
                pseudoLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
                statusLabel.setStyle("-fx-text-fill: #B9BBBE; -fx-font-size: 12px;");
                friendInfo.getChildren().addAll(pseudoLabel, statusLabel);

                // 4) Conteneur principal
                container.setAlignment(Pos.CENTER_LEFT);
                container.setStyle("-fx-background-color: transparent; "
                        + "-fx-border-color: #40444B; "
                        + "-fx-border-width: 0 0 1 0; "
                        + "-fx-padding: 5 10 5 10;");

                // 5) Survol (hover) sur la cellule
                this.setOnMouseEntered(e -> {
                    // On ne colore que si la cellule n’est pas vide
                    if (getItem() != null) {
                        setStyle("-fx-background-color: #40444B;");
                    }
                });
                this.setOnMouseExited(e -> {
                    // On ne restaure le style que si la cellule n’est pas vide
                    if (getItem() != null) {
                        setStyle("-fx-background-color: transparent;");
                    }
                });
            }

            @Override
            protected void updateItem(Friend friend, boolean empty) {
                super.updateItem(friend, empty);

                if (empty || friend == null) {
                    // (A) Cellule vide ou « placeholder »
                    setGraphic(null);
                    setText(null);
                    // Supprimer tout style éventuel :
                    setStyle("");
                    // Vider le contenu (évite qu’un ancien contenu traîne dans une cellule réutilisée)
                    container.getChildren().clear();
                } else {
                    // (B) Cellule avec un Friend réel
                    container.getChildren().clear();  // On repart sur du « propre »

                    // 1) Avatar
                    Image img = new Image(getClass().getResource(friend.getAvatarPath()).toExternalForm());
                    avatar.setImage(img);

                    // 2) Pseudo
                    pseudoLabel.setText(friend.getPseudo());

                    // 3) Statut en ligne
                    switch(friend.getOnlineStatus()) {
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

                    // 4) Statut PENDING ?
                    if (friend.getFriendshipStatus() == FriendStatus.PENDING) {
                        statusLabel.setText("En attente");
                    }

                    // 5) Assembler l’affichage de base
                    container.getChildren().addAll(avatarPane, friendInfo);

                    // 6) Demande en attente ? Boutons Accepter/Refuser/Annuler
                    if (friend.getFriendshipStatus() == FriendStatus.PENDING) {
                        HBox actionsContainer = new HBox(5);
                        Pane spacer = new Pane();
                        HBox.setHgrow(spacer, Priority.ALWAYS);
                        actionsContainer.getChildren().add(spacer);
                        if (friend.isRequestReceived()) {
                            Button acceptBtn = new Button("Accepter");
                            Button rejectBtn = new Button("Refuser");
                            acceptBtn.setOnAction(e -> acceptFriendRequest(friend.getIDFriendship()));
                            rejectBtn.setOnAction(e -> rejectFriendRequest(friend.getIDFriendship()));
                            actionsContainer.getChildren().addAll(acceptBtn, rejectBtn);
                        } else {
                            Button cancelBtn = new Button("Annuler");
                            cancelBtn.setOnAction(e -> cancelFriendRequest(friend.getIDFriendship()));
                            actionsContainer.getChildren().add(cancelBtn);
                        }
                        container.getChildren().add(actionsContainer);
                    }

                    // 7) Appliquer le conteneur à la cellule
                    setGraphic(container);
                    setText(null);

                    // 8) (Ré)initialiser le style normal (pour le survol)
                    setStyle("-fx-background-color: transparent;");
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
                    if (f.getOnlineStatus() != FriendStatus.OFFLINE)
                        filteredFriends.add(f);
                }
                updateFriendsLabel("En ligne", filteredFriends.size());
                break;
            case "ALL":
                for (Friend f : acceptedFriends) {
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
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Gson gson = new Gson();
                try (ResponseBody responseBody = response.body()) {
                    String responseBodyString = responseBody.string();

                    if (response.isSuccessful()) {
                        // En cas de succès, on récupère les informations de l'ami ajouté
                        FriendApiResponse friendResponse = gson.fromJson(responseBodyString, FriendApiResponse.class);

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
                        Friend newFriend = new Friend(friendResponse.friendUsername, "/Image/pp.jpg", FriendStatus.PENDING, onlineStatus, friendResponse.friendshipId, false);

                        Platform.runLater(() -> {
                            // Ajout de l'ami à la liste pending et mise à jour de l'affichage
                            pendingFriends.add(newFriend);
                            filterFriends(currentFilter);
                            showAlert("Succès", "Demande d'ami envoyée avec succès !");
                        });
                    } else {
                        // En cas d'erreur, on parse le JSON d'erreur et on affiche le message approprié
                        ErrorResponse errorResponse = gson.fromJson(responseBodyString, ErrorResponse.class);
                        String errorMsg = errorResponse.message;  // par exemple : "Ami non trouvé", "Vous ne pouvez pas vous ajouter vous-même en ami.", etc.

                        Platform.runLater(() -> showAlert("Erreur", errorMsg));
                    }
                }
            }
        });
    }
    // Fin de l'ajout d'ami

    // Accepter des amis
    private void acceptFriendRequest(int friendshipId) {
        String url = API_URL + "/friends/accept-request";
        String json = "{\"friendshipId\":" + friendshipId + "}";
        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(url)
                .patch(body)
                .addHeader("Authorization", "Bearer " + Utilisateur.getInstance().getToken())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Platform.runLater(() -> showAlert("Erreur", "Impossible d'accepter la demande d'ami."));
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                Gson gson = new Gson();
                try (ResponseBody responseBody = response.body()) {
                    String responseBodyString = responseBody.string();

                    if (response.isSuccessful()) {
                        Platform.runLater(() -> {
                            // Trouver l'ami correspondant dans pendingFriends
                            Friend friend = pendingFriends.stream()
                                    .filter(f -> f.getIDFriendship() == friendshipId)
                                    .findFirst()
                                    .orElse(null);

                            if (friend != null) {
                                // Supprimer de pendingFriends
                                pendingFriends.remove(friend);

                                // Ajouter à acceptedFriends avec les bonnes données
                                acceptedFriends.add(new Friend(
                                        friend.getPseudo(), // Nom d'utilisateur
                                        "/Image/pp.jpg", // Image de profil
                                        FriendStatus.OFFLINE, // Statut
                                        friend.getOnlineStatus()
                                ));

                                // Filtrer et mettre à jour l'affichage
                                filterFriends(currentFilter);
                                showAlert("Succès", "Demande d'ami acceptée !");
                            }
                        });
                    } else {
                        ErrorResponse errorResponse = gson.fromJson(responseBodyString, ErrorResponse.class);
                        Platform.runLater(() -> showAlert("Erreur", errorResponse.message));
                    }
                }
            }
        });
    }

    // Rejeter une demande d'ami
    private void rejectFriendRequest(int friendshipId) {
        String url = API_URL + "/friends/reject-request";
        String json = "{\"friendshipId\":" + friendshipId + "}";
        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(url)
                .patch(body)
                .addHeader("Authorization", "Bearer " + Utilisateur.getInstance().getToken())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Platform.runLater(() -> showAlert("Erreur", "Impossible de refuser la demande d'ami."));
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                Gson gson = new Gson();
                try (ResponseBody responseBody = response.body()) {
                    String responseBodyString = responseBody.string();

                    if (response.isSuccessful()) {
                        Platform.runLater(() -> {
                            pendingFriends.removeIf(f -> f.getIDFriendship() == friendshipId);
                            filterFriends(currentFilter);
                            showAlert("Succès", "Demande d'ami rejetée !");
                        });
                    } else {
                        ErrorResponse errorResponse = gson.fromJson(responseBodyString, ErrorResponse.class);
                        Platform.runLater(() -> showAlert("Erreur", errorResponse.message));
                    }
                }
            }
        });
    }

    // Annuler une demande d'ami envoyée
    private void cancelFriendRequest(int friendshipId) {
        String url = API_URL + "/friends/cancel-request";
        String json = "{\"friendshipId\":" + friendshipId + "}";
        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(url)
                .delete(body)
                .addHeader("Authorization", "Bearer " + Utilisateur.getInstance().getToken())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Platform.runLater(() -> showAlert("Erreur", "Impossible d'annuler la demande d'ami."));
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                Gson gson = new Gson();
                try (ResponseBody responseBody = response.body()) {
                    String responseBodyString = responseBody.string();

                    if (response.isSuccessful()) {
                        Platform.runLater(() -> {
                            pendingFriends.removeIf(f -> f.getIDFriendship() == friendshipId);
                            filterFriends(currentFilter);
                            showAlert("Succès", "Demande d'ami annulée !");
                        });
                    } else {
                        ErrorResponse errorResponse = gson.fromJson(responseBodyString, ErrorResponse.class);
                        Platform.runLater(() -> showAlert("Erreur", errorResponse.message));
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

                                acceptedFriends.add(new Friend(fr.friendUsername, "/Image/pp.jpg", FriendStatus.OFFLINE, status));
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
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        String json = responseBody.string();
                        Gson gson = new Gson();
                        FriendApiResponse[] friendsResponse = gson.fromJson(json, FriendApiResponse[].class);

                        Platform.runLater(() -> {
                            pendingFriends.clear();
                            for (FriendApiResponse fr : friendsResponse) {
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

                                // Si fr.requestType = "SENT" Alors on a envoyé la demande
                                // Si fr.requestType = "RECEIVED" Alors on a reçu la demande

                                // Déterminer si la demande a été envoyée ou reçue
                                boolean requestReceived = "RECEIVED".equals(fr.requestType);

                                // Création de l'objet Friend
                                Friend newFriend = new Friend(fr.friendUsername, "/Image/pp.jpg", FriendStatus.PENDING, onlineStatus, fr.friendshipId, requestReceived);

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
                    container.setOnMouseClicked(e -> onMessagePrivateClicked(item));
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

    @FXML
    protected void onMessagePrivateClicked(String item) {
        if (item == null) {
            return; // Rien n’est sélectionné, on ne fait rien.
        }

        try {
            // Chargement de la vue mp-view.fxml
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/example/discordlike_client/mp-view.fxml"));
            Parent root = fxmlLoader.load();

            // Récupération du contrôleur associé
            PrivateMessagesController pmController = fxmlLoader.getController();
            // On passe le pseudo de l'ami à ce contrôleur
            pmController.setFriendName(item);

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
            stage.show();

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
