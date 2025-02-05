package com.example.discordlike_client.model;

public class Friend {
    private String pseudo;
    private String avatarPath;
    private FriendStatus friendshipStatus; // Statut de la relation (ACCEPTED, PENDING, etc.)
    private FriendStatus onlineStatus;     // Statut en ligne réel (ONLINE, OFFLINE, BLOCKED, etc.)
    private int idFriend;
    private boolean requestReceived;       // true = demande reçue, false = demande envoyée

    // Pour un ami accepté, on considère que les deux statuts sont identiques
    public Friend(String pseudo, String avatarPath, FriendStatus status, FriendStatus onlineStatus) {
        // Pour un ami accepté, on définit par défaut l'id à 0 et requestReceived à false
        this(pseudo, avatarPath, status, onlineStatus, 0, false);
    }

    // Constructeur complet avec le nouveau paramètre
    public Friend(String pseudo, String avatarPath, FriendStatus friendshipStatus, FriendStatus onlineStatus, int idFriend, boolean requestReceived) {
        this.pseudo = pseudo;
        this.avatarPath = avatarPath;
        this.friendshipStatus = friendshipStatus;
        this.onlineStatus = onlineStatus;
        this.idFriend = idFriend;
        this.requestReceived = requestReceived;
    }

    public String getPseudo() {
        return pseudo;
    }
    public String getAvatarPath() {
        return avatarPath;
    }
    public FriendStatus getFriendshipStatus() {
        return friendshipStatus;
    }
    public FriendStatus getOnlineStatus() {
        return onlineStatus;
    }
    public void setOnlineStatus(FriendStatus onlineStatus) {
        this.onlineStatus = onlineStatus;
    }
    public void setFriendshipStatus(FriendStatus friendshipStatus) {
        this.friendshipStatus = friendshipStatus;
    }
    public void setIDFriendship(int newIDFriendship) {
        this.idFriend = newIDFriendship;
    }
    public int getIDFriendship() {
        return this.idFriend;
    }
    public boolean isRequestReceived() {
        return requestReceived;
    }
    public void setRequestReceived(boolean requestReceived) {
        this.requestReceived = requestReceived;
    }
}