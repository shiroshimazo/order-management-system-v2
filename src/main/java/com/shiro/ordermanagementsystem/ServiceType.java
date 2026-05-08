package com.shiro.ordermanagementsystem;

public enum ServiceType {
    DELIVERY, DINE_IN, TAKE_OUT;

    public String label() {
        return switch (this) {
            case DELIVERY -> "Delivery";
            case DINE_IN  -> "Dine-In";
            case TAKE_OUT -> "Take-Out";
        };
    }

    public String cssClass() {
        return "service-" + name().toLowerCase().replace('_', '-');
    }

    public static ServiceType parse(String raw) {
        if (raw == null) return DELIVERY;
        try { return ServiceType.valueOf(raw.trim().toUpperCase()); }
        catch (IllegalArgumentException ex) { return DELIVERY; }
    }
}
