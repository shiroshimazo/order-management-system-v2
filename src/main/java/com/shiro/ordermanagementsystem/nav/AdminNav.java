package com.shiro.ordermanagementsystem.nav;

import com.shiro.ordermanagementsystem.Admin;

/**
 * Data-driven admin sidebar menu. Add a new section = one enum entry + one FXML.
 */
public enum AdminNav {

    DASHBOARD(        "Dashboard",         "fas-th-large",    "/ordermanagementsystem/fxml/admin/DashboardView.fxml",       false),
    ORDERS(           "Orders",            "fas-shopping-cart","/ordermanagementsystem/fxml/admin/OrdersView.fxml",          false),
    PRODUCTS(         "Products",          "fas-box",         "/ordermanagementsystem/fxml/admin/ProductsView.fxml",        false),
    REPORTS(          "Reports",           "fas-chart-line",  "/ordermanagementsystem/fxml/admin/ReportsView.fxml",         false),
    SETTINGS(         "Settings",          "fas-cog",         "/ordermanagementsystem/fxml/admin/SettingsView.fxml",        false),
    ADMIN_MANAGEMENT( "Admin Management",  "fas-user-shield", "/ordermanagementsystem/fxml/admin/AdminManagementView.fxml", true);

    private final String  label;
    private final String  iconLiteral;
    private final String  fxmlPath;
    private final boolean superAdminOnly;

    AdminNav(String label, String iconLiteral, String fxmlPath, boolean superAdminOnly) {
        this.label          = label;
        this.iconLiteral    = iconLiteral;
        this.fxmlPath       = fxmlPath;
        this.superAdminOnly = superAdminOnly;
    }

    public String  getLabel()       { return label; }
    public String  getIconLiteral() { return iconLiteral; }
    public String  getFxmlPath()    { return fxmlPath; }
    public boolean isSuperAdminOnly() { return superAdminOnly; }

    public boolean isVisibleFor(Admin admin) {
        if (!superAdminOnly) return true;
        return admin != null && admin.getAdminLevel() == Admin.Level.SUPER_ADMIN;
    }
}
