package com.shiro.ordermanagementsystem.Controller;

import com.shiro.ordermanagementsystem.AdminDAO;
import com.shiro.ordermanagementsystem.CustomerDAO;
import com.shiro.ordermanagementsystem.mail.MailService;
import com.shiro.ordermanagementsystem.mail.VerificationCodeStore;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class ForgotPasswordDashboardController {

    // ─── Form Fields ──────────────────────────────────────────────────────────
    @FXML private TextField     emailField;
    @FXML private Button        sendButton;

    @FXML private TextField     codeField;
    @FXML private Button        verifyButton;
    @FXML private Label         countdownLabel;

    @FXML private HBox          newPasswordRow;
    @FXML private PasswordField newPasswordField;
    @FXML private TextField     newPasswordVisible;
    @FXML private FontIcon      toggleNewIcon;

    @FXML private HBox          confirmPasswordRow;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField     confirmPasswordVisible;
    @FXML private FontIcon      toggleConfirmIcon;

    @FXML private Label         errorLabel;
    @FXML private Button        resetButton;
    @FXML private Button        backButton;

    private boolean isNewVisible      = false;
    private boolean isConfirmVisible  = false;
    private boolean isVerified        = false;
    private String  verifiedEmail     = null;

    private Timeline cooldownTimeline;
    private Timeline countdownTimeline;
    private Instant  codeExpiresAt;

    private static final int COOLDOWN_SECONDS = 30;

    // ─── Initialize ───────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        setupToggleIcon(toggleNewIcon);
        setupToggleIcon(toggleConfirmIcon);

        codeField.setTextFormatter(new TextFormatter<>(change -> {
            String text = change.getControlNewText();
            return text.matches("\\d{0,6}") ? change : null;
        }));

        emailField.textProperty().addListener((o, ov, nv) -> hideError());
        codeField.textProperty().addListener((o, ov, nv) -> hideError());
        newPasswordField.textProperty().addListener((o, ov, nv) -> hideError());
        newPasswordVisible.textProperty().addListener((o, ov, nv) -> hideError());
        confirmPasswordField.textProperty().addListener((o, ov, nv) -> hideError());
        confirmPasswordVisible.textProperty().addListener((o, ov, nv) -> hideError());
    }

    private void setupToggleIcon(FontIcon icon) {
        icon.setIconLiteral("far-eye-slash");
        icon.setIconSize(14);
        icon.setIconColor(javafx.scene.paint.Color.web("#aaaaaa"));
    }

    // ─── Toggle Password Visibility ───────────────────────────────────────────
    @FXML
    private void toggleNewPasswordVisibility() {
        isNewVisible = !isNewVisible;
        if (isNewVisible) {
            newPasswordVisible.setText(newPasswordField.getText());
            newPasswordVisible.setVisible(true);
            newPasswordVisible.setManaged(true);
            newPasswordField.setVisible(false);
            newPasswordField.setManaged(false);
            toggleNewIcon.setIconLiteral("far-eye");
            toggleNewIcon.setIconColor(javafx.scene.paint.Color.web("#D92A1C"));
        } else {
            newPasswordField.setText(newPasswordVisible.getText());
            newPasswordField.setVisible(true);
            newPasswordField.setManaged(true);
            newPasswordVisible.setVisible(false);
            newPasswordVisible.setManaged(false);
            toggleNewIcon.setIconLiteral("far-eye-slash");
            toggleNewIcon.setIconColor(javafx.scene.paint.Color.web("#cccccc"));
        }
    }

    @FXML
    private void toggleConfirmPasswordVisibility() {
        isConfirmVisible = !isConfirmVisible;
        if (isConfirmVisible) {
            confirmPasswordVisible.setText(confirmPasswordField.getText());
            confirmPasswordVisible.setVisible(true);
            confirmPasswordVisible.setManaged(true);
            confirmPasswordField.setVisible(false);
            confirmPasswordField.setManaged(false);
            toggleConfirmIcon.setIconLiteral("far-eye");
            toggleConfirmIcon.setIconColor(javafx.scene.paint.Color.web("#D92A1C"));
        } else {
            confirmPasswordField.setText(confirmPasswordVisible.getText());
            confirmPasswordField.setVisible(true);
            confirmPasswordField.setManaged(true);
            confirmPasswordVisible.setVisible(false);
            confirmPasswordVisible.setManaged(false);
            toggleConfirmIcon.setIconLiteral("far-eye-slash");
            toggleConfirmIcon.setIconColor(javafx.scene.paint.Color.web("#cccccc"));
        }
    }

    // ─── Send Verification Code ───────────────────────────────────────────────
    @FXML
    private void handleSendCode() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showError("Please enter your email.");
            return;
        }
        if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.-]+$")) {
            showError("Please enter a valid email address.");
            return;
        }
        if (!CustomerDAO.emailExists(email) && !AdminDAO.emailExists(email)) {
            showError("No account found with this email.");
            return;
        }

        sendButton.setDisable(true);
        sendButton.setText("Sending…");

        final String code = VerificationCodeStore.generateCode();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                MailService.sendVerificationCode(email, code);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            Instant expiresAt = VerificationCodeStore.save(email, code);
            startCountdown(expiresAt);
            startCooldown(COOLDOWN_SECONDS);
            showInfoAlert("Code Sent", "A verification code has been sent to " + email + ".");
        });

        task.setOnFailed(e -> {
            sendButton.setDisable(false);
            sendButton.setText("Send");
            Throwable ex = task.getException();
            showError("Could not send email: " + (ex != null ? ex.getMessage() : "unknown error"));
        });

        Thread t = new Thread(task, "mail-sender");
        t.setDaemon(true);
        t.start();
    }

    // ─── Verify Code ──────────────────────────────────────────────────────────
    @FXML
    private void handleVerifyCode() {
        String email = emailField.getText().trim();
        String code  = codeField.getText().trim();

        if (email.isEmpty() || code.isEmpty()) {
            showError("Enter your email and the 6-digit code.");
            return;
        }
        if (code.length() != 6) {
            showError("Verification code must be 6 digits.");
            return;
        }
        if (!VerificationCodeStore.verify(email, code)) {
            showError("Invalid or expired code.");
            return;
        }

        isVerified   = true;
        verifiedEmail = email;

        newPasswordRow.setDisable(false);
        confirmPasswordRow.setDisable(false);
        resetButton.setDisable(false);

        codeField.setDisable(true);
        verifyButton.setDisable(true);
        sendButton.setDisable(true);
        stopCountdown();
        countdownLabel.setText("Verified");
        countdownLabel.getStyleClass().removeAll("countdown-label");
        countdownLabel.getStyleClass().add("countdown-label-ok");
        countdownLabel.setVisible(true);
        countdownLabel.setManaged(true);
    }

    // ─── Reset Password ───────────────────────────────────────────────────────
    @FXML
    private void handleResetPassword() {
        if (!isVerified || verifiedEmail == null) {
            showError("Please verify your email first.");
            return;
        }

        String newPassword = isNewVisible
                ? newPasswordVisible.getText().trim()
                : newPasswordField.getText().trim();

        String confirm = isConfirmVisible
                ? confirmPasswordVisible.getText().trim()
                : confirmPasswordField.getText().trim();

        if (newPassword.isEmpty() || confirm.isEmpty()) {
            showError("Please fill in both password fields.");
            return;
        }
        if (newPassword.length() < 6) {
            showError("Password must be at least 6 characters.");
            return;
        }
        if (!newPassword.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }

        boolean ok = AdminDAO.emailExists(verifiedEmail)
                ? AdminDAO.updatePasswordByEmail(verifiedEmail, newPassword)
                : CustomerDAO.updatePasswordByEmail(verifiedEmail, newPassword);
        if (!ok) {
            showError("Could not update password. Please try again.");
            return;
        }

        VerificationCodeStore.clear(verifiedEmail);
        showInfoAlert("Password Reset", "Your password has been reset. You can now sign in.");
        goToLogin();
    }

    // ─── Countdown (3-min code expiry) ────────────────────────────────────────
    private void startCountdown(Instant expiresAt) {
        codeExpiresAt = expiresAt;
        countdownLabel.getStyleClass().removeAll("countdown-label-ok", "countdown-label-expired");
        if (!countdownLabel.getStyleClass().contains("countdown-label")) {
            countdownLabel.getStyleClass().add("countdown-label");
        }
        countdownLabel.setVisible(true);
        countdownLabel.setManaged(true);
        updateCountdownText();

        if (countdownTimeline != null) countdownTimeline.stop();
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateCountdownText()));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    private void updateCountdownText() {
        long remaining = ChronoUnit.SECONDS.between(Instant.now(), codeExpiresAt);
        if (remaining <= 0) {
            stopCountdown();
            countdownLabel.getStyleClass().removeAll("countdown-label");
            countdownLabel.getStyleClass().add("countdown-label-expired");
            countdownLabel.setText("Expired");
            verifyButton.setDisable(true);
            return;
        }
        long m = remaining / 60;
        long s = remaining % 60;
        countdownLabel.setText(String.format("%d:%02d", m, s));
    }

    private void stopCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
    }

    // ─── Cooldown (resend button) ─────────────────────────────────────────────
    private void startCooldown(int seconds) {
        sendButton.setDisable(true);
        final int[] remaining = { seconds };
        sendButton.setText("Resend " + remaining[0] + "s");

        if (cooldownTimeline != null) cooldownTimeline.stop();
        cooldownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remaining[0]--;
            if (remaining[0] <= 0) {
                cooldownTimeline.stop();
                cooldownTimeline = null;
                if (!isVerified) {
                    sendButton.setDisable(false);
                    sendButton.setText("Resend Code");
                }
            } else {
                sendButton.setText("Resend " + remaining[0] + "s");
            }
        }));
        cooldownTimeline.setCycleCount(seconds);
        cooldownTimeline.play();
    }

    // ─── Alerts ───────────────────────────────────────────────────────────────
    private void showInfoAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.getDialogPane().setStyle(
                "-fx-background-color: #FDFDFD; -fx-font-size: 13px;"
        );
        alert.showAndWait();
    }

    // ─── Back to Login ────────────────────────────────────────────────────────
    @FXML
    private void handleBackToLogin() {
        goToLogin();
    }

    private void goToLogin() {
        try {
            stopCountdown();
            if (cooldownTimeline != null) cooldownTimeline.stop();

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/ordermanagementsystem/fxml/LoginDashBoard.fxml")
            );
            javafx.scene.Parent root = loader.load();

            Stage stage = (Stage) resetButton.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setWidth(420);
            stage.setHeight(660);
            stage.centerOnScreen();
        } catch (Exception e) {
            showError("Navigation error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ─── Show / Hide Error ────────────────────────────────────────────────────
    private void showError(String message) {
        Platform.runLater(() -> {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        });
    }

    private void hideError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }
}
