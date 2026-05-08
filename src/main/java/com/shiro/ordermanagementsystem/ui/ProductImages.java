package com.shiro.ordermanagementsystem.ui;

import com.shiro.ordermanagementsystem.ProductDAO;
import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Resolves a product image into a JavaFX {@link Image}. Two sources:
 *   1. {@link #loadForProduct(int, double, double)}  — byte BLOB stored in product.image_data,
 *      with a small in-memory cache so the same product isn't re-fetched on every grid render.
 *   2. {@link #load(String, double, double)}         — legacy URL or classpath path
 *      (kept for any code still passing image_url strings).
 */
public final class ProductImages {

    private ProductImages() {}

    // ─── In-memory cache for DB-backed images ─────────────────────────────────
    private static final int CACHE_LIMIT = 64;

    /** Bounded LRU cache, keyed by product id. */
    private static final Map<Integer, Image> CACHE = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<Integer, Image> eldest) {
                    return size() > CACHE_LIMIT;
                }
            });

    /** Drop a cached image (call after admin uploads a new one for that product). */
    public static void invalidate(int productId) {
        CACHE.remove(productId);
    }

    /** Wipe the entire cache. */
    public static void invalidateAll() {
        CACHE.clear();
    }

    /**
     * Load the product's BLOB photo, scaled to (width × height) preserving ratio.
     * Returns null if the product has no uploaded image. Result is cached by id.
     */
    public static Image loadForProduct(int productId, double width, double height) {
        Image cached = CACHE.get(productId);
        if (cached != null) return cached;

        byte[] bytes = ProductDAO.loadImageBytes(productId);
        Image img = fromBytes(bytes, width, height);
        if (img != null) CACHE.put(productId, img);
        return img;
    }

    /** Build an Image straight from raw bytes. Returns null if bytes is null/empty/corrupt. */
    public static Image fromBytes(byte[] bytes, double width, double height) {
        if (bytes == null || bytes.length == 0) return null;
        try {
            return new Image(new ByteArrayInputStream(bytes), width, height, true, true);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Legacy loader: resolve a URL string or classpath path into an Image.
     * Kept so any leftover callers continue to compile; new code should use
     * {@link #loadForProduct(int, double, double)} instead.
     */
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
