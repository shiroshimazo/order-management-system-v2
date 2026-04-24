package com.shiro.ordermanagementsystem.Controller.customer;

import com.shiro.ordermanagementsystem.AdminDAO;
import com.shiro.ordermanagementsystem.Customer;
import com.shiro.ordermanagementsystem.CustomerDAO;
import com.shiro.ordermanagementsystem.session.Session;
import com.shiro.ordermanagementsystem.ui.Toast;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.kordamp.ikonli.javafx.FontIcon;
import org.mindrot.jbcrypt.BCrypt;

import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public class CustomerSettingsController {

    // ─── Profile tab ──────────────────────────────────────────────────────────
    @FXML private Label     profileNameLabel;
    @FXML private Label     profileEmailLabel;
    @FXML private Label     profileMemberLabel;

    @FXML private TextField usernameField;
    @FXML private TextField memberSinceField;
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;

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
    private static final DateTimeFormatter MEMBER_FMT = DateTimeFormatter.ofPattern("MMMM yyyy");

    @FXML
    public void initialize() {
        Customer c = Session.getCurrentCustomer();
        if (c != null) loadCustomer(c);

        bindPasswordSync(currentPasswordHidden, currentPasswordShown);
        bindPasswordSync(newPasswordHidden,     newPasswordShown);
        bindPasswordSync(confirmPasswordHidden, confirmPasswordShown);
    }

    private void loadCustomer(Customer c) {
        profileNameLabel.setText(c.getFullName() == null ? "" : c.getFullName());
        profileEmailLabel.setText(c.getEmail() == null ? "" : c.getEmail());
        profileMemberLabel.setText(c.getCreatedAt() == null
                ? "Member"
                : "Member since " + c.getCreatedAt().format(MEMBER_FMT));

        usernameField.setText(c.getUsername());
        memberSinceField.setText(c.getCreatedAt() == null ? "—" : c.getCreatedAt().format(MEMBER_FMT));
        fullNameField.setText(c.getFullName());
        emailField.setText(c.getEmail());
        phoneField.setText(c.getPhone() == null ? "" : c.getPhone());
    }

    private void bindPasswordSync(PasswordField hidden, TextField shown) {
        hidden.textProperty().addListener((o, a, b) -> { if (!shown.getText().equals(b)) shown.setText(b); });
        shown.textProperty().addListener((o, a, b)  -> { if (!hidden.getText().equals(b)) hidden.setText(b); });
    }

    // ─── Profile actions ──────────────────────────────────────────────────────
    @FXML
    private void handleProfileSave() {
        Customer c = Session.getCurrentCustomer();
        if (c == null) return;

        String fullName = fullNameField.getText() == null ? "" : fullNameField.getText().trim();
        String email    = emailField.getText()    == null ? "" : emailField.getText().trim();
        String phone    = phoneField.getText()    == null ? "" : phoneField.getText().trim();

        if (fullName.isEmpty()) { showProfileError("Full name is required."); return; }
        if (email.isEmpty())    { showProfileError("Email is required."); return; }
        if (!EMAIL_RE.matcher(email).matches()) { showProfileError("Enter a valid email address."); return; }

        boolean sameEmail = email.equalsIgnoreCase(c.getEmail());
        if (!sameEmail && (CustomerDAO.emailExistsExcept(email, c.getId()) || AdminDAO.emailExists(email))) {
            showProfileError("That email is already in use.");
            return;
        }

        boolean ok = CustomerDAO.updateCustomer(c.getId(), c.getUsername(), fullName, email,
                phone.isBlank() ? null : phone, c.isActive());
        if (!ok) { showProfileError("Could not save profile. Try again."); return; }

        c.setFullName(fullName);
        c.setEmail(email);
        c.setPhone(phone.isBlank() ? null : phone);

        profileNameLabel.setText(fullName);
        profileEmailLabel.setText(email);
        CustomerHomeController.refreshCustomerInfo();
        clearProfileMessage();
        Toast.success(profileNameLabel, "Profile updated successfully.");
    }

    @FXML
    private void handleProfileCancel() {
        Customer c = Session.getCurrentCustomer();
        if (c != null) loadCustomer(c);
        clearProfileMessage();
    }

    // ─── Password actions ─────────────────────────────────────────────────────
    @FXML
    private void handlePasswordChange() {
        Customer c = Session.getCurrentCustomer();
        if (c == null) return;

        String current = currentPasswordHidden.getText();
        String next    = newPasswordHidden.getText();
        String confirm = confirmPasswordHidden.getText();

        if (current.isEmpty() || next.isEmpty() || confirm.isEmpty()) {
            showPasswordError("Please fill in all password fields."); return;
        }
        if (!BCrypt.checkpw(current, c.getPassword())) {
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

        boolean ok = CustomerDAO.updatePasswordById(c.getId(), next);
        if (!ok) { showPasswordError("Could not update password. Try again."); return; }

        c.setPassword(BCrypt.hashpw(next, BCrypt.gensalt(12)));
        handlePasswordClear();
        Toast.success(passwordMessageLabel, "Password changed successfully.");
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
    private void clearProfileMessage()          { hideMessage(profileMessageLabel); }

    private void showPasswordError(String msg)   { setMessage(passwordMessageLabel, msg, true); }
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
