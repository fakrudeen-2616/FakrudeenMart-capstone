package com.fakrudeenmart.model;

import java.math.BigDecimal;

public class BuyerStats { private int totalOrders; private BigDecimal totalSpent = BigDecimal.ZERO; public int getTotalOrders() { return totalOrders; } public void setTotalOrders(int v) { totalOrders = v; } public BigDecimal getTotalSpent() { return totalSpent; } public void setTotalSpent(BigDecimal v) { totalSpent = v == null ? BigDecimal.ZERO : v; } }
