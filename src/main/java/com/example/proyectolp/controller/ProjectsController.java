package com.example.proyectolp.controller;

import com.example.proyectolp.config.AppConfig;
import com.example.proyectolp.model.Project;
import com.example.proyectolp.model.Stage;
import com.example.proyectolp.repository.ProjectRepository;
import com.example.proyectolp.repository.StageRepository;
import com.example.proyectolp.service.ProjectProgressService;
import com.example.proyectolp.service.SupabaseService;
import com.example.proyectolp.service.SessionTaskExecutor;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ProjectsController {
    @FXML private TableView<Project> tblProjects;
    @FXML private TableColumn<Project, String> colProjectName, colProjectProgress;
    @FXML private TableColumn<Project, LocalDate> colProjectDeadline;
    @FXML private Label lblSelectedName, lblSelectedDeadline, lblProgressText, lblMessage;
    @FXML private ProgressBar barProgress;
    @FXML private ListView<Stage> listProjectStages;
    @FXML private ComboBox<String> cmbStageStatus;
    @FXML private Button btnUpdateStage, btnEdit, btnDelete, btnAttachments;
    @FXML private Parent projectsRoot;
    private final ExecutorService executor = SessionTaskExecutor.get();
    private final ProjectProgressService progressService = new ProjectProgressService();
    private Parent dashboardView;
    private DashboardController dashboardController;

    @FXML private void initialize() {
        colProjectName.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("name"));
        colProjectDeadline.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("deadline"));
        colProjectProgress.setCellValueFactory(cell -> new ReadOnlyStringWrapper(progressService.display(cell.getValue().getStages())));
        cmbStageStatus.getItems().setAll("pendiente", "en_progreso", "completada");
        tblProjects.getSelectionModel().selectedItemProperty().addListener((o, oldProject, project) -> showProject(project));
        listProjectStages.getSelectionModel().selectedItemProperty().addListener((o, oldStage, stage) -> {
            if (stage != null) cmbStageStatus.setValue(stage.getStatus());
            updateStageControls();
        });
        updateSelectionControls();
        cargarProyectos();
    }

    public void setDashboard(Parent view, DashboardController controller) { dashboardView = view; dashboardController = controller; }

    @FXML private void nuevoProyecto() { abrirFormulario(null); }
    @FXML private void editarProyecto() { Project selected = selectedProject(); if (selected != null) abrirFormulario(selected); }
    @FXML private void openAttachments() { Project selected = selectedProject(); if (selected != null) AttachmentsDialog.show(tblProjects.getScene().getWindow(), com.example.proyectolp.model.enums.AttachmentOwnerType.PROJECT, selected.getId(), "Adjuntos del proyecto"); }

    @FXML private void eliminarProyecto() {
        Project project = selectedProject(); if (project == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Al eliminar «" + project.getName() + "», sus etapas se borrarán y las tareas quedarán sin proyecto.",
                ButtonType.CANCEL, ButtonType.OK);
        confirm.setTitle("Confirmar eliminación"); confirm.setHeaderText("Eliminar proyecto");
        confirm.initOwner(tblProjects.getScene().getWindow());
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        lblMessage.setText("Eliminando proyecto…");
        btnDelete.setDisable(true);
        runAsync(() -> { repository().eliminar(project.getId()); return null; }, ignored -> { cargarProyectos(); lblMessage.setText("Proyecto eliminado."); }, "No se pudo eliminar el proyecto.");
    }

    @FXML private void guardarEstadoEtapa() {
        Stage stage = listProjectStages.getSelectionModel().getSelectedItem();
        String status = cmbStageStatus.getValue();
        if (stage == null || status == null) return;
        long projectId = stage.getProjectId();
        Stage updated = new Stage(); updated.setId(stage.getId()); updated.setProjectId(stage.getProjectId());
        updated.setName(stage.getName()); updated.setPosition(stage.getPosition()); updated.setCreatedAt(stage.getCreatedAt()); updated.setStatus(status);
        btnUpdateStage.setDisable(true); lblMessage.setText("Actualizando etapa…");
        runAsync(() -> { new StageRepository(client()).actualizar(updated); return null; }, ignored -> {
            lblMessage.setText("Estado de etapa actualizado."); cargarProyectos(projectId);
        }, "No se pudo actualizar el estado de la etapa.");
    }

    @FXML private void volverDashboard() {
        if (dashboardView == null) { lblMessage.setText("No se encontró el Dashboard."); return; }
        Scene scene = tblProjects.getScene(); scene.setRoot(dashboardView);
        if (dashboardController != null) dashboardController.refrescar();
    }

    private void abrirFormulario(Project project) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/proyectolp/ui/project-form.fxml"));
            DialogPane pane = loader.load();
            ProjectFormController form = loader.getController(); form.configure(project);
            Dialog<Project> dialog = new Dialog<>(); dialog.setTitle(project == null ? "Nuevo proyecto" : "Editar proyecto");
            dialog.setHeaderText(project == null ? "Define el proyecto y sus etapas" : "Actualiza el proyecto y sus etapas");
            dialog.initOwner(tblProjects.getScene().getWindow()); dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setDialogPane(pane); dialog.setResultConverter(button -> button == ButtonType.OK ? form.buildProject() : null);
            dialog.showAndWait().ifPresent(result -> guardarProyecto(result, project == null));
        } catch (IOException e) { lblMessage.setText("No se pudo abrir el formulario: " + e.getMessage()); }
    }

    private void guardarProyecto(Project project, boolean creating) {
        lblMessage.setText("Guardando proyecto y etapas…");
        runAsync(() -> {
            ProjectRepository projects = repository(); StageRepository stages = new StageRepository(client());
            Project saved;
            if (creating) saved = projects.crear(project); else { projects.actualizar(project); saved = project; }
            stages.sincronizar(saved.getId(), project.getStages());
            return saved.getId();
        }, id -> { lblMessage.setText(creating ? "Proyecto creado." : "Proyecto actualizado."); cargarProyectos(id); }, "No se pudo guardar el proyecto o sus etapas.");
    }

    private void cargarProyectos() { cargarProyectos(null); }
    private void cargarProyectos(Long selectId) {
        lblMessage.setText("Cargando proyectos…");
        Task<List<Project>> load = new Task<>() {
            @Override protected List<Project> call() throws Exception { return repository().listarConEtapas(); }
        };
        load.setOnSucceeded(event -> {
            List<Project> projects = load.getValue(); tblProjects.setItems(FXCollections.observableArrayList(projects));
            if (selectId != null) projects.stream().filter(p -> p.getId() == selectId).findFirst().ifPresent(tblProjects.getSelectionModel()::select);
            if (tblProjects.getSelectionModel().getSelectedItem() == null && !projects.isEmpty()) tblProjects.getSelectionModel().selectFirst();
            if (projects.isEmpty()) showProject(null);
            lblMessage.setText(projects.isEmpty() ? "Aún no hay proyectos." : ""); updateSelectionControls();
        });
        load.setOnFailed(event -> { lblMessage.setText("Error al cargar proyectos: " + message(load.getException())); tblProjects.getItems().clear(); showProject(null); });
        executor.submit(load);
    }

    private void cargarProyectos(long projectId) { cargarProyectos(Long.valueOf(projectId)); }

    private void showProject(Project project) {
        if (project == null) {
            lblSelectedName.setText("Selecciona un proyecto"); lblSelectedDeadline.setText(""); lblProgressText.setText("Sin etapas");
            barProgress.setProgress(0); barProgress.setVisible(false); barProgress.setManaged(false); listProjectStages.getItems().clear();
        } else {
            lblSelectedName.setText(project.getName());
            lblSelectedDeadline.setText(project.getDeadline() == null ? "Sin fecha límite" : "Fecha límite: " + project.getDeadline());
            ProjectProgressService.Progress progress = progressService.calculate(project.getStages());
            lblProgressText.setText(progressService.display(project.getStages()));
            barProgress.setVisible(progress.hasStages()); barProgress.setManaged(progress.hasStages());
            barProgress.setProgress(progress.percent() / 100.0);
            listProjectStages.setItems(FXCollections.observableArrayList(project.getStages()));
        }
        updateSelectionControls(); updateStageControls();
    }

    private Project selectedProject() { return tblProjects.getSelectionModel().getSelectedItem(); }
    private void updateSelectionControls() { boolean has = tblProjects != null && selectedProject() != null; if (btnEdit != null) btnEdit.setDisable(!has); if (btnDelete != null) btnDelete.setDisable(!has); if (btnAttachments != null) btnAttachments.setDisable(!has); }
    private void updateStageControls() {
        boolean has = listProjectStages != null && listProjectStages.getSelectionModel().getSelectedItem() != null;
        if (btnUpdateStage != null) btnUpdateStage.setDisable(!has);
        if (cmbStageStatus != null) cmbStageStatus.setDisable(!has);
    }
    private ProjectRepository repository() throws Exception { return new ProjectRepository(client()); }
    private SupabaseService client() throws Exception { return new SupabaseService(AppConfig.load()); }
    @FunctionalInterface private interface Work<T> { T run() throws Exception; }
    private <T> void runAsync(Work<T> work, Consumer<T> success, String errorPrefix) {
        executor.submit(() -> {
            try { T value = work.run(); Platform.runLater(() -> success.accept(value)); }
            catch (Exception e) { Platform.runLater(() -> { lblMessage.setText(errorPrefix + " " + message(e)); updateSelectionControls(); updateStageControls(); }); }
        });
    }
    private static String message(Throwable e) { return e == null || e.getMessage() == null ? "Error inesperado." : e.getMessage(); }
}
