package com.shiro.ordermanagementsystem;

import java.math.BigDecimal;

public class OrderItem {

    private int        id;
    private int        orderId;
    private Integer    productId;       // nullable if product later deleted
    private String     productName;     // snapshot
    private String     productSku;      // snapshot
    private BigDecimal unitPrice;       // snapshot
    private int        quantity;
    private BigDecimal lineTotal;

    public OrderItem() {}

    public OrderItem(int id, int orderId, Integer productId, String productName,
                     String productSku, BigDecimal unitPrice, int quantity,
                     BigDecimal lineTotal) {
        this.id          = id;
        this.orderId     = orderId;
        this.productId   = productId;
        this.productName = productName;
        this.productSku  = productSku;
        this.unitPrice   = unitPrice;
        this.quantity    = quantity;
        this.lineTotal   = lineTotal;
    }

    public int        getId()          { return id; }
    public int        getOrderId()     { return orderId; }
    public Integer    getProductId()   { return productId; }
    public String     getProductName() { return productName; }
    public String     getProductSku()  { return productSku; }
    public BigDecimal getUnitPrice()   { return unitPrice; }
    public int        getQuantity()    { return quantity; }
    public BigDecimal getLineTotal()   { return lineTotal; }
}
