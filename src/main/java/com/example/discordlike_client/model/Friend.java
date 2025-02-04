package com.example.discordlike_client.model;

public class Friend {
    private String pseudo;
    private String avatarPath;
    private FriendStatus friendshipStatus; // Statut de la relation (ACCEPTED ou PENDING, etc.)
    private FriendStatus onlineStatus;     // Statut en ligne réel (ONLINE, OFFLINE, BLOCKED, etc.)
    private int idFriend;

    // Pour un ami accepté, on considère que les deux statuts sont identiques
    public Friend(String pseudo, String avatarPath, FriendStatus status) {
        this(pseudo, avatarPath, status, status);
    }

    // Constructeur complet
    public Friend(String pseudo, String avatarPath, FriendStatus friendshipStatus, FriendStatus onlineStatus) {
        this.pseudo = pseudo;
        this.avatarPath = avatarPath;
        this.friendshipStatus = friendshipStatus;
        this.onlineStatus = onlineStatus;
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
}