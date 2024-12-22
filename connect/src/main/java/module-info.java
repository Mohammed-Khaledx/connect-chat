module com.example.connect {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.graphics;
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires java.net.http;
    exports com.example.connect.model; // Add this line
    opens com.example.connect.model to com.fasterxml.jackson.databind; // Add this line

    requires com.google.gson;
//    requires Java.WebSocket;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.fasterxml.jackson.databind;
    requires org.java_websocket;
    requires com.fasterxml.jackson.core;
//    requires org.java_websocket;

    opens com.example.connect to javafx.fxml;
    exports com.example.connect;
    exports com.example.connect.service;
    opens com.example.connect.service to javafx.fxml;
//    exports com.example.connect.model;
//    exports com.example.connect.model to com.fasterxml.jackson.databind;
//    opens com.example.connect.model to com.fasterxml.jackson.databind;
}