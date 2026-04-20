package com.shiro.ordermanagementsystem.Controller.customer;

import com.shiro.ordermanagementsystem.Customer;
import com.shiro.ordermanagementsystem.Order;
import com.shiro.ordermanagementsystem.OrderDAO;
import com.shiro.ordermanagementsystem.OrderStatus;
import com.shiro.ordermanagementsystem.Controller.shared.OrderDetailsDialogController;
import com.shiro.ordermanagementsystem.session.Session;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

public class MyOrdersController {

    @FXML private TableView<Order>          ordersTable;
    @FXML private TableColumn<Order, String> colCode;
    @FXML private TableColumn<Order, String> colDate;
    @FXML private TableColumn<Order, String> colItems;
    @FXML private TableColumn<Order, String> colTotal;
    @FXML private TableColumn<Order, String> colStatus;
    @FXML private TableColumn<Order, Void>   colActions;

    private static final DecimalFormat MONEY   = new DecimalFormat("₱#,##0.00");
    private static final DateTimeFormatter DT  = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");
    private static MyOrdersController instance;

    @FXML
    public void initialize() {
        instance = this;

        colCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOrderCode()));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCreatedAt() == null ? "" : c.getValue().getCreatedAt().format(DT)));
        colItems.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getItems().size())));
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

        reload();
    }

    private void reload() {
        Customer customer = Session.getCurrentCustomer();
        if (customer == null) return;
        var orders = OrderDAO.findByCustomer(customer.getId());
        // load items for item counts
        for (Order o : orders) {
            Order full = OrderDAO.findById(o.getId());
            if (full != null) o.setItems(full.getItems());
        }
        ordersTable.setItems(FXCollections.observableArrayList(orders));
    }

    public static void refreshIfLoaded() {
        if (instance != null) instance.reload();
    }

    @FXML private void handleRefresh() { reload(); }

    private void openDetails(Order order) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ordermanagementsystem/fxml/shared/OrderDetailsDialog.fxml"));
            Parent root = loader.load();
            OrderDetailsDialogController ctrl = loader.getController();
            ctrl.setOrder(order, false);

            Stage stage = new Stage();
            stage.setTitle("Order " + order.getOrderCode());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(ordersTable.getScene().getWindow());
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/ordermanagementsystem/css/admin.css").toExternalForm());
            stage.setScene(scene);
            ctrl.setStage(stage);
            stage.showAndWait();
            reload();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
