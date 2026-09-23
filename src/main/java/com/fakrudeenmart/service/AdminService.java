package com.fakrudeenmart.service;

import com.fakrudeenmart.dao.AdminDAO;
import com.fakrudeenmart.dao.SellerDAO;
import com.fakrudeenmart.model.AdminOrder;
import com.fakrudeenmart.model.AdminStats;
import com.fakrudeenmart.model.BuyerStats;
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

public class AdminService {
    private final AdminDAO adminDAO; private final SellerDAO sellerDAO; private final UserService userService; private final CategoryService categoryService;
    public AdminService() { this(new AdminDAO(), new SellerDAO(), new UserService(), new CategoryService()); }
    public AdminService(AdminDAO adminDAO, SellerDAO sellerDAO, UserService userService, CategoryService categoryService) { this.adminDAO=adminDAO; this.sellerDAO=sellerDAO; this.userService=userService; this.categoryService=categoryService; }

    public User assertAdmin(Long adminId) { if (adminId == null || adminId <= 0) throw new AdminException("You must be signed in as an administrator."); User admin = userService.findById(adminId).orElseThrow(() -> new AdminException("You must be signed in as an administrator.")); if (admin.getRole()!=UserRole.ADMIN || admin.getStatus()!=UserStatus.ACTIVE) throw new AdminException("Only an active administrator can use this area."); return admin; }
    public AdminStats getDashboardStatistics(Long adminId) { assertAdmin(adminId); return adminDAO.getStats(); }
    public List<User> getUsers(Long adminId, String search, UserRole role, UserStatus status) { assertAdmin(adminId); return adminDAO.findUsers(search, role, status, 300); }
    public List<User> getBuyers(Long adminId, String search, UserStatus status) { return getUsers(adminId, search, UserRole.BUYER, status); }
    public List<User> getSellers(Long adminId, String search, UserStatus status) { return getUsers(adminId, search, UserRole.SELLER, status); }
    public User getAdminProfile(Long adminId) { return assertAdmin(adminId); }
    public User getBuyer(Long adminId, Long buyerId) { assertAdmin(adminId); return adminDAO.findUser(validId(buyerId,"Buyer was not found."), UserRole.BUYER).orElseThrow(() -> new AdminException("Buyer was not found.")); }
    public User getSeller(Long adminId, Long sellerId) { assertAdmin(adminId); return adminDAO.findUser(validId(sellerId,"Seller was not found."), UserRole.SELLER).orElseThrow(() -> new AdminException("Seller was not found.")); }
    public BuyerStats getBuyerStats(Long adminId, Long buyerId) { User buyer=getBuyer(adminId,buyerId); return adminDAO.getBuyerStats(buyer.getId()); }
    public List<com.fakrudeenmart.model.Order> getBuyerOrders(Long adminId, Long buyerId) { User buyer=getBuyer(adminId,buyerId); return adminDAO.findBuyerOrders(buyer.getId()); }
    public SellerStats getSellerStats(Long adminId, Long sellerId) { User seller=getSeller(adminId,sellerId); return sellerDAO.getStats(seller.getId()); }
    public List<Product> getSellerProducts(Long adminId, Long sellerId) { User seller=getSeller(adminId,sellerId); return sellerDAO.findProductsBySellerId(seller.getId()); }
    public List<SellerOrder> getSellerOrders(Long adminId, Long sellerId) { User seller=getSeller(adminId,sellerId); return sellerDAO.findOrdersBySellerId(seller.getId()); }

    public User updateProfile(Long adminId, Long userId, String fullName, String email, String phone, UserRole expectedRole) { assertAdmin(adminId); User existing = adminDAO.findUser(validId(userId,"User was not found."), expectedRole).orElseThrow(() -> new AdminException("User was not found.")); validateProfile(fullName,email); adminDAO.updateProfile(existing.getId(), fullName.trim(), email.trim(), phone == null ? "" : phone.trim()); return adminDAO.findUser(existing.getId(), expectedRole).orElseThrow(() -> new AdminException("User was not found.")); }
    public User updateBuyerProfile(Long adminId, Long buyerId, String name, String email, String phone) { return updateProfile(adminId,buyerId,name,email,phone,UserRole.BUYER); }
    public User updateSellerProfile(Long adminId, Long sellerId, String name, String email, String phone) { return updateProfile(adminId,sellerId,name,email,phone,UserRole.SELLER); }

    public void updateUserStatus(Long adminId, Long userId, UserStatus status) { User admin=assertAdmin(adminId); User target=adminDAO.findUser(validId(userId,"User was not found."),null).orElseThrow(() -> new AdminException("User was not found.")); if (status==null) throw new AdminException("Account status is invalid."); if (target.getId().equals(admin.getId()) && status==UserStatus.INACTIVE) throw new AdminException("You cannot deactivate your own administrator account."); if (target.getRole()==UserRole.ADMIN && status==UserStatus.INACTIVE && adminDAO.findUsers(null,UserRole.ADMIN,UserStatus.ACTIVE,20).size() <= 1) throw new AdminException("At least one active administrator must remain."); if (!adminDAO.updateStatus(target.getId(),status)) throw new AdminException("User was not found."); }
    public void updateBuyerStatus(Long adminId, Long buyerId, UserStatus status) { User buyer=getBuyer(adminId,buyerId); updateUserStatus(adminId,buyer.getId(),status); }
    public void updateSellerStatus(Long adminId, Long sellerId, UserStatus status) { User seller=getSeller(adminId,sellerId); updateUserStatus(adminId,seller.getId(),status); }

    public List<Product> getProducts(Long adminId, String search, Long categoryId, Long sellerId, ProductStatus status, Boolean inStock) { assertAdmin(adminId); return adminDAO.findProducts(search,categoryId,sellerId,status,inStock,300); }
    public Product getProduct(Long adminId, Long productId) { assertAdmin(adminId); return adminDAO.findProduct(validId(productId,"Product was not found.")).orElseThrow(() -> new AdminException("Product was not found.")); }
    public Product updateProduct(Long adminId, Long productId, Long categoryId, String name, String description, BigDecimal price, int stock, String imageUrl, ProductStatus status) { assertAdmin(adminId); Product existing=getProduct(adminId,productId); if(name==null||name.trim().isEmpty()||name.trim().length()>160) throw new AdminException("Product name is required and must be at most 160 characters."); if(description==null||description.trim().isEmpty()||description.trim().length()>2000) throw new AdminException("Product description is required and must be at most 2000 characters."); if(categoryId==null||categoryService.getCategoryById(categoryId).isEmpty()) throw new AdminException("Select a valid category."); if(price==null||price.signum()<=0||price.scale()>2) throw new AdminException("Price must be greater than zero with at most two decimals."); if(stock<0) throw new AdminException("Stock cannot be negative."); if(status==null) throw new AdminException("Product status is invalid."); existing.setCategoryId(categoryId); existing.setName(name.trim()); existing.setDescription(description.trim()); existing.setPrice(price); existing.setStockQuantity(stock); existing.setImageUrl(imageUrl==null?"":imageUrl.trim()); existing.setStatus(status); adminDAO.updateProduct(existing); return existing; }
    public void updateProductStatus(Long adminId, Long productId, ProductStatus status) { assertAdmin(adminId); if(status==null) throw new AdminException("Product status is invalid."); getProduct(adminId,productId); if(!adminDAO.updateProductStatus(productId,status)) throw new AdminException("Product was not found."); }

    public List<AdminOrder> getOrders(Long adminId, String search, OrderStatus status) { assertAdmin(adminId); return adminDAO.findOrders(search,status,300); }
    public AdminOrder getOrder(Long adminId, Long orderId) { assertAdmin(adminId); return adminDAO.findOrder(validId(orderId,"Order was not found.")).orElseThrow(() -> new AdminException("Order was not found.")); }
    public void updateOrderStatus(Long adminId, Long orderId, OrderStatus target) { assertAdmin(adminId); AdminOrder current=getOrder(adminId,orderId); if(target==null||!allowed(current.getStatus(),target)) throw new AdminException("That order status transition is not allowed."); if(!adminDAO.updateOrderStatus(orderId,target)) throw new AdminException("Order was not found."); }
    private boolean allowed(OrderStatus from, OrderStatus to) { return (from==OrderStatus.PENDING&&to==OrderStatus.CONFIRMED)||(from==OrderStatus.CONFIRMED&&to==OrderStatus.PROCESSING)||(from==OrderStatus.PROCESSING&&to==OrderStatus.SHIPPED)||(from==OrderStatus.SHIPPED&&to==OrderStatus.DELIVERED); }
    private void validateProfile(String name,String email) { if(name==null||name.trim().isEmpty()||name.trim().length()>120) throw new AdminException("Name is required and must be at most 120 characters."); if(email==null||!email.contains("@")||email.trim().length()>255) throw new AdminException("Enter a valid email address."); }
    private long validId(Long id,String message) { if(id==null||id<=0) throw new AdminException(message); return id; }
    public static class AdminException extends RuntimeException { public AdminException(String message){super(message);} }
}
