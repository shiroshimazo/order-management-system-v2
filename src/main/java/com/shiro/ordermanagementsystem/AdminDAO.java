package com.shiro.ordermanagementsystem;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

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

    // ─── Existence checks ─────────────────────────────────────────────────────
    public static boolean usernameExists(String username) {
        return checkExists("SELECT 1 FROM admin WHERE username = ?", username);
    }

    public static boolean emailExists(String email) {
        return checkExists("SELECT 1 FROM admin WHERE email = ?", email);
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
