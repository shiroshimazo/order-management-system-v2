package com.shiro.ordermanagementsystem;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Product {

    private int           id;
    private String        sku;
    private String        name;
    private String        description;
    private Integer       categoryId;      // nullable FK
    private String        categoryName;    // denormalized for list views
    private BigDecimal    price;
    private int           stock;
    private boolean       active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Product() {}

    public Product(int id, String sku, String name, String description,
                   Integer categoryId, String categoryName,
                   BigDecimal price, int stock, boolean active,
                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id           = id;
        this.sku          = sku;
        this.name         = name;
        this.description  = description;
        this.categoryId   = categoryId;
        this.categoryName = categoryName;
        this.price        = price;
        this.stock        = stock;
        this.active       = active;
        this.createdAt    = createdAt;
        this.updatedAt    = updatedAt;
    }

    public int           getId()           { return id; }
    public String        getSku()          { return sku; }
    public String        getName()         { return name; }
    public String        getDescription()  { return description; }
    public Integer       getCategoryId()   { return categoryId; }
    public String        getCategoryName() { return categoryName; }
    public BigDecimal    getPrice()        { return price; }
    public int           getStock()        { return stock; }
    public boolean       isActive()        { return active; }
    public LocalDateTime getCreatedAt()    { return createdAt; }
    public LocalDateTime getUpdatedAt()    { return updatedAt; }

    public void setSku(String sku)                 { this.sku = sku; }
    public void setName(String name)               { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setCategoryId(Integer categoryId)  { this.categoryId = categoryId; }
    public void setPrice(BigDecimal price)         { this.price = price; }
    public void setStock(int stock)                { this.stock = stock; }
    public void setActive(boolean active)          { this.active = active; }
}
