package com.shiro.ordermanagementsystem.Controller.admin;

import com.shiro.ordermanagementsystem.Admin;
import com.shiro.ordermanagementsystem.AdminDAO;
import com.shiro.ordermanagementsystem.Customer;
import com.shiro.ordermanagementsystem.CustomerDAO;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

public class ResetPasswordDialogController {

    public enum Kind { ADMIN, CUSTOMER }

    @FXML private Label         titleLabel;
    @FXML private Label         subtitleLabel;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmField;
    @FXML private Label         messageLabel;

    private Kind    kind = Kind.ADMIN;
    private int     targetId;
    private Stage   stage;
    private boolean saved = false;

    public void setStage(Stage stage) { this.stage = stage; }
    public boolean wasSaved()         { return saved; }

    public void setTarget(Admin admin) {
        this.kind     = Kind.ADMIN;
        this.targetId = admin.getId();
        subtitleLabel.setText("Set a new password for " + admin.getFullName()
                + " (" + admin.getUsername() + ")");
    }

    public void setCustomerTarget(Customer customer) {
        this.kind     = Kind.CUSTOMER;
        this.targetId = customer.getId();
        subtitleLabel.setText("Set a new password for " + customer.getFullName()
                + " (" + customer.getUsername() + ")");
    }

    @FXML
    private void handleCancel() {
        if (stage != null) stage.close();
    }

    @FXML
    private void handleReset() {
        String pwd     = newPasswordField.getText();
        String confirm = confirmField.getText();

        if (pwd == null || pwd.length() < 8) {
            showError("New password must be at least 8 characters.");
            return;
        }
        if (!pwd.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }

        boolean ok = (kind == Kind.ADMIN)
                ? AdminDAO.updatePasswordById(targetId, pwd)
                : CustomerDAO.updatePasswordById(targetId, pwd);

        if (!ok) {
            showError("Could not update password.");
            return;
        }
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
