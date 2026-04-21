package com.shiro.ordermanagementsystem.Controller.admin;

import com.shiro.ordermanagementsystem.OrderDAO;
import com.shiro.ordermanagementsystem.OrderStatus;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class ReportsController {

    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;

    @FXML private Label kpiOrders;
    @FXML private Label kpiRevenue;
    @FXML private Label kpiAov;
    @FXML private Label kpiItems;

    @FXML private LineChart<String, Number> salesChart;
    @FXML private CategoryAxis               salesXAxis;
    @FXML private NumberAxis                 salesYAxis;
    @FXML private PieChart                   statusChart;

    @FXML private TableView<OrderDAO.TopProduct>            topProductsTable;
    @FXML private TableColumn<OrderDAO.TopProduct, String>  colName;
    @FXML private TableColumn<OrderDAO.TopProduct, String>  colSku;
    @FXML private TableColumn<OrderDAO.TopProduct, Number>  colSold;
    @FXML private TableColumn<OrderDAO.TopProduct, String>  colRevenue;

    private static final DecimalFormat MONEY    = new DecimalFormat("₱#,##0.00");
    private static final DateTimeFormatter AXIS = DateTimeFormatter.ofPattern("M/d");
    private static final DateTimeFormatter FILE = DateTimeFormatter.ofPattern("yyyyMMdd");

    @FXML
    public void initialize() {
        configureTopProductsTable();

        // Default range: last 30 days
        LocalDate today = LocalDate.now();
        fromDate.setValue(today.minusDays(29));
        toDate.setValue(today);

        reload();
    }

    // ─── Quick ranges ─────────────────────────────────────────────────────────
    @FXML private void handleQuickToday()    { applyRange(LocalDate.now(),               LocalDate.now()); }
    @FXML private void handleQuickLast7()    { applyRange(LocalDate.now().minusDays(6),  LocalDate.now()); }
    @FXML private void handleQuickLast30()   { applyRange(LocalDate.now().minusDays(29), LocalDate.now()); }
    @FXML private void handleQuickThisMonth(){ applyRange(LocalDate.now().withDayOfMonth(1), LocalDate.now()); }
    @FXML private void handleApply()         { reload(); }

    private void applyRange(LocalDate from, LocalDate to) {
        fromDate.setValue(from);
        toDate.setValue(to);
        reload();
    }

    // ─── Reload everything ────────────────────────────────────────────────────
    private void reload() {
        LocalDate from = fromDate.getValue();
        LocalDate to   = toDate.getValue();
        if (from == null || to == null) return;
        if (from.isAfter(to)) {
            LocalDate tmp = from; from = to; to = tmp;
            fromDate.setValue(from);
            toDate.setValue(to);
        }

        populateKpis(from, to);
        populateSalesChart(from, to);
        populateStatusChart(from, to);
        populateTopProducts(from, to);
    }

    private void populateKpis(LocalDate from, LocalDate to) {
        int        orders  = OrderDAO.countBetween(from, to);
        BigDecimal revenue = OrderDAO.revenueBetween(from, to);
        int        items   = OrderDAO.itemsSoldBetween(from, to);
        BigDecimal aov     = orders == 0
                ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(orders), 2, RoundingMode.HALF_UP);

        kpiOrders.setText(String.valueOf(orders));
        kpiRevenue.setText(MONEY.format(revenue));
        kpiAov.setText(MONEY.format(aov));
        kpiItems.setText(String.valueOf(items));
    }

    private void populateSalesChart(LocalDate from, LocalDate to) {
        Map<LocalDate, BigDecimal> series = OrderDAO.salesByDay(from, to);

        XYChart.Series<String, Number> data = new XYChart.Series<>();
        for (Map.Entry<LocalDate, BigDecimal> e : series.entrySet()) {
            data.getData().add(new XYChart.Data<>(e.getKey().format(AXIS), e.getValue()));
        }
        salesChart.getData().clear();
        salesChart.getData().add(data);
    }

    private void populateStatusChart(LocalDate from, LocalDate to) {
        Map<OrderStatus, Integer> byStatus = OrderDAO.ordersByStatusBetween(from, to);
        var data = FXCollections.<PieChart.Data>observableArrayList();
        for (Map.Entry<OrderStatus, Integer> e : byStatus.entrySet()) {
            if (e.getValue() > 0) data.add(new PieChart.Data(e.getKey().label(), e.getValue()));
        }
        if (data.isEmpty()) data.add(new PieChart.Data("No orders in range", 1));
        statusChart.setData(data);
    }

    private void configureTopProductsTable() {
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().name()));
        colSku.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().sku() == null ? "—" : c.getValue().sku()));
        colSold.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().sold()));
        colRevenue.setCellValueFactory(c -> new SimpleStringProperty(MONEY.format(c.getValue().revenue())));
    }

    private void populateTopProducts(LocalDate from, LocalDate to) {
        List<OrderDAO.TopProduct> top = OrderDAO.topProductsBetween(from, to, 10);
        topProductsTable.setItems(FXCollections.observableArrayList(top));
    }

    // ─── Export CSV ───────────────────────────────────────────────────────────
    @FXML
    private void handleExportCsv() {
        LocalDate from = fromDate.getValue();
        LocalDate to   = toDate.getValue();
        if (from == null || to == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Report CSV");
        chooser.setInitialFileName(
                "report_" + from.format(FILE) + "_" + to.format(FILE) + ".csv");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV files", "*.csv"));

        Stage owner = null;
        Node any = kpiOrders;
        if (any != null && any.getScene() != null) owner = (Stage) any.getScene().getWindow();

        File target = chooser.showSaveDialog(owner);
        if (target == null) return;

        try (BufferedWriter w = new BufferedWriter(new FileWriter(target))) {
            w.write("Report Range," + from + "," + to);
            w.newLine();
            w.newLine();

            w.write("Summary");            w.newLine();
            w.write("Orders,"   + kpiOrders.getText());  w.newLine();
            w.write("Revenue,"  + kpiRevenue.getText()); w.newLine();
            w.write("Avg Order Value," + kpiAov.getText()); w.newLine();
            w.write("Items Sold,"  + kpiItems.getText());   w.newLine();
            w.newLine();

            w.write("Revenue By Day"); w.newLine();
            w.write("Date,Revenue");   w.newLine();
            Map<LocalDate, BigDecimal> series = OrderDAO.salesByDay(from, to);
            for (Map.Entry<LocalDate, BigDecimal> e : series.entrySet()) {
                w.write(e.getKey() + "," + e.getValue().toPlainString());
                w.newLine();
            }
            w.newLine();

            w.write("Orders By Status"); w.newLine();
            w.write("Status,Count");     w.newLine();
            Map<OrderStatus, Integer> byStatus = OrderDAO.ordersByStatusBetween(from, to);
            for (Map.Entry<OrderStatus, Integer> e : byStatus.entrySet()) {
                w.write(e.getKey().label() + "," + e.getValue());
                w.newLine();
            }
            w.newLine();

            w.write("Top Products By Revenue"); w.newLine();
            w.write("Product,SKU,Sold,Revenue"); w.newLine();
            for (OrderDAO.TopProduct p : OrderDAO.topProductsBetween(from, to, 10)) {
                w.write(csv(p.name()) + "," + csv(p.sku()) + "," + p.sold() + "," + p.revenue().toPlainString());
                w.newLine();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static String csv(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
