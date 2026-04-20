package com.shiro.ordermanagementsystem;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    private static final String BASE_SELECT =
            "SELECT p.*, c.name AS category_name " +
            "FROM product p LEFT JOIN category c ON p.category_id = c.id ";

    // ─── List (optional search + category filter) ─────────────────────────────
    public static List<Product> search(String query, Integer categoryId) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (query != null && !query.isBlank()) {
            sql.append("AND (p.name LIKE ? OR p.sku LIKE ?) ");
            String like = "%" + query.trim() + "%";
            params.add(like);
            params.add(like);
        }
        if (categoryId != null) {
            sql.append("AND p.category_id = ? ");
            params.add(categoryId);
        }
        sql.append("ORDER BY p.created_at DESC");

        List<Product> list = new ArrayList<>();
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) stmt.setObject(i + 1, params.get(i));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapRow(rs));

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<Product> findAll() { return search(null, null); }

    // ─── Find by id ───────────────────────────────────────────────────────────
    public static Product findById(int id) {
        String sql = BASE_SELECT + "WHERE p.id = ?";
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

    // ─── SKU uniqueness ───────────────────────────────────────────────────────
    public static boolean skuExists(String sku) {
        return skuExistsExcept(sku, -1);
    }

    public static boolean skuExistsExcept(String sku, int excludeId) {
        String sql = "SELECT 1 FROM product WHERE sku = ? AND id <> ?";
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sku);
            stmt.setInt(2, excludeId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ─── Create ───────────────────────────────────────────────────────────────
    public static boolean create(String sku, String name, String description,
                                 Integer categoryId, BigDecimal price, int stock,
                                 boolean active) {
        String sql = "INSERT INTO product (sku, name, description, category_id, price, stock, is_active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sku);
            stmt.setString(2, name);
            if (description != null && !description.isBlank()) stmt.setString(3, description);
            else stmt.setNull(3, java.sql.Types.VARCHAR);
            if (categoryId != null) stmt.setInt(4, categoryId);
            else stmt.setNull(4, java.sql.Types.INTEGER);
            stmt.setBigDecimal(5, price);
            stmt.setInt(6, stock);
            stmt.setBoolean(7, active);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ─── Update ───────────────────────────────────────────────────────────────
    public static boolean update(int id, String sku, String name, String description,
                                 Integer categoryId, BigDecimal price, int stock,
                                 boolean active) {
        String sql = "UPDATE product SET sku = ?, name = ?, description = ?, category_id = ?, " +
                     "price = ?, stock = ?, is_active = ? WHERE id = ?";

        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sku);
            stmt.setString(2, name);
            if (description != null && !description.isBlank()) stmt.setString(3, description);
            else stmt.setNull(3, java.sql.Types.VARCHAR);
            if (categoryId != null) stmt.setInt(4, categoryId);
            else stmt.setNull(4, java.sql.Types.INTEGER);
            stmt.setBigDecimal(5, price);
            stmt.setInt(6, stock);
            stmt.setBoolean(7, active);
            stmt.setInt(8, id);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ─── Delete ───────────────────────────────────────────────────────────────
    public static boolean delete(int id) {
        String sql = "DELETE FROM product WHERE id = ?";
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ─── Counts for dashboard ─────────────────────────────────────────────────
    public static int countAll() {
        return scalarCount("SELECT COUNT(*) FROM product");
    }

    public static int countLowStock(int threshold) {
        String sql = "SELECT COUNT(*) FROM product WHERE stock <= ? AND is_active = TRUE";
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, threshold);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static int scalarCount(String sql) {
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // ─── Row mapper ───────────────────────────────────────────────────────────
    private static Product mapRow(ResultSet rs) throws SQLException {
        Timestamp created  = rs.getTimestamp("created_at");
        Timestamp updated  = rs.getTimestamp("updated_at");
        int       catId    = rs.getInt("category_id");
        boolean   catIsNull = rs.wasNull();

        return new Product(
                rs.getInt("id"),
                rs.getString("sku"),
                rs.getString("name"),
                rs.getString("description"),
                catIsNull ? null : catId,
                rs.getString("category_name"),
                rs.getBigDecimal("price"),
                rs.getInt("stock"),
                rs.getBoolean("is_active"),
                created != null ? created.toLocalDateTime() : null,
                updated != null ? updated.toLocalDateTime() : null
        );
    }
}
