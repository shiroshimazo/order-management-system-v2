package com.shiro.ordermanagementsystem.Controller;

import com.shiro.ordermanagementsystem.User;
import com.shiro.ordermanagementsystem.UserDAO;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

public class SignUpDashboardController {

    // ─── Form Fields ──────────────────────────────────────────────────────────
    @FXML private TextField     usernameField;
    @FXML private TextField     fullNameField;
    @FXML private TextField     emailField;
    @FXML private TextField     phoneField;

    @FXML private PasswordField passwordField;
    @FXML private TextField     passwordVisible;
    @FXML private FontIcon      togglePasswordIcon;

    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField     confirmPasswordVisible;
    @FXML private FontIcon      toggleConfirmIcon;

    @FXML private Label         errorLabel;
    @FXML private Button        createAccountButton;

    private boolean isPasswordVisible = false;
    private boolean isConfirmVisible  = false;

    // ─── Initialize ───────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        setupToggleIcon(togglePasswordIcon);
        setupToggleIcon(toggleConfirmIcon);

        usernameField.textProperty().addListener((o, ov, nv) -> hideError());
        fullNameField.textProperty().addListener((o, ov, nv) -> hideError());
        emailField.textProperty().addListener((o, ov, nv) -> hideError());
        phoneField.textProperty().addListener((o, ov, nv) -> hideError());
        passwordField.textProperty().addListener((o, ov, nv) -> hideError());
        passwordVisible.textProperty().addListener((o, ov, nv) -> hideError());
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
    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            passwordVisible.setText(passwordField.getText());
            passwordVisible.setVisible(true);
            passwordVisible.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            togglePasswordIcon.setIconLiteral("far-eye");
            togglePasswordIcon.setIconColor(javafx.scene.paint.Color.web("#D92A1C"));
        } else {
            passwordField.setText(passwordVisible.getText());
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordVisible.setVisible(false);
            passwordVisible.setManaged(false);
            togglePasswordIcon.setIconLiteral("far-eye-slash");
            togglePasswordIcon.setIconColor(javafx.scene.paint.Color.web("#cccccc"));
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

    // ─── Handle Create Account ────────────────────────────────────────────────
    @FXML
    private void handleCreateAccount() {
        String username = usernameField.getText().trim();
        String fullName = fullNameField.getText().trim();
        String email    = emailField.getText().trim();
        String phone    = phoneField.getText().trim();

        String password = isPasswordVisible
                ? passwordVisible.getText().trim()
                : passwordField.getText().trim();

        String confirm = isConfirmVisible
                ? confirmPasswordVisible.getText().trim()
                : confirmPasswordField.getText().trim();

        // ── Field validation ──
        if (username.isEmpty() || fullName.isEmpty() || email.isEmpty()
                || phone.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            showError("Please fill in all the fields.");
            return;
        }

        if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.-]+$")) {
            showError("Please enter a valid email address.");
            return;
        }

        if (!phone.matches("^[+0-9\\s()-]{7,20}$")) {
            showError("Please enter a valid phone number.");
            return;
        }

        if (password.length() < 6) {
            showError("Password must be at least 6 characters.");
            return;
        }

        if (!password.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }

        // ── Duplicate check ──
        if (UserDAO.usernameExists(username)) {
            showError("Username is already taken.");
            return;
        }

        if (UserDAO.emailExists(email)) {
            showError("Email is already registered.");
            return;
        }

        // ── Save to DB (BCrypt happens inside UserDAO) ──
        boolean saved = UserDAO.createUser(
                username, password, User.Role.CUSTOMER,
                fullName, email, phone
        );

        if (!saved) {
            showError("Could not create account. Please try again.");
            return;
        }

        // ── Success → confirm + redirect to login ──
        showSuccessAlert();
        goToLogin();
    }

    // ─── Success popup ────────────────────────────────────────────────────────
    private void showSuccessAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Account Created");
        alert.setHeaderText("Welcome New User!");
        alert.setContentText("Your account has been created successfully. You can now sign in.");
        alert.getDialogPane().setStyle(
                "-fx-background-color: #FDFDFD; -fx-font-size: 13px;"
        );
        alert.showAndWait();
    }

    // ─── Go Back to Login (420 × 660) ─────────────────────────────────────────
    @FXML
    private void handleGoToLogin() {
        goToLogin();
    }

    private void goToLogin() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/ordermanagementsystem/fxml/LoginDashBoard.fxml")
            );
            javafx.scene.Parent root = loader.load();

            Stage stage = (Stage) createAccountButton.getScene().getWindow();
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
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void hideError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }
}