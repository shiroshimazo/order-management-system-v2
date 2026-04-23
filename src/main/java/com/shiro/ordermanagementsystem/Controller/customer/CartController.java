package com.shiro.ordermanagementsystem.Controller.customer;

import com.shiro.ordermanagementsystem.Customer;
import com.shiro.ordermanagementsystem.Order;
import com.shiro.ordermanagementsystem.OrderDAO;
import com.shiro.ordermanagementsystem.session.Cart;
import com.shiro.ordermanagementsystem.session.Session;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class CartController {

    @FXML private TableView<Cart.Line>         cartTable;
    @FXML private TableColumn<Cart.Line, String> colProduct;
    @FXML private TableColumn<Cart.Line, String> colPrice;
    @FXML private TableColumn<Cart.Line, Cart.Line> colQty;
    @FXML private TableColumn<Cart.Line, String> colSubtotal;
    @FXML private TableColumn<Cart.Line, Void>   colActions;

    @FXML private Label      totalLabel;
    @FXML private TextField  addressField;
    @FXML private TextField  contactField;
    @FXML private TextArea   notesField;
    @FXML private Label      messageLabel;
    @FXML private Button     checkoutButton;

    private static final DecimalFormat MONEY = new DecimalFormat("₱#,##0.00");
    private static CartController instance;

    @FXML
    public void initialize() {
        instance = this;

        colProduct.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().product.getName()));
        colPrice.setCellValueFactory(c -> new SimpleStringProperty(MONEY.format(c.getValue().product.getPrice())));
        colSubtotal.setCellValueFactory(c -> new SimpleStringProperty(MONEY.format(c.getValue().subtotal())));

        colQty.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colQty.setCellFactory(tc -> new TableCell<>() {
            private final Spinner<Integer> qty = new Spinner<>(1, 99, 1);
            private Cart.Line bound;
            private boolean updating;

            {
                qty.setPrefWidth(84);
                qty.setEditable(true);
                qty.valueProperty().addListener((o, a, b) -> {
                    if (updating || bound == null || b == null) return;
                    int max = bound.product.getStock();
                    if (b > max) { qty.getValueFactory().setValue(max); return; }
                    Cart.setQuantity(bound, b);
                    totalLabel.setText(MONEY.format(Cart.total()));
                    getTableView().refresh();
                });
            }

            @Override protected void updateItem(Cart.Line line, boolean empty) {
                super.updateItem(line, empty);
                if (empty || line == null) { setGraphic(null); bound = null; return; }
                bound = line;
                updating = true;
                ((SpinnerValueFactory.IntegerSpinnerValueFactory) qty.getValueFactory())
                        .setMax(Math.max(1, line.product.getStock()));
                qty.getValueFactory().setValue(line.quantity);
                updating = false;
                setGraphic(qty);
            }
        });

        colActions.setCellFactory(tc -> new TableCell<>() {
            private final Button btn = makeRemoveBtn();
            {
                btn.setOnAction(e -> {
                    Cart.Line l = getTableView().getItems().get(getIndex());
                    Cart.remove(l);
                    totalLabel.setText(MONEY.format(Cart.total()));
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
            }
        });

        cartTable.setItems(Cart.lines());
        Cart.lines().addListener((javafx.collections.ListChangeListener<Cart.Line>) c ->
                totalLabel.setText(MONEY.format(Cart.total())));
        totalLabel.setText(MONEY.format(Cart.total()));
    }

    private static Button makeRemoveBtn() {
        FontIcon fi = new FontIcon("fas-trash");
        fi.setIconSize(12);
        fi.setIconColor(javafx.scene.paint.Color.web("#D92A1C"));
        Button b = new Button();
        b.setGraphic(fi);
        b.getStyleClass().add("table-action-button");
        return b;
    }

    public static void refreshIfLoaded() {
        if (instance == null) return;
        instance.cartTable.refresh();
        instance.totalLabel.setText(MONEY.format(Cart.total()));
    }

    @FXML
    private void handleClear() {
        if (Cart.lines().isEmpty()) return;
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Remove all items from your cart?");
        a.setHeaderText("Clear cart");
        a.showAndWait().ifPresent(bt -> { if (bt == ButtonType.OK) Cart.clear(); });
    }

    @FXML
    private void handleCheckout() {
        Customer customer = Session.getCurrentCustomer();
        if (customer == null) { showError("You are not signed in."); return; }
        if (Cart.lines().isEmpty()) { showError("Your cart is empty."); return; }

        String address = addressField.getText().trim();
        String contact = contactField.getText().trim();
        String notes   = notesField.getText().trim();

        if (address.isEmpty()) { showError("Shipping address is required."); return; }
        if (contact.isEmpty()) { showError("Contact number is required."); return; }

        List<OrderDAO.CartLine> payload = new ArrayList<>();
        for (Cart.Line l : Cart.lines()) payload.add(new OrderDAO.CartLine(l.product.getId(), l.quantity));

        checkoutButton.setDisable(true);
        Order order = OrderDAO.placeOrder(customer.getId(), payload, address, contact, notes);
        checkoutButton.setDisable(false);

        if (order == null) {
            showError("Could not place the order. Stock may have changed.");
            return;
        }

        Cart.clear();
        addressField.clear(); contactField.clear(); notesField.clear();
        hideMessage();

        Alert ok = new Alert(Alert.AlertType.INFORMATION,
                "Your order " + order.getOrderCode() + " has been placed and is now pending.");
        ok.setHeaderText("Order placed!");
        ok.showAndWait();

        com.shiro.ordermanagementsystem.Controller.customer.CustomerHomeController.navigate(
                com.shiro.ordermanagementsystem.nav.CustomerNav.ORDER_HISTORY);
    }

    private void showError(String msg) {
        messageLabel.setText(msg);
        messageLabel.getStyleClass().removeAll("form-message-error", "form-message-success");
        messageLabel.getStyleClass().add("form-message-error");
        messageLabel.setManaged(true);
        messageLabel.setVisible(true);
    }

    private void hideMessage() {
        messageLabel.setText("");
        messageLabel.setManaged(false);
        messageLabel.setVisible(false);
    }
}
