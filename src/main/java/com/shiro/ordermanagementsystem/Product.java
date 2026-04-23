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
    private BigDecimal    originalPrice;   // nullable — when set & > price, show as strike-through
    private int           stock;
    private boolean       active;
    private String        imageUrl;        // local classpath path or remote URL
    private BigDecimal    rating;          // 0.00 .. 5.00
    private int           ratingCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Product() {}

    /** Backward-compatible constructor (no image / rating / original-price). */
    public Product(int id, String sku, String name, String description,
                   Integer categoryId, String categoryName,
                   BigDecimal price, int stock, boolean active,
                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, sku, name, description, categoryId, categoryName,
             price, null, stock, active, null, BigDecimal.ZERO, 0,
             createdAt, updatedAt);
    }

    public Product(int id, String sku, String name, String description,
                   Integer categoryId, String categoryName,
                   BigDecimal price, BigDecimal originalPrice,
                   int stock, boolean active,
                   String imageUrl, BigDecimal rating, int ratingCount,
                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id            = id;
        this.sku           = sku;
        this.name          = name;
        this.description   = description;
        this.categoryId    = categoryId;
        this.categoryName  = categoryName;
        this.price         = price;
        this.originalPrice = originalPrice;
        this.stock         = stock;
        this.active        = active;
        this.imageUrl      = imageUrl;
        this.rating        = rating == null ? BigDecimal.ZERO : rating;
        this.ratingCount   = ratingCount;
        this.createdAt     = createdAt;
        this.updatedAt     = updatedAt;
    }

    public int           getId()            { return id; }
    public String        getSku()           { return sku; }
    public String        getName()          { return name; }
    public String        getDescription()   { return description; }
    public Integer       getCategoryId()    { return categoryId; }
    public String        getCategoryName()  { return categoryName; }
    public BigDecimal    getPrice()         { return price; }
    public BigDecimal    getOriginalPrice() { return originalPrice; }
    public int           getStock()         { return stock; }
    public boolean       isActive()         { return active; }
    public String        getImageUrl()      { return imageUrl; }
    public BigDecimal    getRating()        { return rating == null ? BigDecimal.ZERO : rating; }
    public int           getRatingCount()   { return ratingCount; }
    public LocalDateTime getCreatedAt()     { return createdAt; }
    public LocalDateTime getUpdatedAt()     { return updatedAt; }

    public void setId(int id)                              { this.id = id; }
    public void setSku(String sku)                         { this.sku = sku; }
    public void setName(String name)                       { this.name = name; }
    public void setDescription(String description)         { this.description = description; }
    public void setCategoryId(Integer categoryId)          { this.categoryId = categoryId; }
    public void setCategoryName(String categoryName)       { this.categoryName = categoryName; }
    public void setPrice(BigDecimal price)                 { this.price = price; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
    public void setStock(int stock)                        { this.stock = stock; }
    public void setActive(boolean active)                  { this.active = active; }
    public void setImageUrl(String imageUrl)               { this.imageUrl = imageUrl; }
    public void setRating(BigDecimal rating)               { this.rating = rating; }
    public void setRatingCount(int ratingCount)            { this.ratingCount = ratingCount; }
    public void setCreatedAt(LocalDateTime createdAt)      { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt)      { this.updatedAt = updatedAt; }
}
