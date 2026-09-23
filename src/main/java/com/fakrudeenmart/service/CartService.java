package com.fakrudeenmart.service;

import com.fakrudeenmart.dao.CartDAO;
import com.fakrudeenmart.dao.CartItemDAO;
import com.fakrudeenmart.dao.DataAccessException;
import com.fakrudeenmart.model.Cart;
import com.fakrudeenmart.model.CartItem;
import com.fakrudeenmart.model.CartItemView;
import com.fakrudeenmart.model.Product;
import com.fakrudeenmart.model.User;
import com.fakrudeenmart.model.UserRole;
import com.fakrudeenmart.model.UserStatus;

import java.math.BigDecimal;
import java.util.List;

public class CartService {
    public static final int MAX_QUANTITY = 99;
    private final CartDAO cartDAO;
    private final CartItemDAO cartItemDAO;
    private final ProductService productService;
    private final UserService userService;

    public CartService() { this(new CartDAO(), new CartItemDAO(), new ProductService(), new UserService()); }
    public CartService(CartDAO cartDAO, CartItemDAO cartItemDAO, ProductService productService, UserService userService) {
        this.cartDAO = cartDAO; this.cartItemDAO = cartItemDAO; this.productService = productService; this.userService = userService;
    }

    public Cart getCart(Long userId) {
        assertBuyer(userId);
        Cart cart = cartDAO.getOrCreateCart(userId);
        List<CartItemView> views = cartItemDAO.findViewsByCartId(cart.getId());
        cart.setItems(views);
        cart.setTotalItems(views.stream().mapToInt(CartItemView::getQuantity).sum());
        cart.setSubtotal(calculateSubtotal(views));
        return cart;
    }

    public Cart addToCart(Long userId, Long productId, int requestedQuantity) {
        assertBuyer(userId);
        validateQuantity(requestedQuantity);
        Product product = productService.getProductById(productId).orElseThrow(() -> new CartException("This product is currently unavailable."));
        if (product.getStockQuantity() <= 0) throw new CartException("This product is out of stock.");
        Cart cart = cartDAO.getOrCreateCart(userId);
        CartItem existing = cartItemDAO.findByCartAndProduct(cart.getId(), productId).orElse(null);
        int newQuantity = requestedQuantity + (existing == null ? 0 : existing.getQuantity());
        validateQuantity(newQuantity);
        validateStock(newQuantity, product.getStockQuantity());
        if (existing == null) cartItemDAO.addItem(new CartItem(cart.getId(), productId, newQuantity));
        else cartItemDAO.updateQuantity(existing.getId(), newQuantity);
        return getCart(userId);
    }

    public Cart updateQuantity(Long userId, Long itemId, int quantity) {
        assertBuyer(userId);
        validateQuantity(quantity);
        Cart cart = ownedCart(userId);
        CartItem item = ownedItem(cart, itemId);
        Product product = productService.getProductById(item.getProductId()).orElseThrow(() -> new CartException("This product is currently unavailable."));
        validateStock(quantity, product.getStockQuantity());
        cartItemDAO.updateQuantity(item.getId(), quantity);
        return getCart(userId);
    }

    public Cart removeItem(Long userId, Long itemId) {
        assertBuyer(userId);
        Cart cart = ownedCart(userId);
        CartItem item = ownedItem(cart, itemId);
        cartItemDAO.removeItem(item.getId());
        return getCart(userId);
    }

    public Cart clearCart(Long userId) {
        assertBuyer(userId);
        Cart cart = ownedCart(userId);
        cartItemDAO.clearCart(cart.getId());
        return getCart(userId);
    }

    public BigDecimal calculateSubtotal(Cart cart) { return cart == null ? BigDecimal.ZERO : calculateSubtotal(cart.getItems()); }
    public BigDecimal calculateTotal(Cart cart) { return calculateSubtotal(cart); }

    private BigDecimal calculateSubtotal(List<CartItemView> items) {
        return items.stream().map(CartItemView::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Cart ownedCart(Long userId) {
        return cartDAO.findByUserId(userId).orElseThrow(() -> new CartException("Cart was not found."));
    }

    private CartItem ownedItem(Cart cart, Long itemId) {
        if (itemId == null || itemId <= 0) throw new CartException("Cart item was not found.");
        CartItem item = cartItemDAO.findById(itemId).orElseThrow(() -> new CartException("Cart item was not found."));
        if (!cart.getId().equals(item.getCartId())) throw new CartException("You cannot modify another buyer's cart.");
        return item;
    }

    private void assertBuyer(Long userId) {
        if (userId == null || userId <= 0) throw new CartException("You must be signed in as a buyer.");
        User user;
        try { user = userService.findById(userId).orElseThrow(() -> new CartException("You must be signed in as a buyer.")); }
        catch (DataAccessException exception) { throw exception; }
        if (user.getRole() != UserRole.BUYER || user.getStatus() != UserStatus.ACTIVE) throw new CartException("Only active buyers can use a shopping cart.");
    }

    private void validateQuantity(int quantity) {
        if (quantity < 1 || quantity > MAX_QUANTITY) throw new CartException("Quantity must be between 1 and " + MAX_QUANTITY + ".");
    }

    private void validateStock(int quantity, int stock) {
        if (quantity > stock) throw new CartException("Only " + stock + " units are available.");
    }

    public static class CartException extends RuntimeException {
        public CartException(String message) { super(message); }
    }
}
