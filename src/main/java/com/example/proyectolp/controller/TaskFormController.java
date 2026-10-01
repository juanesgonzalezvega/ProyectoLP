package com.example.proyectolp.controller;

import com.example.proyectolp.model.Project;
import com.example.proyectolp.model.TaskItem;
import com.example.proyectolp.service.TaskValidationService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import java.time.LocalDate;
import java.util.List;

public class TaskFormController {
    @FXML private DialogPane dialogPane;
    @FXML private TextField txtName, txtCategory;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<String> cmbPriority, cmbStatus;
    @FXML private DatePicker dateDue;
    @FXML private Slider sldProgress;
    @FXML private Label lblProgress, lblValidation;
    @FXML private ComboBox<Project> cmbProject;
    private final TaskValidationService validation = new TaskValidationService();
    private TaskItem original;
    private List<Project> projects = List.of();
    private boolean creating;

    @FXML private void initialize() {
        cmbPriority.getItems().setAll("baja", "media", "alta");
        cmbStatus.getItems().setAll("pendiente", "en_progreso", "completada");
        cmbProject.setConverter(new StringConverter<>() {
            @Override public String toString(Project project) { return project == null ? "" : project.getName(); }
            @Override public Project fromString(String value) { return null; }
        });
        cmbProject.setCellFactory(list -> new ListCell<>() { @Override protected void updateItem(Project item, boolean empty) { super.updateItem(item, empty); setText(empty ? null : item == null ? "Sin proyecto" : item.getName()); } });
        cmbProject.setButtonCell(new ListCell<>() { @Override protected void updateItem(Project item, boolean empty) { super.updateItem(item, empty); setText(empty || item == null ? "Sin proyecto" : item.getName()); } });
        txtName.textProperty().addListener((o, a, b) -> validateForm());
        cmbPriority.valueProperty().addListener((o, a, b) -> validateForm());
        cmbStatus.valueProperty().addListener((o, a, b) -> validateForm());
        dateDue.valueProperty().addListener((o, a, b) -> validateForm());
        sldProgress.valueProperty().addListener((o, a, b) -> { lblProgress.setText(Integer.toString((int) Math.round(b.doubleValue())) + "%"); validateForm(); });
    }

    public void configure(TaskItem task, List<Project> availableProjects, boolean isCreating) {
        original = task; projects = availableProjects; creating = isCreating;
        cmbProject.getItems().setAll(projects);
        cmbProject.getItems().add(0, null);
        TaskItem value = task == null ? new TaskItem() : task;
        txtName.setText(value.getName()); txtDescription.setText(value.getDescription());
        txtCategory.setText(value.getCategory()); cmbPriority.setValue(value.getPriority() == null ? "media" : value.getPriority());
        cmbStatus.setValue(value.getStatus() == null ? "pendiente" : value.getStatus());
        dateDue.setValue(value.getDueDate()); sldProgress.setValue(value.getProgress());
        if (value.getProjectId() != null) cmbProject.getItems().stream().filter(p -> p.getId() == value.getProjectId()).findFirst().ifPresent(cmbProject::setValue);
        Button save = (Button) dialogPane.lookupButton(ButtonType.OK);
        save.setText("Guardar");
        save.addEventFilter(ActionEvent.ACTION, event -> {
            TaskItem candidate = buildTask();
            String error = validation.validate(candidate, creating, LocalDate.now());
            if (error != null) { lblValidation.setText(error); event.consume(); }
        });
        validateForm();
    }

    public TaskItem buildTask() {
        TaskItem task = new TaskItem();
        if (original != null) { task.setId(original.getId()); task.setCreatedAt(original.getCreatedAt()); }
        task.setName(txtName.getText() == null ? null : txtName.getText().trim());
        task.setDescription(blankToNull(txtDescription.getText())); task.setCategory(blankToNull(txtCategory.getText()));
        task.setPriority(cmbPriority.getValue()); task.setStatus(cmbStatus.getValue());
        task.setDueDate(dateDue.getValue()); task.setProgress((int) Math.round(sldProgress.getValue()));
        Project project = cmbProject.getValue(); task.setProjectId(project == null ? null : project.getId());
        return task;
    }
    private void validateForm() {
        if (lblProgress != null) lblProgress.setText((int) Math.round(sldProgress.getValue()) + "%");
        if (lblValidation == null) return;
        String error = validation.validate(buildTask(), creating, LocalDate.now());
        lblValidation.setText(error == null ? "" : error);
        if (dialogPane != null) ((Button) dialogPane.lookupButton(ButtonType.OK)).setDisable(error != null);
    }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
