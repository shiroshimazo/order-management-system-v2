package com.shiro.ordermanagementsystem.Controller.admin;

import com.shiro.ordermanagementsystem.Category;
import com.shiro.ordermanagementsystem.CategoryDAO;
import com.shiro.ordermanagementsystem.Product;
import com.shiro.ordermanagementsystem.ProductDAO;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.text.DecimalFormat;
import java.util.Optional;

public class ProductsController {

    @FXML private TextField         searchField;
    @FXML private ComboBox<Category> categoryFilter;

    @FXML private TableView<Product>          productsTable;
    @FXML private TableColumn<Product, String> colSku;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, String> colCategory;
    @FXML private TableColumn<Product, String> colPrice;
    @FXML private TableColumn<Product, String> colStock;
    @FXML private TableColumn<Product, String> colStatus;
    @FXML private TableColumn<Product, Void>   colActions;

    @FXML private Label countLabel;

    private static final Category ALL_CATEGORIES = new Category(-1, "All Categories", null, null);
    private static final DecimalFormat PRICE_FMT = new DecimalFormat("₱#,##0.00");

    @FXML
    public void initialize() {
        setupTable();
        loadCategories();
        reload();

        searchField.textProperty().addListener((o, a, b) -> reload());
        categoryFilter.valueProperty().addListener((o, a, b) -> reload());
    }

    // ─── Table setup ──────────────────────────────────────────────────────────
    private void setupTable() {
        colSku.setCellValueFactory(      c -> new SimpleStringProperty(c.getValue().getSku()));
        colName.setCellValueFactory(     c -> new SimpleStringProperty(c.getValue().getName()));
        colCategory.setCellValueFactory( c -> new SimpleStringProperty(
                c.getValue().getCategoryName() == null ? "—" : c.getValue().getCategoryName()));
        colPrice.setCellValueFactory(    c -> new SimpleStringProperty(
                c.getValue().getPrice() == null ? "" : PRICE_FMT.format(c.getValue().getPrice())));
        colStock.setCellValueFactory(    c -> new SimpleStringProperty(String.valueOf(c.getValue().getStock())));
        colStatus.setCellValueFactory(   c -> new SimpleStringProperty(c.getValue().isActive() ? "Active" : "Inactive"));

        colStatus.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("status-delivered", "status-cancelled");
                if (empty || item == null) { setText(null); return; }
                setText(item);
                getStyleClass().add("Active".equals(item) ? "status-delivered" : "status-cancelled");
            }
        });

        colActions.setCellFactory(tc -> new TableCell<>() {
            private final Button editBtn   = makeIconButton("fas-pen",   "#3a72e8");
            private final Button deleteBtn = makeIconButton("fas-trash", "#D92A1C");
            private final HBox   box       = new HBox(6, editBtn, deleteBtn);

            {
                editBtn.setOnAction(e -> {
                    Product p = getTableView().getItems().get(getIndex());
                    openProductDialog(p);
                });
                deleteBtn.setOnAction(e -> {
                    Product p = getTableView().getItems().get(getIndex());
                    confirmAndDelete(p);
                });
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private static Button makeIconButton(String icon, String color) {
        FontIcon fi = new FontIcon(icon);
        fi.setIconSize(12);
        fi.setIconColor(javafx.scene.paint.Color.web(color));
        Button b = new Button();
        b.setGraphic(fi);
        b.getStyleClass().add("table-action-button");
        return b;
    }

    // ─── Data loading ─────────────────────────────────────────────────────────
    private void loadCategories() {
        ObservableList<Category> items = FXCollections.observableArrayList();
        items.add(ALL_CATEGORIES);
        items.addAll(CategoryDAO.findAll());
        Category previous = categoryFilter.getValue();
        categoryFilter.setItems(items);
        categoryFilter.setValue(previous != null && items.contains(previous) ? previous : ALL_CATEGORIES);
    }

    private void reload() {
        String q = searchField.getText();
        Category selected = categoryFilter.getValue();
        Integer categoryId = (selected == null || selected == ALL_CATEGORIES) ? null : selected.getId();

        ObservableList<Product> items = FXCollections.observableArrayList(ProductDAO.search(q, categoryId));
        productsTable.setItems(items);
        countLabel.setText(items.size() + (items.size() == 1 ? " product" : " products"));
    }

    // ─── Actions ──────────────────────────────────────────────────────────────
    @FXML private void handleRefresh()           { loadCategories(); reload(); }
    @FXML private void handleAddProduct()        { openProductDialog(null); }

    @FXML
    private void handleManageCategories() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ordermanagementsystem/fxml/admin/CategoryManagementDialog.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Manage Categories");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(productsTable.getScene().getWindow());
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource(
                    "/ordermanagementsystem/css/admin.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();

            loadCategories();
            reload();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openProductDialog(Product product) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ordermanagementsystem/fxml/admin/ProductFormDialog.fxml"));
            Parent root = loader.load();
            ProductFormDialogController ctrl = loader.getController();
            ctrl.setProduct(product);

            Stage stage = new Stage();
            stage.setTitle(product == null ? "Add Product" : "Edit Product");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(productsTable.getScene().getWindow());
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource(
                    "/ordermanagementsystem/css/admin.css").toExternalForm());
            stage.setScene(scene);
            ctrl.setStage(stage);
            stage.showAndWait();

            if (ctrl.wasSaved()) reload();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void confirmAndDelete(Product p) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Product");
        alert.setHeaderText("Delete \"" + p.getName() + "\"?");
        alert.setContentText("This cannot be undone.");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (ProductDAO.delete(p.getId())) {
                Platform.runLater(this::reload);
            } else {
                new Alert(Alert.AlertType.ERROR, "Could not delete product.").showAndWait();
            }
        }
    }
}
