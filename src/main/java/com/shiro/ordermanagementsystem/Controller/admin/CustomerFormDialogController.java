package com.shiro.ordermanagementsystem.Controller.admin;

import com.shiro.ordermanagementsystem.AdminDAO;
import com.shiro.ordermanagementsystem.Customer;
import com.shiro.ordermanagementsystem.CustomerDAO;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class CustomerFormDialogController {

    @FXML private Label         titleLabel;
    @FXML private TextField     usernameField;
    @FXML private TextField     fullNameField;
    @FXML private TextField     emailField;
    @FXML private TextField     phoneField;
    @FXML private VBox          passwordRow;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox      activeCheck;
    @FXML private Button        saveButton;
    @FXML private Label         messageLabel;

    private Customer editing;
    private Stage    stage;
    private boolean  saved = false;

    public void setStage(Stage stage) { this.stage = stage; }
    public boolean wasSaved()         { return saved; }

    public void setCustomer(Customer customer) {
        this.editing = customer;
        if (customer == null) {
            titleLabel.setText("Add Customer");
            saveButton.setText("Create");
            return;
        }
        titleLabel.setText("Edit Customer");
        saveButton.setText("Save Changes");

        usernameField.setText(customer.getUsername());
        fullNameField.setText(customer.getFullName());
        emailField.setText(customer.getEmail());
        phoneField.setText(customer.getPhone() == null ? "" : customer.getPhone());
        activeCheck.setSelected(customer.isActive());

        // No password field when editing — use Reset Password action instead
        passwordRow.setVisible(false);
        passwordRow.setManaged(false);
    }

    @FXML
    private void handleCancel() {
        if (stage != null) stage.close();
    }

    @FXML
    private void handleSave() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String fullName = fullNameField.getText() == null ? "" : fullNameField.getText().trim();
        String email    = emailField.getText()    == null ? "" : emailField.getText().trim();
        String phone    = phoneField.getText()    == null ? "" : phoneField.getText().trim();
        boolean active  = activeCheck.isSelected();

        if (username.isEmpty()) { showError("Username is required.");   return; }
        if (fullName.isEmpty()) { showError("Full name is required."); return; }
        if (email.isEmpty())    { showError("Email is required.");     return; }
        if (!email.contains("@") || !email.contains(".")) {
            showError("Enter a valid email address."); return;
        }

        int excludeId = editing == null ? -1 : editing.getId();

        if (CustomerDAO.usernameExistsExcept(username, excludeId)) {
            showError("That username is already taken."); return;
        }
        if (CustomerDAO.emailExistsExcept(email, excludeId)) {
            showError("That email is already in use."); return;
        }
        // email must not collide with any admin either
        if (AdminDAO.emailExists(email)) {
            showError("That email is already used by an admin account."); return;
        }

        boolean ok;
        if (editing == null) {
            String password = passwordField.getText();
            if (password == null || password.length() < 8) {
                showError("Initial password must be at least 8 characters.");
                return;
            }
            ok = CustomerDAO.createCustomer(username, password, fullName, email,
                    phone.isBlank() ? null : phone);
            if (ok && !active) {
                Customer created = CustomerDAO.findByUsername(username);
                if (created != null) CustomerDAO.setActive(created.getId(), false);
            }
        } else {
            ok = CustomerDAO.updateCustomer(editing.getId(), username, fullName, email,
                    phone.isBlank() ? null : phone, active);
        }

        if (!ok) { showError("Could not save customer. Try again."); return; }

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
