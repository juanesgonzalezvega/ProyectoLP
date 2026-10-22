package com.example.proyectolp.controller;

import com.example.proyectolp.MainApp;
import com.example.proyectolp.config.AppConfig;
import com.example.proyectolp.service.*;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterController {
    @FXML private TextField nameField,emailField;
    @FXML private PasswordField passwordField,confirmField;
    @FXML private CheckBox rememberCheck;
    @FXML private Button registerButton;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label messageLabel;
    private final ExecutorService executor=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"auth-register");t.setDaemon(true);return t;});
    @FXML private void register(){String name=nameField.getText()==null?"":nameField.getText().trim(),email=emailField.getText()==null?"":emailField.getText().trim(),password=passwordField.getText(),confirm=confirmField.getText();if(name.isBlank()){messageLabel.setText("Escribe tu nombre para mostrar.");return;}if(!AuthService.validEmail(email)){messageLabel.setText("Escribe un correo válido.");return;}if(!AuthService.validPassword(password)){messageLabel.setText("La contraseña debe tener al menos 8 caracteres.");return;}if(!password.equals(confirm)){messageLabel.setText("Las contraseñas no coinciden.");return;}registerButton.setDisable(true);loadingIndicator.setVisible(true);loadingIndicator.setManaged(true);messageLabel.setStyle("-fx-text-fill: #344054;");messageLabel.setText("Creando cuenta…");Task<com.example.proyectolp.model.Session> task=new Task<>(){@Override protected com.example.proyectolp.model.Session call()throws Exception{return new AuthService(AppConfig.load(),SessionManager.getInstance(),new SessionStorage()).register(email,password,name,rememberCheck.isSelected());}};task.setOnSucceeded(e->{registerButton.setDisable(false);loadingIndicator.setVisible(false);loadingIndicator.setManaged(false);if(task.getValue()!=null)MainApp.authenticated();else{messageLabel.setStyle("-fx-text-fill: #176b45;");messageLabel.setText("Cuenta creada. Revisa tu correo para confirmar tu cuenta y luego inicia sesión.");}});task.setOnFailed(e->{registerButton.setDisable(false);loadingIndicator.setVisible(false);loadingIndicator.setManaged(false);Throwable x=task.getException();messageLabel.setText(x instanceof AuthException?x.getMessage():"No se pudo conectar con Supabase. Revisa la configuración.");});executor.submit(task);}
    @FXML private void openLogin(){MainApp.showLogin("");}
}
