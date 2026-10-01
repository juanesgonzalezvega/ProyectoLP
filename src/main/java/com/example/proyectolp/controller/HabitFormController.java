package com.example.proyectolp.controller;

import com.example.proyectolp.model.Habit;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class HabitFormController {
    @FXML private DialogPane dialogPane;
    @FXML private TextField txtName;
    @FXML private TextArea txtDescription;
    @FXML private CheckBox chkActive;
    @FXML private Label lblValidation;
    private Habit original;

    @FXML private void initialize() { txtName.textProperty().addListener((o, oldValue, newValue) -> validate()); }

    public void configure(Habit habit) {
        original = habit;
        if (habit == null) chkActive.setSelected(true);
        else { txtName.setText(habit.getName()); txtDescription.setText(habit.getDescription()); chkActive.setSelected(habit.isActive()); }
        Button save = (Button) dialogPane.lookupButton(ButtonType.OK); save.setText("Guardar");
        save.addEventFilter(ActionEvent.ACTION, event -> { if (!valid()) { validate(); event.consume(); } });
        validate();
    }

    private boolean valid() { return txtName.getText() != null && !txtName.getText().isBlank(); }
    private void validate() {
        if (lblValidation == null) return;
        lblValidation.setText(valid() ? "" : "El nombre del hábito es obligatorio.");
        if (dialogPane != null) ((Button) dialogPane.lookupButton(ButtonType.OK)).setDisable(!valid());
    }
    public Habit buildHabit() {
        Habit habit = new Habit();
        if (original != null) { habit.setId(original.getId()); habit.setCreatedAt(original.getCreatedAt()); }
        habit.setName(txtName.getText().trim());
        habit.setDescription(txtDescription.getText() == null || txtDescription.getText().isBlank() ? null : txtDescription.getText().trim());
        habit.setActive(chkActive.isSelected());
        return habit;
    }
}
