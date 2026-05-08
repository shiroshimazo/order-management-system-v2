package com.shiro.ordermanagementsystem.Controller.admin;

import com.shiro.ordermanagementsystem.Category;
import com.shiro.ordermanagementsystem.CategoryDAO;
import com.shiro.ordermanagementsystem.Product;
import com.shiro.ordermanagementsystem.ProductDAO;
import com.shiro.ordermanagementsystem.ui.ProductImages;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

public class ProductFormDialogController {

    @FXML private Label     titleLabel;
    @FXML private TextField skuField;
    @FXML private TextField nameField;
    @FXML private TextArea  descriptionField;
    @FXML private TextField priceField;
    @FXML private TextField stockField;
    @FXML private TextField originalPriceField;
    @FXML private ComboBox<Category> categoryCombo;
    @FXML private CheckBox  activeCheck;
    @FXML private Button    saveButton;
    @FXML private Label     messageLabel;

    // Image picker
    @FXML private StackPane imagePreviewWrap;
    @FXML private ImageView imagePreview;
    @FXML private FontIcon  imagePreviewPlaceholder;
    @FXML private Button    chooseImageButton;
    @FXML private Button    removeImageButton;
    @FXML private Label     imageStatusLabel;

    private static final Category NO_CATEGORY = new Category(-1, "— No category —", null, null);
    private static final long MAX_IMAGE_BYTES = 2L * 1024 * 1024; // 2 MB

    private Product editingProduct;
    private Stage   stage;
    private boolean saved = false;

    /** New bytes the admin picked this session. Null means "no change". */
    private byte[] pendingImageBytes;
    private String pendingImageMime;

    /** Set when the admin clicked "Remove" — wipes existing BLOB on save. */
    private boolean removeExistingImage = false;

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

        showPlaceholder("No image selected");
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
        activeCheck.setSelected(product.isActive());

        if (product.getCategoryId() != null) {
            for (Category c : categoryCombo.getItems()) {
                if (c.getId() == product.getCategoryId()) {
                    categoryCombo.setValue(c);
                    break;
                }
            }
        }

        // Pre-fill the existing photo (if any) into the preview.
        byte[] existing = ProductDAO.loadImageBytes(product.getId());
        if (existing != null && existing.length > 0) {
            Image img = ProductImages.fromBytes(existing, 200, 200);
            if (img != null) {
                showPreview(img, "Current image (" + formatBytes(existing.length) + ") — uploaded");
            }
        }
    }

    // ─── Image picker actions ─────────────────────────────────────────────────
    @FXML
    private void handleChooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose product image");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"),
                new FileChooser.ExtensionFilter("All files", "*.*"));

        java.io.File file = chooser.showOpenDialog(stage);
        if (file == null) return;

        if (file.length() > MAX_IMAGE_BYTES) {
            imageStatusLabel.setText("File is too large (" + formatBytes(file.length())
                    + "). Max " + formatBytes(MAX_IMAGE_BYTES) + ".");
            imageStatusLabel.getStyleClass().removeAll("form-help");
            imageStatusLabel.getStyleClass().add("form-message-error");
            return;
        }

        byte[] bytes;
        try {
            bytes = Files.readAllBytes(Path.of(file.toURI()));
        } catch (IOException ex) {
            imageStatusLabel.setText("Couldn't read file: " + ex.getMessage());
            imageStatusLabel.getStyleClass().removeAll("form-help");
            imageStatusLabel.getStyleClass().add("form-message-error");
            return;
        }

        Image preview = ProductImages.fromBytes(bytes, 200, 200);
        if (preview == null) {
            imageStatusLabel.setText("Not a valid image file.");
            imageStatusLabel.getStyleClass().removeAll("form-help");
            imageStatusLabel.getStyleClass().add("form-message-error");
            return;
        }

        this.pendingImageBytes    = bytes;
        this.pendingImageMime     = guessMime(file.getName());
        this.removeExistingImage  = false;
        showPreview(preview, file.getName() + " (" + formatBytes(bytes.length) + ")");
    }

    @FXML
    private void handleRemoveImage() {
        this.pendingImageBytes   = null;
        this.pendingImageMime    = null;
        this.removeExistingImage = true;
        showPlaceholder("Image will be removed when you save.");
    }

    private void showPreview(Image img, String status) {
        imagePreview.setImage(img);
        imagePreview.setVisible(true);
        imagePreview.setManaged(true);
        imagePreviewPlaceholder.setVisible(false);
        imagePreviewPlaceholder.setManaged(false);
        imageStatusLabel.setText(status);
        imageStatusLabel.getStyleClass().removeAll("form-message-error");
        if (!imageStatusLabel.getStyleClass().contains("form-help"))
            imageStatusLabel.getStyleClass().add("form-help");
        removeImageButton.setDisable(false);
    }

    private void showPlaceholder(String status) {
        imagePreview.setImage(null);
        imagePreview.setVisible(false);
        imagePreview.setManaged(false);
        imagePreviewPlaceholder.setVisible(true);
        imagePreviewPlaceholder.setManaged(true);
        imageStatusLabel.setText(status);
        imageStatusLabel.getStyleClass().removeAll("form-message-error");
        if (!imageStatusLabel.getStyleClass().contains("form-help"))
            imageStatusLabel.getStyleClass().add("form-help");
        removeImageButton.setDisable(true);
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

        boolean ok;
        int productId;
        if (editingProduct == null) {
            // Create row first (without imageUrl), then attach BLOB.
            ok = ProductDAO.create(sku, name, desc, catId, price, originalPrice, stock, active, null);
            if (!ok) { showError("Could not save product. Try again."); return; }
            // Look up the new id by SKU (unique).
            Product justCreated = ProductDAO.findBySku(sku);
            productId = justCreated == null ? -1 : justCreated.getId();
        } else {
            ok = ProductDAO.update(editingProduct.getId(), sku, name, desc, catId,
                                   price, originalPrice, stock, active, null);
            if (!ok) { showError("Could not save product. Try again."); return; }
            productId = editingProduct.getId();
        }

        // Apply any image change.
        if (productId > 0) {
            if (removeExistingImage) {
                ProductDAO.clearImage(productId);
                ProductImages.invalidate(productId);
            } else if (pendingImageBytes != null) {
                ProductDAO.setImage(productId, pendingImageBytes, pendingImageMime);
                ProductImages.invalidate(productId);
            }
        }

        saved = true;
        if (stage != null) stage.close();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    private static String guessMime(String fileName) {
        String n = fileName.toLowerCase();
        if (n.endsWith(".png"))  return "image/png";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".gif"))  return "image/gif";
        if (n.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }

    private static String formatBytes(long n) {
        if (n < 1024) return n + " B";
        if (n < 1024 * 1024) return String.format("%.1f KB", n / 1024.0);
        return String.format("%.2f MB", n / (1024.0 * 1024.0));
    }

    private void showError(String msg) {
        messageLabel.setText(msg);
        messageLabel.getStyleClass().removeAll("form-message-error", "form-message-success");
        messageLabel.getStyleClass().add("form-message-error");
        messageLabel.setManaged(true);
        messageLabel.setVisible(true);
    }
}
