package com.shiro.ordermanagementsystem.Controller.shared;

import com.shiro.ordermanagementsystem.Order;
import com.shiro.ordermanagementsystem.OrderDAO;
import com.shiro.ordermanagementsystem.OrderItem;
import com.shiro.ordermanagementsystem.OrderStatus;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

public class OrderDetailsDialogController {

    @FXML private Label orderCodeLabel;
    @FXML private Label orderDateLabel;
    @FXML private Label orderStatusLabel;
    @FXML private Label customerLabel;
    @FXML private Label contactLabel;
    @FXML private Label addressLabel;
    @FXML private Label notesLabel;
    @FXML private Label totalLabel;

    @FXML private TableView<OrderItem>           itemsTable;
    @FXML private TableColumn<OrderItem, String> colSku;
    @FXML private TableColumn<OrderItem, String> colName;
    @FXML private TableColumn<OrderItem, String> colPrice;
    @FXML private TableColumn<OrderItem, String> colQty;
    @FXML private TableColumn<OrderItem, String> colLineTotal;

    @FXML private HBox                  adminControls;
    @FXML private ComboBox<OrderStatus> statusCombo;
    @FXML private Label                 messageLabel;

    private static final DecimalFormat    MONEY = new DecimalFormat("₱#,##0.00");
    private static final DateTimeFormatter DT   = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    private Order   order;
    private Stage   stage;
    private boolean adminMode;
    private boolean changed;

    @FXML
    public void initialize() {
        colSku.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProductSku()));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProductName()));
        colPrice.setCellValueFactory(c -> new SimpleStringProperty(MONEY.format(c.getValue().getUnitPrice())));
        colQty.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getQuantity())));
        colLineTotal.setCellValueFactory(c -> new SimpleStringProperty(MONEY.format(c.getValue().getLineTotal())));

        statusCombo.setItems(FXCollections.observableArrayList(OrderStatus.values()));
    }

    public void setStage(Stage stage) { this.stage = stage; }
    public boolean hasChanged()       { return changed; }

    public void setOrder(Order order, boolean adminMode) {
        this.order = order;
        this.adminMode = adminMode;

        // Reload with items if not loaded
        if (order.getItems() == null || order.getItems().isEmpty()) {
            Order full = OrderDAO.findById(order.getId());
            if (full != null) order.setItems(full.getItems());
        }

        orderCodeLabel.setText(order.getOrderCode());
        orderDateLabel.setText("Placed " +
                (order.getCreatedAt() == null ? "" : order.getCreatedAt().format(DT)));
        orderStatusLabel.setText(order.getStatus().label());
        orderStatusLabel.getStyleClass().add(order.getStatus().cssClass());

        customerLabel.setText(order.getCustomerName() == null ? "—" : order.getCustomerName());
        contactLabel.setText(order.getContactNumber() == null ? "—" : order.getContactNumber());
        addressLabel.setText(order.getShippingAddress() == null ? "—" : order.getShippingAddress());
        notesLabel.setText(order.getNotes() == null || order.getNotes().isBlank() ? "—" : order.getNotes());

        itemsTable.setItems(FXCollections.observableArrayList(order.getItems()));
        totalLabel.setText(MONEY.format(order.getTotal()));

        adminControls.setManaged(adminMode);
        adminControls.setVisible(adminMode);
        if (adminMode) statusCombo.setValue(order.getStatus());
    }

    @FXML
    private void handleUpdateStatus() {
        OrderStatus selected = statusCombo.getValue();
        if (selected == null || selected == order.getStatus()) return;

        boolean ok = OrderDAO.updateStatus(order.getId(), selected);
        if (!ok) {
            messageLabel.setText("Could not update status.");
            messageLabel.getStyleClass().removeAll("form-message-error", "form-message-success");
            messageLabel.getStyleClass().add("form-message-error");
            messageLabel.setManaged(true); messageLabel.setVisible(true);
            return;
        }

        order.setStatus(selected);
        orderStatusLabel.getStyleClass().removeAll(
                "status-pending", "status-processing", "status-shipped",
                "status-delivered", "status-cancelled");
        orderStatusLabel.setText(selected.label());
        orderStatusLabel.getStyleClass().add(selected.cssClass());

        messageLabel.setText("Status updated to " + selected.label() + ".");
        messageLabel.getStyleClass().removeAll("form-message-error", "form-message-success");
        messageLabel.getStyleClass().add("form-message-success");
        messageLabel.setManaged(true); messageLabel.setVisible(true);

        changed = true;
    }

    @FXML
    private void handleClose() {
        if (stage != null) stage.close();
    }
}
