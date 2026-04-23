package com.shiro.ordermanagementsystem.Controller.admin;

import com.shiro.ordermanagementsystem.Customer;
import com.shiro.ordermanagementsystem.CustomerDAO;
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

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

public class CustomerManagementController {

    @FXML private TextField                searchField;
    @FXML private ComboBox<Boolean>        statusFilter;

    @FXML private TableView<CustomerDAO.Row>            customersTable;
    @FXML private TableColumn<CustomerDAO.Row, String>  colUsername;
    @FXML private TableColumn<CustomerDAO.Row, String>  colName;
    @FXML private TableColumn<CustomerDAO.Row, String>  colEmail;
    @FXML private TableColumn<CustomerDAO.Row, String>  colPhone;
    @FXML private TableColumn<CustomerDAO.Row, String>  colOrders;
    @FXML private TableColumn<CustomerDAO.Row, String>  colSpent;
    @FXML private TableColumn<CustomerDAO.Row, String>  colStatus;
    @FXML private TableColumn<CustomerDAO.Row, String>  colJoined;
    @FXML private TableColumn<CustomerDAO.Row, Void>    colActions;

    @FXML private Label countLabel;

    private static final Boolean ALL_STATUSES = null;
    private static final DateTimeFormatter JOINED_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final NumberFormat PHP =
            NumberFormat.getCurrencyInstance(new Locale("en", "PH"));

    @FXML
    public void initialize() {
        setupStatusFilter();
        setupTable();
        reload();

        searchField.textProperty().addListener((o, a, b) -> reload());
        statusFilter.valueProperty().addListener((o, a, b) -> reload());
    }

    private void setupStatusFilter() {
        ObservableList<Boolean> items = FXCollections.observableArrayList();
        items.add(ALL_STATUSES);
        items.add(Boolean.TRUE);
        items.add(Boolean.FALSE);
        statusFilter.setItems(items);
        statusFilter.setValue(ALL_STATUSES);
        statusFilter.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Boolean b) {
                if (b == null) return "All";
                return b ? "Active" : "Archived";
            }
            @Override public Boolean fromString(String s) { return null; }
        });
    }

    private void setupTable() {
        colUsername.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().customer.getUsername()));
        colName.setCellValueFactory(    c -> new SimpleStringProperty(c.getValue().customer.getFullName()));
        colEmail.setCellValueFactory(   c -> new SimpleStringProperty(c.getValue().customer.getEmail()));
        colPhone.setCellValueFactory(   c -> new SimpleStringProperty(
                c.getValue().customer.getPhone() == null ? "—" : c.getValue().customer.getPhone()));
        colOrders.setCellValueFactory(  c -> new SimpleStringProperty(String.valueOf(c.getValue().ordersCount)));
        colSpent.setCellValueFactory(   c -> new SimpleStringProperty(formatMoney(c.getValue().totalSpent)));
        colStatus.setCellValueFactory(  c -> new SimpleStringProperty(
                c.getValue().customer.isActive() ? "Active" : "Archived"));
        colJoined.setCellValueFactory(  c -> new SimpleStringProperty(
                c.getValue().customer.getCreatedAt() == null
                        ? "—"
                        : c.getValue().customer.getCreatedAt().format(JOINED_FMT)));

        colStatus.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("status-delivered", "status-cancelled");
                if (empty || item == null) { setText(null); return; }
                setText(item);
                getStyleClass().add("Active".equals(item) ? "status-delivered" : "status-cancelled");
            }
        });

        colActions.setCellFactory(tc -> new TableCell<>() {
            private final Button editBtn    = makeIconButton("fas-pen",     "#3a72e8");
            private final Button keyBtn     = makeIconButton("fas-key",     "#b58005");
            private final Button archiveBtn = makeIconButton("fas-archive", "#D92A1C");
            private final HBox   box        = new HBox(6, editBtn, keyBtn, archiveBtn);

            {
                editBtn.setOnAction(e -> {
                    CustomerDAO.Row row = getTableView().getItems().get(getIndex());
                    openCustomerDialog(row.customer);
                });
                keyBtn.setOnAction(e -> {
                    CustomerDAO.Row row = getTableView().getItems().get(getIndex());
                    openResetPasswordDialog(row.customer);
                });
                archiveBtn.setOnAction(e -> {
                    CustomerDAO.Row row = getTableView().getItems().get(getIndex());
                    confirmAndToggleArchive(row.customer);
                });
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                CustomerDAO.Row row = (CustomerDAO.Row) getTableRow().getItem();
                FontIcon fi = (FontIcon) archiveBtn.getGraphic();
                if (row.customer.isActive()) {
                    fi.setIconLiteral("fas-archive");
                    fi.setIconColor(javafx.scene.paint.Color.web("#D92A1C"));
                    archiveBtn.setTooltip(new Tooltip("Archive customer"));
                } else {
                    fi.setIconLiteral("fas-undo");
                    fi.setIconColor(javafx.scene.paint.Color.web("#1f8a3b"));
                    archiveBtn.setTooltip(new Tooltip("Restore customer"));
                }
                setGraphic(box);
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

    private static String formatMoney(BigDecimal v) {
        if (v == null) v = BigDecimal.ZERO;
        return PHP.format(v);
    }

    // ─── Data ─────────────────────────────────────────────────────────────────
    private void reload() {
        String q = searchField.getText();
        Boolean status = statusFilter.getValue();
        ObservableList<CustomerDAO.Row> items =
                FXCollections.observableArrayList(CustomerDAO.search(q, status));
        customersTable.setItems(items);
        countLabel.setText(items.size() + (items.size() == 1 ? " customer" : " customers"));
    }

    @FXML private void handleRefresh()     { reload(); }
    @FXML private void handleAddCustomer() { openCustomerDialog(null); }

    // ─── Dialogs ──────────────────────────────────────────────────────────────
    private void openCustomerDialog(Customer customer) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ordermanagementsystem/fxml/admin/CustomerFormDialog.fxml"));
            Parent root = loader.load();
            CustomerFormDialogController ctrl = loader.getController();
            ctrl.setCustomer(customer);

            Stage stage = new Stage();
            stage.setTitle(customer == null ? "Add Customer" : "Edit Customer");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(customersTable.getScene().getWindow());
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

    private void openResetPasswordDialog(Customer customer) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ordermanagementsystem/fxml/admin/ResetPasswordDialog.fxml"));
            Parent root = loader.load();
            ResetPasswordDialogController ctrl = loader.getController();
            ctrl.setCustomerTarget(customer);

            Stage stage = new Stage();
            stage.setTitle("Reset Password");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(customersTable.getScene().getWindow());
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource(
                    "/ordermanagementsystem/css/admin.css").toExternalForm());
            stage.setScene(scene);
            ctrl.setStage(stage);
            stage.showAndWait();

            if (ctrl.wasSaved()) {
                Toast.success(customersTable, "Password updated for " + customer.getUsername() + ".");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ─── Archive / restore ────────────────────────────────────────────────────
    private void confirmAndToggleArchive(Customer c) {
        boolean archiving = c.isActive();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(archiving ? "Archive Customer" : "Restore Customer");
        alert.setHeaderText((archiving ? "Archive \"" : "Restore \"") + c.getFullName() + "\"?");
        alert.setContentText(archiving
                ? "They will not be able to log in. Their order history is preserved. You can restore this account later."
                : "They will be able to log in again.");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        if (!CustomerDAO.setActive(c.getId(), !archiving)) {
            new Alert(Alert.AlertType.ERROR, "Could not update customer status.").showAndWait();
            return;
        }
        Toast.success(customersTable, archiving
                ? c.getUsername() + " archived."
                : c.getUsername() + " restored.");
        Platform.runLater(this::reload);
    }
}
