package com.shiro.ordermanagementsystem;

import org.mindrot.jbcrypt.BCrypt;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {

    /** Row-model for the admin Customers table — includes derived order totals. */
    public static class Row {
        public final Customer   customer;
        public final int        ordersCount;
        public final BigDecimal totalSpent;

        public Row(Customer customer, int ordersCount, BigDecimal totalSpent) {
            this.customer    = customer;
            this.ordersCount = ordersCount;
            this.totalSpent  = totalSpent == null ? BigDecimal.ZERO : totalSpent;
        }
    }

    // ─── Find by username ─────────────────────────────────────────────────────
    public static Customer findByUsername(String username) {
        String sql = "SELECT * FROM user_customer WHERE username = ?";
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Customer findById(int id) {
        String sql = "SELECT * FROM user_customer WHERE id = ?";
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ─── Existence checks ─────────────────────────────────────────────────────
    public static boolean usernameExists(String username) {
        return checkExists("SELECT 1 FROM user_customer WHERE username = ?", username);
    }

    public static boolean emailExists(String email) {
        return checkExists("SELECT 1 FROM user_customer WHERE email = ?", email);
    }

    public static boolean usernameExistsExcept(String username, int excludeId) {
        return checkExistsExcept("SELECT 1 FROM user_customer WHERE username = ? AND id <> ?", username, excludeId);
    }

    public static boolean emailExistsExcept(String email, int excludeId) {
        return checkExistsExcept("SELECT 1 FROM user_customer WHERE email = ? AND id <> ?", email, excludeId);
    }

    private static boolean checkExists(String sql, String value) {
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, value);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean checkExistsExcept(String sql, String value, int excludeId) {
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, value);
            stmt.setInt(2, excludeId);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ─── Create customer (BCrypt-hashes password) ─────────────────────────────
    public static boolean createCustomer(String username, String plainPassword,
                                         String fullName, String email, String phone) {
        String sql = "INSERT INTO user_customer (username, password, full_name, email, phone) " +
                     "VALUES (?, ?, ?, ?, ?)";

        String hashed = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));

        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, hashed);
            stmt.setString(3, fullName);
            stmt.setString(4, email);
            stmt.setString(5, phone);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ─── Update profile fields (no password) ──────────────────────────────────
    public static boolean updateCustomer(int id, String username, String fullName,
                                         String email, String phone, boolean active) {
        try (Connection conn = Databaseconnection.getConnection()) {
            ensureSchemaProbed(conn);

            String sql = hasActiveCol
                    ? "UPDATE user_customer SET username = ?, full_name = ?, email = ?, phone = ?, active = ? WHERE id = ?"
                    : "UPDATE user_customer SET username = ?, full_name = ?, email = ?, phone = ? WHERE id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                stmt.setString(2, fullName);
                stmt.setString(3, email);
                if (phone == null) stmt.setNull(4, java.sql.Types.VARCHAR);
                else stmt.setString(4, phone);
                if (hasActiveCol) {
                    stmt.setBoolean(5, active);
                    stmt.setInt(6, id);
                } else {
                    stmt.setInt(5, id);
                }
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean setActive(int id, boolean active) {
        try (Connection conn = Databaseconnection.getConnection()) {
            ensureSchemaProbed(conn);
            if (!hasActiveCol) {
                System.err.println("[CustomerDAO] setActive skipped — 'active' column not present. Run the migration.");
                return false;
            }
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE user_customer SET active = ? WHERE id = ?")) {
                stmt.setBoolean(1, active);
                stmt.setInt(2, id);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ─── Password updates ─────────────────────────────────────────────────────
    public static boolean updatePasswordByEmail(String email, String plainPassword) {
        String sql = "UPDATE user_customer SET password = ? WHERE email = ?";
        String hashed = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, hashed);
            stmt.setString(2, email);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updatePasswordById(int id, String plainPassword) {
        String sql = "UPDATE user_customer SET password = ? WHERE id = ?";
        String hashed = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, hashed);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ─── Schema probing (works with or without the active/created_at migration) ─
    private static Boolean hasActiveCol;
    private static Boolean hasCreatedAtCol;

    private static void ensureSchemaProbed(Connection conn) throws SQLException {
        if (hasActiveCol != null && hasCreatedAtCol != null) return;
        boolean active = false, created = false;
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, "user_customer", null)) {
            while (rs.next()) {
                String name = rs.getString("COLUMN_NAME");
                if ("active".equalsIgnoreCase(name))     active = true;
                if ("created_at".equalsIgnoreCase(name)) created = true;
            }
        }
        hasActiveCol    = active;
        hasCreatedAtCol = created;
    }

    // ─── Search with derived order totals ─────────────────────────────────────
    /**
     * @param query  matches username/full_name/email/phone (nullable)
     * @param status null = all, TRUE = active only, FALSE = archived only
     *               (ignored if the `active` column hasn't been added yet)
     */
    public static List<Row> search(String query, Boolean status) {
        List<Row> rows = new ArrayList<>();
        try (Connection conn = Databaseconnection.getConnection()) {
            ensureSchemaProbed(conn);

            String activeExpr    = hasActiveCol    ? "c.active"     : "1 AS active";
            String createdExpr   = hasCreatedAtCol ? "c.created_at" : "NULL AS created_at";

            StringBuilder sql = new StringBuilder();
            sql.append("SELECT c.id, c.username, c.password, c.full_name, c.email, c.phone, ");
            sql.append("       ").append(activeExpr).append(", ");
            sql.append("       ").append(createdExpr).append(", ");
            sql.append("       COUNT(o.id)               AS orders_count, ");
            sql.append("       COALESCE(SUM(o.total), 0) AS total_spent ");
            sql.append("FROM user_customer c ");
            sql.append("LEFT JOIN customer_order o ");
            sql.append("       ON o.customer_id = c.id AND o.status <> 'CANCELLED' ");
            sql.append("WHERE 1=1 ");

            List<Object> params = new ArrayList<>();
            if (query != null && !query.isBlank()) {
                sql.append("AND (c.username LIKE ? OR c.full_name LIKE ? OR c.email LIKE ? OR c.phone LIKE ?) ");
                String like = "%" + query.trim() + "%";
                params.add(like); params.add(like); params.add(like); params.add(like);
            }
            if (status != null && hasActiveCol) {
                sql.append("AND c.active = ? ");
                params.add(status);
            }

            sql.append("GROUP BY c.id, c.username, c.password, c.full_name, c.email, c.phone");
            if (hasActiveCol)    sql.append(", c.active");
            if (hasCreatedAtCol) sql.append(", c.created_at");
            sql.append(" ");

            if (hasCreatedAtCol) sql.append("ORDER BY c.created_at DESC, c.id DESC");
            else                 sql.append("ORDER BY c.id DESC");

            try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    Object p = params.get(i);
                    if (p instanceof Boolean b) stmt.setBoolean(i + 1, b);
                    else                        stmt.setObject(i + 1, p);
                }
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    Customer c = mapRow(rs);
                    rows.add(new Row(c, rs.getInt("orders_count"), rs.getBigDecimal("total_spent")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rows;
    }

    // ─── Row mapper ───────────────────────────────────────────────────────────
    private static Customer mapRow(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setId(rs.getInt("id"));
        c.setUsername(rs.getString("username"));
        c.setPassword(rs.getString("password"));
        c.setFullName(rs.getString("full_name"));
        c.setEmail(rs.getString("email"));
        c.setPhone(rs.getString("phone"));
        try {
            c.setActive(rs.getBoolean("active"));
        } catch (SQLException ignored) { c.setActive(true); }
        try {
            Timestamp ts = rs.getTimestamp("created_at");
            if (ts != null) c.setCreatedAt(ts.toLocalDateTime());
        } catch (SQLException ignored) { /* column may not exist on legacy rows */ }
        return c;
    }
}
