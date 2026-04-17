package com.shiro.ordermanagementsystem;

public class User {

    public enum Role { ADMIN, CUSTOMER }

    private int    id;
    private String username;
    private String password;   // BCrypt hash — never expose raw
    private Role   role;
    private String fullName;
    private String email;

    public User() {}

    public User(int id, String username, String password, Role role, String fullName, String email) {
        this.id       = id;
        this.username = username;
        this.password = password;
        this.role     = role;
        this.fullName = fullName;
        this.email    = email;
    }

    // ── Getters ───────────────────────────────────────────────
    public int    getId()       { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public Role   getRole()     { return role; }
    public String getFullName() { return fullName; }
    public String getEmail()    { return email; }

    // ── Setters ───────────────────────────────────────────────
    public void setId(int id)             { this.id = id; }
    public void setUsername(String u)     { this.username = u; }
    public void setPassword(String p)     { this.password = p; }
    public void setRole(Role r)           { this.role = r; }
    public void setFullName(String name)  { this.fullName = name; }
    public void setEmail(String email)    { this.email = email; }
}