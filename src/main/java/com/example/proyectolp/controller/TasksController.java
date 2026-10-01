package com.example.proyectolp.controller;

import com.example.proyectolp.config.AppConfig;
import com.example.proyectolp.model.Project;
import com.example.proyectolp.model.TaskItem;
import com.example.proyectolp.repository.ProjectRepository;
import com.example.proyectolp.repository.TaskRepository;
import com.example.proyectolp.service.SupabaseService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.concurrent.Task;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.scene.control.cell.PropertyValueFactory;

public class TasksController {
    @FXML private TableView<TaskItem> tblTasks;
    @FXML private TableColumn<TaskItem, String> colName, colPriority, colStatus, colDueDate, colCategory;
    @FXML private TableColumn<TaskItem, Integer> colProgress;
    @FXML private ComboBox<String> cmbFilter;
    @FXML private Button btnNew, btnEdit, btnDelete, btnComplete;
    @FXML private Label lblMessage;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "tasks-worker"); t.setDaemon(true); return t; });
    private List<Project> projects = List.of();
    private Parent dashboardView;
    private DashboardController dashboardController;

    @FXML private void initialize() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPriority.setCellValueFactory(new PropertyValueFactory<>("priority"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDueDate.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        colProgress.setCellValueFactory(new PropertyValueFactory<>("progress"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        cmbFilter.getItems().setAll("Todas", "Pendientes", "En progreso", "Completadas");
        cmbFilter.setValue("Todas");
        cmbFilter.setOnAction(event -> cargarTareas());
        tblTasks.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> updateActions());
        updateActions();
        cargarTareas();
        cargarProyectos();
    }

    public void setDashboard(Parent view, DashboardController controller) { dashboardView = view; dashboardController = controller; }

    @FXML private void nuevaTarea() { abrirFormulario(null); }
    @FXML private void editarTarea() { TaskItem selected = tblTasks.getSelectionModel().getSelectedItem(); if (selected != null) abrirFormulario(selected); }
    @FXML private void completarTarea() {
        TaskItem selected = tblTasks.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        selected.setStatus("completada"); guardarAsync(selected, false);
    }
    @FXML private void eliminarTarea() {
        TaskItem selected = tblTasks.getSelectionModel().getSelectedItem(); if (selected == null) return;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar la tarea «" + selected.getName() + "»?", ButtonType.CANCEL, ButtonType.OK);
        confirmation.setTitle("Confirmar eliminación"); confirmation.setHeaderText("Eliminar tarea"); confirmation.initOwner(tblTasks.getScene().getWindow());
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        btnDelete.setDisable(true); lblMessage.setText("Eliminando tarea…");
        runRepository(repo -> { repo.eliminar(selected.getId()); return null; }, ignored -> cargarTareas(), "Tarea eliminada.");
    }

    @FXML private void volverDashboard() {
        if (dashboardView == null) { lblMessage.setText("No se encontró la pantalla Dashboard."); return; }
        Scene scene = tblTasks.getScene(); scene.setRoot(dashboardView);
        if (dashboardController != null) dashboardController.refrescar();
    }

    private void abrirFormulario(TaskItem selected) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/proyectolp/ui/task-form.fxml"));
            DialogPane pane = loader.load();
            TaskFormController form = loader.getController();
            boolean creating = selected == null;
            form.configure(selected, projects, creating);
            Dialog<TaskItem> dialog = new Dialog<>();
            dialog.setTitle(creating ? "Nueva tarea" : "Editar tarea");
            dialog.setHeaderText(creating ? "Completa los datos de la tarea" : "Actualiza los datos de la tarea");
            dialog.initOwner(tblTasks.getScene().getWindow()); dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setDialogPane(pane);
            dialog.setResultConverter(button -> button == ButtonType.OK ? form.buildTask() : null);
            dialog.showAndWait().ifPresent(task -> guardarAsync(task, creating));
        } catch (IOException e) { lblMessage.setText("No se pudo abrir el formulario: " + e.getMessage()); }
    }

    private void guardarAsync(TaskItem task, boolean creating) {
        setBusy(true); lblMessage.setText("Guardando tarea…");
        runRepository(repo -> { if (creating) repo.crear(task); else repo.actualizar(task); return null; }, ignored -> cargarTareas(), creating ? "Tarea creada." : "Tarea actualizada.");
    }

    private void cargarTareas() {
        String filter = cmbFilter.getValue();
        lblMessage.setText("Cargando tareas…");
        runRepository(repo -> switch (filter == null ? "Todas" : filter) {
            case "Pendientes" -> repo.listarPorEstado("pendiente");
            case "En progreso" -> repo.listarPorEstado("en_progreso");
            case "Completadas" -> repo.listarPorEstado("completada");
            default -> repo.listar();
        }, rows -> { tblTasks.setItems(FXCollections.observableArrayList(rows)); lblMessage.setText(rows.isEmpty() ? "No hay tareas para mostrar." : ""); updateActions(); }, "");
    }

    private void cargarProyectos() {
        btnNew.setDisable(true);
        executor.submit(() -> {
            try { List<Project> found = new ProjectRepository(new SupabaseService(AppConfig.load())).listar();
                javafx.application.Platform.runLater(() -> { projects = found; btnNew.setDisable(false); });
            } catch (Exception e) { javafx.application.Platform.runLater(() -> { projects = List.of(); btnNew.setDisable(false); lblMessage.setText("No se cargaron proyectos; las tareas podrán quedar sin proyecto."); }); }
        });
    }

    @FunctionalInterface private interface RepositoryWork<T> { T apply(TaskRepository repository) throws Exception; }
    private <T> void runRepository(RepositoryWork<T> work, java.util.function.Consumer<T> success, String successMessage) {
        executor.submit(() -> {
            try {
                T result = work.apply(new TaskRepository(new SupabaseService(AppConfig.load())));
                javafx.application.Platform.runLater(() -> { if (successMessage != null && !successMessage.isBlank()) lblMessage.setText(successMessage); success.accept(result); setBusy(false); });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> { lblMessage.setText("Error: " + (e.getMessage() == null ? "no se pudo completar la operación." : e.getMessage())); setBusy(false); });
            }
        });
    }
    private void updateActions() {
        boolean selected = tblTasks != null && tblTasks.getSelectionModel().getSelectedItem() != null;
        if (btnEdit != null) btnEdit.setDisable(!selected); if (btnDelete != null) btnDelete.setDisable(!selected);
        if (btnComplete != null) btnComplete.setDisable(!selected || "completada".equals(tblTasks.getSelectionModel().getSelectedItem().getStatus()));
    }
    private void setBusy(boolean busy) { btnNew.setDisable(busy || projects == null); btnEdit.setDisable(busy || tblTasks.getSelectionModel().getSelectedItem() == null); btnDelete.setDisable(busy || tblTasks.getSelectionModel().getSelectedItem() == null); btnComplete.setDisable(busy || tblTasks.getSelectionModel().getSelectedItem() == null); }
}
