package com.example.discordlike_client.controller;

import com.example.discordlike_client.model.Utilisateur;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.Parent;
import javafx.scene.Scene;
import java.io.IOException;

public class MainViewController {

    @FXML
    private HBox rootPane;  // lié à fx:id="rootPane"
    @FXML
    private Label usernameLabel;

    @FXML
    private ListView<String> friendsList;
    @FXML
    private ListView<String> privateMessagesList;
    @FXML
    private ListView<ServerItem> serversListView;

    @FXML private ImageView userAvatar;
    @FXML private Circle statusIndicator;
    @FXML private Label statusText;

    @FXML
    public void initialize() {
        // Forcer la fenêtre en maximisé et non redimensionnable
        Platform.runLater(() -> {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setMaximized(true);
            stage.setResizable(true);
        });

        // Récupérer les infos de l'utilisateur
        Utilisateur utilisateur = Utilisateur.getInstance();

        // Mettre à jour l'affichage avec le pseudo
        usernameLabel.setText(utilisateur.getPseudo());

        // Charger l'avatar
        String imagePath = getClass().getResource("/Image/pp.jpg").toExternalForm();
        userAvatar.setImage(new Image(imagePath));

        // Rendre l'avatar circulaire
        applyCircularClip(userAvatar);

        // Appliquer la couleur du statut
        updateStatusColor();

        // Créer et styliser le menu contextuel
        ContextMenu statusMenu = createStatusMenu();

        // Ouvrir le menu contextuel au clic sur la pastille
        userAvatar.setOnMouseClicked((MouseEvent event) -> {
            statusMenu.show(statusIndicator, event.getScreenX(), event.getScreenY());
        });

        // Remplir la liste de MP (avec un petit cercle coloré devant chaque pseudo)
        privateMessagesList.getItems().addAll("alexpel", "b-KOS", "cloudbray", "Joris HURTEL");
        privateMessagesCellFactory();

        // Liste de serveurs : on crée quelques serveurs fictifs
        serversListView.getItems().addAll(
                new ServerItem("/Image/testDiscord.png"),
                new ServerItem("/Image/testDiscord.png"),
                new ServerItem("/Image/testDiscord.png")
        );

        // Pour pas avoir le focus
        serversListView.setFocusTraversable(false);
        privateMessagesList.setFocusTraversable(false);
        friendsList.setFocusTraversable(false);

        // CellFactory pour avoir seulement un icône rond (pas de texte)
        serversListView.setCellFactory(list -> new ListCell<ServerItem>() {
            private final ImageView imageView = new ImageView();

            @Override
            protected void updateItem(ServerItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;"); // Fond transparent quand vide
                    return;
                }

                // Charger l'image (chemin dans item.getImagePath())
                Image img = new Image(getClass().getResource(item.getImagePath()).toExternalForm());
                imageView.setImage(img);

                // Agrandir l'icône (ex: 64 au lieu de 48)
                imageView.setFitWidth(64);
                imageView.setFitHeight(64);
                imageView.setPreserveRatio(true);

                // Clip circulaire plus grand (moitié de 64 => 32)
                Circle clip = new Circle(32, 32, 32);
                imageView.setClip(clip);

                // On place juste l'ImageView
                setGraphic(imageView);

                // Ajouter effet hover (changement de couleur au survol)
                setOnMouseEntered(event -> setStyle("-fx-background-color: #40444B; -fx-background-radius: 5;"));
                setOnMouseExited(event -> setStyle("-fx-background-color: transparent;"));
            }
        });
    }

    // Cell factory pour la liste de MP : petit cercle coloré + pseudo
    private void privateMessagesCellFactory() {
        privateMessagesList.setCellFactory(list -> new ListCell<String>() {
            private HBox container = new HBox(8);
            private Circle logo = new Circle(12, Color.web("#5865F2"));
            private Label friendName = new Label();

            {
                friendName.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
                container.getChildren().addAll(logo, friendName);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    friendName.setText(item);
                    setGraphic(container);
                }
            }
        });
    }

    private void applyCircularClip(ImageView imageView) {
        // Appliquer un masque circulaire sur l'ImageView
        double radius = Math.min(imageView.getFitWidth(), imageView.getFitHeight()) / 2;
        Circle clip = new Circle(radius);
        clip.setCenterX(imageView.getFitWidth() / 2);
        clip.setCenterY(imageView.getFitHeight() / 2);
        imageView.setClip(clip);
    }

    @FXML
    private void handleLogoutClick() {
        try {
            // Réinitialiser les données utilisateur
            Utilisateur.reset();

            // Charger hello-view.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/discordlike_client/hello-view.fxml"));
            Parent root = loader.load();

            // Obtenir la scène et changer le contenu
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(false);
        } catch (IOException e) {
            e.printStackTrace();  // Log en cas d'erreur
        }
    }

    /**
     * Créer un menu contextuel stylisé avec effet hover
     */
    private ContextMenu createStatusMenu() {
        ContextMenu menu = new ContextMenu();
        menu.setStyle("-fx-background-color: #2F3136; -fx-background-radius: 5;");

        // Ajouter les items de statut
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

    /**
     * Créer un item du menu stylisé avec hover
     */
    private MenuItem createStyledMenuItem(String text, String color) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 2;");

        HBox container = new HBox(label);
        container.setStyle("-fx-background-color: transparent; -fx-padding: 2;");

        // Effet hover (changement de couleur)
        container.setOnMouseExited(e -> container.setStyle("-fx-background-color: transparent; -fx-padding: 2;"));

        MenuItem menuItem = new MenuItem();
        menuItem.setGraphic(container);
        return menuItem;
    }


    // Changer le statut de l'utilisateur et mettre à jour la couleur de la pastille
    private void changeStatus(Utilisateur.Status newStatus) {
        Utilisateur.getInstance().setStatut(newStatus);
        updateStatusColor();
    }

    // Met à jour la couleur de la pastille selon le statut actuel
    private void updateStatusColor() {
        Utilisateur.Status currentStatus = Utilisateur.getInstance().getStatut();
        switch (currentStatus) {
            case ONLINE:
                statusIndicator.setFill(Color.web("#43B581")); // Vert
                statusText.setText("En ligne");
                break;
            case BUSY:
                statusIndicator.setFill(Color.web("#F04747")); // Rouge
                statusText.setText("Occupé");
                break;
            case DND:
                statusIndicator.setFill(Color.web("#FAA61A")); // Orange/Jaune
                statusText.setText("Absent");
                break;
            case INVISIBLE:
                statusIndicator.setFill(Color.web("#747F8D")); // Gris
                statusText.setText("Invisible");
                break;
        }
    }
}
