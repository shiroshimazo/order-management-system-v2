package com.shiro.ordermanagementsystem.Controller.admin;

import com.shiro.ordermanagementsystem.session.Session;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class DashboardController {

    @FXML private Label greetingLabel;
    @FXML private Label dateLabel;

    @FXML private Label kpiOrdersToday;
    @FXML private Label kpiOrdersDelta;
    @FXML private Label kpiPending;
    @FXML private Label kpiRevenueToday;
    @FXML private Label kpiRevenueMtd;
    @FXML private Label kpiLowStock;

    @FXML private LineChart<String, Number> salesChart;
    @FXML private PieChart                  statusChart;
    @FXML private BarChart<String, Number>  topProductsChart;

    @FXML private TableView<RecentOrder>          recentOrdersTable;
    @FXML private TableColumn<RecentOrder, String> colOrderId;
    @FXML private TableColumn<RecentOrder, String> colCustomer;
    @FXML private TableColumn<RecentOrder, String> colAmount;
    @FXML private TableColumn<RecentOrder, String> colStatus;

    @FXML
    public void initialize() {
        populateGreeting();
        populateKpis();
        populateSalesChart();
        populateStatusChart();
        populateTopProducts();
        populateRecentOrders();
    }

    // ─── Greeting ─────────────────────────────────────────────────────────────
    private void populateGreeting() {
        String name = "Admin";
        if (Session.isAdmin() && Session.getCurrentAdmin().getFullName() != null) {
            String full = Session.getCurrentAdmin().getFullName();
            name = full.isBlank() ? "Admin" : full.split(" ")[0];
        }
        greetingLabel.setText("Welcome back, " + name + "!");
        dateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
    }

    // ─── KPI cards (MOCK DATA) ────────────────────────────────────────────────
    private void populateKpis() {
        kpiOrdersToday.setText("34");
        kpiOrdersDelta.setText("▲ 12% vs yesterday");
        kpiPending.setText("8");
        kpiRevenueToday.setText("₱24,380");
        kpiRevenueMtd.setText("MTD: ₱486,120");
        kpiLowStock.setText("5");
    }

    // ─── Sales trend line (MOCK DATA) ─────────────────────────────────────────
    private void populateSalesChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        Random rng = new Random(42);
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M/d");
        for (int i = 29; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            int value = 18_000 + rng.nextInt(15_000) + (29 - i) * 120;
            series.getData().add(new XYChart.Data<>(d.format(fmt), value));
        }
        salesChart.getData().add(series);
    }

    // ─── Orders by status pie (MOCK DATA) ─────────────────────────────────────
    private void populateStatusChart() {
        statusChart.setData(FXCollections.observableArrayList(
                new PieChart.Data("Pending",    8),
                new PieChart.Data("Processing", 14),
                new PieChart.Data("Shipped",    22),
                new PieChart.Data("Delivered",  86),
                new PieChart.Data("Cancelled",  4)
        ));
    }

    // ─── Top products bar (MOCK DATA) ─────────────────────────────────────────
    private void populateTopProducts() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Wireless Mouse", 42));
        series.getData().add(new XYChart.Data<>("USB-C Cable",    38));
        series.getData().add(new XYChart.Data<>("Notebook A5",    31));
        series.getData().add(new XYChart.Data<>("Coffee Mug",     27));
        series.getData().add(new XYChart.Data<>("Desk Lamp",      22));
        topProductsChart.getData().add(series);
    }

    // ─── Recent orders mini-table (MOCK DATA) ─────────────────────────────────
    private void populateRecentOrders() {
        colOrderId.setCellValueFactory( c -> new SimpleStringProperty(c.getValue().orderId()));
        colCustomer.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().customer()));
        colAmount.setCellValueFactory(  c -> new SimpleStringProperty(c.getValue().amount()));
        colStatus.setCellValueFactory(  c -> new SimpleStringProperty(c.getValue().status()));

        colStatus.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll(
                        "status-pending", "status-processing",
                        "status-shipped", "status-delivered", "status-cancelled"
                );
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    getStyleClass().add("status-" + item.toLowerCase());
                }
            }
        });

        recentOrdersTable.setItems(FXCollections.observableArrayList(
                new RecentOrder("ORD-0142", "Juan Dela Cruz", "₱1,250", "Pending"),
                new RecentOrder("ORD-0141", "Maria Santos",   "₱890",   "Shipped"),
                new RecentOrder("ORD-0140", "Pedro Reyes",    "₱2,180", "Delivered"),
                new RecentOrder("ORD-0139", "Ana Cruz",       "₱650",   "Pending"),
                new RecentOrder("ORD-0138", "Luis Garcia",    "₱3,420", "Processing")
        ));
    }

    public record RecentOrder(String orderId, String customer, String amount, String status) {}
}
