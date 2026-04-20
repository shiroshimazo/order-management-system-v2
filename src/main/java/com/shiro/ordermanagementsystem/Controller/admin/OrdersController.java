package com.shiro.ordermanagementsystem.Controller.admin;

import com.shiro.ordermanagementsystem.Controller.shared.OrderDetailsDialogController;
import com.shiro.ordermanagementsystem.Order;
import com.shiro.ordermanagementsystem.OrderDAO;
import com.shiro.ordermanagementsystem.OrderStatus;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

public class OrdersController {

    @FXML private TextField             searchField;
    @FXML private ComboBox<OrderStatus> statusFilter;

    @FXML private TableView<Order>          ordersTable;
    @FXML private TableColumn<Order, String> colCode;
    @FXML private TableColumn<Order, String> colCustomer;
    @FXML private TableColumn<Order, String> colDate;
    @FXML private TableColumn<Order, String> colTotal;
    @FXML private TableColumn<Order, String> colStatus;
    @FXML private TableColumn<Order, Void>   colActions;

    @FXML private Label countLabel;

    private static final DecimalFormat     MONEY = new DecimalFormat("₱#,##0.00");
    private static final DateTimeFormatter DT    = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    @FXML
    public void initialize() {
        colCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOrderCode()));
        colCustomer.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCustomerName() == null ? "—" : c.getValue().getCustomerName()));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCreatedAt() == null ? "" : c.getValue().getCreatedAt().format(DT)));
        colTotal.setCellValueFactory(c -> new SimpleStringProperty(MONEY.format(c.getValue().getTotal())));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().label()));

        colStatus.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("status-pending", "status-processing",
                        "status-shipped", "status-delivered", "status-cancelled");
                if (empty || item == null) { setText(null); return; }
                setText(item);
                getStyleClass().add("status-" + item.toLowerCase());
            }
        });

        colActions.setCellFactory(tc -> new TableCell<>() {
            private final Button btn = new Button("View");
            {
                btn.getStyleClass().add("button-secondary");
                btn.setOnAction(e -> openDetails(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
            }
        });

        ObservableList<OrderStatus> statuses = FXCollections.observableArrayList();
        statuses.add(null);
        statuses.addAll(OrderStatus.values());
        statusFilter.setItems(statuses);
        statusFilter.setValue(null);
        statusFilter.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(OrderStatus s) { return s == null ? "All Statuses" : s.label(); }
            @Override public OrderStatus fromString(String s) { return null; }
        });

        searchField.textProperty().addListener((o, a, b) -> reload());
        statusFilter.valueProperty().addListener((o, a, b) -> reload());

        reload();
    }

    private void reload() {
        String q = searchField.getText();
        OrderStatus status = statusFilter.getValue();
        ObservableList<Order> items = FXCollections.observableArrayList(OrderDAO.searchAdmin(status, q));
        ordersTable.setItems(items);
        countLabel.setText(items.size() + (items.size() == 1 ? " order" : " orders"));
    }

    @FXML private void handleRefresh() { reload(); }

    private void openDetails(Order order) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ordermanagementsystem/fxml/shared/OrderDetailsDialog.fxml"));
            Parent root = loader.load();
            OrderDetailsDialogController ctrl = loader.getController();
            ctrl.setOrder(order, true);

            Stage stage = new Stage();
            stage.setTitle("Order " + order.getOrderCode());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(ordersTable.getScene().getWindow());
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/ordermanagementsystem/css/admin.css").toExternalForm());
            stage.setScene(scene);
            ctrl.setStage(stage);
            stage.showAndWait();
            if (ctrl.hasChanged()) reload();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
