package com.shiro.ordermanagementsystem.ui;

import com.shiro.ordermanagementsystem.CustomerDAO;
import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Resolves a customer's avatar BLOB into a JavaFX {@link Image}, with a small
 * in-memory cache so the same avatar isn't re-fetched on every render. Mirrors
 * {@link ProductImages} so the two image pipelines behave the same way.
 */
public final class CustomerImages {

    private CustomerImages() {}

    private static final int CACHE_LIMIT = 32;

    /** Bounded LRU cache, keyed by customer id. */
    private static final Map<Integer, Image> CACHE = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<Integer, Image> eldest) {
                    return size() > CACHE_LIMIT;
                }
            });

    /** Drop a cached avatar (call after the customer uploads or removes their photo). */
    public static void invalidate(int customerId) {
        CACHE.remove(customerId);
    }

    /** Wipe the entire cache. */
    public static void invalidateAll() {
        CACHE.clear();
    }

    /**
     * Load the customer's avatar BLOB scaled to (width × height) preserving ratio.
     * Returns null if there is no uploaded avatar. Result is cached by id.
     */
    public static Image loadForCustomer(int customerId, double width, double height) {
        Image cached = CACHE.get(customerId);
        if (cached != null) return cached;

        byte[] bytes = CustomerDAO.loadAvatarBytes(customerId);
        Image img = fromBytes(bytes, width, height);
        if (img != null) CACHE.put(customerId, img);
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
}
