package com.shiro.ordermanagementsystem.ui;

import javafx.scene.image.Image;

import java.net.URL;

/**
 * Resolves a product's image_url into a JavaFX Image, supporting either
 * remote URLs (http/https), classpath resources (e.g. "/ordermanagementsystem/images/…"),
 * or file:/jar:/data: URIs. Returns null if the source can't be resolved.
 */
public final class ProductImages {

    private ProductImages() {}

    public static Image load(String urlOrPath, double width, double height) {
        if (urlOrPath == null || urlOrPath.isBlank()) return null;

        String trimmed = urlOrPath.trim();
        try {
            String resolved;
            if (trimmed.startsWith("http://")  || trimmed.startsWith("https://") ||
                trimmed.startsWith("file:")    || trimmed.startsWith("jar:")     ||
                trimmed.startsWith("data:")) {
                resolved = trimmed;
            } else {
                String path = trimmed.startsWith("/") ? trimmed : "/" + trimmed;
                URL res = ProductImages.class.getResource(path);
                if (res == null) return null;
                resolved = res.toExternalForm();
            }
            return new Image(resolved, width, height, true, true, true);
        } catch (Exception ex) {
            return null;
        }
    }
}
