module com.example.discordlike_client {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires org.json;
    requires okhttp3;
    requires com.google.gson;
    requires Java.WebSocket;

    opens com.example.discordlike_client to javafx.fxml;
    exports com.example.discordlike_client;
    exports com.example.discordlike_client.controller;
    opens com.example.discordlike_client.controller to javafx.fxml, com.google.gson;
    exports com.example.discordlike_client.model;
    opens com.example.discordlike_client.model to com.google.gson, javafx.fxml;
}