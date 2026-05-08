package com.shiro.ordermanagementsystem.Controller.customer;

import com.shiro.ordermanagementsystem.Customer;
import com.shiro.ordermanagementsystem.Order;
import com.shiro.ordermanagementsystem.OrderDAO;
import com.shiro.ordermanagementsystem.ServiceType;
import com.shiro.ordermanagementsystem.session.Cart;
import com.shiro.ordermanagementsystem.session.Session;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.kordamp.ikonli.javafx.FontIcon;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class CartController {

    @FXML private TableView<Cart.Line>           cartTable;
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

    // Service-type toggles
    @FXML private ToggleButton deliveryToggle;
    @FXML private ToggleButton restaurantToggle;
    @FXML private javafx.scene.layout.HBox restaurantSubBar;
    @FXML private ToggleButton dineInToggle;
    @FXML private ToggleButton takeOutToggle;

    // Conditional details labels / row
    @FXML private Label detailsTitleLabel;
    @FXML private javafx.scene.layout.VBox locationRow;
    @FXML private Label locationLabel;

    private static final DecimalFormat MONEY = new DecimalFormat("₱#,##0.00");
    private static final int MAX_PHONE_DIGITS = 13;
    private static CartController instance;

    private ServiceType selectedService = ServiceType.DELIVERY;

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

        // ─── Service-type toggles ────────────────────────────────────────────
        ToggleGroup top = new ToggleGroup();
        deliveryToggle.setToggleGroup(top);
        restaurantToggle.setToggleGroup(top);

        ToggleGroup sub = new ToggleGroup();
        dineInToggle.setToggleGroup(sub);
        takeOutToggle.setToggleGroup(sub);

        // Don't let a toggle be deselected into nothing — keep the previous one.
        top.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null && oldT != null) oldT.setSelected(true);
        });
        // Sub group: only enforce a selection while in Restaurant mode. When the
        // user switches to Delivery we intentionally clear the sub selection.
        sub.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null && oldT != null && selectedService != ServiceType.DELIVERY) {
                oldT.setSelected(true);
            }
        });

        deliveryToggle.setOnAction(e -> selectService(ServiceType.DELIVERY));
        restaurantToggle.setOnAction(e -> {
            // Default to Dine-In when entering Restaurant mode.
            if (!dineInToggle.isSelected() && !takeOutToggle.isSelected()) {
                dineInToggle.setSelected(true);
                selectService(ServiceType.DINE_IN);
            } else if (takeOutToggle.isSelected()) {
                selectService(ServiceType.TAKE_OUT);
            } else {
                selectService(ServiceType.DINE_IN);
            }
        });
        dineInToggle.setOnAction(e   -> selectService(ServiceType.DINE_IN));
        takeOutToggle.setOnAction(e  -> selectService(ServiceType.TAKE_OUT));

        // Default: Delivery
        deliveryToggle.setSelected(true);
        applyServiceTypeUi(ServiceType.DELIVERY);

        // ─── Contact field: digits only, capped length ───────────────────────
        contactField.setTextFormatter(new TextFormatter<>(change -> {
            String next = change.getControlNewText();
            if (!next.matches("\\d*")) return null;
            if (next.length() > MAX_PHONE_DIGITS) return null;
            return change;
        }));
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

    // ─── Service-type plumbing ───────────────────────────────────────────────
    private void selectService(ServiceType type) {
        this.selectedService = type;
        // Keep the top toggle state coherent.
        if (type == ServiceType.DELIVERY) {
            deliveryToggle.setSelected(true);
            // No sub-selection while in Delivery mode.
            dineInToggle.setSelected(false);
            takeOutToggle.setSelected(false);
        } else {
            restaurantToggle.setSelected(true);
        }
        applyServiceTypeUi(type);
    }

    private void applyServiceTypeUi(ServiceType type) {
        boolean restaurant = (type == ServiceType.DINE_IN || type == ServiceType.TAKE_OUT);
        restaurantSubBar.setManaged(restaurant);
        restaurantSubBar.setVisible(restaurant);

        switch (type) {
            case DELIVERY -> {
                detailsTitleLabel.setText("Delivery Details");
                showLocationRow("Shipping Address",
                                "Street, Barangay, City, Province");
            }
            case DINE_IN -> {
                detailsTitleLabel.setText("Dine-In Details");
                showLocationRow("Table Number (optional)",
                                "e.g. 5");
            }
            case TAKE_OUT -> {
                detailsTitleLabel.setText("Take-Out Details");
                hideLocationRow();
            }
        }
    }

    private void showLocationRow(String label, String prompt) {
        locationLabel.setText(label);
        addressField.setPromptText(prompt);
        locationRow.setManaged(true);
        locationRow.setVisible(true);
    }

    private void hideLocationRow() {
        locationRow.setManaged(false);
        locationRow.setVisible(false);
        addressField.clear();
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

        String address = addressField.getText() == null ? "" : addressField.getText().trim();
        String contact = contactField.getText() == null ? "" : contactField.getText().trim();
        String notes   = notesField.getText()   == null ? "" : notesField.getText().trim();

        // Service-type-specific validation
        switch (selectedService) {
            case DELIVERY -> {
                if (address.isEmpty()) { showError("Shipping address is required for delivery."); return; }
            }
            case DINE_IN  -> { /* table # optional, falls through to contact check */ }
            case TAKE_OUT -> { /* address not used */ }
        }

        if (contact.isEmpty()) { showError("Contact number is required."); return; }
        if (contact.length() < 7) { showError("Enter a valid contact number (at least 7 digits)."); return; }

        // Persist null for the address column when there is nothing meaningful to store.
        String storedAddress = address.isEmpty() ? null : address;

        List<OrderDAO.CartLine> payload = new ArrayList<>();
        for (Cart.Line l : Cart.lines()) payload.add(new OrderDAO.CartLine(l.product.getId(), l.quantity));

        checkoutButton.setDisable(true);
        Order order = OrderDAO.placeOrder(
                customer.getId(), payload, storedAddress, contact, notes, selectedService);
        checkoutButton.setDisable(false);

        if (order == null) {
            showError("Could not place the order. Stock may have changed.");
            return;
        }

        Cart.clear();
        addressField.clear(); contactField.clear(); notesField.clear();
        // Reset back to Delivery for the next order.
        selectService(ServiceType.DELIVERY);
        hideMessage();

        Alert ok = new Alert(Alert.AlertType.INFORMATION,
                "Your " + selectedService.label().toLowerCase() + " order " + order.getOrderCode()
                        + " has been placed and is now pending.");
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
