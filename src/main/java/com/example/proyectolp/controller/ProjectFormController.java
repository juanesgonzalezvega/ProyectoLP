package com.example.proyectolp.controller;

import com.example.proyectolp.model.Project;
import com.example.proyectolp.model.Stage;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import java.util.ArrayList;
import java.util.List;

public class ProjectFormController {
    @FXML private DialogPane dialogPane;
    @FXML private TextField txtName, txtStageName;
    @FXML private TextArea txtGoal;
    @FXML private DatePicker dateDeadline;
    @FXML private ListView<Stage> listStages;
    @FXML private CheckBox chkNoStages;
    @FXML private Label lblValidation;
    private Project original;
    private final javafx.collections.ObservableList<Stage> stages = FXCollections.observableArrayList();

    @FXML private void initialize() {
        listStages.setItems(stages);
        listStages.getSelectionModel().selectedItemProperty().addListener((o, oldValue, newValue) -> validate());
        txtName.textProperty().addListener((o, oldValue, newValue) -> validate());
        chkNoStages.selectedProperty().addListener((o, oldValue, newValue) -> validate());
    }

    public void configure(Project project) {
        original = project;
        if (project != null) {
            txtName.setText(project.getName()); txtGoal.setText(project.getMainGoal()); dateDeadline.setValue(project.getDeadline());
            stages.setAll(project.getStages().stream().map(this::copyStage).toList());
        }
        Button save = (Button) dialogPane.lookupButton(ButtonType.OK);
        save.setText("Guardar");
        save.addEventFilter(ActionEvent.ACTION, event -> {
            if (!valid()) { event.consume(); validate(); }
        });
        validate();
    }

    @FXML private void agregarEtapa() {
        String name = txtStageName.getText() == null ? "" : txtStageName.getText().trim();
        if (name.isEmpty()) { lblValidation.setText("Escribe el nombre de la etapa antes de agregarla."); return; }
        Stage stage = new Stage(); stage.setName(name); stage.setStatus("pendiente"); stage.setPosition(stages.size() + 1);
        stages.add(stage); txtStageName.clear(); chkNoStages.setSelected(false); listStages.getSelectionModel().select(stage); validate();
    }
    @FXML private void quitarEtapa() {
        Stage selected = listStages.getSelectionModel().getSelectedItem();
        if (selected != null) stages.remove(selected);
        renumber(); validate();
    }
    @FXML private void subirEtapa() { move(-1); }
    @FXML private void bajarEtapa() { move(1); }

    private void move(int offset) {
        int current = listStages.getSelectionModel().getSelectedIndex(); int target = current + offset;
        if (current < 0 || target < 0 || target >= stages.size()) return;
        Stage stage = stages.remove(current); stages.add(target, stage); listStages.getSelectionModel().select(target); renumber();
    }
    private void renumber() { for (int i = 0; i < stages.size(); i++) stages.get(i).setPosition(i + 1); listStages.refresh(); }
    private boolean valid() { return txtName.getText() != null && !txtName.getText().isBlank() && (!stages.isEmpty() || chkNoStages.isSelected()); }
    private Stage copyStage(Stage source) {
        Stage copy = new Stage(); copy.setId(source.getId()); copy.setCreatedAt(source.getCreatedAt());
        copy.setProjectId(source.getProjectId()); copy.setName(source.getName()); copy.setPosition(source.getPosition()); copy.setStatus(source.getStatus());
        return copy;
    }
    private void validate() {
        if (lblValidation == null) return;
        if (txtName.getText() == null || txtName.getText().isBlank()) lblValidation.setText("El nombre del proyecto es obligatorio.");
        else if (stages.isEmpty() && !chkNoStages.isSelected()) lblValidation.setText("Agrega al menos una etapa o confirma que las agregarás después.");
        else lblValidation.setText("");
        if (dialogPane != null) ((Button) dialogPane.lookupButton(ButtonType.OK)).setDisable(!valid());
    }

    public Project buildProject() {
        Project project = new Project();
        if (original != null) { project.setId(original.getId()); project.setCreatedAt(original.getCreatedAt()); project.setStatus(original.getStatus()); }
        project.setName(txtName.getText().trim());
        project.setMainGoal(txtGoal.getText() == null || txtGoal.getText().isBlank() ? null : txtGoal.getText().trim());
        project.setDeadline(dateDeadline.getValue()); project.setStages(new ArrayList<>(stages));
        if (project.getStatus() == null) project.setStatus("en_progreso");
        return project;
    }
}
