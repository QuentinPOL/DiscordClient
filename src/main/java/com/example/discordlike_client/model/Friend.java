package com.example.discordlike_client.model;

public class Friend {
    private final String pseudo;
    private final String status; // "online", "dnd", "idle", "offline"
    private final boolean blocked;
    private final boolean pendingRequest;

    public Friend(String pseudo, String status, boolean blocked, boolean pendingRequest) {
        this.pseudo = pseudo;
        this.status = status;
        this.blocked = blocked;
        this.pendingRequest = pendingRequest;
    }

    public String getPseudo() { return pseudo; }
    public String getStatus() { return status; }
    public boolean isBlocked() { return blocked; }
    public boolean isPendingRequest() { return pendingRequest; }

    // Méthodes utilitaires
    public boolean isOnline() {
        return status.equalsIgnoreCase("online");
    }
}

