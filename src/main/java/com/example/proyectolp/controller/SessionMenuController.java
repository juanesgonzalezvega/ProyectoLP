package com.example.proyectolp.controller;

import com.example.proyectolp.MainApp;
import com.example.proyectolp.service.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class SessionMenuController {
    @FXML private Label userLabel;
    @FXML private void initialize(){var session=SessionManager.getInstance().current();userLabel.setText(session==null?"":(session.displayName()==null||session.displayName().isBlank()?session.email():session.displayName()));}
    @FXML private void logout(){MainApp.logout();}
}
