package com.shiro.ordermanagementsystem.Controller;

import com.shiro.ordermanagementsystem.Admin;
import com.shiro.ordermanagementsystem.nav.AdminNav;
import com.shiro.ordermanagementsystem.session.Session;
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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public class AdminShellController {

    // ─── Top bar ──────────────────────────────────────────────────────────────
    @FXML private HBox      topBar;
    @FXML private Button    hamburgerButton;
    @FXML private Label     pageTitle;
    @FXML private Label     adminNameLabel;
    @FXML private Label     adminLevelLabel;
    @FXML private Label     adminAvatarLabel;

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
    private static final Duration ANIM_DURATION         = Duration.millis(200);

    private boolean collapsed = false;
    private AdminNav activeNav;

    private final Map<AdminNav, Button> menuButtons = new EnumMap<>(AdminNav.class);

    private static AdminShellController instance;

    // ─── Initialize ───────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        instance = this;

        Admin admin = Session.getCurrentAdmin();
        if (admin != null) paintAdminBadge(admin);

        buildMenu(admin);
        selectNav(AdminNav.DASHBOARD);

        contentArea.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) registerSearchAccelerator(newScene);
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

    /** Called from child controllers (e.g. Settings) after profile edits. */
    public static void refreshAdminInfo() {
        if (instance == null) return;
        Admin admin = Session.getCurrentAdmin();
        if (admin == null) return;
        instance.paintAdminBadge(admin);
    }

    private void paintAdminBadge(Admin admin) {
        adminNameLabel.setText(admin.getFullName());
        adminLevelLabel.setText(admin.getAdminLevel().name());
        adminAvatarLabel.setText(initialsOf(admin.getFullName()));
    }

    private static String initialsOf(String fullName) {
        if (fullName == null || fullName.isBlank()) return "A";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    // ─── Build sidebar menu from AdminNav enum ────────────────────────────────
    private void buildMenu(Admin admin) {
        menuContainer.getChildren().clear();
        menuButtons.clear();

        for (AdminNav nav : AdminNav.values()) {
            if (!nav.isVisibleFor(admin)) continue;

            Button btn = buildMenuButton(nav);
            btn.setOnAction(e -> selectNav(nav));

            menuContainer.getChildren().add(btn);
            menuButtons.put(nav, btn);
        }
    }

    private Button buildMenuButton(AdminNav nav) {
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

    // ─── Select / swap content ────────────────────────────────────────────────
    private void selectNav(AdminNav nav) {
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

    // ─── Sidebar collapse toggle ──────────────────────────────────────────────
    @FXML
    private void toggleSidebar() {
        collapsed = !collapsed;
        double target = collapsed ? SIDEBAR_COLLAPSED_WIDTH : SIDEBAR_EXPANDED_WIDTH;

        Timeline tl = new Timeline(new KeyFrame(ANIM_DURATION,
                new KeyValue(sidebar.prefWidthProperty(),  target),
                new KeyValue(sidebar.minWidthProperty(),   target),
                new KeyValue(sidebar.maxWidthProperty(),   target)));
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

    // ─── Logout (with confirmation) ───────────────────────────────────────────
    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.initOwner(logoutButton.getScene().getWindow());
        confirm.setTitle("Sign Out");
        confirm.setHeaderText("Sign out of your admin session?");
        Admin admin = Session.getCurrentAdmin();
        confirm.setContentText(admin == null
                ? "You'll be returned to the login screen."
                : "You'll be returned to the login screen, " + admin.getFullName().split("\\s+")[0] + ".");

        ButtonType signOut = new ButtonType("Sign Out", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel  = new ButtonType("Cancel",   ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(cancel, signOut);

        // Make Cancel the safe default
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
