package com.shiro.ordermanagementsystem.Controller.customer;

import com.shiro.ordermanagementsystem.Customer;
import com.shiro.ordermanagementsystem.nav.CustomerNav;
import com.shiro.ordermanagementsystem.session.Cart;
import com.shiro.ordermanagementsystem.session.Session;
import com.shiro.ordermanagementsystem.ui.CustomerImages;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public class CustomerHomeController {

    // ─── Top bar ──────────────────────────────────────────────────────────────
    @FXML private HBox      topBar;
    @FXML private Button    hamburgerButton;
    @FXML private Label     pageTitle;
    @FXML private Button    cartButton;
    @FXML private Label     cartCountLabel;
    @FXML private Label     customerNameLabel;
    @FXML private Label     customerEmailLabel;
    @FXML private Label     customerAvatarLabel;
    @FXML private StackPane customerAvatarStack;
    @FXML private ImageView customerAvatarImage;

    // ─── Sidebar ──────────────────────────────────────────────────────────────
    @FXML private VBox      sidebar;
    @FXML private Label     brandLabel;
    @FXML private VBox      menuContainer;
    @FXML private Button    logoutButton;
    @FXML private Label     logoutLabel;

    // ─── Content ──────────────────────────────────────────────────────────────
    @FXML private StackPane contentArea;

    // ─── State ────────────────────────────────────────────────────────────────
    private static final double SIDEBAR_EXPANDED_WIDTH  = 240;
    private static final double SIDEBAR_COLLAPSED_WIDTH = 64;
    private static final double SIDEBAR_AUTO_COLLAPSE   = 920;   // px
    private static final double SIDEBAR_AUTO_HIDE       = 600;   // px
    private static final Duration ANIM_DURATION         = Duration.millis(200);

    private boolean collapsed = false;
    private boolean userTouchedSidebar = false;
    private CustomerNav activeNav;

    private final Map<CustomerNav, Button> menuButtons = new EnumMap<>(CustomerNav.class);

    private static CustomerHomeController instance;

    // ─── Initialize ───────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        instance = this;

        // Clip the topbar avatar to a 36px circle (matches the StackPane frame).
        if (customerAvatarImage != null) {
            customerAvatarImage.setClip(new Circle(18, 18, 18));
        }

        Customer customer = Session.getCurrentCustomer();
        if (customer != null) paintCustomerBadge(customer);

        buildMenu();
        selectNav(CustomerNav.DASHBOARD);

        updateCartBadge();
        Cart.lines().addListener((javafx.collections.ListChangeListener<Cart.Line>) c -> updateCartBadge());

        contentArea.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                registerSearchAccelerator(newScene);
                newScene.widthProperty().addListener((o, a, w) -> applyResponsiveLayout(w.doubleValue()));
                applyResponsiveLayout(newScene.getWidth());
            }
        });
    }

    private void registerSearchAccelerator(Scene scene) {
        KeyCombination combo = new KeyCodeCombination(KeyCode.K, KeyCombination.CONTROL_DOWN);
        scene.getAccelerators().put(combo, () -> {
            if (contentArea.getChildren().isEmpty()) return;
            Node view = contentArea.getChildren().get(0);
            Node target = view.lookup("#searchField");
            if (target != null) target.requestFocus();
        });
    }

    /** Refresh badge from child controllers (e.g. Settings) after profile edits. */
    public static void refreshCustomerInfo() {
        if (instance == null) return;
        Customer c = Session.getCurrentCustomer();
        if (c == null) return;
        instance.paintCustomerBadge(c);
    }

    private void paintCustomerBadge(Customer c) {
        customerNameLabel.setText(c.getFullName());
        customerEmailLabel.setText(c.getEmail());
        customerAvatarLabel.setText(initialsOf(c.getFullName()));

        // Show the uploaded avatar if there is one, otherwise fall back to initials.
        Image avatar = CustomerImages.loadForCustomer(c.getId(), 72, 72);
        boolean hasAvatar = avatar != null;
        if (customerAvatarImage != null) {
            customerAvatarImage.setImage(avatar);
            customerAvatarImage.setVisible(hasAvatar);
            customerAvatarImage.setManaged(hasAvatar);
        }
        customerAvatarLabel.setVisible(!hasAvatar);
        customerAvatarLabel.setManaged(!hasAvatar);
    }

    private static String initialsOf(String fullName) {
        if (fullName == null || fullName.isBlank()) return "C";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    // ─── Build sidebar menu from CustomerNav enum ─────────────────────────────
    private void buildMenu() {
        menuContainer.getChildren().clear();
        menuButtons.clear();

        for (CustomerNav nav : CustomerNav.values()) {
            Button btn = buildMenuButton(nav);
            btn.setOnAction(e -> selectNav(nav));

            menuContainer.getChildren().add(btn);
            menuButtons.put(nav, btn);
        }
    }

    private Button buildMenuButton(CustomerNav nav) {
        FontIcon icon = new FontIcon(nav.getIconLiteral());
        icon.setIconSize(15);
        icon.setIconColor(javafx.scene.paint.Color.web("#cccccc"));

        Label label = new Label(nav.getLabel());
        label.getStyleClass().add("menu-label");

        HBox graphic = new HBox(12, icon, label);
        graphic.setStyle("-fx-alignment: CENTER_LEFT;");

        Button btn = new Button();
        btn.setGraphic(graphic);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.getStyleClass().add("menu-button");
        btn.getProperties().put("nav", nav);
        return btn;
    }

    /** Public so sub-views (Dashboard quick-actions) can jump to other sections. */
    public static void navigate(CustomerNav nav) {
        if (instance != null) instance.selectNav(nav);
    }

    private void selectNav(CustomerNav nav) {
        if (nav == activeNav) return;

        menuButtons.forEach((key, btn) -> {
            btn.getStyleClass().remove("menu-button-active");
            if (key == nav) btn.getStyleClass().add("menu-button-active");
        });

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(nav.getFxmlPath()));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
            pageTitle.setText(nav.getLabel());
            activeNav = nav;

            FadeTransition fade = new FadeTransition(Duration.millis(160), view);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.play();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // ─── Cart badge ───────────────────────────────────────────────────────────
    private void updateCartBadge() {
        int n = Cart.itemCount();
        cartCountLabel.setText(String.valueOf(n));
        cartButton.setVisible(true);
        cartButton.setManaged(true);
    }

    @FXML
    private void openCart() {
        navigate(CustomerNav.FOOD_ORDER);
        if (contentArea.getChildren().isEmpty()) return;
        // Ask the food-order view to scroll to its cart panel if it's listening
        Node view = contentArea.getChildren().get(0);
        Node target = view.lookup("#cartPanel");
        if (target != null) target.requestFocus();
    }

    // ─── Sidebar collapse toggle ──────────────────────────────────────────────
    @FXML
    private void toggleSidebar() {
        userTouchedSidebar = true;
        setSidebarCollapsed(!collapsed);
    }

    private void setSidebarCollapsed(boolean newCollapsed) {
        collapsed = newCollapsed;
        double target = collapsed ? SIDEBAR_COLLAPSED_WIDTH : SIDEBAR_EXPANDED_WIDTH;

        Timeline tl = new Timeline(new KeyFrame(ANIM_DURATION,
                new KeyValue(sidebar.prefWidthProperty(), target),
                new KeyValue(sidebar.minWidthProperty(),  target),
                new KeyValue(sidebar.maxWidthProperty(),  target)));
        tl.play();

        boolean showText = !collapsed;
        brandLabel.setVisible(showText);
        brandLabel.setManaged(showText);
        logoutLabel.setVisible(showText);
        logoutLabel.setManaged(showText);

        menuButtons.values().forEach(btn -> {
            HBox graphic = (HBox) btn.getGraphic();
            if (graphic.getChildren().size() >= 2) {
                Label lbl = (Label) graphic.getChildren().get(1);
                lbl.setVisible(showText);
                lbl.setManaged(showText);
            }
        });
    }

    /** Auto-collapse when window narrows (modern phones); auto-hide entirely on tiny widths. */
    private void applyResponsiveLayout(double width) {
        // Hide email + name detail on very narrow widths to avoid clipping
        boolean roomForEmail = width >= 720;
        customerEmailLabel.setVisible(roomForEmail);
        customerEmailLabel.setManaged(roomForEmail);

        boolean roomForName = width >= 540;
        customerNameLabel.setVisible(roomForName);
        customerNameLabel.setManaged(roomForName);

        if (width < SIDEBAR_AUTO_HIDE) {
            sidebar.setVisible(false);
            sidebar.setManaged(false);
            return;
        } else {
            sidebar.setVisible(true);
            sidebar.setManaged(true);
        }

        // Honour the user's manual toggle once they've used it
        if (userTouchedSidebar) return;

        boolean shouldCollapse = width < SIDEBAR_AUTO_COLLAPSE;
        if (shouldCollapse != collapsed) setSidebarCollapsed(shouldCollapse);
    }

    // ─── Logout (with confirmation) ───────────────────────────────────────────
    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.initOwner(logoutButton.getScene().getWindow());
        confirm.setTitle("Sign Out");
        confirm.setHeaderText("Sign out of your account?");
        Customer c = Session.getCurrentCustomer();
        confirm.setContentText(c == null
                ? "You'll be returned to the login screen."
                : "You'll be returned to the login screen, " + c.getFullName().split("\\s+")[0] + ".");

        ButtonType signOut = new ButtonType("Sign Out", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel  = new ButtonType("Cancel",   ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(cancel, signOut);

        Button cancelBtn = (Button) confirm.getDialogPane().lookupButton(cancel);
        cancelBtn.setDefaultButton(true);
        Button signOutBtn = (Button) confirm.getDialogPane().lookupButton(signOut);
        signOutBtn.setDefaultButton(false);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != signOut) return;

        performLogout();
    }

    private void performLogout() {
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
