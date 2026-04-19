package com.shiro.ordermanagementsystem.tools;

import com.shiro.ordermanagementsystem.Admin;
import com.shiro.ordermanagementsystem.AdminDAO;

/**
 * One-time seeder for the first SUPER_ADMIN.
 *
 * HOW TO RUN (IntelliJ):
 *   Right-click this file → Run 'SeedAdmin.main()'
 *
 * Run AFTER executing sql/migration_split_users.sql.
 * Safe to run once; a second run will fail the UNIQUE constraint and print "Seed failed."
 */
public class SeedAdmin {

    public static void main(String[] args) {
        String adminCode = "ADMIN-001";
        String username  = "superadmin";
        String password  = "Admin@123";                 // CHANGE THIS IMMEDIATELY AFTER FIRST LOGIN
        String email     = "shiroshiimazo@gmail.com";
        String fullName  = "Super Admin";
        String phone     = null;
        String position  = "System Administrator";

        boolean ok = AdminDAO.createAdmin(
                adminCode, username, password, email, fullName, phone,
                Admin.Level.SUPER_ADMIN, position, null
        );

        if (ok) {
            System.out.println("Super admin seeded successfully.");
            System.out.println("  admin_code : " + adminCode);
            System.out.println("  username   : " + username);
            System.out.println("  password   : " + password + "   (change it!)");
        } else {
            System.out.println("Seed failed (admin may already exist).");
        }
    }
}
