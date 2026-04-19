package com.shiro.ordermanagementsystem;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

    // ─── Find by username ─────────────────────────────────────────────────────
    public static User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        User.Role.valueOf(rs.getString("role")),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("phone")
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    // ─── Check existence (used for duplicate-prevention in signup) ────────────
    public static boolean usernameExists(String username) {
        return checkExists("SELECT 1 FROM users WHERE username = ?", username);
    }

    public static boolean emailExists(String email) {
        return checkExists("SELECT 1 FROM users WHERE email = ?", email);
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

    // ─── Update password by email (hashes password with BCrypt) ──────────────
    public static boolean updatePasswordByEmail(String email, String plainPassword) {
        String sql = "UPDATE users SET password = ? WHERE email = ?";
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

    // ─── Create new user (hashes password with BCrypt before saving) ──────────
    public static boolean createUser(String username, String plainPassword,
                                     User.Role role, String fullName,
                                     String email, String phone) {
        String sql = "INSERT INTO users (username, password, role, full_name, email, phone) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        String hashed = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));

        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, hashed);
            stmt.setString(3, role.name());
            stmt.setString(4, fullName);
            stmt.setString(5, email);
            stmt.setString(6, phone);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}