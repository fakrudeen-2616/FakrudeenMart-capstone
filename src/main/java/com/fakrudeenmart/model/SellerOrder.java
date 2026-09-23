package com.fakrudeenmart.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SellerOrder {
    private Long orderId;
    private Long buyerId;
    private String buyerName;
    private LocalDateTime createdAt;
    private OrderStatus status;
    private BigDecimal sellerSubtotal = BigDecimal.ZERO;
    private List<OrderItem> items = new ArrayList<>();
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long value) { orderId = value; }
    public Long getBuyerId() { return buyerId; }
    public void setBuyerId(Long value) { buyerId = value; }
    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String value) { buyerName = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt = value; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus value) { status = value; }
    public BigDecimal getSellerSubtotal() { return sellerSubtotal; }
    public void setSellerSubtotal(BigDecimal value) { sellerSubtotal = value == null ? BigDecimal.ZERO : value; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> value) { items = value == null ? new ArrayList<>() : value; }
    public void addItem(OrderItem item) { items.add(item); sellerSubtotal = sellerSubtotal.add(item.getSubtotal()); }
}
