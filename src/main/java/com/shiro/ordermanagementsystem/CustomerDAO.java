package com.shiro.ordermanagementsystem;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CustomerDAO {

    // ─── Find by username ─────────────────────────────────────────────────────
    public static Customer findByUsername(String username) {
        String sql = "SELECT * FROM user_customer WHERE username = ?";

        try (Connection conn = Databaseconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new Customer(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
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

    // ─── Existence checks (used for duplicate-prevention in signup) ───────────
    public static boolean usernameExists(String username) {
        return checkExists("SELECT 1 FROM user_customer WHERE username = ?", username);
    }

    public static boolean emailExists(String email) {
        return checkExists("SELECT 1 FROM user_customer WHERE email = ?", email);
    }

    public static boolean emailExistsExcept(String email, int excludeId) {
        String sql = "SELECT 1 FROM user_customer WHERE email = ? AND id <> ?";
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

    // ─── Update password by email ─────────────────────────────────────────────
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
}
