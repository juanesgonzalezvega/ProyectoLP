package com.example.proyectolp.controller;

import com.example.proyectolp.config.AppConfig;
import com.example.proyectolp.model.Habit;
import com.example.proyectolp.model.HabitHistoryDay;
import com.example.proyectolp.model.HabitLog;
import com.example.proyectolp.model.HabitSummary;
import com.example.proyectolp.repository.HabitRepository;
import com.example.proyectolp.service.HabitStatisticsService;
import com.example.proyectolp.service.SupabaseService;
import com.example.proyectolp.service.SessionTaskExecutor;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.concurrent.Task;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HabitsController {
    @FXML private TableView<HabitSummary> tblHabits;
    @FXML private TableColumn<HabitSummary, String> colHabitName, colCompliance, colActive;
    @FXML private TableColumn<HabitSummary, Integer> colCurrentStreak, colBestStreak;
    @FXML private TableColumn<HabitSummary, Boolean> colDone;
    @FXML private TableView<HabitHistoryDay> tblHistory;
    @FXML private TableColumn<HabitHistoryDay, LocalDate> colHistoryDate;
    @FXML private TableColumn<HabitHistoryDay, String> colHistoryStatus;
    @FXML private DatePicker dateSelected;
    @FXML private CheckBox chkShowInactive;
    @FXML private Button btnEdit, btnToggleActive, btnNewHabit;
    @FXML private Label lblMessage, lblHistoryTitle;
    @FXML private Parent habitsRoot;

    private final ExecutorService executor = SessionTaskExecutor.get();
    private final HabitStatisticsService statisticsService = new HabitStatisticsService();
    private final ObservableList<HabitSummary> allRows = FXCollections.observableArrayList();
    private FilteredList<HabitSummary> filteredRows;
    private List<HabitLog> allLogs = List.of();
    private Parent dashboardView;
    private DashboardController dashboardController;

    @FXML private void initialize() {
        colHabitName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCurrentStreak.setCellValueFactory(new PropertyValueFactory<>("currentStreak"));
        colBestStreak.setCellValueFactory(new PropertyValueFactory<>("bestStreak"));
        colCompliance.setCellValueFactory(new PropertyValueFactory<>("complianceText"));
        colActive.setCellValueFactory(new PropertyValueFactory<>("activeText"));
        colDone.setCellValueFactory(cell -> cell.getValue().completedOnSelectedDateProperty());
        colDone.setCellFactory(CheckBoxTableCell.forTableColumn(colDone));
        colDone.setOnEditCommit(event -> guardarCumplimiento(event.getRowValue(), event.getNewValue()));
        tblHabits.setEditable(true);
        colHistoryDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colHistoryStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        dateSelected.setValue(LocalDate.now());
        dateSelected.valueProperty().addListener((o, oldDate, newDate) -> actualizarFechaSeleccionada());
        chkShowInactive.selectedProperty().addListener((o, oldValue, newValue) -> aplicarFiltro());
        tblHabits.getSelectionModel().selectedItemProperty().addListener((o, oldValue, newValue) -> mostrarHistorial(newValue));
        filteredRows = new FilteredList<>(allRows, row -> row.getHabit().isActive());
        tblHabits.setItems(filteredRows);
        actualizarAcciones();
        cargarDatos();
    }

    public void setDashboard(Parent view, DashboardController controller) { dashboardView = view; dashboardController = controller; }

    @FXML private void nuevoHabito() { abrirFormulario(null); }
    @FXML private void editarHabito() {
        HabitSummary row = tblHabits.getSelectionModel().getSelectedItem(); if (row != null) abrirFormulario(row.getHabit());
    }
    @FXML private void activarDesactivar() {
        HabitSummary row = tblHabits.getSelectionModel().getSelectedItem(); if (row == null) return;
        boolean newActive = !row.getHabit().isActive(); lblMessage.setText(newActive ? "Activando hábito…" : "Desactivando hábito…");
        btnToggleActive.setDisable(true);
        runRepository(repo -> repo.actualizarActivo(row.getId(), newActive),
                "No se pudo actualizar el hábito.", newActive ? "Hábito activado." : "Hábito desactivado.");
    }

    @FXML private void volverDashboard() {
        if (dashboardView == null) { lblMessage.setText("No se encontró el Dashboard."); return; }
        Scene scene = tblHabits.getScene(); scene.setRoot(dashboardView);
        if (dashboardController != null) dashboardController.refrescar();
    }

    private void abrirFormulario(Habit existing) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/proyectolp/ui/habit-form.fxml"));
            DialogPane pane = loader.load(); HabitFormController form = loader.getController(); form.configure(existing);
            Dialog<Habit> dialog = new Dialog<>(); dialog.setTitle(existing == null ? "Nuevo hábito" : "Editar hábito");
            dialog.setHeaderText(existing == null ? "Crea un hábito" : "Actualiza el hábito");
            dialog.initOwner(tblHabits.getScene().getWindow()); dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setDialogPane(pane); dialog.setResultConverter(button -> button == ButtonType.OK ? form.buildHabit() : null);
            dialog.showAndWait().ifPresent(habit -> {
                String action = existing == null ? "creado" : "actualizado";
                runRepository(repo -> { if (existing == null) repo.crear(habit); else repo.actualizar(habit); },
                        "No se pudo guardar el hábito.", "Hábito " + action + ".");
            });
        } catch (IOException e) { lblMessage.setText("No se pudo abrir el formulario: " + e.getMessage()); }
    }

    private void cargarDatos() {
        lblMessage.setText("Cargando hábitos e historial…");
        Task<Snapshot> load = new Task<>() {
            @Override protected Snapshot call() throws Exception {
                HabitRepository repository = new HabitRepository(new SupabaseService(AppConfig.load()));
                return new Snapshot(repository.listar(), repository.listarTodosLogs());
            }
        };
        load.setOnSucceeded(event -> {
            Snapshot snapshot = load.getValue(); allLogs = snapshot.logs();
            LocalDate today = LocalDate.now(), selectedDate = selectedDate();
            Map<Long, List<HabitLog>> logsByHabit = new HashMap<>();
            for (HabitLog log : allLogs) logsByHabit.computeIfAbsent(log.getHabitId(), ignored -> new ArrayList<>()).add(log);
            allRows.clear();
            for (Habit habit : snapshot.habits()) {
                List<HabitLog> logs = logsByHabit.getOrDefault(habit.getId(), List.of());
                HabitStatisticsService.Statistics stats = statisticsService.calculate(logs, today);
                boolean done = hasCompletion(logs, selectedDate);
                allRows.add(new HabitSummary(habit, stats.currentStreak(), stats.bestStreak(), stats.compliancePercent(), done));
            }
            aplicarFiltro();
            if (tblHabits.getSelectionModel().getSelectedItem() == null && !filteredRows.isEmpty()) tblHabits.getSelectionModel().selectFirst();
            tblHabits.refresh(); lblMessage.setText("Hábitos actualizados."); actualizarAcciones();
        });
        load.setOnFailed(event -> lblMessage.setText("Error al cargar hábitos: " + message(load.getException())));
        executor.submit(load);
    }

    private void guardarCumplimiento(HabitSummary row, boolean completed) {
        LocalDate date = selectedDate();
        if (date == null) return;
        lblMessage.setText("Guardando cumplimiento del " + date + "…"); tblHabits.setDisable(true);
        runRepository(repo -> repo.upsertLog(row.getId(), date, completed),
                "No se pudo guardar el cumplimiento.", "Cumplimiento actualizado.");
    }

    private void runRepository(RepositoryWork work, String error, String success) {
        executor.submit(() -> {
            try {
                work.run(new HabitRepository(new SupabaseService(AppConfig.load())));
                Platform.runLater(() -> { lblMessage.setText(success); tblHabits.setDisable(false); cargarDatos(); });
            } catch (Exception e) {
                Platform.runLater(() -> { lblMessage.setText(error + " " + message(e)); tblHabits.setDisable(false); cargarDatos(); });
            }
        });
    }

    private void actualizarFechaSeleccionada() {
        if (dateSelected.getValue() == null) return;
        for (HabitSummary row : allRows) {
            List<HabitLog> logs = allLogs.stream().filter(log -> log.getHabitId() == row.getId()).toList();
            row.completedOnSelectedDateProperty().set(hasCompletion(logs, dateSelected.getValue()));
        }
        tblHabits.refresh();
    }

    private void mostrarHistorial(HabitSummary row) {
        if (row == null) { lblHistoryTitle.setText("Historial de los últimos 30 días"); tblHistory.getItems().clear(); return; }
        lblHistoryTitle.setText("Últimos 30 días · " + row.getName());
        LocalDate today = LocalDate.now(), from = today.minusDays(29);
        Map<LocalDate, Boolean> statusByDate = new HashMap<>();
        for (HabitLog log : allLogs) if (log.getHabitId() == row.getId() && log.getLogDate() != null) statusByDate.put(log.getLogDate(), log.isCompleted());
        List<HabitHistoryDay> history = new ArrayList<>();
        for (LocalDate day = from; !day.isAfter(today); day = day.plusDays(1)) history.add(new HabitHistoryDay(day, statusByDate.getOrDefault(day, false)));
        tblHistory.setItems(FXCollections.observableArrayList(history));
    }

    private boolean hasCompletion(List<HabitLog> logs, LocalDate date) {
        return logs.stream().anyMatch(log -> log.isCompleted() && date.equals(log.getLogDate()));
    }
    private LocalDate selectedDate() { return dateSelected.getValue() == null ? LocalDate.now() : dateSelected.getValue(); }
    private void aplicarFiltro() {
        if (filteredRows == null) return;
        boolean showInactive = chkShowInactive.isSelected();
        filteredRows.setPredicate(row -> showInactive || row.getHabit().isActive());
        actualizarAcciones();
    }
    private void actualizarAcciones() {
        HabitSummary row = tblHabits == null ? null : tblHabits.getSelectionModel().getSelectedItem();
        boolean hasSelection = row != null;
        if (btnEdit != null) btnEdit.setDisable(!hasSelection);
        if (btnToggleActive != null) { btnToggleActive.setDisable(!hasSelection); btnToggleActive.setText(hasSelection && row.getHabit().isActive() ? "Desactivar" : "Activar"); }
    }
    private static String message(Throwable error) { return error == null || error.getMessage() == null ? "Error inesperado." : error.getMessage(); }
    @FunctionalInterface private interface RepositoryWork { void run(HabitRepository repository) throws Exception; }
    private record Snapshot(List<Habit> habits, List<HabitLog> logs) { }
}
