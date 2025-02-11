package com.example.discordlike_client.websocket;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class GlobalWebSocketClient {
    private static GlobalWebSocketClient instance;
    private WebSocketClient webSocketClient;
    private final List<MessageListener> listeners = new ArrayList<>();  // Liste des listeners

    private GlobalWebSocketClient() {}

    public static GlobalWebSocketClient getInstance() {
        if (instance == null) {
            instance = new GlobalWebSocketClient();
        }
        return instance;
    }

    public void connect(String url) {
        try {
            webSocketClient = new WebSocketClient(new URI(url)) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    System.out.println("WebSocket connected");
                }

                @Override
                public void onMessage(String message) {
                    handleGlobalMessage(message);  // Gestion globale du message reçu
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("WebSocket closed: " + reason);
                }

                @Override
                public void onError(Exception ex) {
                    System.out.println("WebSocket error: " + ex.getMessage());
                }
            };
            webSocketClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendGlobalMessage(String message) {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.send(message);
            System.out.println("Message envoyé : " + message);
        } else {
            System.out.println("WebSocket non connecté. Impossible d'envoyer le message.");
        }
    }

    public void close() {
        if (webSocketClient != null) {
            webSocketClient.close();
        }
    }

    // Gestion des messages reçus
    private void handleGlobalMessage(String message) {
        Platform.runLater(() -> {
            System.out.println("Message global reçu : " + message);

            // Notifier tous les listeners inscrits
            for (MessageListener listener : listeners) {
                listener.onMessageReceived(message);
            }
        });
    }

    // Méthode pour enregistrer un listener
    public void addMessageListener(MessageListener listener) {
        listeners.add(listener);
    }

    // Méthode pour supprimer un listener
    public void removeMessageListener(MessageListener listener) {
        listeners.remove(listener);
    }

    // Interface pour les listeners
    public interface MessageListener {
        void onMessageReceived(String message);
    }
}