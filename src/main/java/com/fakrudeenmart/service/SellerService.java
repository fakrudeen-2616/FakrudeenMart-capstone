package com.fakrudeenmart.service;

import com.fakrudeenmart.dao.ProductDAO;
import com.fakrudeenmart.dao.SellerDAO;
import com.fakrudeenmart.model.Category;
import com.fakrudeenmart.model.OrderStatus;
import com.fakrudeenmart.model.Product;
import com.fakrudeenmart.model.ProductStatus;
import com.fakrudeenmart.model.SellerOrder;
import com.fakrudeenmart.model.SellerStats;
import com.fakrudeenmart.model.User;
import com.fakrudeenmart.model.UserRole;
import com.fakrudeenmart.model.UserStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class SellerService {
    private final SellerDAO sellerDAO;
    private final ProductDAO productDAO;
    private final CategoryService categoryService;
    private final UserService userService;

    public SellerService() { this(new SellerDAO(), new ProductDAO(), new CategoryService(), new UserService()); }
    public SellerService(SellerDAO sellerDAO, ProductDAO productDAO, CategoryService categoryService, UserService userService) { this.sellerDAO = sellerDAO; this.productDAO = productDAO; this.categoryService = categoryService; this.userService = userService; }

    public User assertSeller(Long sellerId) {
        if (sellerId == null || sellerId <= 0) throw new SellerException("You must be signed in as a seller.");
        User user = userService.findById(sellerId).orElseThrow(() -> new SellerException("You must be signed in as a seller."));
        if (user.getRole() != UserRole.SELLER || user.getStatus() != UserStatus.ACTIVE) throw new SellerException("Only active sellers can use seller management.");
        return user;
    }

    public SellerStats getDashboardStatistics(Long sellerId) { assertSeller(sellerId); return sellerDAO.getStats(sellerId); }
    public List<Product> getSellerProducts(Long sellerId) { assertSeller(sellerId); return sellerDAO.findProductsBySellerId(sellerId); }
    public Product getSellerProduct(Long sellerId, Long productId) { assertSeller(sellerId); return ownedProduct(sellerId, productId); }

    public Product createProduct(Long sellerId, Long categoryId, String name, String description, BigDecimal price, int stock, String imageUrl, ProductStatus status) {
        assertSeller(sellerId); Product product = validatedProduct(sellerId, categoryId, name, description, price, stock, imageUrl, status); return productDAO.create(product);
    }

    public Product updateProduct(Long sellerId, Long productId, Long categoryId, String name, String description, BigDecimal price, int stock, String imageUrl, ProductStatus status) {
        assertSeller(sellerId); Product existing = ownedProduct(sellerId, productId); Product replacement = validatedProduct(sellerId, categoryId, name, description, price, stock, imageUrl, status); replacement.setId(existing.getId()); replacement.setCreatedAt(existing.getCreatedAt()); productDAO.update(replacement); return replacement;
    }

    public Product updateStock(Long sellerId, Long productId, int stock) { assertSeller(sellerId); if (stock < 0) throw new SellerException("Stock cannot be negative."); Product product = ownedProduct(sellerId, productId); product.setStockQuantity(stock); productDAO.update(product); return product; }
    public Product activateProduct(Long sellerId, Long productId) { return updateStatus(sellerId, productId, ProductStatus.ACTIVE); }
    public Product deactivateProduct(Long sellerId, Long productId) { return updateStatus(sellerId, productId, ProductStatus.INACTIVE); }
    private Product updateStatus(Long sellerId, Long productId, ProductStatus status) { assertSeller(sellerId); Product product = ownedProduct(sellerId, productId); product.setStatus(status); productDAO.update(product); return product; }

    public List<SellerOrder> getSellerOrders(Long sellerId) { assertSeller(sellerId); return sellerDAO.findOrdersBySellerId(sellerId); }
    public SellerOrder getSellerOrder(Long sellerId, Long orderId) { assertSeller(sellerId); if (orderId == null || orderId <= 0) throw new SellerException("Order was not found."); return sellerDAO.findOrderByIdAndSellerId(orderId, sellerId).orElseThrow(() -> new SellerException("Order was not found.")); }

    public SellerOrder updateOrderStatus(Long sellerId, Long orderId, OrderStatus target) {
        assertSeller(sellerId); if (target == null) throw new SellerException("Order status is required."); SellerOrder current = getSellerOrder(sellerId, orderId);
        if (!allowed(current.getStatus(), target)) throw new SellerException("That order status transition is not allowed.");
        if (!sellerDAO.updateOrderStatus(orderId, sellerId, target)) throw new SellerException("Order was not found.");
        return getSellerOrder(sellerId, orderId);
    }

    public User getSellerProfile(Long sellerId) { return assertSeller(sellerId); }

    public User updateSellerProfile(Long sellerId, String fullName, String phone) {
        User seller = assertSeller(sellerId);
        if (fullName == null || fullName.trim().isEmpty() || fullName.trim().length() > 120) {
            throw new SellerException("Name is required and must be at most 120 characters.");
        }
        if (phone != null && !phone.isBlank() && !phone.matches("^[0-9+()\\-\\s]{7,30}$")) {
            throw new SellerException("Invalid phone number.");
        }
        userService.updateProfile(seller.getId(), fullName.trim(), phone == null ? "" : phone.trim());
        return userService.findById(seller.getId()).orElseThrow(() -> new SellerException("Seller was not found."));
    }

    private Product ownedProduct(Long sellerId, Long productId) { if (productId == null || productId <= 0) throw new SellerException("Product was not found."); return sellerDAO.findProductByIdAndSellerId(productId, sellerId).orElseThrow(() -> new SellerException("Product was not found.")); }
    private Product validatedProduct(Long sellerId, Long categoryId, String name, String description, BigDecimal price, int stock, String imageUrl, ProductStatus status) {
        if (name == null || name.trim().isEmpty() || name.trim().length() > 160) throw new SellerException("Product name is required and must be at most 160 characters.");
        if (description == null || description.trim().isEmpty() || description.trim().length() > 2000) throw new SellerException("Product description is required and must be at most 2000 characters.");
        if (categoryId == null || categoryId <= 0 || categoryService.getCategoryById(categoryId).isEmpty()) throw new SellerException("Select a valid category.");
        if (price == null || price.signum() <= 0 || price.scale() > 2) throw new SellerException("Price must be greater than zero with at most two decimals.");
        if (stock < 0) throw new SellerException("Stock cannot be negative.");
        if (imageUrl != null && (imageUrl.length() > 1000 || imageUrl.contains("\n") || imageUrl.contains("\r") || imageUrl.contains(" "))) throw new SellerException("Image URL is invalid.");
        ProductStatus safeStatus = status == null ? ProductStatus.ACTIVE : status;
        return new Product(sellerId, categoryId, name.trim(), description.trim(), price, stock, imageUrl == null ? "" : imageUrl.trim(), safeStatus);
    }
    private boolean allowed(OrderStatus from, OrderStatus to) {
        if (from == null || to == null || from == OrderStatus.CANCELLED || from == OrderStatus.DELIVERED) return false;
        if (to == OrderStatus.CANCELLED) return true;
        return (from == OrderStatus.PENDING && to == OrderStatus.CONFIRMED)
                || (from == OrderStatus.CONFIRMED && to == OrderStatus.PROCESSING)
                || (from == OrderStatus.PROCESSING && to == OrderStatus.SHIPPED)
                || (from == OrderStatus.SHIPPED && to == OrderStatus.DELIVERED);
    }
    public static class SellerException extends RuntimeException { public SellerException(String message) { super(message); } }
}
