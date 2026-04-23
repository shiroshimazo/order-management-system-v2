package com.shiro.ordermanagementsystem;

import java.time.LocalDateTime;

public class Customer {

    private int           id;
    private String        username;
    private String        password;   // BCrypt hash — never expose raw
    private String        fullName;
    private String        email;
    private String        phone;
    private boolean       active = true;
    private LocalDateTime createdAt;

    public Customer() {}

    public Customer(int id, String username, String password,
                    String fullName, String email, String phone) {
        this(id, username, password, fullName, email, phone, true, null);
    }

    public Customer(int id, String username, String password,
                    String fullName, String email, String phone,
                    boolean active, LocalDateTime createdAt) {
        this.id        = id;
        this.username  = username;
        this.password  = password;
        this.fullName  = fullName;
        this.email     = email;
        this.phone     = phone;
        this.active    = active;
        this.createdAt = createdAt;
    }

    public int           getId()        { return id; }
    public String        getUsername()  { return username; }
    public String        getPassword()  { return password; }
    public String        getFullName()  { return fullName; }
    public String        getEmail()     { return email; }
    public String        getPhone()     { return phone; }
    public boolean       isActive()     { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(int id)                    { this.id = id; }
    public void setUsername(String u)            { this.username = u; }
    public void setPassword(String p)            { this.password = p; }
    public void setFullName(String name)         { this.fullName = name; }
    public void setEmail(String email)           { this.email = email; }
    public void setPhone(String phone)           { this.phone = phone; }
    public void setActive(boolean active)        { this.active = active; }
    public void setCreatedAt(LocalDateTime cat)  { this.createdAt = cat; }
}
