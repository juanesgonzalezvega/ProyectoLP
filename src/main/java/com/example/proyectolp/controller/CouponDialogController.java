package com.example.proyectolp.controller;

import com.example.proyectolp.model.RedemptionResult;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;
import java.time.format.DateTimeFormatter;

public class CouponDialogController {
    @FXML private Label codeLabel,merchantLabel,productLabel,creditsLabel,datesLabel;
    public void configure(RedemptionResult r){codeLabel.setText(r.couponCode);merchantLabel.setText(r.merchantName);productLabel.setText(r.productName);creditsLabel.setText(r.creditsSpent+" CRÉDITOS");DateTimeFormatter f=DateTimeFormatter.ofPattern("dd/MM/yyyy");datesLabel.setText("Creado: "+r.createdAt.format(f)+" · Vence: "+r.expiresAt.format(f));}
    @FXML private void copy(){ClipboardContent c=new ClipboardContent();c.putString(codeLabel.getText());Clipboard.getSystemClipboard().setContent(c);}
    @FXML private void close(){((Stage)codeLabel.getScene().getWindow()).close();}
}
