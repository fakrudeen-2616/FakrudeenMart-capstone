package com.fakrudeenmart.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CheckoutData {
    private List<OrderItem> items = new ArrayList<>();
    private BigDecimal total = BigDecimal.ZERO;
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items == null ? new ArrayList<>() : items; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total == null ? BigDecimal.ZERO : total; }
    public boolean isEmpty() { return items.isEmpty(); }
}
