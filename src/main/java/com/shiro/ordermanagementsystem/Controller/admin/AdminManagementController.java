package com.shiro.ordermanagementsystem.Controller.admin;

import com.shiro.ordermanagementsystem.Admin;
import com.shiro.ordermanagementsystem.AdminDAO;
import com.shiro.ordermanagementsystem.session.Session;
import com.shiro.ordermanagementsystem.ui.Toast;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class AdminManagementController {

    @FXML private TextField              searchField;
    @FXML private ComboBox<Admin.Level>  levelFilter;

    @FXML private TableView<Admin>           adminsTable;
    @FXML private TableColumn<Admin, String> colCode;
    @FXML private TableColumn<Admin, String> colUsername;
    @FXML private TableColumn<Admin, String> colName;
    @FXML private TableColumn<Admin, String> colEmail;
    @FXML private TableColumn<Admin, String> colLevel;
    @FXML private TableColumn<Admin, String> colActive;
    @FXML private TableColumn<Admin, String> colLastLogin;
    @FXML private TableColumn<Admin, Void>   colActions;

    @FXML private Label countLabel;

    private static final Admin.Level ALL_LEVELS = null;
    private static final DateTimeFormatter LAST_LOGIN_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    @FXML
    public void initialize() {
        setupLevelFilter();
        setupTable();
        reload();

        searchField.textProperty().addListener((o, a, b) -> reload());
        levelFilter.valueProperty().addListener((o, a, b) -> reload());
    }

    private void setupLevelFilter() {
        ObservableList<Admin.Level> items = FXCollections.observableArrayList();
        items.add(ALL_LEVELS);
        items.addAll(Admin.Level.values());
        levelFilter.setItems(items);
        levelFilter.setValue(ALL_LEVELS);
        levelFilter.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Admin.Level lvl) { return lvl == null ? "All Roles" : lvl.name(); }
            @Override public Admin.Level fromString(String s) { return null; }
        });
    }

    private void setupTable() {
        colCode.setCellValueFactory(     c -> new SimpleStringProperty(c.getValue().getAdminCode()));
        colUsername.setCellValueFactory( c -> new SimpleStringProperty(c.getValue().getUsername()));
        colName.setCellValueFactory(     c -> new SimpleStringProperty(c.getValue().getFullName()));
        colEmail.setCellValueFactory(    c -> new SimpleStringProperty(c.getValue().getEmail()));
        colLevel.setCellValueFactory(    c -> new SimpleStringProperty(c.getValue().getAdminLevel().name()));
        colActive.setCellValueFactory(   c -> new SimpleStringProperty(c.getValue().isActive() ? "Active" : "Inactive"));
        colLastLogin.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getLastLoginAt() == null ? "—" : c.getValue().getLastLoginAt().format(LAST_LOGIN_FMT)));

        colActive.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("status-delivered", "status-cancelled");
                if (empty || item == null) { setText(null); return; }
                setText(item);
                getStyleClass().add("Active".equals(item) ? "status-delivered" : "status-cancelled");
            }
        });

        colActions.setCellFactory(tc -> new TableCell<>() {
            private final Button editBtn   = makeIconButton("fas-pen",    "#3a72e8");
            private final Button keyBtn    = makeIconButton("fas-key",    "#b58005");
            private final Button deleteBtn = makeIconButton("fas-trash",  "#D92A1C");
            private final HBox   box       = new HBox(6, editBtn, keyBtn, deleteBtn);

            {
                editBtn.setOnAction(e -> {
                    Admin a = getTableView().getItems().get(getIndex());
                    openAdminDialog(a);
                });
                keyBtn.setOnAction(e -> {
                    Admin a = getTableView().getItems().get(getIndex());
                    openResetPasswordDialog(a);
                });
                deleteBtn.setOnAction(e -> {
                    Admin a = getTableView().getItems().get(getIndex());
                    confirmAndDelete(a);
                });
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private static Button makeIconButton(String icon, String color) {
        FontIcon fi = new FontIcon(icon);
        fi.setIconSize(12);
        fi.setIconColor(javafx.scene.paint.Color.web(color));
        Button b = new Button();
        b.setGraphic(fi);
        b.getStyleClass().add("table-action-button");
        return b;
    }

    // ─── Data ─────────────────────────────────────────────────────────────────
    private void reload() {
        String q = searchField.getText();
        Admin.Level lvl = levelFilter.getValue();
        ObservableList<Admin> items = FXCollections.observableArrayList(AdminDAO.search(q, lvl));
        adminsTable.setItems(items);
        countLabel.setText(items.size() + (items.size() == 1 ? " admin" : " admins"));
    }

    @FXML private void handleRefresh()  { reload(); }
    @FXML private void handleAddAdmin() { openAdminDialog(null); }

    // ─── Dialogs ──────────────────────────────────────────────────────────────
    private void openAdminDialog(Admin admin) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ordermanagementsystem/fxml/admin/AdminFormDialog.fxml"));
            Parent root = loader.load();
            AdminFormDialogController ctrl = loader.getController();
            ctrl.setAdmin(admin);

            Admin current = Session.getCurrentAdmin();
            ctrl.setCreatedBy(current == null ? null : current.getId());

            Stage stage = new Stage();
            stage.setTitle(admin == null ? "Add Admin" : "Edit Admin");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(adminsTable.getScene().getWindow());
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource(
                    "/ordermanagementsystem/css/admin.css").toExternalForm());
            stage.setScene(scene);
            ctrl.setStage(stage);
            stage.showAndWait();

            if (ctrl.wasSaved()) reload();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openResetPasswordDialog(Admin admin) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ordermanagementsystem/fxml/admin/ResetPasswordDialog.fxml"));
            Parent root = loader.load();
            ResetPasswordDialogController ctrl = loader.getController();
            ctrl.setTarget(admin);

            Stage stage = new Stage();
            stage.setTitle("Reset Password");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(adminsTable.getScene().getWindow());
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource(
                    "/ordermanagementsystem/css/admin.css").toExternalForm());
            stage.setScene(scene);
            ctrl.setStage(stage);
            stage.showAndWait();

            if (ctrl.wasSaved()) {
                Toast.success(adminsTable, "Password updated for " + admin.getUsername() + ".");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ─── Delete with safeguards ───────────────────────────────────────────────
    private void confirmAndDelete(Admin a) {
        Admin current = Session.getCurrentAdmin();
        if (current != null && current.getId() == a.getId()) {
            new Alert(Alert.AlertType.WARNING, "You cannot delete your own account.").showAndWait();
            return;
        }
        if (a.getAdminLevel() == Admin.Level.SUPER_ADMIN
                && a.isActive()
                && AdminDAO.countActiveSuperAdmins() <= 1) {
            new Alert(Alert.AlertType.WARNING,
                    "Cannot delete the last active SUPER_ADMIN.").showAndWait();
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Admin");
        alert.setHeaderText("Delete \"" + a.getFullName() + "\"?");
        alert.setContentText("This permanently removes the account. This cannot be undone.");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (AdminDAO.delete(a.getId())) {
                Platform.runLater(this::reload);
            } else {
                new Alert(Alert.AlertType.ERROR,
                        "Could not delete admin. They may have related records.").showAndWait();
            }
        }
    }
}
