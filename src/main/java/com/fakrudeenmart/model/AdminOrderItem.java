package com.fakrudeenmart.model;

import java.math.BigDecimal;

public class AdminOrderItem {
    private Long id; private Long productId; private String productName; private String sellerName; private int quantity; private BigDecimal unitPrice; private BigDecimal subtotal;
    public Long getId() { return id; } public void setId(Long v) { id = v; }
    public Long getProductId() { return productId; } public void setProductId(Long v) { productId = v; }
    public String getProductName() { return productName; } public void setProductName(String v) { productName = v; }
    public String getSellerName() { return sellerName; } public void setSellerName(String v) { sellerName = v; }
    public int getQuantity() { return quantity; } public void setQuantity(int v) { quantity = v; }
    public BigDecimal getUnitPrice() { return unitPrice; } public void setUnitPrice(BigDecimal v) { unitPrice = v; }
    public BigDecimal getSubtotal() { return subtotal; } public void setSubtotal(BigDecimal v) { subtotal = v; }
}
