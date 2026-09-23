package com.fakrudeenmart.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AdminOrder {
    private Long id; private Long buyerId; private String buyerName; private String buyerEmail; private String buyerPhone; private LocalDateTime createdAt; private OrderStatus status; private BigDecimal totalAmount = BigDecimal.ZERO; private List<AdminOrderItem> items = new ArrayList<>();
    public Long getId() { return id; } public void setId(Long v) { id = v; }
    public Long getBuyerId() { return buyerId; } public void setBuyerId(Long v) { buyerId = v; }
    public String getBuyerName() { return buyerName; } public void setBuyerName(String v) { buyerName = v; }
    public String getBuyerEmail() { return buyerEmail; } public void setBuyerEmail(String v) { buyerEmail = v; }
    public String getBuyerPhone() { return buyerPhone; } public void setBuyerPhone(String v) { buyerPhone = v; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime v) { createdAt = v; }
    public OrderStatus getStatus() { return status; } public void setStatus(OrderStatus v) { status = v; }
    public BigDecimal getTotalAmount() { return totalAmount; } public void setTotalAmount(BigDecimal v) { totalAmount = v == null ? BigDecimal.ZERO : v; }
    public List<AdminOrderItem> getItems() { return items; } public void setItems(List<AdminOrderItem> v) { items = v == null ? new ArrayList<>() : v; }
}
