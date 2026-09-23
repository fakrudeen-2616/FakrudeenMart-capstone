package com.fakrudeenmart.model;

import java.time.LocalDateTime;

public class CartItem {
    private Long id;
    private Long cartId;
    private Long productId;
    private int quantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CartItem() {}
    public CartItem(Long cartId, Long productId, int quantity) { this.cartId = cartId; this.productId = productId; this.quantity = quantity; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCartId() { return cartId; }
    public void setCartId(Long cartId) { this.cartId = cartId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
