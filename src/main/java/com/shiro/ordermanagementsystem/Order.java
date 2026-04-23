package com.shiro.ordermanagementsystem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Order {

    private int             id;
    private String          orderCode;
    private int             customerId;
    private String          customerName;    // denormalized for admin list
    private OrderStatus     status;
    private BigDecimal      subtotal;
    private BigDecimal      tax;
    private BigDecimal      total;
    private String          shippingAddress;
    private String          contactNumber;
    private String          notes;
    private LocalDateTime   createdAt;
    private LocalDateTime   updatedAt;
    private List<OrderItem> items = new ArrayList<>();

    public Order() {}

    /** Backward-compatible constructor (no tax). */
    public Order(int id, String orderCode, int customerId, String customerName,
                 OrderStatus status, BigDecimal subtotal, BigDecimal total,
                 String shippingAddress, String contactNumber, String notes,
                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, orderCode, customerId, customerName, status,
             subtotal, BigDecimal.ZERO, total,
             shippingAddress, contactNumber, notes, createdAt, updatedAt);
    }

    public Order(int id, String orderCode, int customerId, String customerName,
                 OrderStatus status, BigDecimal subtotal, BigDecimal tax, BigDecimal total,
                 String shippingAddress, String contactNumber, String notes,
                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id              = id;
        this.orderCode       = orderCode;
        this.customerId      = customerId;
        this.customerName    = customerName;
        this.status          = status;
        this.subtotal        = subtotal;
        this.tax             = tax == null ? BigDecimal.ZERO : tax;
        this.total           = total;
        this.shippingAddress = shippingAddress;
        this.contactNumber   = contactNumber;
        this.notes           = notes;
        this.createdAt       = createdAt;
        this.updatedAt       = updatedAt;
    }

    public int             getId()              { return id; }
    public String          getOrderCode()       { return orderCode; }
    public int             getCustomerId()      { return customerId; }
    public String          getCustomerName()    { return customerName; }
    public OrderStatus     getStatus()          { return status; }
    public BigDecimal      getSubtotal()        { return subtotal; }
    public BigDecimal      getTax()             { return tax == null ? BigDecimal.ZERO : tax; }
    public BigDecimal      getTotal()           { return total; }
    public String          getShippingAddress() { return shippingAddress; }
    public String          getContactNumber()   { return contactNumber; }
    public String          getNotes()           { return notes; }
    public LocalDateTime   getCreatedAt()       { return createdAt; }
    public LocalDateTime   getUpdatedAt()       { return updatedAt; }
    public List<OrderItem> getItems()           { return items; }

    public void setItems(List<OrderItem> items) { this.items = items; }
    public void setStatus(OrderStatus status)   { this.status = status; }
    public void setTax(BigDecimal tax)          { this.tax = tax; }
}
