package com.fakrudeenmart.model;

import java.math.BigDecimal;

public class SellerStats {
    private int totalProducts;
    private int activeProducts;
    private int outOfStockProducts;
    private int totalOrders;
    private int pendingOrders;
    private int processingOrders;
    private int shippedOrders;
    private int deliveredOrders;
    private BigDecimal totalSales = BigDecimal.ZERO;
    public int getTotalProducts() { return totalProducts; }
    public void setTotalProducts(int value) { totalProducts = value; }
    public int getActiveProducts() { return activeProducts; }
    public void setActiveProducts(int value) { activeProducts = value; }
    public int getOutOfStockProducts() { return outOfStockProducts; }
    public void setOutOfStockProducts(int value) { outOfStockProducts = value; }
    public int getTotalOrders() { return totalOrders; }
    public void setTotalOrders(int value) { totalOrders = value; }
    public int getPendingOrders() { return pendingOrders; }
    public void setPendingOrders(int value) { pendingOrders = value; }
    public int getProcessingOrders() { return processingOrders; }
    public void setProcessingOrders(int value) { processingOrders = value; }
    public int getShippedOrders() { return shippedOrders; }
    public void setShippedOrders(int value) { shippedOrders = value; }
    public int getDeliveredOrders() { return deliveredOrders; }
    public void setDeliveredOrders(int value) { deliveredOrders = value; }
    public BigDecimal getTotalSales() { return totalSales; }
    public void setTotalSales(BigDecimal value) { totalSales = value == null ? BigDecimal.ZERO : value; }
}
