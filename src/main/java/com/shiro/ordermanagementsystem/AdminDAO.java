package com.shiro.ordermanagementsystem;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AdminDAO {

    // ─── Find by username (active admins only) ────────────────────────────────
    public static Admin findByUsername(String username) {
        String sql = "SELECT * FROM admin WHERE username = ? AND is_active = TRUE";

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

    // ─── Find by id ───────────────────────────────────────────────────────────
    public static Admin findById(int id) {
        String sql = "SELECT * FROM admin WHERE id = ?";

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
        return checkExists("SELECT 1 FROM admin WHERE username = ?", username);
    }

    public static boolean emailExists(String email) {
        return checkExists("SELECT 1 FROM admin WHERE email = ?", email);
    }

    public static boolean emailExistsExcept(String email, int excludeId) {
        String sql = "SELECT 1 FROM admin WHERE email = ? AND id <> ?";
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setInt(2, excludeId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean checkExists(String sql, String value) {
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, value);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ─── Create admin (BCrypt-hashes password) ────────────────────────────────
    public static boolean createAdmin(String adminCode, String username, String plainPassword,
                                      String email, String fullName, String phone,
                                      Admin.Level level, String position, Integer createdBy) {
        String sql = "INSERT INTO admin " +
                "(admin_code, username, password, email, full_name, phone, " +
                " admin_level, position, is_active, created_by) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE, ?)";

        String hashed = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));

        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, adminCode);
            stmt.setString(2, username);
            stmt.setString(3, hashed);
            stmt.setString(4, email);
            stmt.setString(5, fullName);
            if (phone != null) stmt.setString(6, phone); else stmt.setNull(6, java.sql.Types.VARCHAR);
            stmt.setString(7, level.name());
            if (position != null) stmt.setString(8, position); else stmt.setNull(8, java.sql.Types.VARCHAR);
            if (createdBy != null) stmt.setInt(9, createdBy); else stmt.setNull(9, java.sql.Types.INTEGER);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ─── Update profile (editable fields only) ────────────────────────────────
    public static boolean updateProfile(int id, String fullName, String email,
                                        String phone, String position) {
        String sql = "UPDATE admin SET full_name = ?, email = ?, phone = ?, position = ? " +
                     "WHERE id = ?";

        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, fullName);
            stmt.setString(2, email);
            if (phone    != null && !phone.isBlank())    stmt.setString(3, phone);    else stmt.setNull(3, java.sql.Types.VARCHAR);
            if (position != null && !position.isBlank()) stmt.setString(4, position); else stmt.setNull(4, java.sql.Types.VARCHAR);
            stmt.setInt(5, id);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ─── Update password by id ────────────────────────────────────────────────
    public static boolean updatePasswordById(int id, String plainPassword) {
        String sql = "UPDATE admin SET password = ? WHERE id = ?";
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

    // ─── Update password by email ─────────────────────────────────────────────
    public static boolean updatePasswordByEmail(String email, String plainPassword) {
        String sql = "UPDATE admin SET password = ? WHERE email = ?";
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

    // ─── Username existence (excluding id) ────────────────────────────────────
    public static boolean usernameExistsExcept(String username, int excludeId) {
        String sql = "SELECT 1 FROM admin WHERE username = ? AND id <> ?";
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setInt(2, excludeId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean adminCodeExists(String adminCode) {
        return checkExists("SELECT 1 FROM admin WHERE admin_code = ?", adminCode);
    }

    public static boolean adminCodeExistsExcept(String adminCode, int excludeId) {
        String sql = "SELECT 1 FROM admin WHERE admin_code = ? AND id <> ?";
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, adminCode);
            stmt.setInt(2, excludeId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ─── List / search admins ─────────────────────────────────────────────────
    public static List<Admin> search(String query, Admin.Level level) {
        StringBuilder sql = new StringBuilder("SELECT * FROM admin WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (level != null) {
            sql.append("AND admin_level = ? ");
            params.add(level.name());
        }
        if (query != null && !query.isBlank()) {
            sql.append("AND (username LIKE ? OR full_name LIKE ? OR email LIKE ? OR admin_code LIKE ?) ");
            String like = "%" + query.trim() + "%";
            params.add(like); params.add(like); params.add(like); params.add(like);
        }
        sql.append("ORDER BY admin_level ASC, created_at ASC");

        List<Admin> list = new ArrayList<>();
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

    // ─── Update admin (level + active + profile) ──────────────────────────────
    public static boolean updateAdmin(int id, String username, String fullName, String email,
                                      String phone, Admin.Level level, String position, boolean active) {
        String sql = "UPDATE admin SET username = ?, full_name = ?, email = ?, phone = ?, " +
                     "admin_level = ?, position = ?, is_active = ? WHERE id = ?";
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, fullName);
            stmt.setString(3, email);
            if (phone    != null && !phone.isBlank())    stmt.setString(4, phone);    else stmt.setNull(4, java.sql.Types.VARCHAR);
            stmt.setString(5, level.name());
            if (position != null && !position.isBlank()) stmt.setString(6, position); else stmt.setNull(6, java.sql.Types.VARCHAR);
            stmt.setBoolean(7, active);
            stmt.setInt(8, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean setActive(int id, boolean active) {
        String sql = "UPDATE admin SET is_active = ? WHERE id = ?";
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, active);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean delete(int id) {
        String sql = "DELETE FROM admin WHERE id = ?";
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static int countActiveSuperAdmins() {
        String sql = "SELECT COUNT(*) FROM admin WHERE admin_level = 'SUPER_ADMIN' AND is_active = TRUE";
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static String nextAdminCode() {
        String sql = "SELECT admin_code FROM admin " +
                     "WHERE admin_code LIKE 'ADMIN-%' " +
                     "ORDER BY CAST(SUBSTRING(admin_code, 7) AS UNSIGNED) DESC LIMIT 1";
        int next = 1;
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String code = rs.getString(1);
                try {
                    next = Integer.parseInt(code.substring(6)) + 1;
                } catch (NumberFormatException ignored) {}
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return String.format("ADMIN-%03d", next);
    }

    // ─── Update last login ────────────────────────────────────────────────────
    public static void updateLastLogin(int adminId) {
        String sql = "UPDATE admin SET last_login_at = NOW() WHERE id = ?";
        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, adminId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ─── Row mapper ───────────────────────────────────────────────────────────
    private static Admin mapRow(ResultSet rs) throws SQLException {
        Timestamp created  = rs.getTimestamp("created_at");
        Timestamp lastLog  = rs.getTimestamp("last_login_at");
        int       createdBy = rs.getInt("created_by");
        boolean   cbWasNull = rs.wasNull();

        return new Admin(
                rs.getInt("id"),
                rs.getString("admin_code"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("email"),
                rs.getString("full_name"),
                rs.getString("phone"),
                Admin.Level.valueOf(rs.getString("admin_level")),
                rs.getString("position"),
                rs.getBoolean("is_active"),
                created != null ? created.toLocalDateTime() : null,
                lastLog != null ? lastLog.toLocalDateTime() : null,
                cbWasNull ? null : createdBy
        );
    }
}
