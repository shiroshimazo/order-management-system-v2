package com.shiro.ordermanagementsystem.Controller.admin;

import com.shiro.ordermanagementsystem.Admin;
import com.shiro.ordermanagementsystem.AdminDAO;
import com.shiro.ordermanagementsystem.CustomerDAO;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AdminFormDialogController {

    @FXML private Label         titleLabel;
    @FXML private TextField     adminCodeField;
    @FXML private TextField     usernameField;
    @FXML private TextField     fullNameField;
    @FXML private TextField     emailField;
    @FXML private TextField     phoneField;
    @FXML private ComboBox<Admin.Level> levelCombo;
    @FXML private TextField     positionField;
    @FXML private VBox          passwordRow;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox      activeCheck;
    @FXML private Button        saveButton;
    @FXML private Label         messageLabel;

    private Admin   editing;
    private Integer createdByAdminId;
    private Stage   stage;
    private boolean saved = false;

    @FXML
    public void initialize() {
        levelCombo.setItems(FXCollections.observableArrayList(Admin.Level.values()));
        levelCombo.setValue(Admin.Level.STAFF);
    }

    public void setStage(Stage stage)                          { this.stage = stage; }
    public void setCreatedBy(Integer id)                        { this.createdByAdminId = id; }
    public boolean wasSaved()                                   { return saved; }

    public void setAdmin(Admin admin) {
        this.editing = admin;
        if (admin == null) {
            titleLabel.setText("Add Admin");
            saveButton.setText("Create");
            adminCodeField.setText(AdminDAO.nextAdminCode());
            return;
        }
        titleLabel.setText("Edit Admin");
        saveButton.setText("Save Changes");

        adminCodeField.setText(admin.getAdminCode());
        usernameField.setText(admin.getUsername());
        fullNameField.setText(admin.getFullName());
        emailField.setText(admin.getEmail());
        phoneField.setText(admin.getPhone() == null ? "" : admin.getPhone());
        levelCombo.setValue(admin.getAdminLevel());
        positionField.setText(admin.getPosition() == null ? "" : admin.getPosition());
        activeCheck.setSelected(admin.isActive());

        // No password field when editing — handled via reset dialog
        passwordRow.setVisible(false);
        passwordRow.setManaged(false);
    }

    @FXML
    private void handleCancel() {
        if (stage != null) stage.close();
    }

    @FXML
    private void handleSave() {
        String code     = adminCodeField.getText().trim();
        String username = usernameField.getText().trim();
        String fullName = fullNameField.getText().trim();
        String email    = emailField.getText().trim();
        String phone    = phoneField.getText().trim();
        String position = positionField.getText().trim();
        Admin.Level level = levelCombo.getValue();
        boolean active  = activeCheck.isSelected();

        if (code.isEmpty())     { showError("Admin code is required."); return; }
        if (username.isEmpty()) { showError("Username is required."); return; }
        if (fullName.isEmpty()) { showError("Full name is required."); return; }
        if (email.isEmpty())    { showError("Email is required."); return; }
        if (level == null)      { showError("Select a role."); return; }

        int excludeId = editing == null ? -1 : editing.getId();

        if (AdminDAO.adminCodeExistsExcept(code, excludeId)) {
            showError("That admin code is already in use.");
            return;
        }
        if (AdminDAO.usernameExistsExcept(username, excludeId)) {
            showError("That username is already taken.");
            return;
        }
        if (AdminDAO.emailExistsExcept(email, excludeId)) {
            showError("That email is already in use.");
            return;
        }
        if (CustomerDAO.emailExists(email)
                && (editing == null || !email.equalsIgnoreCase(editing.getEmail()))) {
            showError("That email is already in use.");
            return;
        }

        // Prevent demoting the last active super admin
        if (editing != null
                && editing.getAdminLevel() == Admin.Level.SUPER_ADMIN
                && (level != Admin.Level.SUPER_ADMIN || !active)
                && AdminDAO.countActiveSuperAdmins() <= 1) {
            showError("At least one active SUPER_ADMIN must remain.");
            return;
        }

        boolean ok;
        if (editing == null) {
            String password = passwordField.getText();
            if (password == null || password.length() < 8) {
                showError("Initial password must be at least 8 characters.");
                return;
            }
            ok = AdminDAO.createAdmin(code, username, password, email, fullName,
                    phone.isBlank() ? null : phone, level,
                    position.isBlank() ? null : position, createdByAdminId);
            if (ok && !active) {
                // honor Active=false on create
                Admin created = AdminDAO.findByUsername(username);
                if (created != null) AdminDAO.setActive(created.getId(), false);
            }
        } else {
            ok = AdminDAO.updateAdmin(editing.getId(), username, fullName, email,
                    phone.isBlank() ? null : phone, level,
                    position.isBlank() ? null : position, active);
            if (ok && !code.equals(editing.getAdminCode())) {
                try (var conn = com.shiro.ordermanagementsystem.Databaseconnection.getConnection();
                     var stmt = conn.prepareStatement("UPDATE admin SET admin_code = ? WHERE id = ?")) {
                    stmt.setString(1, code);
                    stmt.setInt(2, editing.getId());
                    stmt.executeUpdate();
                } catch (java.sql.SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        if (!ok) { showError("Could not save admin. Try again."); return; }

        saved = true;
        if (stage != null) stage.close();
    }

    private void showError(String msg) {
        messageLabel.setText(msg);
        messageLabel.getStyleClass().removeAll("form-message-error", "form-message-success");
        messageLabel.getStyleClass().add("form-message-error");
        messageLabel.setManaged(true);
        messageLabel.setVisible(true);
    }
}
