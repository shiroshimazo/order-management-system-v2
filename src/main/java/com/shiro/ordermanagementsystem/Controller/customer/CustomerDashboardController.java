package com.shiro.ordermanagementsystem.Controller.customer;

import com.shiro.ordermanagementsystem.Customer;
import com.shiro.ordermanagementsystem.Order;
import com.shiro.ordermanagementsystem.OrderDAO;
import com.shiro.ordermanagementsystem.OrderStatus;
import com.shiro.ordermanagementsystem.Product;
import com.shiro.ordermanagementsystem.ProductDAO;
import com.shiro.ordermanagementsystem.nav.CustomerNav;
import com.shiro.ordermanagementsystem.session.Cart;
import com.shiro.ordermanagementsystem.session.Session;
import com.shiro.ordermanagementsystem.ui.ProductImages;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

public class CustomerDashboardController {

    @FXML private Label    greetingEyebrow;
    @FXML private Label    greetingTitle;
    @FXML private Label    activeOrdersLabel;
    @FXML private Label    totalOrdersLabel;
    @FXML private Label    lifetimeLabel;
    @FXML private FlowPane featuredGrid;

    private static final DecimalFormat MONEY = new DecimalFormat("₱#,##0.00");

    @FXML
    public void initialize() {
        Customer c = Session.getCurrentCustomer();
        if (c != null) {
            greetingEyebrow.setText(timeOfDayGreeting().toUpperCase() + ", " + firstName(c.getFullName()).toUpperCase());
        }

        loadStats(c);
        loadFeatured();
    }

    private static String timeOfDayGreeting() {
        int h = LocalTime.now().getHour();
        if (h < 12) return "Good morning";
        if (h < 18) return "Good afternoon";
        return "Good evening";
    }

    private static String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "there";
        return fullName.trim().split("\\s+")[0];
    }

    private void loadStats(Customer c) {
        if (c == null) return;
        List<Order> orders = OrderDAO.findByCustomer(c.getId());
        int active = 0;
        BigDecimal lifetime = BigDecimal.ZERO;
        for (Order o : orders) {
            if (o.getStatus() != OrderStatus.DELIVERED && o.getStatus() != OrderStatus.CANCELLED) active++;
            if (o.getStatus() != OrderStatus.CANCELLED && o.getTotal() != null)
                lifetime = lifetime.add(o.getTotal());
        }
        activeOrdersLabel.setText(String.valueOf(active));
        totalOrdersLabel.setText(String.valueOf(orders.size()));
        lifetimeLabel.setText(MONEY.format(lifetime));
    }

    private void loadFeatured() {
        List<Product> products = ProductDAO.searchActive(null, null);
        // Sort by rating desc, take top 4
        products.sort(Comparator.comparing(
                (Product p) -> p.getRating() == null ? BigDecimal.ZERO : p.getRating()).reversed());
        int count = Math.min(4, products.size());

        featuredGrid.getChildren().clear();
        for (int i = 0; i < count; i++) {
            featuredGrid.getChildren().add(buildFeaturedCard(products.get(i)));
        }
    }

    private VBox buildFeaturedCard(Product p) {
        StackPane imageWrap = new StackPane();
        imageWrap.getStyleClass().add("product-image-wrap");
        Image img = ProductImages.load(p.getImageUrl(), 220, 140);
        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(220);
            iv.setFitHeight(140);
            iv.setPreserveRatio(true);
            imageWrap.getChildren().add(iv);
        } else {
            imageWrap.getChildren().add(placeholder());
        }

        Label name = new Label(p.getName());
        name.getStyleClass().add("product-name");
        name.setWrapText(true);

        HBox rating = new HBox(4);
        rating.setAlignment(Pos.CENTER_LEFT);
        FontIcon star = new FontIcon("fas-star");
        star.setIconSize(11);
        star.getStyleClass().add("rating-star");
        Label rv = new Label(String.format("%.1f", p.getRating().doubleValue()));
        rv.getStyleClass().add("rating-text");
        rating.getChildren().addAll(star, rv);

        Label price = new Label(MONEY.format(p.getPrice()));
        price.getStyleClass().add("product-price");

        Button add = new Button("Add to Cart");
        add.getStyleClass().add("add-to-cart-button");
        add.setMaxWidth(Double.MAX_VALUE);
        add.setDisable(p.getStock() <= 0);
        add.setOnAction(e -> Cart.add(p, 1));

        VBox body = new VBox(8, name, rating, price, add);
        body.setPadding(new Insets(12, 14, 14, 14));

        VBox card = new VBox(imageWrap, body);
        card.getStyleClass().add("product-card");
        card.setPrefWidth(240);
        card.setMaxWidth(240);
        return card;
    }

    private FontIcon placeholder() {
        FontIcon icon = new FontIcon("fas-utensils");
        icon.setIconSize(36);
        icon.setIconColor(javafx.scene.paint.Color.web("#cfd2d8"));
        return icon;
    }

    @FXML
    private void goToFoodOrder() {
        CustomerHomeController.navigate(CustomerNav.FOOD_ORDER);
    }
}
