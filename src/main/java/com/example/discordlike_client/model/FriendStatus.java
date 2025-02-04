package com.example.discordlike_client.model;

/**
 * Représente un ami avec un pseudo, un statut, un message de statut,
 * et un chemin d'image pour l'avatar.
 */

public enum FriendStatus {
    ONLINE,     // En ligne
    BUSY,       // Occupé
    DND,        // Ne pas déranger (Absent)
    INVISIBLE,  // Invisible
    BLOCKED,    // Bloqué
    PENDING,    // En attente
    OFFLINE     // Hors-ligne
}
