package com.shiro.ordermanagementsystem.Controller.customer;

import com.shiro.ordermanagementsystem.Customer;
import com.shiro.ordermanagementsystem.FeedbackDAO;
import com.shiro.ordermanagementsystem.session.Session;
import com.shiro.ordermanagementsystem.ui.Toast;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class FeedbackController {

    @FXML private HBox      starsRow;
    @FXML private TextField subjectField;
    @FXML private TextArea  messageField;
    @FXML private Button    submitButton;
    @FXML private Label     messageLabel;
    @FXML private VBox      historyList;
    @FXML private Label     historyEmpty;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");
    private int rating = 5;

    @FXML
    public void initialize() {
        buildStars();
        loadHistory();
    }

    private void buildStars() {
        starsRow.getChildren().clear();
        for (int i = 1; i <= 5; i++) {
            int value = i;
            Button b = new Button();
            FontIcon fi = new FontIcon("fas-star");
            fi.setIconSize(20);
            b.setGraphic(fi);
            b.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;");
            b.setOnAction(e -> { rating = value; paintStars(); });
            starsRow.getChildren().add(b);
        }
        paintStars();
    }

    private void paintStars() {
        for (int i = 0; i < starsRow.getChildren().size(); i++) {
            Button b = (Button) starsRow.getChildren().get(i);
            FontIcon fi = (FontIcon) b.getGraphic();
            fi.setIconColor(i < rating
                    ? javafx.scene.paint.Color.web("#f0a020")
                    : javafx.scene.paint.Color.web("#d1d5db"));
        }
    }

    @FXML
    private void handleSubmit() {
        Customer c = Session.getCurrentCustomer();
        if (c == null) { showError("You are not signed in."); return; }
        String message = messageField.getText() == null ? "" : messageField.getText().trim();
        String subject = subjectField.getText() == null ? "" : subjectField.getText().trim();

        if (message.isEmpty()) { showError("Please write a message before submitting."); return; }

        submitButton.setDisable(true);
        boolean ok = FeedbackDAO.submit(c.getId(), rating, subject, message);
        submitButton.setDisable(false);

        if (!ok) {
            showError("Could not submit feedback. Please try again.");
            return;
        }

        Toast.success(starsRow, "Thanks for the feedback!");
        subjectField.clear();
        messageField.clear();
        rating = 5;
        paintStars();
        hideMessage();
        loadHistory();
    }

    private void loadHistory() {
        Customer c = Session.getCurrentCustomer();
        historyList.getChildren().clear();
        if (c == null) {
            historyEmpty.setVisible(true);
            historyEmpty.setManaged(true);
            return;
        }
        List<FeedbackDAO.Entry> entries = FeedbackDAO.findByCustomer(c.getId());
        boolean empty = entries.isEmpty();
        historyEmpty.setVisible(empty);
        historyEmpty.setManaged(empty);

        for (FeedbackDAO.Entry e : entries) historyList.getChildren().add(buildEntryCard(e));
    }

    private VBox buildEntryCard(FeedbackDAO.Entry e) {
        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);
        for (int i = 1; i <= 5; i++) {
            FontIcon fi = new FontIcon("fas-star");
            fi.setIconSize(11);
            fi.setIconColor(i <= e.rating()
                    ? javafx.scene.paint.Color.web("#f0a020")
                    : javafx.scene.paint.Color.web("#d1d5db"));
            top.getChildren().add(fi);
        }
        if (e.subject() != null && !e.subject().isBlank()) {
            Label s = new Label(e.subject());
            s.getStyleClass().add("invoice-line-name");
            top.getChildren().add(s);
        }
        Label when = new Label(e.createdAt() == null ? "" : e.createdAt().format(DT));
        when.getStyleClass().add("dashboard-subgreeting");
        HBox.setHgrow(when, javafx.scene.layout.Priority.ALWAYS);
        when.setStyle("-fx-alignment: CENTER_RIGHT;");
        when.setMaxWidth(Double.MAX_VALUE);
        top.getChildren().add(when);

        Label body = new Label(e.message());
        body.setWrapText(true);
        body.getStyleClass().add("invoice-line-meta");

        VBox card = new VBox(6, top, body);
        card.getStyleClass().add("settings-card");
        card.setStyle("-fx-padding: 12 14 12 14;");
        return card;
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
}
