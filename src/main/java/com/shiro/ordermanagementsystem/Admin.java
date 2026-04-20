package com.shiro.ordermanagementsystem;

import java.time.LocalDateTime;

public class Admin {

    public enum Level { SUPER_ADMIN, MANAGER, STAFF }

    private int           id;
    private String        adminCode;       // e.g. ADMIN-001
    private String        username;
    private String        password;        // BCrypt hash
    private String        email;
    private String        fullName;
    private String        phone;
    private Level         adminLevel;
    private String        position;
    private boolean       active;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private Integer       createdBy;       // nullable FK → admin.id

    public Admin() {}

    public Admin(int id, String adminCode, String username, String password,
                 String email, String fullName, String phone, Level adminLevel,
                 String position, boolean active, LocalDateTime createdAt,
                 LocalDateTime lastLoginAt, Integer createdBy) {
        this.id          = id;
        this.adminCode   = adminCode;
        this.username    = username;
        this.password    = password;
        this.email       = email;
        this.fullName    = fullName;
        this.phone       = phone;
        this.adminLevel  = adminLevel;
        this.position    = position;
        this.active      = active;
        this.createdAt   = createdAt;
        this.lastLoginAt = lastLoginAt;
        this.createdBy   = createdBy;
    }

    public int           getId()          { return id; }
    public String        getAdminCode()   { return adminCode; }
    public String        getUsername()    { return username; }
    public String        getPassword()    { return password; }
    public String        getEmail()       { return email; }
    public String        getFullName()    { return fullName; }
    public String        getPhone()       { return phone; }
    public Level         getAdminLevel()  { return adminLevel; }
    public String        getPosition()    { return position; }
    public boolean       isActive()       { return active; }
    public LocalDateTime getCreatedAt()   { return createdAt; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public Integer       getCreatedBy()   { return createdBy; }

    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email)       { this.email = email; }
    public void setPhone(String phone)       { this.phone = phone; }
    public void setPosition(String position) { this.position = position; }
    public void setPassword(String password) { this.password = password; }
}
