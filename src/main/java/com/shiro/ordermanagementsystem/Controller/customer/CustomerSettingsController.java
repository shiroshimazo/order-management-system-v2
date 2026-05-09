package com.shiro.ordermanagementsystem.Controller.customer;

import com.shiro.ordermanagementsystem.AdminDAO;
import com.shiro.ordermanagementsystem.Customer;
import com.shiro.ordermanagementsystem.CustomerDAO;
import com.shiro.ordermanagementsystem.session.Session;
import com.shiro.ordermanagementsystem.ui.CustomerImages;
import com.shiro.ordermanagementsystem.ui.Toast;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;
import org.mindrot.jbcrypt.BCrypt;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public class CustomerSettingsController {

    // ─── Profile tab ──────────────────────────────────────────────────────────
    @FXML private Label     profileNameLabel;
    @FXML private Label     profileEmailLabel;
    @FXML private Label     profileMemberLabel;

    @FXML private StackPane avatarFrame;
    @FXML private ImageView avatarImage;
    @FXML private FontIcon  avatarPlaceholder;
    @FXML private Button    changeAvatarButton;
    @FXML private Button    removeAvatarButton;
    @FXML private Label     avatarStatusLabel;

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
    private static final long MAX_AVATAR_BYTES = 2L * 1024 * 1024;   // 2 MB

    /** New bytes the user picked this session. Null means "no change". */
    private byte[]  pendingAvatarBytes;
    private String  pendingAvatarMime;
    /** Set when the user clicks Remove — wipes the existing BLOB on save. */
    private boolean removeExistingAvatar;

    @FXML
    public void initialize() {
        // Crop the avatar image to a circle (matches the StackPane frame).
        Circle clip = new Circle(36, 36, 36);
        avatarImage.setClip(clip);

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

        // Reset any pending edit, then paint whatever is on the row.
        pendingAvatarBytes   = null;
        pendingAvatarMime    = null;
        removeExistingAvatar = false;

        Image existing = CustomerImages.loadForCustomer(c.getId(), 144, 144);
        if (existing != null) {
            showAvatarImage(existing, "PNG / JPG, up to 2 MB");
        } else {
            showAvatarPlaceholder("PNG / JPG, up to 2 MB");
        }
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

        // Apply any avatar change in the same save.
        if (removeExistingAvatar) {
            CustomerDAO.clearAvatar(c.getId());
            CustomerImages.invalidate(c.getId());
        } else if (pendingAvatarBytes != null) {
            CustomerDAO.setAvatar(c.getId(), pendingAvatarBytes, pendingAvatarMime);
            CustomerImages.invalidate(c.getId());
        }
        pendingAvatarBytes   = null;
        pendingAvatarMime    = null;
        removeExistingAvatar = false;

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

    // ─── Avatar picker ────────────────────────────────────────────────────────
    @FXML
    private void handleChooseAvatar() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose profile photo");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"),
                new FileChooser.ExtensionFilter("All files", "*.*"));

        Node owner = avatarFrame == null ? profileNameLabel : avatarFrame;
        File file = chooser.showOpenDialog(owner.getScene() == null ? null : owner.getScene().getWindow());
        if (file == null) return;

        if (file.length() > MAX_AVATAR_BYTES) {
            showAvatarError("File is too large (" + formatBytes(file.length())
                    + "). Max " + formatBytes(MAX_AVATAR_BYTES) + ".");
            return;
        }

        byte[] bytes;
        try {
            bytes = Files.readAllBytes(Path.of(file.toURI()));
        } catch (IOException ex) {
            showAvatarError("Couldn't read file: " + ex.getMessage());
            return;
        }

        Image preview = CustomerImages.fromBytes(bytes, 144, 144);
        if (preview == null) {
            showAvatarError("Not a valid image file.");
            return;
        }

        this.pendingAvatarBytes   = bytes;
        this.pendingAvatarMime    = guessMime(file.getName());
        this.removeExistingAvatar = false;

        showAvatarImage(preview, file.getName() + " (" + formatBytes(bytes.length) + ") — save to apply");
    }

    @FXML
    private void handleRemoveAvatar() {
        this.pendingAvatarBytes   = null;
        this.pendingAvatarMime    = null;
        this.removeExistingAvatar = true;
        showAvatarPlaceholder("Photo will be removed when you save.");
    }

    private void showAvatarImage(Image img, String status) {
        avatarImage.setImage(img);
        avatarImage.setVisible(true);
        avatarImage.setManaged(true);
        avatarPlaceholder.setVisible(false);
        avatarPlaceholder.setManaged(false);
        setAvatarStatus(status, false);
    }

    private void showAvatarPlaceholder(String status) {
        avatarImage.setImage(null);
        avatarImage.setVisible(false);
        avatarImage.setManaged(false);
        avatarPlaceholder.setVisible(true);
        avatarPlaceholder.setManaged(true);
        setAvatarStatus(status, false);
    }

    private void showAvatarError(String msg) { setAvatarStatus(msg, true); }

    private void setAvatarStatus(String msg, boolean isError) {
        avatarStatusLabel.setText(msg);
        avatarStatusLabel.getStyleClass().removeAll("form-help", "form-message-error");
        avatarStatusLabel.getStyleClass().add(isError ? "form-message-error" : "form-help");
    }

    private static String guessMime(String fileName) {
        String n = fileName.toLowerCase();
        if (n.endsWith(".png"))  return "image/png";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".gif"))  return "image/gif";
        if (n.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }

    private static String formatBytes(long n) {
        if (n < 1024) return n + " B";
        if (n < 1024 * 1024) return String.format("%.1f KB", n / 1024.0);
        return String.format("%.2f MB", n / (1024.0 * 1024.0));
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
