package com.shiro.ordermanagementsystem.session;

import com.shiro.ordermanagementsystem.Product;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.math.BigDecimal;

/**
 * Session-scoped shopping cart for the logged-in customer.
 * Kept as a JavaFX ObservableList so views can bind directly.
 */
public final class Cart {

    private static final ObservableList<Line> lines = FXCollections.observableArrayList();

    private Cart() {}

    public static ObservableList<Line> lines() { return lines; }

    public static void add(Product product, int quantity) {
        if (product == null || quantity <= 0) return;
        for (Line l : lines) {
            if (l.product.getId() == product.getId()) {
                l.quantity += quantity;
                // trigger observable refresh
                int idx = lines.indexOf(l);
                lines.set(idx, l);
                return;
            }
        }
        lines.add(new Line(product, quantity));
    }

    public static void setQuantity(Line line, int quantity) {
        if (line == null) return;
        if (quantity <= 0) { lines.remove(line); return; }
        line.quantity = quantity;
        int idx = lines.indexOf(line);
        if (idx >= 0) lines.set(idx, line);
    }

    public static void remove(Line line) { lines.remove(line); }

    public static void clear() { lines.clear(); }

    public static int itemCount() {
        int n = 0;
        for (Line l : lines) n += l.quantity;
        return n;
    }

    public static BigDecimal total() {
        BigDecimal sum = BigDecimal.ZERO;
        for (Line l : lines) sum = sum.add(l.subtotal());
        return sum;
    }

    public static class Line {
        public Product product;
        public int     quantity;

        public Line(Product product, int quantity) {
            this.product  = product;
            this.quantity = quantity;
        }

        public BigDecimal subtotal() {
            return product.getPrice().multiply(BigDecimal.valueOf(quantity));
        }
    }
}
