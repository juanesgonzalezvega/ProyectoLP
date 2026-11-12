package com.example.proyectolp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import com.example.proyectolp.service.CreditRules;
import java.util.Locale;

public class ConfirmRedemptionController {
    @FXML private Label productLabel, merchantLabel, costLabel, balanceLabel;
    private boolean confirmed;
    public void configure(String merchant,String product,int cost,int balance) {
        merchantLabel.setText(merchant); productLabel.setText(product);
        costLabel.setText(cost+" CRÉDITOS · ≈ $"+String.format(Locale.forLanguageTag("es-CO"),"%,d",cost*CreditRules.COP_PER_CREDIT)+" COP en créditos");
        balanceLabel.setText("Saldo después del canje: "+(balance-cost)+" créditos");
    }
    @FXML private void confirm(){confirmed=true;close();}
    @FXML private void cancel(){close();}
    private void close(){((Stage)productLabel.getScene().getWindow()).close();}
    public boolean isConfirmed(){return confirmed;}
}
