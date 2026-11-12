package com.example.proyectolp.controller;

import com.example.proyectolp.model.Merchant;
import com.example.proyectolp.model.MerchantProduct;
import com.example.proyectolp.service.RedemptionRules;
import com.example.proyectolp.MainApp;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import java.io.IOException;
import java.util.function.Consumer;

public class MerchantMenuController {
    @FXML private Label categoryLabel,nameLabel,merchantName,merchantCategory;
    @FXML private Button websiteButton;
    @FXML private FlowPane productList;
    private Merchant merchant;
    private RedeemController owner;

    public void configureCard(Merchant item,RedeemController controller){merchant=item;owner=controller;categoryLabel.setText(item.category.toUpperCase());nameLabel.setText(item.name);}
    public void configureMenu(Merchant item,java.util.List<MerchantProduct> products,int balance,RedeemController controller){merchant=item;owner=controller;merchantName.setText(item.name);merchantCategory.setText(item.category.toUpperCase());websiteButton.setVisible(RedemptionRules.validWebsite(item.websiteUrl));websiteButton.setManaged(websiteButton.isVisible());productList.getChildren().clear();for(MerchantProduct p:products)try{FXMLLoader l=new FXMLLoader(getClass().getResource("/com/example/proyectolp/ui/product-card.fxml"));Parent node=l.load();l.<ProductCardController>getController().configure(item,p,balance,controller::redeem);productList.getChildren().add(node);}catch(IOException ignored){}}
    @FXML private void open(){if(owner!=null)owner.openMerchant(merchant);}
    @FXML private void back(){if(owner!=null)owner.showMerchants();}
    @FXML private void visitWebsite(){if(merchant!=null&&RedemptionRules.validWebsite(merchant.websiteUrl)){if(!MainApp.openLink(merchant.websiteUrl))owner.showMessage("No se pudo abrir el sitio web.");}else if(owner!=null)owner.showMessage("El enlace del catálogo no es válido.");}
}
