package com.shiro.ordermanagementsystem.nav;

/**
 * Data-driven customer sidebar menu. Add a new section = one enum entry + one FXML.
 */
public enum CustomerNav {

    DASHBOARD(    "Dashboard",     "fas-th-large",      "/ordermanagementsystem/fxml/customer/CustomerDashboardView.fxml"),
    FOOD_ORDER(   "Food Order",    "fas-utensils",      "/ordermanagementsystem/fxml/customer/FoodOrderView.fxml"),
    ORDER_HISTORY("Order History", "fas-receipt",       "/ordermanagementsystem/fxml/customer/MyOrdersView.fxml"),
    FEEDBACK(     "Feedback",      "fas-comment-dots",  "/ordermanagementsystem/fxml/customer/FeedbackView.fxml"),
    SETTINGS(     "Settings",      "fas-cog",           "/ordermanagementsystem/fxml/customer/CustomerSettingsView.fxml");

    private final String label;
    private final String iconLiteral;
    private final String fxmlPath;

    CustomerNav(String label, String iconLiteral, String fxmlPath) {
        this.label       = label;
        this.iconLiteral = iconLiteral;
        this.fxmlPath    = fxmlPath;
    }

    public String getLabel()       { return label; }
    public String getIconLiteral() { return iconLiteral; }
    public String getFxmlPath()    { return fxmlPath; }
}
