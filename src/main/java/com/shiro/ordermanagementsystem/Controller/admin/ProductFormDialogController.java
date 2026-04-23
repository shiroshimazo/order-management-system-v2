package com.shiro.ordermanagementsystem.Controller.admin;

import com.shiro.ordermanagementsystem.Category;
import com.shiro.ordermanagementsystem.CategoryDAO;
import com.shiro.ordermanagementsystem.Product;
import com.shiro.ordermanagementsystem.ProductDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.math.BigDecimal;

public class ProductFormDialogController {

    @FXML private Label     titleLabel;
    @FXML private TextField skuField;
    @FXML private TextField nameField;
    @FXML private TextArea  descriptionField;
    @FXML private TextField priceField;
    @FXML private TextField stockField;
    @FXML private TextField originalPriceField;
    @FXML private TextField imageUrlField;
    @FXML private ComboBox<Category> categoryCombo;
    @FXML private CheckBox  activeCheck;
    @FXML private Button    saveButton;
    @FXML private Label     messageLabel;

    private static final Category NO_CATEGORY = new Category(-1, "— No category —", null, null);

    private Product editingProduct;
    private Stage   stage;
    private boolean saved = false;

    @FXML
    public void initialize() {
        ObservableList<Category> cats = FXCollections.observableArrayList();
        cats.add(NO_CATEGORY);
        cats.addAll(CategoryDAO.findAll());
        categoryCombo.setItems(cats);
        categoryCombo.setValue(NO_CATEGORY);

        categoryCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Category c) { return c == null ? "" : c.getName(); }
            @Override public Category fromString(String s) { return null; }
        });
    }

    public void setStage(Stage stage)       { this.stage = stage; }
    public boolean wasSaved()               { return saved; }

    public void setProduct(Product product) {
        this.editingProduct = product;
        if (product == null) {
            titleLabel.setText("Add Product");
            saveButton.setText("Create");
            return;
        }
        titleLabel.setText("Edit Product");
        saveButton.setText("Save Changes");

        skuField.setText(product.getSku());
        nameField.setText(product.getName());
        descriptionField.setText(product.getDescription() == null ? "" : product.getDescription());
        priceField.setText(product.getPrice() == null ? "" : product.getPrice().toPlainString());
        stockField.setText(String.valueOf(product.getStock()));
        originalPriceField.setText(product.getOriginalPrice() == null ? "" : product.getOriginalPrice().toPlainString());
        imageUrlField.setText(product.getImageUrl() == null ? "" : product.getImageUrl());
        activeCheck.setSelected(product.isActive());

        if (product.getCategoryId() != null) {
            for (Category c : categoryCombo.getItems()) {
                if (c.getId() == product.getCategoryId()) {
                    categoryCombo.setValue(c);
                    break;
                }
            }
        }
    }

    @FXML
    private void handleCancel() {
        if (stage != null) stage.close();
    }

    @FXML
    private void handleSave() {
        String sku  = skuField.getText().trim();
        String name = nameField.getText().trim();
        String desc = descriptionField.getText().trim();
        String img  = imageUrlField.getText() == null ? "" : imageUrlField.getText().trim();

        if (sku.isEmpty())  { showError("SKU is required."); return; }
        if (name.isEmpty()) { showError("Name is required."); return; }

        BigDecimal price;
        try {
            price = new BigDecimal(priceField.getText().trim());
            if (price.signum() < 0) { showError("Price cannot be negative."); return; }
        } catch (NumberFormatException e) {
            showError("Enter a valid price (e.g. 199.00).");
            return;
        }

        BigDecimal originalPrice = null;
        String originalRaw = originalPriceField.getText() == null ? "" : originalPriceField.getText().trim();
        if (!originalRaw.isEmpty()) {
            try {
                originalPrice = new BigDecimal(originalRaw);
                if (originalPrice.signum() < 0) { showError("Original price cannot be negative."); return; }
                if (originalPrice.compareTo(price) <= 0) {
                    showError("Original price should be higher than the current price (used for strike-through).");
                    return;
                }
            } catch (NumberFormatException e) {
                showError("Enter a valid original price.");
                return;
            }
        }

        int stock;
        try {
            stock = Integer.parseInt(stockField.getText().trim());
            if (stock < 0) { showError("Stock cannot be negative."); return; }
        } catch (NumberFormatException e) {
            showError("Enter a valid stock quantity.");
            return;
        }

        int excludeId = editingProduct == null ? -1 : editingProduct.getId();
        if (ProductDAO.skuExistsExcept(sku, excludeId)) {
            showError("A product with that SKU already exists.");
            return;
        }

        Category selected = categoryCombo.getValue();
        Integer catId = (selected == null || selected == NO_CATEGORY) ? null : selected.getId();
        boolean active = activeCheck.isSelected();
        String imageOrNull = img.isEmpty() ? null : img;

        boolean ok;
        if (editingProduct == null) {
            ok = ProductDAO.create(sku, name, desc, catId, price, originalPrice, stock, active, imageOrNull);
        } else {
            ok = ProductDAO.update(editingProduct.getId(), sku, name, desc, catId,
                                   price, originalPrice, stock, active, imageOrNull);
        }

        if (!ok) { showError("Could not save product. Try again."); return; }

        saved = true;
        if (stage != null) stage.close();
    }

    private void showError(String msg) {
        messageLabel.setText(msg);
        messageLabel.getStyleClass().removeAll("form-message-error", "form-message-success");
        messageLabel.getStyleClass().add("form-message-error");
        messageLabel.setManaged(true);
        messageLabel.setVisible(true);
    }
}
