package com.example.discordlike_client.model;

import java.util.Objects;

public class Utilisateur {
    private static Utilisateur instance; // Singleton

    private String adresseMail;
    private String pseudo;
    private String token;
    private Status statut;
    private String imagePath;

    // Enum pour les statuts disponibles
    public enum Status {
        ONLINE, BUSY, DND, INVISIBLE
    }

    // Constructeur privé : on ne peut pas créer un utilisateur directement
    private Utilisateur() {
        this.statut = Status.ONLINE; // Statut par défaut
    }

    // Obtenir l'instance unique
    public static Utilisateur getInstance() {
        if (instance == null) {
            instance = new Utilisateur();
        }
        return instance;
    }

    // Réinitialiser l'utilisateur lors de la déconnexion
    public static void reset() {
        instance = null;  // Supprime l'instance pour forcer une nouvelle connexion
    }

    // Getters et Setters
    public String getAdresseMail() {
        return adresseMail;
    }

    public void setAdresseMail(String adresseMail) {
        this.adresseMail = adresseMail;
    }

    public void setToken(String tokenNew) {
        this.token = tokenNew;
    }

    public String getToken() {
        return token;
    }

    public String getPseudo() {
        return pseudo;
    }

    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }

    // Gestion du statut
    public Status getStatut() {
        return statut;
    }

    public void setStatutEnum(Status statut) {
        this.statut = statut;
    }

    public void setStatutString(String statut) {
        if (Objects.equals(statut, "ONLINE")) {
            this.statut = Status.ONLINE;
        }
        else if (Objects.equals(statut, "IDLE")) {
            this.statut = Status.BUSY;
        }
        else if (Objects.equals(statut, "DO_NOT_DISTURB")) {
            this.statut = Status.DND;
        }
        else if (Objects.equals(statut, "INVISIBLE")) {
            this.statut = Status.INVISIBLE;
        }
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}
