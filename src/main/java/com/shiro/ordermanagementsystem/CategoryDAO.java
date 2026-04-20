package com.shiro.ordermanagementsystem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {

    // ─── List all ─────────────────────────────────────────────────────────────
    public static List<Category> findAll() {
        String sql = "SELECT * FROM category ORDER BY name ASC";
        List<Category> list = new ArrayList<>();

        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ─── Find by id ───────────────────────────────────────────────────────────
    public static Category findById(int id) {
        String sql = "SELECT * FROM category WHERE id = ?";
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

    // ─── Existence check ──────────────────────────────────────────────────────
    public static boolean nameExists(String name) {
        return nameExistsExcept(name, -1);
    }

    public static boolean nameExistsExcept(String name, int excludeId) {
        String sql = "SELECT 1 FROM category WHERE name = ? AND id <> ?";
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setInt(2, excludeId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ─── Create ───────────────────────────────────────────────────────────────
    public static boolean create(String name, String description) {
        String sql = "INSERT INTO category (name, description) VALUES (?, ?)";
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            if (description != null && !description.isBlank()) stmt.setString(2, description);
            else stmt.setNull(2, java.sql.Types.VARCHAR);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ─── Update ───────────────────────────────────────────────────────────────
    public static boolean update(int id, String name, String description) {
        String sql = "UPDATE category SET name = ?, description = ? WHERE id = ?";
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            if (description != null && !description.isBlank()) stmt.setString(2, description);
            else stmt.setNull(2, java.sql.Types.VARCHAR);
            stmt.setInt(3, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ─── Delete (product.category_id → NULL via FK) ───────────────────────────
    public static boolean delete(int id) {
        String sql = "DELETE FROM category WHERE id = ?";
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ─── Row mapper ───────────────────────────────────────────────────────────
    private static Category mapRow(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        return new Category(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                ts != null ? ts.toLocalDateTime() : null
        );
    }
}
