package com.shiro.ordermanagementsystem.Controller.customer;

import com.shiro.ordermanagementsystem.Category;
import com.shiro.ordermanagementsystem.CategoryDAO;
import com.shiro.ordermanagementsystem.Product;
import com.shiro.ordermanagementsystem.ProductDAO;
import com.shiro.ordermanagementsystem.session.Cart;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import org.kordamp.ikonli.javafx.FontIcon;

import java.text.DecimalFormat;
import java.util.stream.Collectors;

public class BrowseController {

    @FXML private TextField           searchField;
    @FXML private ComboBox<Category>  categoryFilter;
    @FXML private TableView<Product>  productsTable;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, String> colCategory;
    @FXML private TableColumn<Product, String> colPrice;
    @FXML private TableColumn<Product, String> colStock;
    @FXML private TableColumn<Product, Void>   colActions;
    @FXML private Label               countLabel;

    private static final Category ALL = new Category(-1, "All Categories", null, null);
    private static final DecimalFormat PRICE_FMT = new DecimalFormat("₱#,##0.00");

    @FXML
    public void initialize() {
        setupTable();
        loadCategories();
        reload();

        searchField.textProperty().addListener((o, a, b) -> reload());
        categoryFilter.valueProperty().addListener((o, a, b) -> reload());
    }

    private void setupTable() {
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colCategory.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCategoryName() == null ? "—" : c.getValue().getCategoryName()));
        colPrice.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getPrice() == null ? "" : PRICE_FMT.format(c.getValue().getPrice())));
        colStock.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getStock())));

        colActions.setCellFactory(tc -> new TableCell<>() {
            private final Spinner<Integer> qty = new Spinner<>(1, 99, 1);
            private final Button addBtn = makeAddBtn();
            private final HBox box = new HBox(8, qty, addBtn);

            {
                qty.setPrefWidth(72);
                box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                addBtn.setOnAction(e -> {
                    Product p = getTableView().getItems().get(getIndex());
                    int q = qty.getValue();
                    if (p.getStock() <= 0) {
                        new Alert(Alert.AlertType.WARNING, "Out of stock.").showAndWait();
                        return;
                    }
                    if (q > p.getStock()) {
                        new Alert(Alert.AlertType.WARNING, "Only " + p.getStock() + " available.").showAndWait();
                        return;
                    }
                    Cart.add(p, q);
                    com.shiro.ordermanagementsystem.Controller.customer.CustomerHomeController.navigate(
                            com.shiro.ordermanagementsystem.nav.CustomerNav.FOOD_ORDER);
                });
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Product p = getTableView().getItems().get(getIndex());
                addBtn.setDisable(p.getStock() <= 0);
                qty.getValueFactory().setValue(1);
                ((SpinnerValueFactory.IntegerSpinnerValueFactory) qty.getValueFactory())
                        .setMax(Math.max(1, p.getStock()));
                setGraphic(box);
            }
        });
    }

    private static Button makeAddBtn() {
        FontIcon fi = new FontIcon("fas-cart-plus");
        fi.setIconSize(11);
        fi.setIconColor(javafx.scene.paint.Color.web("#ffffff"));
        Label lbl = new Label("Add");
        lbl.setStyle("-fx-text-fill: #ffffff;");
        HBox g = new HBox(6, fi, lbl);
        g.setAlignment(javafx.geometry.Pos.CENTER);
        Button b = new Button();
        b.setGraphic(g);
        b.getStyleClass().add("button-primary");
        return b;
    }

    private void loadCategories() {
        ObservableList<Category> items = FXCollections.observableArrayList();
        items.add(ALL);
        items.addAll(CategoryDAO.findAll());
        categoryFilter.setItems(items);
        categoryFilter.setValue(ALL);
        categoryFilter.setConverter(new StringConverter<>() {
            @Override public String toString(Category c) { return c == null ? "" : c.getName(); }
            @Override public Category fromString(String s) { return null; }
        });
    }

    private void reload() {
        String q = searchField.getText();
        Category sel = categoryFilter.getValue();
        Integer catId = (sel == null || sel == ALL) ? null : sel.getId();

        // Show only active products
        var active = ProductDAO.search(q, catId).stream()
                .filter(Product::isActive)
                .collect(Collectors.toList());

        ObservableList<Product> items = FXCollections.observableArrayList(active);
        productsTable.setItems(items);
        countLabel.setText(items.size() + (items.size() == 1 ? " product" : " products"));
    }

    @FXML private void handleRefresh() { loadCategories(); reload(); }
}
