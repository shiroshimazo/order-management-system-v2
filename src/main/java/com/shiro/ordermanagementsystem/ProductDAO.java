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

    // ─── Schema probing (works with or without the rich-product migration) ────
    private static Boolean hasImageUrl;
    private static Boolean hasOriginalPrice;
    private static Boolean hasRating;
    private static Boolean hasRatingCount;

    private static void ensureSchemaProbed(Connection conn) throws SQLException {
        if (hasImageUrl != null) return;
        boolean img = false, orig = false, rate = false, rateN = false;
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, "product", null)) {
            while (rs.next()) {
                String name = rs.getString("COLUMN_NAME");
                if ("image_url".equalsIgnoreCase(name))      img   = true;
                if ("original_price".equalsIgnoreCase(name)) orig  = true;
                if ("rating".equalsIgnoreCase(name))         rate  = true;
                if ("rating_count".equalsIgnoreCase(name))   rateN = true;
            }
        }
        hasImageUrl      = img;
        hasOriginalPrice = orig;
        hasRating        = rate;
        hasRatingCount   = rateN;
    }

    private static String baseSelect(Connection conn) throws SQLException {
        ensureSchemaProbed(conn);
        StringBuilder sb = new StringBuilder("SELECT p.id, p.sku, p.name, p.description, ");
        sb.append("p.category_id, p.price, p.stock, p.is_active, p.created_at, p.updated_at, ");
        sb.append(hasImageUrl      ? "p.image_url, "      : "NULL AS image_url, ");
        sb.append(hasOriginalPrice ? "p.original_price, " : "NULL AS original_price, ");
        sb.append(hasRating        ? "p.rating, "         : "0.00 AS rating, ");
        sb.append(hasRatingCount   ? "p.rating_count, "   : "0 AS rating_count, ");
        sb.append("c.name AS category_name ");
        sb.append("FROM product p LEFT JOIN category c ON p.category_id = c.id ");
        return sb.toString();
    }

    // ─── List (optional search + category filter) ─────────────────────────────
    public static List<Product> search(String query, Integer categoryId) {
        List<Product> list = new ArrayList<>();
        try (Connection conn = Databaseconnection.getConnection()) {
            StringBuilder sql = new StringBuilder(baseSelect(conn)).append("WHERE 1=1 ");
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

            try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) stmt.setObject(i + 1, params.get(i));
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<Product> findAll() { return search(null, null); }

    /** Active-only search for the customer storefront. */
    public static List<Product> searchActive(String query, Integer categoryId) {
        List<Product> list = new ArrayList<>();
        try (Connection conn = Databaseconnection.getConnection()) {
            StringBuilder sql = new StringBuilder(baseSelect(conn))
                    .append("WHERE p.is_active = TRUE ");
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

            try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) stmt.setObject(i + 1, params.get(i));
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ─── Find by id ───────────────────────────────────────────────────────────
    public static Product findById(int id) {
        try (Connection conn = Databaseconnection.getConnection()) {
            String sql = baseSelect(conn) + "WHERE p.id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) return mapRow(rs);
            }
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
        return create(sku, name, description, categoryId, price, null, stock, active, null);
    }

    public static boolean create(String sku, String name, String description,
                                 Integer categoryId, BigDecimal price, BigDecimal originalPrice,
                                 int stock, boolean active, String imageUrl) {
        try (Connection conn = Databaseconnection.getConnection()) {
            ensureSchemaProbed(conn);

            StringBuilder cols = new StringBuilder("sku, name, description, category_id, price, stock, is_active");
            StringBuilder vals = new StringBuilder("?, ?, ?, ?, ?, ?, ?");
            if (hasImageUrl)      { cols.append(", image_url");      vals.append(", ?"); }
            if (hasOriginalPrice) { cols.append(", original_price"); vals.append(", ?"); }

            String sql = "INSERT INTO product (" + cols + ") VALUES (" + vals + ")";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                int i = 1;
                stmt.setString(i++, sku);
                stmt.setString(i++, name);
                if (description != null && !description.isBlank()) stmt.setString(i++, description);
                else { stmt.setNull(i++, java.sql.Types.VARCHAR); }
                if (categoryId != null) stmt.setInt(i++, categoryId);
                else { stmt.setNull(i++, java.sql.Types.INTEGER); }
                stmt.setBigDecimal(i++, price);
                stmt.setInt(i++, stock);
                stmt.setBoolean(i++, active);
                if (hasImageUrl) {
                    if (imageUrl == null || imageUrl.isBlank()) stmt.setNull(i++, java.sql.Types.VARCHAR);
                    else stmt.setString(i++, imageUrl);
                }
                if (hasOriginalPrice) {
                    if (originalPrice == null) stmt.setNull(i++, java.sql.Types.DECIMAL);
                    else stmt.setBigDecimal(i++, originalPrice);
                }
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ─── Update ───────────────────────────────────────────────────────────────
    public static boolean update(int id, String sku, String name, String description,
                                 Integer categoryId, BigDecimal price, int stock,
                                 boolean active) {
        return update(id, sku, name, description, categoryId, price, null, stock, active, null);
    }

    public static boolean update(int id, String sku, String name, String description,
                                 Integer categoryId, BigDecimal price, BigDecimal originalPrice,
                                 int stock, boolean active, String imageUrl) {
        try (Connection conn = Databaseconnection.getConnection()) {
            ensureSchemaProbed(conn);

            StringBuilder sets = new StringBuilder(
                    "sku = ?, name = ?, description = ?, category_id = ?, price = ?, stock = ?, is_active = ?");
            if (hasImageUrl)      sets.append(", image_url = ?");
            if (hasOriginalPrice) sets.append(", original_price = ?");

            String sql = "UPDATE product SET " + sets + " WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                int i = 1;
                stmt.setString(i++, sku);
                stmt.setString(i++, name);
                if (description != null && !description.isBlank()) stmt.setString(i++, description);
                else { stmt.setNull(i++, java.sql.Types.VARCHAR); }
                if (categoryId != null) stmt.setInt(i++, categoryId);
                else { stmt.setNull(i++, java.sql.Types.INTEGER); }
                stmt.setBigDecimal(i++, price);
                stmt.setInt(i++, stock);
                stmt.setBoolean(i++, active);
                if (hasImageUrl) {
                    if (imageUrl == null || imageUrl.isBlank()) stmt.setNull(i++, java.sql.Types.VARCHAR);
                    else stmt.setString(i++, imageUrl);
                }
                if (hasOriginalPrice) {
                    if (originalPrice == null) stmt.setNull(i++, java.sql.Types.DECIMAL);
                    else stmt.setBigDecimal(i++, originalPrice);
                }
                stmt.setInt(i, id);
                return stmt.executeUpdate() > 0;
            }
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

        BigDecimal originalPrice = rs.getBigDecimal("original_price"); // may be null
        String     imageUrl      = rs.getString("image_url");
        BigDecimal rating        = rs.getBigDecimal("rating");
        int        ratingCount   = rs.getInt("rating_count");

        return new Product(
                rs.getInt("id"),
                rs.getString("sku"),
                rs.getString("name"),
                rs.getString("description"),
                catIsNull ? null : catId,
                rs.getString("category_name"),
                rs.getBigDecimal("price"),
                originalPrice,
                rs.getInt("stock"),
                rs.getBoolean("is_active"),
                imageUrl,
                rating == null ? BigDecimal.ZERO : rating,
                ratingCount,
                created != null ? created.toLocalDateTime() : null,
                updated != null ? updated.toLocalDateTime() : null
        );
    }
}
