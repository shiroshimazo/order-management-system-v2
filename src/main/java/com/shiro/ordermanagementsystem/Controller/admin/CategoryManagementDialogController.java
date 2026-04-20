package com.shiro.ordermanagementsystem.Controller.admin;

import com.shiro.ordermanagementsystem.Category;
import com.shiro.ordermanagementsystem.CategoryDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Optional;

public class CategoryManagementDialogController {

    @FXML private TextField nameField;
    @FXML private TextField descriptionField;
    @FXML private Button    saveCategoryButton;
    @FXML private Button    clearButton;
    @FXML private Label     messageLabel;

    @FXML private TableView<Category>          categoriesTable;
    @FXML private TableColumn<Category, String> colName;
    @FXML private TableColumn<Category, String> colDescription;
    @FXML private TableColumn<Category, Void>   colActions;

    private Category editingCategory;

    @FXML
    public void initialize() {
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colDescription.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDescription() == null ? "" : c.getValue().getDescription()));

        colActions.setCellFactory(tc -> new TableCell<>() {
            private final Button editBtn   = iconBtn("fas-pen",   "#3a72e8");
            private final Button deleteBtn = iconBtn("fas-trash", "#D92A1C");
            private final HBox   box       = new HBox(6, editBtn, deleteBtn);

            {
                editBtn.setOnAction(e -> beginEdit(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> confirmDelete(getTableView().getItems().get(getIndex())));
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        reload();
    }

    private static Button iconBtn(String icon, String color) {
        FontIcon fi = new FontIcon(icon);
        fi.setIconSize(12);
        fi.setIconColor(javafx.scene.paint.Color.web(color));
        Button b = new Button();
        b.setGraphic(fi);
        b.getStyleClass().add("table-action-button");
        return b;
    }

    private void reload() {
        categoriesTable.setItems(FXCollections.observableArrayList(CategoryDAO.findAll()));
    }

    private void beginEdit(Category c) {
        editingCategory = c;
        nameField.setText(c.getName());
        descriptionField.setText(c.getDescription() == null ? "" : c.getDescription());
        saveCategoryButton.setText("Update");
        hideMessage();
    }

    @FXML
    private void handleSaveCategory() {
        String name = nameField.getText().trim();
        String desc = descriptionField.getText().trim();

        if (name.isEmpty()) { showError("Name is required."); return; }

        int excludeId = editingCategory == null ? -1 : editingCategory.getId();
        if (CategoryDAO.nameExistsExcept(name, excludeId)) {
            showError("A category with that name already exists.");
            return;
        }

        boolean ok;
        if (editingCategory == null) {
            ok = CategoryDAO.create(name, desc);
        } else {
            ok = CategoryDAO.update(editingCategory.getId(), name, desc);
        }

        if (!ok) { showError("Could not save category."); return; }

        showSuccess(editingCategory == null ? "Category added." : "Category updated.");
        handleClear();
        reload();
    }

    @FXML
    private void handleClear() {
        editingCategory = null;
        nameField.clear();
        descriptionField.clear();
        saveCategoryButton.setText("Add");
    }

    @FXML
    private void handleClose() {
        ((javafx.stage.Stage) nameField.getScene().getWindow()).close();
    }

    private void confirmDelete(Category c) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Category");
        alert.setHeaderText("Delete \"" + c.getName() + "\"?");
        alert.setContentText("Products using this category will become uncategorised.");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (CategoryDAO.delete(c.getId())) {
                if (editingCategory != null && editingCategory.getId() == c.getId()) handleClear();
                reload();
            } else {
                new Alert(Alert.AlertType.ERROR, "Could not delete category.").showAndWait();
            }
        }
    }

    private void showError(String msg) {
        messageLabel.setText(msg);
        messageLabel.getStyleClass().removeAll("form-message-error", "form-message-success");
        messageLabel.getStyleClass().add("form-message-error");
        messageLabel.setManaged(true);
        messageLabel.setVisible(true);
    }

    private void showSuccess(String msg) {
        messageLabel.setText(msg);
        messageLabel.getStyleClass().removeAll("form-message-error", "form-message-success");
        messageLabel.getStyleClass().add("form-message-success");
        messageLabel.setManaged(true);
        messageLabel.setVisible(true);
    }

    private void hideMessage() {
        messageLabel.setText("");
        messageLabel.setManaged(false);
        messageLabel.setVisible(false);
    }
}
