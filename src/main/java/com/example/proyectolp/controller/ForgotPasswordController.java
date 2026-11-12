package com.example.proyectolp.controller;

import com.example.proyectolp.MainApp;
import com.example.proyectolp.config.AppConfig;
import com.example.proyectolp.model.enums.AccountStatus;
import com.example.proyectolp.service.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Controlador de recuperación por OTP de correo. */
public class ForgotPasswordController {
    @FXML private VBox root;
    @FXML private VBox stepEmail, stepCode;
    @FXML private TextField emailField, codeField;
    @FXML private PasswordField newPasswordField, confirmPasswordField;
    @FXML private Label deliveryLabel, step1Message, step2Message;
    @FXML private Button sendCodeButton, changePasswordButton;
    @FXML private Hyperlink resendLink;
    @FXML private ProgressIndicator loadingIndicator;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "auth-password-recovery");
        thread.setDaemon(true);
        return thread;
    });
    private final RecoveryThrottle throttle = RecoveryThrottle.SHARED;
    private Timeline countdown;
    private String recoveryEmail = "";

    @FXML private void initialize() {
        codeField.setTextFormatter(new TextFormatter<String>(change -> {
            String value = change.getControlNewText();
            return value.matches("[0-9]{0,6}") ? change : null;
        }));
        root.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (oldScene != null && newScene == null) close();
        });
        showStep1();
    }

    @FXML private void sendCode() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        if (!AuthService.validEmail(email)) { step1Message.setText("Escribe un correo válido."); return; }
        recoveryEmail = email;
        setBusy(true);
        Task<PasswordRecoveryService.StartResult> task = new Task<>() {
            @Override protected PasswordRecoveryService.StartResult call() throws Exception {
                return service().start(email);
            }
        };
        task.setOnSucceeded(event -> {
            setBusy(false);
            PasswordRecoveryService.StartResult result = task.getValue();
            switch (result.status()) {
                case NOT_FOUND -> step1Message.setText("Cuenta no encontrada, por favor ingresar un correo válido.");
                case UNVERIFIED -> step1Message.setText("Tu cuenta aún no está verificada. Revisa tu correo y confirma tu cuenta antes de recuperar la contraseña.");
                case THROTTLED -> step1Message.setText("Alcanzaste el límite de consultas. Espera 10 minutos e inténtalo de nuevo.");
                case RESEND_WAIT -> step1Message.setText("Espera " + result.waitSeconds() + " segundos antes de solicitar otro código.");
                case EMAIL_LIMIT -> step1Message.setText("Alcanzaste el máximo de 5 correos por hora. Inténtalo de nuevo en " + Math.max(1, (result.waitSeconds() + 59) / 60) + " minutos.");
                case CODE_SENT -> enterCodeStep(false);
                case GENERIC_SENT -> enterCodeStep(true);
            }
        });
        task.setOnFailed(event -> {
            setBusy(false);
            step1Message.setText(userMessage(task.getException()));
            long remaining = throttle.resendSeconds(recoveryEmail);
            if (stepCode.isVisible() && remaining > 0) startCountdown(remaining);
        });
        executor.submit(task);
    }

    @FXML private void changePassword() {
        String code = codeField.getText() == null ? "" : codeField.getText();
        String password = newPasswordField.getText();
        String confirmation = confirmPasswordField.getText();
        if (!code.matches("[0-9]{6}")) { step2Message.setText("Escribe el código de 6 dígitos."); return; }
        if (!AuthService.validPassword(password)) { step2Message.setText("La contraseña debe tener al menos 8 caracteres."); return; }
        if (!password.equals(confirmation)) { step2Message.setText("Las contraseñas no coinciden."); return; }
        codeField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
        setBusy(true);
        Task<PasswordRecoveryService.CompleteStatus> task = new Task<>() {
            @Override protected PasswordRecoveryService.CompleteStatus call() throws Exception {
                return service().complete(recoveryEmail, code, password);
            }
        };
        task.setOnSucceeded(event -> {
            setBusy(false);
            switch (task.getValue()) {
                case SUCCESS -> MainApp.showLogin("Contraseña actualizada. Inicia sesión con tu nueva contraseña.");
                case INVALID_CODE -> step2Message.setText("El código es incorrecto o ya venció. Solicita uno nuevo.");
                case LOCKED -> {
                    String blockedEmail = recoveryEmail;
                    recoveryEmail = "";
                    emailField.setText(blockedEmail);
                    showStep1();
                    step1Message.setText("Se bloquearon los intentos de código. Solicita un nuevo código para continuar.");
                }
            }
        });
        task.setOnFailed(event -> { setBusy(false); step2Message.setText(userMessage(task.getException())); });
        executor.submit(task);
    }

    @FXML private void resendCode() { sendCode(); }
    @FXML private void useOtherEmail() {
        stopCountdown();
        recoveryEmail = "";
        codeField.clear(); newPasswordField.clear(); confirmPasswordField.clear();
        emailField.clear(); step1Message.setText(""); step2Message.setText("");
        showStep1();
    }
    @FXML private void backToLogin() { MainApp.showLogin(""); }

    private PasswordRecoveryService service() throws IOException {
        return new PasswordRecoveryService(new AuthService(AppConfig.load(), SessionManager.getInstance(), new SessionStorage()), throttle);
    }
    private void enterCodeStep(boolean generic) {
        step1Message.setText(""); step2Message.setText("");
        deliveryLabel.setText(generic
                ? "Si el correo está registrado y verificado, recibirás un código."
                : "Te enviamos un código de 6 dígitos a " + recoveryEmail);
        stepEmail.setVisible(false); stepEmail.setManaged(false);
        stepCode.setVisible(true); stepCode.setManaged(true);
        startCountdown(RecoveryPolicy.RESEND_INTERVAL_SECONDS);
    }
    private void showStep1() {
        stepEmail.setVisible(true); stepEmail.setManaged(true);
        stepCode.setVisible(false); stepCode.setManaged(false);
    }
    private void setBusy(boolean busy) {
        sendCodeButton.setDisable(busy);
        changePasswordButton.setDisable(busy);
        if (resendLink != null && busy) resendLink.setDisable(true);
        if (resendLink != null && !busy && stepCode.isVisible()) resendLink.setDisable(throttle.resendSeconds(recoveryEmail) > 0);
        loadingIndicator.setVisible(busy); loadingIndicator.setManaged(busy);
    }
    private void startCountdown(long seconds) {
        stopCountdown();
        final long[] remaining = {seconds};
        resendLink.setDisable(true);
        resendLink.setText("Reenviar código (" + remaining[0] + " s)");
        countdown = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            remaining[0]--;
            if (remaining[0] <= 0) {
                stopCountdown(); resendLink.setDisable(false); resendLink.setText("Reenviar código");
            } else resendLink.setText("Reenviar código (" + remaining[0] + " s)");
        }));
        countdown.setCycleCount((int) seconds);
        countdown.play();
    }
    private void stopCountdown() { if (countdown != null) { countdown.stop(); countdown = null; } }
    private String userMessage(Throwable error) {
        Throwable cause = error;
        while (cause != null && !(cause instanceof AuthException)) cause = cause.getCause();
        return cause instanceof AuthException auth ? auth.getMessage()
                : "No se pudo completar la solicitud. Revisa tu conexión e inténtalo de nuevo.";
    }
    private void close() { stopCountdown(); executor.shutdownNow(); }
}
