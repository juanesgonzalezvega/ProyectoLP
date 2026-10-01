package com.example.proyectolp.controller;

import com.example.proyectolp.config.AppConfig;
import com.example.proyectolp.model.*;
import com.example.proyectolp.repository.*;
import com.example.proyectolp.service.*;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.io.IOException;

public class DashboardController {
    @FXML private Label lblPendientes, lblEnProgreso, lblCompletadas, lblVencidas, lblTareasError;
    @FXML private VBox boxProyectos, boxHabitos;
    @FXML private Label lblProyectosError, lblHabitosError, lblRacha, lblRachaError;
    @FXML private Label lblFinancialBalance, lblFinancialError;
    @FXML private Button btnActualizar;
    @FXML private Button btnTareas;
    @FXML private Button btnProyectos;
    @FXML private Button btnHabitos;
    @FXML private Button btnFinanzas;
    @FXML private Parent dashboardRoot;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "dashboard-loader"); t.setDaemon(true); return t; });
    private final ProjectProgressService progressService = new ProjectProgressService();
    private final ProductivityStreakService streakService = new ProductivityStreakService();
    private final FinanceCalculationService financeService = new FinanceCalculationService();

    @FXML private void initialize() { refrescar(); }

    @FXML private void abrirTareas() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/proyectolp/ui/tasks.fxml"));
            Parent tasksView = loader.load();
            TasksController controller = loader.getController();
            controller.setDashboard(dashboardRoot, this);
            javafx.stage.Stage stage = (javafx.stage.Stage) dashboardRoot.getScene().getWindow();
            stage.getScene().setRoot(tasksView);
        } catch (IOException e) { lblTareasError.setText("No se pudo abrir tareas: " + e.getMessage()); }
    }

    @FXML private void abrirProyectos() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/proyectolp/ui/projects.fxml"));
            Parent projectsView = loader.load();
            ProjectsController controller = loader.getController();
            controller.setDashboard(dashboardRoot, this);
            javafx.stage.Stage stage = (javafx.stage.Stage) dashboardRoot.getScene().getWindow();
            stage.getScene().setRoot(projectsView);
        } catch (IOException e) { lblProyectosError.setText("No se pudo abrir proyectos: " + e.getMessage()); }
    }

    @FXML private void abrirHabitos() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/proyectolp/ui/habits.fxml"));
            Parent habitsView = loader.load();
            HabitsController controller = loader.getController(); controller.setDashboard(dashboardRoot, this);
            javafx.stage.Stage stage = (javafx.stage.Stage) dashboardRoot.getScene().getWindow();
            stage.getScene().setRoot(habitsView);
        } catch (IOException e) { lblHabitosError.setText("No se pudo abrir hábitos: " + e.getMessage()); }
    }

    @FXML private void abrirFinanzas() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/proyectolp/ui/finances.fxml"));
            Parent view = loader.load();
            loader.<FinancesController>getController().setDashboard(dashboardRoot, this);
            dashboardRoot.getScene().setRoot(view);
        } catch (IOException e) { lblFinancialError.setText("No se pudo abrir Finanzas."); }
    }

    /** Recarga todos los datos visibles desde Supabase. */
    public void refrescar() {
        btnActualizar.setDisable(true);
        lblTareasError.setText("Cargando tareas…"); lblProyectosError.setText("Cargando proyectos…");
        lblHabitosError.setText("Cargando hábitos…"); lblRachaError.setText("Calculando racha…");
        try {
            AppConfig config = AppConfig.load();
            SupabaseService supabase = new SupabaseService(config);
            Task<DashboardData> load = new Task<>() {
                @Override protected DashboardData call() {
                    DashboardData data = new DashboardData();
                    TaskRepository taskRepo = new TaskRepository(supabase);
                    ProjectRepository projectRepo = new ProjectRepository(supabase);
                    HabitRepository habitRepo = new HabitRepository(supabase);
                    try { data.tasks = taskRepo.findDashboardTasks(); } catch (Exception e) { data.taskError = readable(e); }
                    try { data.projects = projectRepo.listarConEtapas(); } catch (Exception e) { data.projectError = readable(e); }
                    try { data.habits = habitRepo.findActive(); } catch (Exception e) { data.habitError = readable(e); }
                    try { data.logs = habitRepo.findLogs(LocalDate.now()); } catch (Exception e) { data.habitError = append(data.habitError, readable(e)); }
                    try {
                        List<FinancialMovement> movements = new ArrayList<>();
                        movements.addAll(new IncomeRepository(supabase).listar());
                        movements.addAll(new ExpenseRepository(supabase).listar());
                        data.financialBalance = financeService.balance(movements).available();
                    } catch (Exception e) { data.financeError = readable(e); }
                    return data;
                }
            };
            load.setOnSucceeded(event -> { render(load.getValue()); btnActualizar.setDisable(false); });
            load.setOnFailed(event -> { String error = readable(load.getException()); showAllErrors(error); btnActualizar.setDisable(false); });
            executor.submit(load);
        } catch (Exception e) {
            showAllErrors(readable(e)); btnActualizar.setDisable(false);
        }
    }

    private void render(DashboardData data) {
        LocalDate today = LocalDate.now();
        if (data.taskError == null) {
            int pending = 0, progress = 0, done = 0, overdue = 0;
            for (TaskItem task : data.tasks) {
                String status = task.getStatus() == null ? "" : task.getStatus();
                if (status.equals("pendiente")) pending++;
                if (status.equals("en_progreso")) progress++;
                if (status.equals("completada")) done++;
                if (!status.equals("completada") && task.getDueDate() != null && task.getDueDate().isBefore(today)) overdue++;
            }
            lblPendientes.setText(Integer.toString(pending)); lblEnProgreso.setText(Integer.toString(progress));
            lblCompletadas.setText(Integer.toString(done)); lblVencidas.setText(Integer.toString(overdue));
            lblTareasError.setText("");
        } else lblTareasError.setText(data.taskError);

        if (data.projectError == null) {
            boxProyectos.getChildren().clear();
            int activeProjects = 0;
            for (Project project : data.projects) {
                if ("completada".equals(project.getStatus())) continue;
                activeProjects++;
                List<Stage> stages = project.getStages();
                ProjectProgressService.Progress p = progressService.calculate(stages);
                Label name = new Label(project.getName()); name.setStyle("-fx-font-weight: bold;");
                if (!p.hasStages()) boxProyectos.getChildren().add(new VBox(3, name, new Label("Sin etapas")));
                else {
                    ProgressBar bar = new ProgressBar(p.percent() / 100.0); bar.setMaxWidth(Double.MAX_VALUE);
                    boxProyectos.getChildren().add(new VBox(4, name, bar, new Label(p.percent() + "% · " + p.completed() + "/" + p.total() + " etapas")));
                }
            }
            if (activeProjects == 0) boxProyectos.getChildren().add(new Label("No hay proyectos activos."));
            lblProyectosError.setText("");
        } else lblProyectosError.setText(data.projectError);

        if (data.habitError == null) {
            boxHabitos.getChildren().clear();
            Set<Long> completed = data.logs.stream().filter(HabitLog::isCompleted).map(HabitLog::getHabitId).collect(Collectors.toSet());
            for (Habit habit : data.habits) {
                CheckBox check = new CheckBox(habit.getName()); check.setSelected(completed.contains(habit.getId()));
                check.setOnAction(event -> {
                    boolean value = check.isSelected(); LocalDate date = LocalDate.now(); check.setDisable(true);
                    executor.submit(() -> {
                        try {
                            new HabitRepository(new SupabaseService(AppConfig.load())).upsertLog(habit.getId(), date, value);
                            javafx.application.Platform.runLater(() -> { check.setDisable(false); refrescar(); });
                        } catch (Exception e) {
                            javafx.application.Platform.runLater(() -> { check.setSelected(!value); check.setDisable(false); lblHabitosError.setText(readable(e)); });
                        }
                    });
                });
                boxHabitos.getChildren().add(check);
            }
            if (data.habits.isEmpty()) boxHabitos.getChildren().add(new Label("No hay hábitos activos."));
            lblHabitosError.setText("");
        } else lblHabitosError.setText(data.habitError);

        if (data.taskError == null && data.habitError == null) {
            Set<LocalDate> activity = new HashSet<>();
            for (TaskItem task : data.tasks) if ("completada".equals(task.getStatus())) parseDate(task.getCreatedAt()).ifPresent(activity::add);
            for (HabitLog log : data.logs) if (log.isCompleted() && log.getLogDate() != null) activity.add(log.getLogDate());
            lblRacha.setText(Integer.toString(streakService.calculate(activity, today)));
            lblRachaError.setText("días consecutivos");
        } else lblRachaError.setText("No fue posible cargar tareas y hábitos.");
        if (data.financeError == null) {
            lblFinancialBalance.setText("$" + data.financialBalance.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
            lblFinancialError.setText("");
        } else { lblFinancialBalance.setText("Sin datos" ); lblFinancialError.setText(data.financeError); }
    }

    private static Optional<LocalDate> parseDate(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        try { return Optional.of(OffsetDateTime.parse(value).toLocalDate()); }
        catch (DateTimeParseException ignored) { try { return Optional.of(LocalDateTime.parse(value).toLocalDate()); } catch (DateTimeParseException e) { return Optional.empty(); } }
    }
    private static String readable(Throwable e) {
        if (e instanceof SupabaseException s) {
            if (s.getStatusCode() == 401 || s.getStatusCode() == 403) return "Supabase rechazó el acceso. Revisa la clave y las políticas RLS.";
            if (s.getStatusCode() == 404) return "Falta una tabla del sistema. Ejecuta supabase/00_schema.sql.";
            if (s.getStatusCode() >= 500) return "Supabase presenta un problema temporal. Inténtalo de nuevo.";
        }
        if (e instanceof java.io.IOException || e instanceof InterruptedException) return "No fue posible conectar con Supabase. Revisa internet y config.properties.";
        return "No fue posible cargar algunos datos. Revisa la conexión y vuelve a intentarlo.";
    }
    private static String append(String current, String next) { return current == null ? next : current + " | " + next; }
    private void showAllErrors(String error) { lblTareasError.setText(error); lblProyectosError.setText(error); lblHabitosError.setText(error); lblRachaError.setText(error); }
    private static class DashboardData {
        List<TaskItem> tasks = List.of(); List<Project> projects = List.of();
        List<Habit> habits = List.of(); List<HabitLog> logs = List.of();
        java.math.BigDecimal financialBalance = java.math.BigDecimal.ZERO;
        String financeError;
        String taskError, projectError, habitError;
    }
}
