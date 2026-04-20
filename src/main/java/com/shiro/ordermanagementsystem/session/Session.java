package com.shiro.ordermanagementsystem.session;

import com.shiro.ordermanagementsystem.Admin;
import com.shiro.ordermanagementsystem.Customer;

/**
 * Holds the currently-logged-in actor for the lifetime of the JVM.
 * Exactly one of currentAdmin / currentCustomer is set at a time.
 */
public final class Session {

    private static Admin    currentAdmin;
    private static Customer currentCustomer;

    private Session() {}

    public static void setCurrentAdmin(Admin admin) {
        currentAdmin    = admin;
        currentCustomer = null;
    }

    public static void setCurrentCustomer(Customer customer) {
        currentCustomer = customer;
        currentAdmin    = null;
    }

    public static Admin    getCurrentAdmin()    { return currentAdmin; }
    public static Customer getCurrentCustomer() { return currentCustomer; }

    public static boolean isAdmin()    { return currentAdmin    != null; }
    public static boolean isCustomer() { return currentCustomer != null; }

    public static void clear() {
        currentAdmin    = null;
        currentCustomer = null;
    }
}
