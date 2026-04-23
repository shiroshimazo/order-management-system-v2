package com.shiro.ordermanagementsystem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FeedbackDAO {

    public record Entry(int id, int customerId, int rating, String subject,
                        String message, LocalDateTime createdAt) {}

    /** Probe table existence so the app keeps running if migration not applied. */
    private static Boolean tableExists;

    private static boolean ensureTable(Connection conn) throws SQLException {
        if (tableExists != null) return tableExists;
        try (ResultSet rs = conn.getMetaData().getTables(null, null, "customer_feedback", null)) {
            tableExists = rs.next();
        }
        return tableExists;
    }

    public static boolean submit(int customerId, int rating, String subject, String message) {
        try (Connection conn = Databaseconnection.getConnection()) {
            if (!ensureTable(conn)) {
                System.err.println("[FeedbackDAO] customer_feedback table missing — run create_feedback_table.sql");
                return false;
            }
            String sql = "INSERT INTO customer_feedback (customer_id, rating, subject, message) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, customerId);
                stmt.setInt(2, rating);
                if (subject == null || subject.isBlank()) stmt.setNull(3, java.sql.Types.VARCHAR);
                else stmt.setString(3, subject);
                stmt.setString(4, message);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<Entry> findByCustomer(int customerId) {
        List<Entry> out = new ArrayList<>();
        try (Connection conn = Databaseconnection.getConnection()) {
            if (!ensureTable(conn)) return out;
            String sql = "SELECT * FROM customer_feedback WHERE customer_id = ? ORDER BY created_at DESC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, customerId);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("created_at");
                    out.add(new Entry(
                            rs.getInt("id"),
                            rs.getInt("customer_id"),
                            rs.getInt("rating"),
                            rs.getString("subject"),
                            rs.getString("message"),
                            ts != null ? ts.toLocalDateTime() : null
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }
}
