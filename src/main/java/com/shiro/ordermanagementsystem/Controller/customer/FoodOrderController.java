package com.shiro.ordermanagementsystem.Controller.customer;

import com.shiro.ordermanagementsystem.Category;
import com.shiro.ordermanagementsystem.CategoryDAO;
import com.shiro.ordermanagementsystem.Customer;
import com.shiro.ordermanagementsystem.Order;
import com.shiro.ordermanagementsystem.OrderDAO;
import com.shiro.ordermanagementsystem.Product;
import com.shiro.ordermanagementsystem.ProductDAO;
import com.shiro.ordermanagementsystem.session.Cart;
import com.shiro.ordermanagementsystem.session.Session;
import com.shiro.ordermanagementsystem.ui.ProductImages;
import com.shiro.ordermanagementsystem.ui.Toast;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FoodOrderController {

    // ─── Catalog side ─────────────────────────────────────────────────────────
    @FXML private VBox       catalogColumn;
    @FXML private TextField  searchField;
    @FXML private HBox       chipBar;
    @FXML private FlowPane   productGrid;
    @FXML private ScrollPane gridScroll;
    @FXML private Label      emptyLabel;
    @FXML private Button     toggleCartButton;
    @FXML private Label      toggleCartCount;

    // ─── Cart side ────────────────────────────────────────────────────────────
    @FXML private VBox       cartPanel;
    @FXML private Button     closeCartButton;
    @FXML private VBox       cartLines;
    @FXML private Label      cartEmptyLabel;
    @FXML private Label      subtotalLabel;
    @FXML private Label      vatLabel;
    @FXML private Label      totalLabel;
    @FXML private TextField  addressField;
    @FXML private TextField  contactField;
    @FXML private TextArea   notesField;
    @FXML private Label      messageLabel;
    @FXML private Button     checkoutButton;

    private static final DecimalFormat MONEY = new DecimalFormat("₱#,##0.00");
    private static final double NARROW_THRESHOLD = 820;

    private static final String CHIP_BASE   = "category-chip";
    private static final String CHIP_ACTIVE = "category-chip-active";
    private static final Object ALL_CATS    = new Object();

    private Object selectedCategory = ALL_CATS;   // Either ALL_CATS or a Category instance
    private boolean cartPanelManuallyHidden = false;
    private boolean narrowMode = false;

    @FXML
    public void initialize() {
        buildCategoryChips();
        searchField.textProperty().addListener((o, a, b) -> reloadProducts());
        Cart.lines().addListener((javafx.collections.ListChangeListener<Cart.Line>) c -> rebuildCart());

        reloadProducts();
        rebuildCart();

        // Pre-fill delivery details from customer profile if available
        Customer c = Session.getCurrentCustomer();
        if (c != null && c.getPhone() != null) contactField.setText(c.getPhone());

        catalogColumn.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) bindResponsive(newScene);
        });
    }

    // ─── Responsiveness ───────────────────────────────────────────────────────
    private void bindResponsive(Scene scene) {
        scene.widthProperty().addListener((o, a, w) -> applyResponsive(w.doubleValue()));
        Platform.runLater(() -> applyResponsive(scene.getWidth()));
    }

    private void applyResponsive(double sceneWidth) {
        boolean newNarrow = sceneWidth < NARROW_THRESHOLD;
        if (newNarrow != narrowMode) {
            narrowMode = newNarrow;
            // In narrow mode, the cart panel is overlay-only — hidden by default,
            // shown via the cart toggle button.
            if (narrowMode) {
                cartPanel.setVisible(false);
                cartPanel.setManaged(false);
                toggleCartButton.setVisible(true);
                toggleCartButton.setManaged(true);
                closeCartButton.setVisible(true);
                closeCartButton.setManaged(true);
            } else {
                cartPanel.setVisible(true);
                cartPanel.setManaged(true);
                toggleCartButton.setVisible(false);
                toggleCartButton.setManaged(false);
                closeCartButton.setVisible(false);
                closeCartButton.setManaged(false);
                cartPanelManuallyHidden = false;
            }
        }
    }

    @FXML
    private void toggleCartPanel() {
        boolean show = !cartPanel.isVisible();
        cartPanel.setVisible(show);
        cartPanel.setManaged(show);
        cartPanelManuallyHidden = !show;
    }

    @FXML
    private void hideCartPanel() {
        cartPanel.setVisible(false);
        cartPanel.setManaged(false);
        cartPanelManuallyHidden = true;
    }

    // ─── Category chips ───────────────────────────────────────────────────────
    private void buildCategoryChips() {
        chipBar.getChildren().clear();
        chipBar.getChildren().add(makeChip("All", ALL_CATS));
        for (Category cat : CategoryDAO.findAll()) {
            chipBar.getChildren().add(makeChip(cat.getName(), cat));
        }
        repaintChips();
    }

    private Button makeChip(String label, Object value) {
        Button b = new Button(label);
        b.getStyleClass().add(CHIP_BASE);
        b.setOnAction(e -> {
            selectedCategory = value;
            repaintChips();
            reloadProducts();
        });
        b.getProperties().put("value", value);
        return b;
    }

    private void repaintChips() {
        for (var node : chipBar.getChildren()) {
            if (!(node instanceof Button b)) continue;
            Object v = b.getProperties().get("value");
            b.getStyleClass().remove(CHIP_ACTIVE);
            if (v == selectedCategory) b.getStyleClass().add(CHIP_ACTIVE);
        }
    }

    // ─── Product grid ─────────────────────────────────────────────────────────
    private void reloadProducts() {
        Integer catId = (selectedCategory instanceof Category c) ? c.getId() : null;
        List<Product> products = ProductDAO.searchActive(searchField.getText(), catId);

        productGrid.getChildren().clear();
        for (Product p : products) productGrid.getChildren().add(buildCard(p));

        boolean empty = products.isEmpty();
        emptyLabel.setVisible(empty);
        emptyLabel.setManaged(empty);
    }

    private VBox buildCard(Product p) {
        // Image
        StackPane imageWrap = new StackPane();
        imageWrap.getStyleClass().add("product-image-wrap");
        Image img = ProductImages.loadForProduct(p.getId(), 220, 140);
        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(220);
            iv.setFitHeight(140);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            imageWrap.getChildren().add(iv);
        } else {
            imageWrap.getChildren().add(placeholderIcon());
        }

        // Body
        Label name = new Label(p.getName());
        name.getStyleClass().add("product-name");
        name.setWrapText(true);

        Label desc = new Label(p.getDescription() == null ? "" : p.getDescription());
        desc.getStyleClass().add("product-desc");
        desc.setWrapText(true);
        desc.setMaxHeight(32);

        // Rating row
        HBox ratingRow = new HBox(4);
        ratingRow.setAlignment(Pos.CENTER_LEFT);
        FontIcon star = new FontIcon("fas-star");
        star.setIconSize(11);
        star.getStyleClass().add("rating-star");
        Label ratingValue = new Label(formatRating(p.getRating()));
        ratingValue.getStyleClass().add("rating-text");
        Label ratingCount = new Label("(" + p.getRatingCount() + ")");
        ratingCount.getStyleClass().add("rating-count");
        ratingRow.getChildren().addAll(star, ratingValue, ratingCount);

        // Price row
        HBox priceRow = new HBox(8);
        priceRow.setAlignment(Pos.CENTER_LEFT);
        Label price = new Label(MONEY.format(p.getPrice()));
        price.getStyleClass().add("product-price");
        priceRow.getChildren().add(price);
        if (p.getOriginalPrice() != null && p.getOriginalPrice().compareTo(p.getPrice()) > 0) {
            Label strike = new Label(MONEY.format(p.getOriginalPrice()));
            strike.getStyleClass().add("product-price-strike");
            priceRow.getChildren().add(strike);
        }

        // Footer (stock + add to cart)
        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_LEFT);
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);

        Label stock = new Label();
        if (p.getStock() <= 0) {
            stock.setText("Out of stock");
            stock.getStyleClass().add("product-stock-out");
        } else if (p.getStock() <= 5) {
            stock.setText("Only " + p.getStock() + " left");
            stock.getStyleClass().add("product-stock-low");
        }

        Button add = new Button("Add");
        add.getStyleClass().add("add-to-cart-button");
        FontIcon plus = new FontIcon("fas-plus");
        plus.setIconSize(10);
        plus.setIconColor(javafx.scene.paint.Color.WHITE);
        add.setGraphic(plus);
        add.setDisable(p.getStock() <= 0);
        add.setOnAction(e -> {
            Cart.add(p, 1);
            Toast.success(productGrid, "Added " + p.getName() + " to cart.");
        });

        footer.getChildren().addAll(stock, grow, add);

        VBox body = new VBox(8, name, desc, ratingRow, priceRow, footer);
        body.setPadding(new Insets(12, 14, 14, 14));

        VBox card = new VBox(imageWrap, body);
        card.getStyleClass().add("product-card");
        card.setPrefWidth(240);
        card.setMaxWidth(240);
        return card;
    }

    private FontIcon placeholderIcon() {
        FontIcon icon = new FontIcon("fas-utensils");
        icon.setIconSize(36);
        icon.setIconColor(javafx.scene.paint.Color.web("#cfd2d8"));
        return icon;
    }

    private static String formatRating(BigDecimal r) {
        if (r == null) return "0.0";
        return String.format("%.1f", r.doubleValue());
    }

    // ─── Cart side rendering ──────────────────────────────────────────────────
    private void rebuildCart() {
        cartLines.getChildren().clear();

        // Group identical lines (Cart already does this — render as-is)
        for (Cart.Line line : new ArrayList<>(Cart.lines())) {
            cartLines.getChildren().add(buildCartLine(line));
        }

        boolean empty = Cart.lines().isEmpty();
        cartEmptyLabel.setVisible(empty);
        cartEmptyLabel.setManaged(empty);

        // Update cart-toggle pill in toolbar
        toggleCartCount.setText(String.valueOf(Cart.itemCount()));

        BigDecimal subtotal = Cart.total().setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal vat      = OrderDAO.computeTax(subtotal);
        BigDecimal total    = subtotal.add(vat);

        subtotalLabel.setText(MONEY.format(subtotal));
        vatLabel.setText(MONEY.format(vat));
        totalLabel.setText(MONEY.format(total));

        checkoutButton.setDisable(empty);
    }

    private HBox buildCartLine(Cart.Line line) {
        VBox text = new VBox(2);
        Label name = new Label(line.product.getName());
        name.getStyleClass().add("invoice-line-name");
        name.setWrapText(true);
        Label meta = new Label(MONEY.format(line.product.getPrice()) + " • Subtotal " + MONEY.format(line.subtotal()));
        meta.getStyleClass().add("invoice-line-meta");
        text.getChildren().addAll(name, meta);
        HBox.setHgrow(text, Priority.ALWAYS);

        Button minus = makeStepperBtn("fas-minus");
        Label  qty   = new Label(String.valueOf(line.quantity));
        qty.getStyleClass().add("qty-stepper-value");
        Button plus  = makeStepperBtn("fas-plus");

        minus.setOnAction(e -> Cart.setQuantity(line, line.quantity - 1));
        plus.setOnAction(e -> {
            if (line.quantity < line.product.getStock()) Cart.setQuantity(line, line.quantity + 1);
        });

        Button remove = makeStepperBtn("fas-trash");
        ((FontIcon) remove.getGraphic()).setIconColor(javafx.scene.paint.Color.web("#D92A1C"));
        remove.setOnAction(e -> Cart.remove(line));

        HBox stepper = new HBox(4, minus, qty, plus);
        stepper.setAlignment(Pos.CENTER);

        HBox row = new HBox(8, text, stepper, remove);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("invoice-line");
        return row;
    }

    private Button makeStepperBtn(String iconLiteral) {
        FontIcon fi = new FontIcon(iconLiteral);
        fi.setIconSize(10);
        fi.setIconColor(javafx.scene.paint.Color.web("#444444"));
        Button b = new Button();
        b.setGraphic(fi);
        b.getStyleClass().add("qty-stepper-button");
        return b;
    }

    // ─── Checkout ─────────────────────────────────────────────────────────────
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

        if (address.isEmpty()) { showError("Shipping address is required."); return; }
        if (contact.isEmpty()) { showError("Contact number is required."); return; }

        BigDecimal subtotal = Cart.total().setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal vat      = OrderDAO.computeTax(subtotal);
        BigDecimal total    = subtotal.add(vat);

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.initOwner(checkoutButton.getScene().getWindow());
        confirm.setTitle("Confirm Order");
        confirm.setHeaderText("Place this order?");
        confirm.setContentText(
                "Items: "    + Cart.itemCount() + "\n" +
                "Subtotal: " + MONEY.format(subtotal) + "\n" +
                "VAT (12%): " + MONEY.format(vat) + "\n" +
                "Total: "    + MONEY.format(total) + "\n\n" +
                "Deliver to:\n" + address + "\nContact: " + contact);

        ButtonType place  = new ButtonType("Place Order", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancel",      ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(cancel, place);

        Button cancelBtn = (Button) confirm.getDialogPane().lookupButton(cancel);
        cancelBtn.setDefaultButton(true);
        Button placeBtn  = (Button) confirm.getDialogPane().lookupButton(place);
        placeBtn.setDefaultButton(false);

        java.util.Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != place) return;

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
        addressField.clear(); notesField.clear();
        hideMessage();

        Alert ok = new Alert(Alert.AlertType.INFORMATION,
                "Your order " + order.getOrderCode() + " is now pending.\n\n" +
                "Subtotal: " + MONEY.format(order.getSubtotal()) + "\n" +
                "VAT (12%): " + MONEY.format(order.getTax()) + "\n" +
                "Total: " + MONEY.format(order.getTotal()));
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

    // Surface for legacy callers that used CartController.refreshIfLoaded()
    public static void refreshIfLoaded() { /* observable list drives updates */ }
}
