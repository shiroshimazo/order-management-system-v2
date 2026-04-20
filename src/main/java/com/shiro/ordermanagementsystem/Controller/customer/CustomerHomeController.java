package com.shiro.ordermanagementsystem.Controller.customer;

import com.shiro.ordermanagementsystem.Customer;
import com.shiro.ordermanagementsystem.session.Cart;
import com.shiro.ordermanagementsystem.session.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

public class CustomerHomeController {

    @FXML private Label    customerNameLabel;
    @FXML private Label    customerEmailLabel;
    @FXML private Button   logoutButton;
    @FXML private TabPane  tabs;
    @FXML private Tab      browseTab;
    @FXML private Tab      cartTab;
    @FXML private Tab      ordersTab;

    private static CustomerHomeController instance;

    @FXML
    public void initialize() {
        instance = this;

        Customer customer = Session.getCurrentCustomer();
        if (customer != null) {
            customerNameLabel.setText(customer.getFullName());
            customerEmailLabel.setText(customer.getEmail());
        }

        loadTab(browseTab, "/ordermanagementsystem/fxml/customer/BrowseView.fxml");
        loadTab(cartTab,   "/ordermanagementsystem/fxml/customer/CartView.fxml");
        loadTab(ordersTab, "/ordermanagementsystem/fxml/customer/MyOrdersView.fxml");

        updateCartBadge();
        Cart.lines().addListener((javafx.collections.ListChangeListener<Cart.Line>) c -> updateCartBadge());

        tabs.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (b == ordersTab) MyOrdersController.refreshIfLoaded();
            if (b == cartTab)   CartController.refreshIfLoaded();
        });
    }

    private void loadTab(Tab tab, String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            tab.setContent(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateCartBadge() {
        int n = Cart.itemCount();
        cartTab.setText(n == 0 ? "Cart" : "Cart (" + n + ")");
    }

    /** Called from BrowseController after adding; jumps to Cart tab. */
    public static void selectCartTab() {
        if (instance != null) instance.tabs.getSelectionModel().select(instance.cartTab);
    }

    /** Called from CartController after checkout; jumps to My Orders tab. */
    public static void selectOrdersTab() {
        if (instance != null) instance.tabs.getSelectionModel().select(instance.ordersTab);
    }

    @FXML
    private void handleLogout() {
        try {
            Session.clear();
            Cart.clear();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ordermanagementsystem/fxml/LoginDashBoard.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setMinWidth(0);
            stage.setMinHeight(0);
            stage.setResizable(false);
            stage.setWidth(420);
            stage.setHeight(660);
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
