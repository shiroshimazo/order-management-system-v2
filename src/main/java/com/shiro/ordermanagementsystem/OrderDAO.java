package com.shiro.ordermanagementsystem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {

    /** Philippine VAT applied on top of the listed price. */
    public static final BigDecimal VAT_RATE = new BigDecimal("0.12");

    private static final String BASE_SELECT =
            "SELECT o.*, uc.full_name AS customer_name " +
            "FROM customer_order o " +
            "LEFT JOIN user_customer uc ON o.customer_id = uc.id ";

    // ─── Schema probing for `tax` and `service_type` ──────────────────────────
    private static Boolean hasTaxCol;
    private static Boolean hasServiceTypeCol;

    private static void ensureSchemaProbed(Connection conn) throws SQLException {
        if (hasTaxCol != null && hasServiceTypeCol != null) return;
        boolean tax = false, serviceType = false;
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, "customer_order", null)) {
            while (rs.next()) {
                String col = rs.getString("COLUMN_NAME");
                if ("tax".equalsIgnoreCase(col))          tax = true;
                if ("service_type".equalsIgnoreCase(col)) serviceType = true;
            }
        }
        hasTaxCol         = tax;
        hasServiceTypeCol = serviceType;
    }

    /** subtotal × 12%, rounded HALF_UP to 2dp. */
    public static BigDecimal computeTax(BigDecimal subtotal) {
        if (subtotal == null) return BigDecimal.ZERO;
        return subtotal.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    // ─── Place order (transaction: header + items + stock decrement) ──────────
    /** Backward-compatible overload — defaults to DELIVERY. */
    public static Order placeOrder(int customerId, List<CartLine> lines,
                                   String shippingAddress, String contactNumber,
                                   String notes) {
        return placeOrder(customerId, lines, shippingAddress, contactNumber, notes, ServiceType.DELIVERY);
    }

    public static Order placeOrder(int customerId, List<CartLine> lines,
                                   String shippingAddress, String contactNumber,
                                   String notes, ServiceType serviceType) {
        if (lines == null || lines.isEmpty()) return null;
        if (serviceType == null) serviceType = ServiceType.DELIVERY;

        Connection conn = null;
        try {
            conn = Databaseconnection.getConnection();
            ensureSchemaProbed(conn);
            conn.setAutoCommit(false);

            // Stock check + compute totals
            BigDecimal subtotal = BigDecimal.ZERO;
            for (CartLine line : lines) {
                Product fresh = loadProductLocked(conn, line.productId());
                if (fresh == null) throw new SQLException("Product no longer available.");
                if (fresh.getStock() < line.quantity())
                    throw new SQLException("Not enough stock for '" + fresh.getName() + "'. Only " + fresh.getStock() + " left.");
                subtotal = subtotal.add(fresh.getPrice().multiply(BigDecimal.valueOf(line.quantity())));
            }
            subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
            BigDecimal tax   = computeTax(subtotal);
            BigDecimal total = subtotal.add(tax);

            // Insert header (temporary order_code, patch after we know id).
            // Column list is built dynamically based on what the schema supports.
            StringBuilder cols = new StringBuilder(
                    "order_code, customer_id, status, subtotal, ");
            StringBuilder vals = new StringBuilder("?, ?, 'PENDING', ?, ");
            if (hasTaxCol)         { cols.append("tax, ");           vals.append("?, "); }
            cols.append("total, shipping_address, contact_number, notes");
            vals.append("?, ?, ?, ?");
            if (hasServiceTypeCol) { cols.append(", service_type");   vals.append(", ?"); }
            String insertHeader = "INSERT INTO customer_order (" + cols + ") VALUES (" + vals + ")";

            int orderId;
            try (PreparedStatement hs = conn.prepareStatement(insertHeader, Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                hs.setString(i++, "ORD-TEMP");
                hs.setInt(i++, customerId);
                hs.setBigDecimal(i++, subtotal);
                if (hasTaxCol) hs.setBigDecimal(i++, tax);
                hs.setBigDecimal(i++, total);
                setNullable(hs, i++, shippingAddress);
                setNullable(hs, i++, contactNumber);
                setNullable(hs, i++, notes);
                if (hasServiceTypeCol) hs.setString(i, serviceType.name());
                hs.executeUpdate();
                try (ResultSet keys = hs.getGeneratedKeys()) {
                    keys.next();
                    orderId = keys.getInt(1);
                }
            }

            // Patch order_code
            String code = String.format("ORD-%06d", orderId);
            try (PreparedStatement up = conn.prepareStatement(
                    "UPDATE customer_order SET order_code = ? WHERE id = ?")) {
                up.setString(1, code);
                up.setInt(2, orderId);
                up.executeUpdate();
            }

            // Insert items + decrement stock
            String insertItem =
                    "INSERT INTO order_item " +
                    "(order_id, product_id, product_name, product_sku, unit_price, quantity, line_total) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
            String decrementStock = "UPDATE product SET stock = stock - ? WHERE id = ?";
            try (PreparedStatement is = conn.prepareStatement(insertItem);
                 PreparedStatement ds = conn.prepareStatement(decrementStock)) {

                for (CartLine line : lines) {
                    Product fresh = loadProduct(conn, line.productId());
                    BigDecimal lineTotal = fresh.getPrice().multiply(BigDecimal.valueOf(line.quantity()));

                    is.setInt(1, orderId);
                    is.setInt(2, fresh.getId());
                    is.setString(3, fresh.getName());
                    is.setString(4, fresh.getSku());
                    is.setBigDecimal(5, fresh.getPrice());
                    is.setInt(6, line.quantity());
                    is.setBigDecimal(7, lineTotal);
                    is.addBatch();

                    ds.setInt(1, line.quantity());
                    ds.setInt(2, fresh.getId());
                    ds.addBatch();
                }
                is.executeBatch();
                ds.executeBatch();
            }

            conn.commit();
            return findById(orderId);

        } catch (SQLException e) {
            e.printStackTrace();
            try { if (conn != null) conn.rollback(); } catch (SQLException ignored) {}
            return null;
        } finally {
            try { if (conn != null) { conn.setAutoCommit(true); conn.close(); } } catch (SQLException ignored) {}
        }
    }

    private static Product loadProduct(Connection conn, int id) throws SQLException {
        try (PreparedStatement s = conn.prepareStatement(
                "SELECT * FROM product WHERE id = ?")) {
            s.setInt(1, id);
            ResultSet rs = s.executeQuery();
            if (!rs.next()) return null;
            return new Product(
                    rs.getInt("id"), rs.getString("sku"), rs.getString("name"),
                    rs.getString("description"),
                    (Integer) (rs.getObject("category_id")),
                    null,
                    rs.getBigDecimal("price"), rs.getInt("stock"),
                    rs.getBoolean("is_active"), null, null);
        }
    }

    private static Product loadProductLocked(Connection conn, int id) throws SQLException {
        try (PreparedStatement s = conn.prepareStatement(
                "SELECT * FROM product WHERE id = ? FOR UPDATE")) {
            s.setInt(1, id);
            ResultSet rs = s.executeQuery();
            if (!rs.next()) return null;
            return new Product(
                    rs.getInt("id"), rs.getString("sku"), rs.getString("name"),
                    rs.getString("description"),
                    (Integer) (rs.getObject("category_id")),
                    null,
                    rs.getBigDecimal("price"), rs.getInt("stock"),
                    rs.getBoolean("is_active"), null, null);
        }
    }

    private static void setNullable(PreparedStatement s, int i, String v) throws SQLException {
        if (v != null && !v.isBlank()) s.setString(i, v);
        else s.setNull(i, Types.VARCHAR);
    }

    // ─── Admin list (with optional status / search) ───────────────────────────
    public static List<Order> searchAdmin(OrderStatus status, String query) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (status != null) {
            sql.append("AND o.status = ? ");
            params.add(status.name());
        }
        if (query != null && !query.isBlank()) {
            sql.append("AND (o.order_code LIKE ? OR uc.full_name LIKE ?) ");
            String like = "%" + query.trim() + "%";
            params.add(like); params.add(like);
        }
        sql.append("ORDER BY o.created_at DESC");

        return runListQuery(sql.toString(), params);
    }

    public static List<Order> findByCustomer(int customerId) {
        String sql = BASE_SELECT + "WHERE o.customer_id = ? ORDER BY o.created_at DESC";
        List<Object> params = new ArrayList<>();
        params.add(customerId);
        return runListQuery(sql, params);
    }

    private static List<Order> runListQuery(String sql, List<Object> params) {
        List<Order> list = new ArrayList<>();
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) stmt.setObject(i + 1, params.get(i));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ─── Find by id (with items) ──────────────────────────────────────────────
    public static Order findById(int id) {
        String sql = BASE_SELECT + "WHERE o.id = ?";
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) return null;
            Order order = mapRow(rs);
            order.setItems(findItems(id));
            return order;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static List<OrderItem> findItems(int orderId) {
        List<OrderItem> items = new ArrayList<>();
        String sql = "SELECT * FROM order_item WHERE order_id = ? ORDER BY id ASC";
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int pid  = rs.getInt("product_id");
                boolean pidNull = rs.wasNull();
                items.add(new OrderItem(
                        rs.getInt("id"),
                        rs.getInt("order_id"),
                        pidNull ? null : pid,
                        rs.getString("product_name"),
                        rs.getString("product_sku"),
                        rs.getBigDecimal("unit_price"),
                        rs.getInt("quantity"),
                        rs.getBigDecimal("line_total")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    // ─── Admin: update status ─────────────────────────────────────────────────
    public static boolean updateStatus(int orderId, OrderStatus newStatus) {
        String sql = "UPDATE customer_order SET status = ? WHERE id = ?";
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus.name());
            stmt.setInt(2, orderId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ─── Count helpers ────────────────────────────────────────────────────────
    public static int countByStatus(OrderStatus status) {
        String sql = "SELECT COUNT(*) FROM customer_order WHERE status = ?";
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int countOn(java.time.LocalDate date) {
        String sql = "SELECT COUNT(*) FROM customer_order WHERE DATE(created_at) = ?";
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, java.sql.Date.valueOf(date));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static java.math.BigDecimal revenueOn(java.time.LocalDate date) {
        String sql = "SELECT COALESCE(SUM(total), 0) FROM customer_order " +
                     "WHERE DATE(created_at) = ? AND status <> 'CANCELLED'";
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, java.sql.Date.valueOf(date));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getBigDecimal(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return java.math.BigDecimal.ZERO;
    }

    public static java.math.BigDecimal revenueBetween(java.time.LocalDate from, java.time.LocalDate to) {
        String sql = "SELECT COALESCE(SUM(total), 0) FROM customer_order " +
                     "WHERE DATE(created_at) BETWEEN ? AND ? AND status <> 'CANCELLED'";
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, java.sql.Date.valueOf(from));
            stmt.setDate(2, java.sql.Date.valueOf(to));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getBigDecimal(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return java.math.BigDecimal.ZERO;
    }

    public static int countBetween(java.time.LocalDate from, java.time.LocalDate to) {
        String sql = "SELECT COUNT(*) FROM customer_order " +
                     "WHERE DATE(created_at) BETWEEN ? AND ?";
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, java.sql.Date.valueOf(from));
            stmt.setDate(2, java.sql.Date.valueOf(to));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int itemsSoldBetween(java.time.LocalDate from, java.time.LocalDate to) {
        String sql = "SELECT COALESCE(SUM(oi.quantity), 0) FROM order_item oi " +
                     "JOIN customer_order o ON oi.order_id = o.id " +
                     "WHERE DATE(o.created_at) BETWEEN ? AND ? AND o.status <> 'CANCELLED'";
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, java.sql.Date.valueOf(from));
            stmt.setDate(2, java.sql.Date.valueOf(to));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // ─── Sales by day (for line chart) ────────────────────────────────────────
    public static java.util.Map<java.time.LocalDate, java.math.BigDecimal> salesByDay(
            java.time.LocalDate from, java.time.LocalDate to) {
        String sql = "SELECT DATE(created_at) AS d, COALESCE(SUM(total), 0) AS rev " +
                     "FROM customer_order " +
                     "WHERE DATE(created_at) BETWEEN ? AND ? AND status <> 'CANCELLED' " +
                     "GROUP BY DATE(created_at)";
        java.util.Map<java.time.LocalDate, java.math.BigDecimal> rows = new java.util.HashMap<>();
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, java.sql.Date.valueOf(from));
            stmt.setDate(2, java.sql.Date.valueOf(to));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                rows.put(rs.getDate("d").toLocalDate(), rs.getBigDecimal("rev"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Fill missing days with zero so charts show continuous line
        java.util.LinkedHashMap<java.time.LocalDate, java.math.BigDecimal> filled = new java.util.LinkedHashMap<>();
        for (java.time.LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            filled.put(d, rows.getOrDefault(d, java.math.BigDecimal.ZERO));
        }
        return filled;
    }

    // ─── Orders by status ─────────────────────────────────────────────────────
    public static java.util.Map<OrderStatus, Integer> ordersByStatus() {
        return ordersByStatusBetween(null, null);
    }

    public static java.util.Map<OrderStatus, Integer> ordersByStatusBetween(
            java.time.LocalDate from, java.time.LocalDate to) {
        StringBuilder sql = new StringBuilder(
                "SELECT status, COUNT(*) AS n FROM customer_order ");
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (from != null && to != null) {
            sql.append("WHERE DATE(created_at) BETWEEN ? AND ? ");
            params.add(java.sql.Date.valueOf(from));
            params.add(java.sql.Date.valueOf(to));
        }
        sql.append("GROUP BY status");

        java.util.EnumMap<OrderStatus, Integer> out = new java.util.EnumMap<>(OrderStatus.class);
        for (OrderStatus s : OrderStatus.values()) out.put(s, 0);

        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) stmt.setObject(i + 1, params.get(i));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                out.put(OrderStatus.valueOf(rs.getString("status")), rs.getInt("n"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    // ─── Top products ─────────────────────────────────────────────────────────
    public record TopProduct(String name, String sku, int sold, java.math.BigDecimal revenue) {}

    public static java.util.List<TopProduct> topProducts(int limit) {
        return topProductsBetween(null, null, limit);
    }

    public static java.util.List<TopProduct> topProductsBetween(
            java.time.LocalDate from, java.time.LocalDate to, int limit) {
        StringBuilder sql = new StringBuilder(
                "SELECT oi.product_name, oi.product_sku, " +
                "       SUM(oi.quantity) AS sold, SUM(oi.line_total) AS revenue " +
                "FROM order_item oi " +
                "JOIN customer_order o ON oi.order_id = o.id " +
                "WHERE o.status <> 'CANCELLED' ");
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (from != null && to != null) {
            sql.append("AND DATE(o.created_at) BETWEEN ? AND ? ");
            params.add(java.sql.Date.valueOf(from));
            params.add(java.sql.Date.valueOf(to));
        }
        sql.append("GROUP BY oi.product_name, oi.product_sku ")
           .append("ORDER BY revenue DESC LIMIT ?");
        params.add(limit);

        java.util.List<TopProduct> list = new java.util.ArrayList<>();
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) stmt.setObject(i + 1, params.get(i));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new TopProduct(
                        rs.getString("product_name"),
                        rs.getString("product_sku"),
                        rs.getInt("sold"),
                        rs.getBigDecimal("revenue")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ─── Recent orders (for dashboard) ────────────────────────────────────────
    public static java.util.List<Order> recent(int limit) {
        String sql = BASE_SELECT + "ORDER BY o.created_at DESC LIMIT ?";
        java.util.List<Order> list = new java.util.ArrayList<>();
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ─── Row mapper ───────────────────────────────────────────────────────────
    private static Order mapRow(ResultSet rs) throws SQLException {
        Timestamp created = rs.getTimestamp("created_at");
        Timestamp updated = rs.getTimestamp("updated_at");
        BigDecimal tax;
        try { tax = rs.getBigDecimal("tax"); }
        catch (SQLException ignored) { tax = BigDecimal.ZERO; }
        if (tax == null) tax = BigDecimal.ZERO;

        Order order = new Order(
                rs.getInt("id"),
                rs.getString("order_code"),
                rs.getInt("customer_id"),
                rs.getString("customer_name"),
                OrderStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("subtotal"),
                tax,
                rs.getBigDecimal("total"),
                rs.getString("shipping_address"),
                rs.getString("contact_number"),
                rs.getString("notes"),
                created != null ? created.toLocalDateTime() : null,
                updated != null ? updated.toLocalDateTime() : null
        );
        try { order.setServiceType(ServiceType.parse(rs.getString("service_type"))); }
        catch (SQLException ignored) { /* column not present yet */ }
        return order;
    }

    // ─── Minimal DTO for placeOrder ───────────────────────────────────────────
    public record CartLine(int productId, int quantity) {}
}
