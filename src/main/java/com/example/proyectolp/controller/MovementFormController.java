package com.example.proyectolp.controller;

import com.example.proyectolp.model.FinancialMovement;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class MovementFormController {
    @FXML private DialogPane dialogPane;
    @FXML private ComboBox<String> cmbType,cmbCategory;
    @FXML private TextField txtAmount;
    @FXML private DatePicker dateMovement;
    @FXML private TextArea txtDescription;
    @FXML private Label lblValidation;
    private FinancialMovement original;
    @FXML private void initialize(){cmbType.getItems().setAll("Ingreso","Gasto");cmbType.valueProperty().addListener((o,a,b)->validate());dateMovement.valueProperty().addListener((o,a,b)->validate());txtAmount.textProperty().addListener((o,a,b)->validate());cmbCategory.valueProperty().addListener((o,a,b)->validate());}
    public void configure(FinancialMovement movement){original=movement;cmbCategory.getItems().setAll("Salario","Trabajos independientes","Ventas","Bonificaciones","Regalos","Transporte","Mercado","Comida","Universidad","Entretenimiento","Tecnología","Salud","Servicios","Otro");if(movement==null){cmbType.setValue("Ingreso");dateMovement.setValue(LocalDate.now());}else{cmbType.setValue(movement.getType()==FinancialMovement.Type.INGRESO?"Ingreso":"Gasto");txtAmount.setText(movement.getAmount().toPlainString());cmbCategory.setValue(movement.getCategory());dateMovement.setValue(movement.getDate());txtDescription.setText(movement.getDescription());}Button ok=(Button)dialogPane.lookupButton(ButtonType.OK);ok.setText("Guardar");ok.addEventFilter(ActionEvent.ACTION,e->{String err=validation();if(err!=null){lblValidation.setText(err);e.consume();}});validate();}
    public FinancialMovement build(){return new FinancialMovement(original==null?0:original.getId(),new BigDecimal(txtAmount.getText().trim()),cmbCategory.getValue(),txtDescription.getText()==null?null:txtDescription.getText().trim(),dateMovement.getValue(),"Ingreso".equals(cmbType.getValue())?FinancialMovement.Type.INGRESO:FinancialMovement.Type.GASTO);}
    private void validate(){if(lblValidation==null)return;String error=validation();lblValidation.setText(error==null?"":error);Button ok=(Button)dialogPane.lookupButton(ButtonType.OK);ok.setDisable(error!=null);}
    private String validation(){try{if(txtAmount.getText()==null||new BigDecimal(txtAmount.getText().trim()).signum()<=0)return "El monto debe ser mayor que cero.";}catch(Exception e){return "Ingresa un monto válido.";}if(dateMovement.getValue()==null)return "Selecciona una fecha.";if(cmbType.getValue()==null)return "Selecciona ingreso o gasto.";if(cmbCategory.getValue()==null||cmbCategory.getValue().isBlank())return "Selecciona una categoría.";return null;}
}
