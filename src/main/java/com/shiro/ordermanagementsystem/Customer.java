package com.shiro.ordermanagementsystem;

public class Customer {

    private int    id;
    private String username;
    private String password;   // BCrypt hash — never expose raw
    private String fullName;
    private String email;
    private String phone;

    public Customer() {}

    public Customer(int id, String username, String password,
                    String fullName, String email, String phone) {
        this.id       = id;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email    = email;
        this.phone    = phone;
    }

    public int    getId()       { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getFullName() { return fullName; }
    public String getEmail()    { return email; }
    public String getPhone()    { return phone; }

    public void setId(int id)            { this.id = id; }
    public void setUsername(String u)    { this.username = u; }
    public void setPassword(String p)    { this.password = p; }
    public void setFullName(String name) { this.fullName = name; }
    public void setEmail(String email)   { this.email = email; }
    public void setPhone(String phone)   { this.phone = phone; }
}
