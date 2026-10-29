package com.example.proyectolp.controller;

import com.example.proyectolp.model.enums.AttachmentOwnerType;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.Parent;
import javafx.stage.Modality;
import javafx.stage.Window;
import java.io.IOException;

public final class AttachmentsDialog {
    private AttachmentsDialog(){}
    public static void show(Window owner,AttachmentOwnerType type,long id,String title){
        try{FXMLLoader loader=new FXMLLoader(AttachmentsDialog.class.getResource("/com/example/proyectolp/ui/attachments-panel.fxml"));Parent content=loader.load();AttachmentsPanelController controller=loader.getController();controller.configure(type,id);Dialog<ButtonType> dialog=new Dialog<>();dialog.setTitle(title);dialog.initModality(Modality.WINDOW_MODAL);if(owner!=null)dialog.initOwner(owner);dialog.getDialogPane().setContent(content);dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);dialog.setOnHidden(event->controller.close());dialog.showAndWait();}
        catch(IOException ex){throw new IllegalStateException("No se pudo abrir el panel de adjuntos.",ex);}
    }
}
