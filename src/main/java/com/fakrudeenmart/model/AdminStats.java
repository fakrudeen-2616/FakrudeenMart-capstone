package com.fakrudeenmart.model;

import java.math.BigDecimal;

public class AdminStats {
    private int totalUsers, buyers, sellers, activeUsers, inactiveUsers;
    private int totalProducts, activeProducts, inactiveProducts, outOfStockProducts;
    private int totalOrders, pendingOrders, processingOrders, shippedOrders, deliveredOrders, cancelledOrders;
    private BigDecimal totalSales = BigDecimal.ZERO;
    public int getTotalUsers() { return totalUsers; } public void setTotalUsers(int v) { totalUsers = v; }
    public int getBuyers() { return buyers; } public void setBuyers(int v) { buyers = v; }
    public int getSellers() { return sellers; } public void setSellers(int v) { sellers = v; }
    public int getActiveUsers() { return activeUsers; } public void setActiveUsers(int v) { activeUsers = v; }
    public int getInactiveUsers() { return inactiveUsers; } public void setInactiveUsers(int v) { inactiveUsers = v; }
    public int getTotalProducts() { return totalProducts; } public void setTotalProducts(int v) { totalProducts = v; }
    public int getActiveProducts() { return activeProducts; } public void setActiveProducts(int v) { activeProducts = v; }
    public int getInactiveProducts() { return inactiveProducts; } public void setInactiveProducts(int v) { inactiveProducts = v; }
    public int getOutOfStockProducts() { return outOfStockProducts; } public void setOutOfStockProducts(int v) { outOfStockProducts = v; }
    public int getTotalOrders() { return totalOrders; } public void setTotalOrders(int v) { totalOrders = v; }
    public int getPendingOrders() { return pendingOrders; } public void setPendingOrders(int v) { pendingOrders = v; }
    public int getProcessingOrders() { return processingOrders; } public void setProcessingOrders(int v) { processingOrders = v; }
    public int getShippedOrders() { return shippedOrders; } public void setShippedOrders(int v) { shippedOrders = v; }
    public int getDeliveredOrders() { return deliveredOrders; } public void setDeliveredOrders(int v) { deliveredOrders = v; }
    public int getCancelledOrders() { return cancelledOrders; } public void setCancelledOrders(int v) { cancelledOrders = v; }
    public BigDecimal getTotalSales() { return totalSales; } public void setTotalSales(BigDecimal v) { totalSales = v == null ? BigDecimal.ZERO : v; }
}
