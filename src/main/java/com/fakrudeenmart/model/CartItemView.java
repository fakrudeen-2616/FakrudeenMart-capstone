package com.fakrudeenmart.model;

import java.math.BigDecimal;

public class CartItemView {
    private Long itemId;
    private Long productId;
    private String productName;
    private String imageUrl;
    private BigDecimal unitPrice;
    private int quantity;
    private int stockQuantity;
    private ProductStatus productStatus;

    public CartItemView() {}
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }
    public ProductStatus getProductStatus() { return productStatus; }
    public void setProductStatus(ProductStatus productStatus) { this.productStatus = productStatus; }
    public boolean isAvailable() { return productStatus == ProductStatus.ACTIVE && stockQuantity > 0 && quantity <= stockQuantity; }
    public String getAvailabilityMessage() {
        if (productStatus != ProductStatus.ACTIVE) return "Product unavailable";
        if (stockQuantity <= 0) return "Out of stock";
        if (quantity > stockQuantity) return "Only " + stockQuantity + " available";
        return "In stock";
    }
    public BigDecimal getSubtotal() { return unitPrice == null ? BigDecimal.ZERO : unitPrice.multiply(BigDecimal.valueOf(quantity)); }
}
