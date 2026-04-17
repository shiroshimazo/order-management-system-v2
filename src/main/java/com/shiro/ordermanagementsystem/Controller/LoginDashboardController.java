package com.shiro.ordermanagementsystem.Controller;

import com.shiro.ordermanagementsystem.User;
import com.shiro.ordermanagementsystem.UserDAO;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.kordamp.ikonli.javafx.FontIcon;
import org.mindrot.jbcrypt.BCrypt;

public class LoginDashboardController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisible;
    @FXML private FontIcon toggleIcon;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    private boolean isPasswordVisible = false;

    // ─── Initialize ───────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        toggleIcon.setIconLiteral("far-eye-slash");
        toggleIcon.setIconSize(14);
        toggleIcon.setIconColor(javafx.scene.paint.Color.web("#aaaaaa"));

        // Hide error label as soon as the user starts typing
        usernameField.textProperty().addListener((obs, oldVal, newVal) -> hideError());
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> hideError());
        passwordVisible.textProperty().addListener((obs, oldVal, newVal) -> hideError());
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
            toggleIcon.setIconLiteral("far-eye");
            toggleIcon.setIconColor(javafx.scene.paint.Color.web("#D92A1C"));
        } else {
            passwordField.setText(passwordVisible.getText());
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordVisible.setVisible(false);
            passwordVisible.setManaged(false);
            toggleIcon.setIconLiteral("far-eye-slash");
            toggleIcon.setIconColor(javafx.scene.paint.Color.web("#cccccc"));
        }
    }

    // ─── Handle Login ─────────────────────────────────────────────────────────
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = isPasswordVisible
                ? passwordVisible.getText().trim()
                : passwordField.getText().trim();

        // 1. Empty field check
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter your username and password.");
            return;
        }

        // 2. Look up user in DB
        User user = UserDAO.findByUsername(username);
        if (user == null) {
            showError("Invalid username or password.");
            return;
        }

        // 3. Verify BCrypt password
        if (!BCrypt.checkpw(password, user.getPassword())) {
            showError("Invalid username or password.");
            return;
        }

        // 4. Route by role
        switch (user.getRole()) {
            case ADMIN    -> showAdminAuthPopup();
            case CUSTOMER -> navigateTo("/ordermanagementsystem/fxml/CustomerHome.fxml");
        }
    }

    // ─── Admin ID Pop-up ──────────────────────────────────────────────────────
    private void showAdminAuthPopup() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Admin Verification");
        dialog.setHeaderText("Admin Access Only");
        dialog.setContentText("Enter your Admin ID:");
        dialog.getDialogPane().setStyle(
                "-fx-background-color: #FDFDFD; -fx-font-size: 13px;"
        );
        dialog.showAndWait().ifPresent(adminId -> {
            if (adminId.equals("ADMIN-001")) {
                navigateTo("/ordermanagementsystem/fxml/AdminDashboard.fxml");
            } else {
                showError("Invalid Admin ID. Access denied.");
            }
        });
    }

    // ─── Navigation ───────────────────────────────────────────────────────────
    private void navigateTo(String fxmlPath) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource(fxmlPath)
            );
            javafx.scene.Parent root = loader.load();
            loginButton.getScene().setRoot(root);
        } catch (Exception e) {
            showError("Navigation error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ─── Forgot Password ──────────────────────────────────────────────────────
    @FXML
    private void handleForgotPassword() {
        System.out.println("Forgot password clicked");
    }

    // ─── Create Account ───────────────────────────────────────────────────────
    @FXML
    private void handleCreateAccount() {
        System.out.println("Create account clicked");
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