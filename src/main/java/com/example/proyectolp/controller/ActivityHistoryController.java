package com.example.proyectolp.controller;

import com.example.proyectolp.MainApp;
import com.example.proyectolp.config.AppConfig;
import com.example.proyectolp.model.ActivityEntry;
import com.example.proyectolp.model.enums.ActivityCategory;
import com.example.proyectolp.repository.ActivityRepository;
import com.example.proyectolp.service.ActivityFormatter;
import com.example.proyectolp.service.ActivityHistoryService;
import com.example.proyectolp.service.SessionManager;
import com.example.proyectolp.service.SessionTaskExecutor;
import com.example.proyectolp.service.SupabaseException;
import com.example.proyectolp.service.SupabaseService;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.time.Clock;
import java.util.concurrent.ExecutorService;

public class ActivityHistoryController {
    @FXML private ComboBox<ActivityCategory> categoryFilter;
    @FXML private DatePicker fromFilter;
    @FXML private DatePicker throughFilter;
    @FXML private TextField searchField;
    @FXML private TableView<ActivityEntry> activityTable;
    @FXML private TableColumn<ActivityEntry, String> dateColumn;
    @FXML private TableColumn<ActivityEntry, String> categoryColumn;
    @FXML private TableColumn<ActivityEntry, String> summaryColumn;
    @FXML private TextArea detailArea;
    @FXML private Label statusLabel;
    @FXML private Button loadMoreButton;
    @FXML private Button filterButton;
    @FXML private Button clearButton;

    private final ExecutorService executor = SessionTaskExecutor.get();
    private final ActivityFormatter formatter = new ActivityFormatter(Clock.systemUTC());
    private ActivityHistoryService history;
    private Parent dashboardRoot;
    private DashboardController dashboard;
    private ActivityHistoryService.Filter displayedFilter =
            new ActivityHistoryService.Filter(ActivityCategory.ALL, null, null, "");
    private long sessionGeneration;
    private boolean loading;

    @FXML private void initialize() {
        sessionGeneration = SessionManager.getInstance().generation();
        categoryFilter.setItems(FXCollections.observableArrayList(ActivityCategory.values()));
        categoryFilter.getSelectionModel().select(ActivityCategory.ALL);
        dateColumn.setCellValueFactory(value -> new javafx.beans.property.SimpleStringProperty(
                formatter.formatDate(value.getValue().getOccurredAt())));
        categoryColumn.setCellValueFactory(value -> new javafx.beans.property.SimpleStringProperty(
                formatter.categoryLabel(value.getValue().getCategory())));
        summaryColumn.setCellValueFactory(value -> new javafx.beans.property.SimpleStringProperty(
                formatter.truncate(value.getValue().getSummary())));
        activityTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, entry) ->
                showDetail(entry));
        try {
            history = new ActivityHistoryService(new ActivityRepository(new SupabaseService(AppConfig.load())));
            applyFilters();
        } catch (Exception exception) {
            statusLabel.setText("No se pudo preparar la carga del historial.");
        }
    }

    public void setDashboard(Parent root, DashboardController controller) {
        dashboardRoot = root;
        dashboard = controller;
    }

    public void clearForLogout() {
        if (!javafx.application.Platform.isFxApplicationThread()) {
            javafx.application.Platform.runLater(this::clearForLogout);
            return;
        }
        sessionGeneration = Long.MIN_VALUE;
        activityTable.getItems().clear();
        categoryFilter.getSelectionModel().select(ActivityCategory.ALL);
        fromFilter.setValue(null);
        throughFilter.setValue(null);
        searchField.clear();
        detailArea.clear();
        statusLabel.setText("");
        loadMoreButton.setDisable(true);
    }

    @FXML private void applyFilters() {
        if (loading || history == null) return;
        ActivityHistoryService.Filter filter = new ActivityHistoryService.Filter(
                categoryFilter.getValue(), fromFilter.getValue(), throughFilter.getValue(), searchField.getText());
        runLoad(filter);
    }

    @FXML private void clearFilters() {
        categoryFilter.getSelectionModel().select(ActivityCategory.ALL);
        fromFilter.setValue(null);
        throughFilter.setValue(null);
        searchField.clear();
        applyFilters();
    }

    @FXML private void loadMore() {
        if (loading || history == null || !history.snapshot().hasMore()) return;
        setLoading(true);
        Task<ActivityHistoryService.Snapshot> task = new Task<>() {
            @Override protected ActivityHistoryService.Snapshot call() throws Exception {
                return history.loadMore();
            }
        };
        task.setOnSucceeded(event -> {
            if (!SessionManager.getInstance().isCurrent(sessionGeneration)) return;
            render(task.getValue());
            setLoading(false);
            statusLabel.setText("");
        });
        task.setOnFailed(event -> {
            if (!SessionManager.getInstance().isCurrent(sessionGeneration)) return;
            setLoading(false);
            statusLabel.setText(readable(task.getException()));
        });
        executor.submit(task);
    }

    @FXML private void backToDashboard() {
        if (dashboardRoot != null && activityTable.getScene() != null) {
            activityTable.getScene().setRoot(dashboardRoot);
            if (dashboard != null) dashboard.refrescar();
        } else {
            MainApp.showDashboard();
        }
    }

    private void runLoad(ActivityHistoryService.Filter filter) {
        setLoading(true);
        Task<ActivityHistoryService.Snapshot> task = new Task<>() {
            @Override protected ActivityHistoryService.Snapshot call() throws Exception {
                return history.loadFirst(filter);
            }
        };
        task.setOnSucceeded(event -> {
            if (!SessionManager.getInstance().isCurrent(sessionGeneration)) return;
            render(task.getValue());
            displayedFilter = filter;
            setLoading(false);
            statusLabel.setText("");
        });
        task.setOnFailed(event -> {
            if (!SessionManager.getInstance().isCurrent(sessionGeneration)) return;
            setLoading(false);
            restoreDisplayedFilter();
            statusLabel.setText(readable(task.getException()));
        });
        executor.submit(task);
    }

    private void render(ActivityHistoryService.Snapshot snapshot) {
        activityTable.setItems(FXCollections.observableArrayList(snapshot.entries()));
        loadMoreButton.setDisable(!snapshot.hasMore());
        if (snapshot.entries().isEmpty()) detailArea.clear();
    }

    private void restoreDisplayedFilter() {
        categoryFilter.getSelectionModel().select(displayedFilter.category());
        fromFilter.setValue(displayedFilter.from());
        throughFilter.setValue(displayedFilter.through());
        searchField.setText(displayedFilter.search());
    }

    private void showDetail(ActivityEntry entry) {
        if (entry == null) {
            detailArea.clear();
            return;
        }
        String details = formatter.detailText(entry);
        detailArea.setText(details.isBlank() ? entry.getSummary() : entry.getSummary()
                + System.lineSeparator() + System.lineSeparator() + details);
    }

    private void setLoading(boolean value) {
        loading = value;
        categoryFilter.setDisable(value);
        fromFilter.setDisable(value);
        throughFilter.setDisable(value);
        searchField.setDisable(value);
        filterButton.setDisable(value);
        clearButton.setDisable(value);
        loadMoreButton.setDisable(value || history == null || !history.snapshot().hasMore());
    }

    private static String readable(Throwable error) {
        if (error instanceof SupabaseException exception) {
            if (exception.getStatusCode() == 401 || exception.getStatusCode() == 403)
                return "No fue posible acceder al historial de esta cuenta.";
            if (exception.getStatusCode() == 404)
                return "El historial no está instalado. Ejecuta supabase/13_historial_actividad.sql.";
            if (exception.getStatusCode() >= 500)
                return "El servicio de historial no está disponible. Inténtalo de nuevo.";
        }
        if (error instanceof IOException || error instanceof InterruptedException)
            return "No se pudo conectar para cargar el historial.";
        if (error instanceof IllegalArgumentException && error.getMessage() != null)
            return error.getMessage();
        return "No se pudo cargar el historial. Inténtalo de nuevo.";
    }
}
