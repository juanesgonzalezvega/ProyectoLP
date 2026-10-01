package com.example.proyectolp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {
    @Override public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/com/example/proyectolp/ui/dashboard.fxml"));
        stage.setTitle("ProyectoLP · Dashboard");
        stage.setScene(new Scene(loader.load(), 1050, 720));
        stage.show();
    }
    @Override public void stop() { }
}
