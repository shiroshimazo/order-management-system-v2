package com.shiro.ordermanagementsystem.Controller.admin;

import com.shiro.ordermanagementsystem.Admin;
import com.shiro.ordermanagementsystem.AdminDAO;
import com.shiro.ordermanagementsystem.Controller.AdminShellController;
import com.shiro.ordermanagementsystem.CustomerDAO;
import com.shiro.ordermanagementsystem.session.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.kordamp.ikonli.javafx.FontIcon;
import org.mindrot.jbcrypt.BCrypt;

import java.util.regex.Pattern;

public class SettingsController {

    // ─── Profile tab ──────────────────────────────────────────────────────────
    @FXML private Label     profileNameLabel;
    @FXML private Label     profileLevelLabel;
    @FXML private Label     profileCodeLabel;

    @FXML private TextField usernameField;
    @FXML private TextField adminLevelField;
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField positionField;

    @FXML private Label     profileMessageLabel;

    // ─── Password tab ─────────────────────────────────────────────────────────
    @FXML private PasswordField currentPasswordHidden;
    @FXML private TextField     currentPasswordShown;
    @FXML private FontIcon      currentEyeIcon;

    @FXML private PasswordField newPasswordHidden;
    @FXML private TextField     newPasswordShown;
    @FXML private FontIcon      newEyeIcon;

    @FXML private PasswordField confirmPasswordHidden;
    @FXML private TextField     confirmPasswordShown;
    @FXML private FontIcon      confirmEyeIcon;

    @FXML private Label passwordMessageLabel;

    private static final Pattern EMAIL_RE = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @FXML
    public void initialize() {
        Admin admin = Session.getCurrentAdmin();
        if (admin != null) loadAdmin(admin);

        bindPasswordSync(currentPasswordHidden, currentPasswordShown);
        bindPasswordSync(newPasswordHidden,     newPasswordShown);
        bindPasswordSync(confirmPasswordHidden, confirmPasswordShown);
    }

    private void loadAdmin(Admin admin) {
        profileNameLabel.setText(admin.getFullName() == null ? "" : admin.getFullName());
        profileLevelLabel.setText(admin.getAdminLevel().name());
        profileCodeLabel.setText(admin.getAdminCode());

        usernameField.setText(admin.getUsername());
        adminLevelField.setText(admin.getAdminLevel().name());
        fullNameField.setText(admin.getFullName());
        emailField.setText(admin.getEmail());
        phoneField.setText(admin.getPhone() == null ? "" : admin.getPhone());
        positionField.setText(admin.getPosition() == null ? "" : admin.getPosition());
    }

    private void bindPasswordSync(PasswordField hidden, TextField shown) {
        hidden.textProperty().addListener((o, a, b) -> { if (!shown.getText().equals(b)) shown.setText(b); });
        shown.textProperty().addListener((o, a, b)  -> { if (!hidden.getText().equals(b)) hidden.setText(b); });
    }

    // ─── Profile actions ──────────────────────────────────────────────────────
    @FXML
    private void handleProfileSave() {
        Admin admin = Session.getCurrentAdmin();
        if (admin == null) return;

        String fullName = fullNameField.getText().trim();
        String email    = emailField.getText().trim();
        String phone    = phoneField.getText().trim();
        String position = positionField.getText().trim();

        if (fullName.isEmpty()) { showProfileError("Full name is required."); return; }
        if (email.isEmpty())    { showProfileError("Email is required."); return; }
        if (!EMAIL_RE.matcher(email).matches()) { showProfileError("Enter a valid email address."); return; }

        boolean sameEmail = email.equalsIgnoreCase(admin.getEmail());
        if (!sameEmail && (AdminDAO.emailExistsExcept(email, admin.getId()) || CustomerDAO.emailExists(email))) {
            showProfileError("That email is already in use.");
            return;
        }

        boolean ok = AdminDAO.updateProfile(admin.getId(), fullName, email, phone, position);
        if (!ok) { showProfileError("Could not save profile. Try again."); return; }

        admin.setFullName(fullName);
        admin.setEmail(email);
        admin.setPhone(phone.isBlank() ? null : phone);
        admin.setPosition(position.isBlank() ? null : position);

        profileNameLabel.setText(fullName);
        AdminShellController.refreshAdminInfo();
        showProfileSuccess("Profile updated successfully.");
    }

    @FXML
    private void handleProfileCancel() {
        Admin admin = Session.getCurrentAdmin();
        if (admin != null) loadAdmin(admin);
        clearProfileMessage();
    }

    // ─── Password actions ─────────────────────────────────────────────────────
    @FXML
    private void handlePasswordChange() {
        Admin admin = Session.getCurrentAdmin();
        if (admin == null) return;

        String current = currentPasswordHidden.getText();
        String next    = newPasswordHidden.getText();
        String confirm = confirmPasswordHidden.getText();

        if (current.isEmpty() || next.isEmpty() || confirm.isEmpty()) {
            showPasswordError("Please fill in all password fields."); return;
        }
        if (!BCrypt.checkpw(current, admin.getPassword())) {
            showPasswordError("Current password is incorrect."); return;
        }
        if (next.length() < 8) {
            showPasswordError("New password must be at least 8 characters."); return;
        }
        if (!next.equals(confirm)) {
            showPasswordError("New passwords do not match."); return;
        }
        if (next.equals(current)) {
            showPasswordError("New password must differ from current password."); return;
        }

        boolean ok = AdminDAO.updatePasswordById(admin.getId(), next);
        if (!ok) { showPasswordError("Could not update password. Try again."); return; }

        admin.setPassword(BCrypt.hashpw(next, BCrypt.gensalt(12)));
        handlePasswordClear();
        showPasswordSuccess("Password changed successfully.");
    }

    @FXML
    private void handlePasswordClear() {
        currentPasswordHidden.clear(); currentPasswordShown.clear();
        newPasswordHidden.clear();     newPasswordShown.clear();
        confirmPasswordHidden.clear(); confirmPasswordShown.clear();
        clearPasswordMessage();
    }

    // ─── Eye toggles ──────────────────────────────────────────────────────────
    @FXML private void toggleCurrentPassword() { toggle(currentPasswordHidden, currentPasswordShown, currentEyeIcon); }
    @FXML private void toggleNewPassword()     { toggle(newPasswordHidden,     newPasswordShown,     newEyeIcon); }
    @FXML private void toggleConfirmPassword() { toggle(confirmPasswordHidden, confirmPasswordShown, confirmEyeIcon); }

    private void toggle(PasswordField hidden, TextField shown, FontIcon eye) {
        boolean showNow = hidden.isVisible();
        hidden.setVisible(!showNow); hidden.setManaged(!showNow);
        shown.setVisible(showNow);   shown.setManaged(showNow);
        eye.setIconLiteral(showNow ? "fas-eye-slash" : "fas-eye");
    }

    // ─── Message helpers ──────────────────────────────────────────────────────
    private void showProfileError(String msg)   { setMessage(profileMessageLabel, msg, true); }
    private void showProfileSuccess(String msg) { setMessage(profileMessageLabel, msg, false); }
    private void clearProfileMessage()          { hideMessage(profileMessageLabel); }

    private void showPasswordError(String msg)   { setMessage(passwordMessageLabel, msg, true); }
    private void showPasswordSuccess(String msg) { setMessage(passwordMessageLabel, msg, false); }
    private void clearPasswordMessage()          { hideMessage(passwordMessageLabel); }

    private void setMessage(Label label, String msg, boolean isError) {
        label.setText(msg);
        label.getStyleClass().removeAll("form-message-error", "form-message-success");
        label.getStyleClass().add(isError ? "form-message-error" : "form-message-success");
        label.setManaged(true);
        label.setVisible(true);
    }

    private void hideMessage(Label label) {
        label.setText("");
        label.setManaged(false);
        label.setVisible(false);
    }
}
