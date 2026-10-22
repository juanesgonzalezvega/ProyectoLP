package com.example.proyectolp.controller;

import com.example.proyectolp.MainApp;
import com.example.proyectolp.config.AppConfig;
import com.example.proyectolp.service.*;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberCheck;
    @FXML private Button loginButton;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label messageLabel;
    private final ExecutorService executor=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"auth-login");t.setDaemon(true);return t;});
    @FXML private void initialize(){if(!MainApp.loginMessage().isBlank())messageLabel.setText(MainApp.loginMessage());}
    @FXML private void login(){String email=emailField.getText()==null?"":emailField.getText().trim();String password=passwordField.getText();if(!AuthService.validEmail(email)){messageLabel.setText("Escribe un correo válido.");return;}if(password==null||password.isBlank()){messageLabel.setText("Escribe tu contraseña.");return;}loginButton.setDisable(true);loadingIndicator.setVisible(true);loadingIndicator.setManaged(true);messageLabel.setStyle("-fx-text-fill: #344054;");messageLabel.setText("Iniciando sesión…");Task<Void> task=new Task<>(){@Override protected Void call()throws Exception{new AuthService(AppConfig.load(),SessionManager.getInstance(),new SessionStorage()).login(email,password,rememberCheck.isSelected());return null;}};task.setOnSucceeded(e->{loginButton.setDisable(false);loadingIndicator.setVisible(false);loadingIndicator.setManaged(false);MainApp.authenticated();});task.setOnFailed(e->{loginButton.setDisable(false);loadingIndicator.setVisible(false);loadingIndicator.setManaged(false);Throwable x=task.getException();messageLabel.setText(x instanceof AuthException?x.getMessage():"No se pudo conectar con Supabase. Revisa la configuración.");});executor.submit(task);}
    @FXML private void openRegister(){MainApp.showRegister();}
}
