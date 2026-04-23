package com.shiro.ordermanagementsystem.Controller;

import com.shiro.ordermanagementsystem.Admin;
import com.shiro.ordermanagementsystem.AdminDAO;
import com.shiro.ordermanagementsystem.Customer;
import com.shiro.ordermanagementsystem.CustomerDAO;
import com.shiro.ordermanagementsystem.session.Session;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
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

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter your username and password.");
            return;
        }

        // ── Admin first ──
        Admin admin = AdminDAO.findByUsername(username);
        if (admin != null) {
            if (!BCrypt.checkpw(password, admin.getPassword())) {
                showError("Invalid username or password.");
                return;
            }
            showAdminAuthPopup(admin);
            return;
        }

        // ── Then customer ──
        Customer customer = CustomerDAO.findByUsername(username);
        if (customer != null && BCrypt.checkpw(password, customer.getPassword())) {
            Session.setCurrentCustomer(customer);
            navigateToCustomer("/ordermanagementsystem/fxml/CustomerHome.fxml", 1100, 720);
            return;
        }

        showError("Invalid username or password.");
    }

    // ─── Admin ID Pop-up (validates against DB admin_code) ────────────────────
    private void showAdminAuthPopup(Admin admin) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Admin Verification");
        dialog.setHeaderText("Admin Access Only");
        dialog.setContentText("Enter your Admin ID:");
        dialog.getDialogPane().setStyle(
                "-fx-background-color: #FDFDFD; -fx-font-size: 13px;"
        );
        dialog.showAndWait().ifPresent(adminId -> {
            if (adminId.trim().equalsIgnoreCase(admin.getAdminCode())) {
                AdminDAO.updateLastLogin(admin.getId());
                Session.setCurrentAdmin(admin);
                navigateToAdmin("/ordermanagementsystem/fxml/AdminShell.fxml", 1280, 800);
            } else {
                showError("Invalid Admin ID. Access denied.");
            }
        });
    }

    // ─── Navigation ───────────────────────────────────────────────────────────
    private void navigateTo(String fxmlPath, double width, double height) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource(fxmlPath)
            );
            javafx.scene.Parent root = loader.load();

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setWidth(width);
            stage.setHeight(height);
            stage.centerOnScreen();
        } catch (Exception e) {
            showError("Navigation error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void navigateToAdmin(String fxmlPath, double width, double height) {
        navigateToShell(fxmlPath, width, height, 1024, 680);
    }

    private void navigateToCustomer(String fxmlPath, double width, double height) {
        navigateToShell(fxmlPath, width, height, 360, 600);
    }

    private void navigateToShell(String fxmlPath, double width, double height,
                                 double minWidth, double minHeight) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource(fxmlPath)
            );
            javafx.scene.Parent root = loader.load();

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setResizable(true);
            stage.setMinWidth(minWidth);
            stage.setMinHeight(minHeight);
            stage.setWidth(width);
            stage.setHeight(height);
            stage.centerOnScreen();
        } catch (Exception e) {
            showError("Navigation error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ─── Forgot Password ──────────────────────────────────────────────────────
    @FXML
    private void handleForgotPassword() {
        navigateTo("/ordermanagementsystem/fxml/ForgotPasswordDashboard.fxml", 420, 760);
    }

    // ─── Create Account → SignUp form (420 × 880) ─────────────────────────────
    @FXML
    private void handleCreateAccount() {
        navigateTo("/ordermanagementsystem/fxml/SignUpDashboard.fxml", 420, 880);
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
