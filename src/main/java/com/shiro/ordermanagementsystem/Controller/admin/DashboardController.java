package com.shiro.ordermanagementsystem.Controller.admin;

import com.shiro.ordermanagementsystem.Order;
import com.shiro.ordermanagementsystem.OrderDAO;
import com.shiro.ordermanagementsystem.OrderStatus;
import com.shiro.ordermanagementsystem.ProductDAO;
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

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

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

    @FXML private TableView<Order>           recentOrdersTable;
    @FXML private TableColumn<Order, String> colOrderId;
    @FXML private TableColumn<Order, String> colCustomer;
    @FXML private TableColumn<Order, String> colAmount;
    @FXML private TableColumn<Order, String> colStatus;

    private static final DecimalFormat MONEY       = new DecimalFormat("₱#,##0.00");
    private static final DecimalFormat MONEY_SHORT = new DecimalFormat("₱#,##0");
    private static final int LOW_STOCK_THRESHOLD   = 5;

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

    // ─── KPI cards (LIVE DATA) ────────────────────────────────────────────────
    private void populateKpis() {
        LocalDate today     = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate mtdStart  = today.withDayOfMonth(1);

        int todayCount     = OrderDAO.countOn(today);
        int yesterdayCount = OrderDAO.countOn(yesterday);
        int pending        = OrderDAO.countByStatus(OrderStatus.PENDING);
        BigDecimal revToday = OrderDAO.revenueOn(today);
        BigDecimal revMtd   = OrderDAO.revenueBetween(mtdStart, today);
        int lowStock       = ProductDAO.countLowStock(LOW_STOCK_THRESHOLD);

        kpiOrdersToday.setText(String.valueOf(todayCount));
        kpiOrdersDelta.setText(formatDelta(todayCount, yesterdayCount));
        styleDelta(kpiOrdersDelta, todayCount - yesterdayCount);

        kpiPending.setText(String.valueOf(pending));
        kpiRevenueToday.setText(MONEY.format(revToday));
        kpiRevenueMtd.setText("MTD: " + MONEY_SHORT.format(revMtd));
        kpiLowStock.setText(String.valueOf(lowStock));
    }

    private static String formatDelta(int now, int prev) {
        if (prev == 0) return now == 0 ? "— vs yesterday" : "▲ new today";
        int diff = now - prev;
        double pct = (diff * 100.0) / prev;
        String arrow = diff > 0 ? "▲" : (diff < 0 ? "▼" : "—");
        return String.format("%s %.0f%% vs yesterday", arrow, Math.abs(pct));
    }

    private static void styleDelta(Label label, int diff) {
        label.getStyleClass().removeAll("kpi-delta-up", "kpi-delta-down", "kpi-delta-neutral");
        if (diff > 0)      label.getStyleClass().add("kpi-delta-up");
        else if (diff < 0) label.getStyleClass().add("kpi-delta-down");
        else               label.getStyleClass().add("kpi-delta-neutral");
    }

    // ─── Sales trend (last 30 days) ───────────────────────────────────────────
    private void populateSalesChart() {
        LocalDate today = LocalDate.now();
        LocalDate from  = today.minusDays(29);

        Map<LocalDate, BigDecimal> series = OrderDAO.salesByDay(from, today);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M/d");

        XYChart.Series<String, Number> data = new XYChart.Series<>();
        for (Map.Entry<LocalDate, BigDecimal> e : series.entrySet()) {
            data.getData().add(new XYChart.Data<>(e.getKey().format(fmt), e.getValue()));
        }
        salesChart.getData().clear();
        salesChart.getData().add(data);
    }

    // ─── Orders by status pie ─────────────────────────────────────────────────
    private void populateStatusChart() {
        Map<OrderStatus, Integer> byStatus = OrderDAO.ordersByStatus();
        var data = FXCollections.<PieChart.Data>observableArrayList();
        for (Map.Entry<OrderStatus, Integer> e : byStatus.entrySet()) {
            if (e.getValue() > 0) data.add(new PieChart.Data(e.getKey().label(), e.getValue()));
        }
        if (data.isEmpty()) data.add(new PieChart.Data("No orders yet", 1));
        statusChart.setData(data);
    }

    // ─── Top products this month ──────────────────────────────────────────────
    private void populateTopProducts() {
        LocalDate today    = LocalDate.now();
        LocalDate mtdStart = today.withDayOfMonth(1);
        List<OrderDAO.TopProduct> top = OrderDAO.topProductsBetween(mtdStart, today, 5);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (OrderDAO.TopProduct p : top) {
            series.getData().add(new XYChart.Data<>(p.name(), p.sold()));
        }
        topProductsChart.getData().clear();
        topProductsChart.getData().add(series);
    }

    // ─── Recent orders table ──────────────────────────────────────────────────
    private void populateRecentOrders() {
        colOrderId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOrderCode()));
        colCustomer.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCustomerName() == null ? "—" : c.getValue().getCustomerName()));
        colAmount.setCellValueFactory(c -> new SimpleStringProperty(MONEY.format(c.getValue().getTotal())));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().label()));

        colStatus.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("status-pending", "status-processing",
                        "status-shipped", "status-delivered", "status-cancelled");
                if (empty || item == null) { setText(null); return; }
                setText(item);
                getStyleClass().add("status-" + item.toLowerCase());
            }
        });

        recentOrdersTable.setItems(FXCollections.observableArrayList(OrderDAO.recent(5)));
    }
}
