package com.example.proyectolp.controller;

import com.example.proyectolp.model.Merchant;
import com.example.proyectolp.model.MerchantProduct;
import com.example.proyectolp.service.RedemptionRules;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.function.BiConsumer;

public class ProductCardController {
    @FXML private Label kindLabel,nameLabel,descriptionLabel,costLabel,copLabel,missingLabel;
    @FXML private Button redeemButton;
    private Merchant merchant;private MerchantProduct product;private BiConsumer<Merchant,MerchantProduct> action;
    public void configure(Merchant m,MerchantProduct p,int balance,BiConsumer<Merchant,MerchantProduct> a){merchant=m;product=p;action=a;kindLabel.setText(p.kind);nameLabel.setText(p.name);descriptionLabel.setText(p.description);costLabel.setText(p.creditCost+" CRÉDITOS");copLabel.setText("≈ $"+String.format(java.util.Locale.forLanguageTag("es-CO"),"%,d",RedemptionRules.copValue(p.creditCost))+" COP en créditos");int missing=RedemptionRules.missing(balance,p.creditCost);missingLabel.setText(missing==0?"":"Te faltan "+missing+" créditos");redeemButton.setDisable(missing>0);}
    @FXML private void redeem(){redeemButton.setDisable(true);if(action!=null)action.accept(merchant,product);}
}
