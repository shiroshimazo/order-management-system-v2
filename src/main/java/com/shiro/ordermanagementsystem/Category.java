package com.shiro.ordermanagementsystem;

import java.time.LocalDateTime;

public class Category {

    private int           id;
    private String        name;
    private String        description;
    private LocalDateTime createdAt;

    public Category() {}

    public Category(int id, String name, String description, LocalDateTime createdAt) {
        this.id          = id;
        this.name        = name;
        this.description = description;
        this.createdAt   = createdAt;
    }

    public int           getId()          { return id; }
    public String        getName()        { return name; }
    public String        getDescription() { return description; }
    public LocalDateTime getCreatedAt()   { return createdAt; }

    public void setName(String name)               { this.name = name; }
    public void setDescription(String description) { this.description = description; }

    @Override public String toString() { return name; }
}
