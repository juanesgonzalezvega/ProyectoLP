package com.example.proyectolp.controller;

import com.example.proyectolp.model.FinancialGoal;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.math.BigDecimal;

public class GoalFormController {
    @FXML private DialogPane dialogPane;
    @FXML private TextField txtName,txtAmount;
    @FXML private Label lblTitle,lblAmount,lblValidation;
    private FinancialGoal original;
    private boolean contribution;
    @FXML private void initialize(){txtName.textProperty().addListener((o,a,b)->validate());txtAmount.textProperty().addListener((o,a,b)->validate());}
    public void configure(FinancialGoal goal,boolean aporte){original=goal;contribution=aporte;if(aporte){lblTitle.setText("Aporte");lblAmount.setText("Monto a aportar");txtName.setVisible(false);txtName.setManaged(false);txtAmount.setPromptText("Monto mayor que cero");}else if(goal!=null){txtName.setText(goal.getName());txtAmount.setText(goal.getTargetAmount().toPlainString());}Button ok=(Button)dialogPane.lookupButton(ButtonType.OK);ok.setText(aporte?"Aportar":"Guardar");ok.addEventFilter(ActionEvent.ACTION,e->{String error=validation();if(error!=null){lblValidation.setText(error);e.consume();}});validate();}
    public FinancialGoal buildGoal(){FinancialGoal result=new FinancialGoal();if(original!=null)result.setId(original.getId());result.setName(txtName.getText()==null?null:txtName.getText().trim());result.setTargetAmount(new BigDecimal(txtAmount.getText().trim()));result.setSavedAmount(original==null?BigDecimal.ZERO:original.getSavedAmount());return result;}
    public BigDecimal buildContribution(){return new BigDecimal(txtAmount.getText().trim());}
    private void validate(){if(lblValidation==null)return;String error=validation();lblValidation.setText(error==null?"":error);((Button)dialogPane.lookupButton(ButtonType.OK)).setDisable(error!=null);}
    private String validation(){if(!contribution&&(txtName.getText()==null||txtName.getText().isBlank()))return "El nombre es obligatorio.";try{if(new BigDecimal(txtAmount.getText().trim()).signum()<=0)return contribution?"El aporte debe ser mayor que cero.":"El objetivo debe ser mayor que cero.";}catch(Exception e){return "Ingresa un monto válido.";}return null;}
}
