module com.example.proyectolp {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires java.net.http;
    opens com.example.proyectolp.controller to javafx.fxml;
    opens com.example.proyectolp.model to javafx.base, com.fasterxml.jackson.databind;
    exports com.example.proyectolp;
    exports com.example.proyectolp.config;
    exports com.example.proyectolp.controller;
    exports com.example.proyectolp.model;
    exports com.example.proyectolp.model.enums;
    exports com.example.proyectolp.repository;
    exports com.example.proyectolp.service;
}
