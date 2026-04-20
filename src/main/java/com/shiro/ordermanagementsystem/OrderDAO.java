package com.shiro.ordermanagementsystem;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {

    private static final String BASE_SELECT =
            "SELECT o.*, uc.full_name AS customer_name " +
            "FROM customer_order o " +
            "LEFT JOIN user_customer uc ON o.customer_id = uc.id ";

    // ─── Place order (transaction: header + items + stock decrement) ──────────
    public static Order placeOrder(int customerId, List<CartLine> lines,
                                   String shippingAddress, String contactNumber,
                                   String notes) {
        if (lines == null || lines.isEmpty()) return null;

        Connection conn = null;
        try {
            conn = Databaseconnection.getConnection();
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
            BigDecimal total = subtotal; // no tax/shipping for now

            // Insert header (temporary order_code, patch after we know id)
            String insertHeader =
                    "INSERT INTO customer_order " +
                    "(order_code, customer_id, status, subtotal, total, shipping_address, contact_number, notes) " +
                    "VALUES (?, ?, 'PENDING', ?, ?, ?, ?, ?)";
            int orderId;
            try (PreparedStatement hs = conn.prepareStatement(insertHeader, Statement.RETURN_GENERATED_KEYS)) {
                hs.setString(1, "ORD-TEMP");
                hs.setInt(2, customerId);
                hs.setBigDecimal(3, subtotal);
                hs.setBigDecimal(4, total);
                setNullable(hs, 5, shippingAddress);
                setNullable(hs, 6, contactNumber);
                setNullable(hs, 7, notes);
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

    // ─── Count helpers (for dashboard wiring later) ───────────────────────────
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

    // ─── Row mapper ───────────────────────────────────────────────────────────
    private static Order mapRow(ResultSet rs) throws SQLException {
        Timestamp created = rs.getTimestamp("created_at");
        Timestamp updated = rs.getTimestamp("updated_at");
        return new Order(
                rs.getInt("id"),
                rs.getString("order_code"),
                rs.getInt("customer_id"),
                rs.getString("customer_name"),
                OrderStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("total"),
                rs.getString("shipping_address"),
                rs.getString("contact_number"),
                rs.getString("notes"),
                created != null ? created.toLocalDateTime() : null,
                updated != null ? updated.toLocalDateTime() : null
        );
    }

    // ─── Minimal DTO for placeOrder ───────────────────────────────────────────
    public record CartLine(int productId, int quantity) {}
}
