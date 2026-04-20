package com.shiro.ordermanagementsystem;

public enum OrderStatus {
    PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED;

    public String label() {
        return switch (this) {
            case PENDING    -> "Pending";
            case PROCESSING -> "Processing";
            case SHIPPED    -> "Shipped";
            case DELIVERED  -> "Delivered";
            case CANCELLED  -> "Cancelled";
        };
    }

    public String cssClass() {
        return "status-" + name().toLowerCase();
    }
}
