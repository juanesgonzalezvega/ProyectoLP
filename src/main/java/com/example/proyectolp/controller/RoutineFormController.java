package com.example.proyectolp.controller;

import com.example.proyectolp.model.TrainingModels.Exercise;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.*;

public class RoutineFormController {
    public static final List<String> MUSCLES=List.of("pecho","espalda","hombros","bíceps","tríceps","piernas","core","otro");
    @FXML private TextField nameField,descriptionField,exerciseField;
    @FXML private ComboBox<String> muscleCombo;
    @FXML private ListView<Exercise> exerciseList;
    @FXML private Label errorLabel;
    private Long routineId;
    @FXML private void initialize(){muscleCombo.setItems(FXCollections.observableArrayList(MUSCLES));exerciseList.setCellFactory(v->new ListCell<>(){@Override protected void updateItem(Exercise e,boolean empty){super.updateItem(e,empty);setText(empty||e==null?null:e.name()+" · "+e.muscleGroup());}});}
    public void setRoutine(String name,String description,List<Exercise> exercises,Long id){routineId=id;nameField.setText(name==null?"":name);descriptionField.setText(description==null?"":description);exerciseList.getItems().setAll(exercises==null?List.of():exercises);}
    @FXML private void addExercise(){String name=exerciseField.getText().trim();String group=muscleCombo.getValue();if(name.isEmpty()||group==null){errorLabel.setText("Indica el nombre y el grupo muscular.");return;}exerciseList.getItems().add(new Exercise(null,routineId,name,group,exerciseList.getItems().size()));exerciseField.clear();errorLabel.setText("");}
    @FXML private void removeExercise(){int i=exerciseList.getSelectionModel().getSelectedIndex();if(i>=0)exerciseList.getItems().remove(i);}
    @FXML private void moveUp(){move(-1);} @FXML private void moveDown(){move(1);}
    private void move(int delta){int i=exerciseList.getSelectionModel().getSelectedIndex(),j=i+delta;if(i>=0&&j>=0&&j<exerciseList.getItems().size()){Collections.swap(exerciseList.getItems(),i,j);exerciseList.getSelectionModel().select(j);}}
    public boolean validateForm(){if(nameField.getText().isBlank()){errorLabel.setText("El nombre de la rutina es obligatorio.");return false;}return true;}
    public String getName(){return nameField.getText().trim();} public String getDescription(){return descriptionField.getText().trim();} public List<Exercise> getExercises(){return List.copyOf(exerciseList.getItems());}
}
