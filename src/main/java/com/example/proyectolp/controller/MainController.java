package com.example.proyectolp.controller;

import com.example.proyectolp.config.AppConfig;
import com.example.proyectolp.model.ConnectionTest;
import com.example.proyectolp.service.SupabaseService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainController {
    @FXML private Label lblEstado;
    @FXML private Button btnProbar;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "supabase-worker"); t.setDaemon(true); return t; });
    @FXML private void probarConexion() {
        btnProbar.setDisable(true);
        lblEstado.setText("Conectando con Supabase…");
        Task<String> task = new Task<>() {
            @Override protected String call() throws Exception {
                AppConfig config = AppConfig.load();
                SupabaseService service = new SupabaseService(config);
                String message = "Prueba de conexión desde ProyectoLP";
                service.post("connection_test", java.util.Map.of("message", message), "return=representation");
                List<ConnectionTest> rows = service.getList("connection_test?select=id,message&order=id.desc&limit=1", ConnectionTest.class);
                if (rows.isEmpty()) throw new IllegalStateException("Se insertó el registro, pero no se pudo leer. Revisa las políticas RLS de SELECT.");
                return "Conexión correcta · id=" + rows.get(0).getId() + " · " + rows.get(0).getMessage();
            }
        };
        task.setOnSucceeded(e -> { lblEstado.setText(task.getValue()); btnProbar.setDisable(false); });
        task.setOnFailed(e -> { Throwable error = task.getException(); lblEstado.setText("Error: " + (error == null ? "desconocido" : error.getMessage())); btnProbar.setDisable(false); });
        executor.submit(task);
    }
}
