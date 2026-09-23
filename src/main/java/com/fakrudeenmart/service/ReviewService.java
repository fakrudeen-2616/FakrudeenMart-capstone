package com.fakrudeenmart.service;

import com.fakrudeenmart.dao.DataAccessException;
import com.fakrudeenmart.dao.OrderDAO;
import com.fakrudeenmart.dao.ReviewDAO;
import com.fakrudeenmart.model.Product;
import com.fakrudeenmart.model.Review;
import com.fakrudeenmart.model.ReviewSummary;
import com.fakrudeenmart.model.User;
import com.fakrudeenmart.model.UserRole;
import com.fakrudeenmart.model.UserStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class ReviewService {
    public static final int MAX_COMMENT_LENGTH = 2000;
    private final ReviewDAO reviewDAO; private final OrderDAO orderDAO; private final ProductService productService; private final UserService userService;
    public ReviewService() { this(new ReviewDAO(), new OrderDAO(), new ProductService(), new UserService()); }
    public ReviewService(ReviewDAO reviewDAO, OrderDAO orderDAO, ProductService productService, UserService userService) { this.reviewDAO=reviewDAO;this.orderDAO=orderDAO;this.productService=productService;this.userService=userService; }

    public Review createReview(Long buyerId, Long productId, int rating, String comment) { User buyer=activeBuyer(buyerId); Product product=product(productId); if(!orderDAO.hasBuyerPurchasedProduct(buyer.getId(),product.getId()))throw new ReviewException("You must purchase this product before reviewing it."); if(reviewDAO.hasUserReviewedProduct(buyer.getId(),product.getId()))throw new ReviewException("You have already reviewed this product."); validateRating(rating); String validComment=validateComment(comment); Review review=new Review();review.setUserId(buyer.getId());review.setProductId(product.getId());review.setRating(rating);review.setComment(validComment);try{return reviewDAO.createReview(review);}catch(DataAccessException e){if(isDuplicate(e))throw new ReviewException("You have already reviewed this product.");throw e;} }
    public Review updateReview(Long buyerId, Long reviewId, int rating, String comment) { User buyer=activeBuyer(buyerId); if(reviewId==null||reviewId<=0)throw new ReviewException("Review not found."); Review existing=reviewDAO.findById(reviewId).orElseThrow(()->new ReviewException("Review not found.")); if(!buyer.getId().equals(existing.getUserId()))throw new ReviewException("You are not authorized to edit this review.");validateRating(rating);String valid=validateComment(comment);reviewDAO.updateReview(reviewId,buyer.getId(),rating,valid);return reviewDAO.findById(reviewId).orElseThrow(()->new ReviewException("Review not found.")); }
    public void deleteReview(Long buyerId, Long reviewId) { User buyer=activeBuyer(buyerId); if(reviewId==null||reviewId<=0)throw new ReviewException("Review not found."); Review existing=reviewDAO.findById(reviewId).orElseThrow(()->new ReviewException("Review not found."));if(!buyer.getId().equals(existing.getUserId()))throw new ReviewException("You are not authorized to delete this review.");reviewDAO.deleteReview(reviewId,buyer.getId()); }
    public List<Review> getProductReviews(Long productId, String sort, int page, int pageSize) { product(productId);if(page<1)throw new ReviewException("Page must be positive.");return reviewDAO.findByProductId(productId,sort,page,pageSize); }
    public ReviewSummary getProductRatingSummary(Long productId, Long viewerId) { product(productId);ReviewSummary summary=new ReviewSummary();summary.setAverageRating(reviewDAO.getAverageRating(productId));summary.setReviewCount(reviewDAO.getReviewCount(productId));if(viewerId!=null){User user=userService.findById(viewerId).orElse(null);if(user!=null&&user.getRole()==UserRole.BUYER&&user.getStatus()==UserStatus.ACTIVE){summary.setPurchased(orderDAO.hasBuyerPurchasedProduct(viewerId,productId));summary.setUserReview(reviewDAO.findByUserIdAndProductId(viewerId,productId).orElse(null));}}return summary; }
    public boolean canUserReviewProduct(Long buyerId, Long productId) { ReviewSummary s=getProductRatingSummary(productId,buyerId);return s.isPurchased()&&!s.isReviewed(); }
    public Review getOwnedReview(Long buyerId, Long reviewId) { activeBuyer(buyerId);Review review=reviewDAO.findById(reviewId).orElseThrow(()->new ReviewException("Review not found."));if(!buyerId.equals(review.getUserId()))throw new ReviewException("You are not authorized to edit this review.");return review; }
    private User activeBuyer(Long id){if(id==null||id<=0)throw new ReviewException("Please login as a buyer to review products.");User user=userService.findById(id).orElseThrow(()->new ReviewException("Please login as a buyer to review products."));if(user.getRole()!=UserRole.BUYER||user.getStatus()!=UserStatus.ACTIVE)throw new ReviewException("Only active buyers can review products.");return user;}
    private Product product(Long id){if(id==null||id<=0)throw new ReviewException("Product not found.");return productService.getProductById(id).orElseThrow(()->new ReviewException("Product not found."));}
    private void validateRating(int rating){if(rating<1||rating>5)throw new ReviewException("Rating must be between 1 and 5.");}
    private String validateComment(String comment){if(comment==null||comment.trim().isEmpty())throw new ReviewException("Please enter a valid review.");String value=comment.trim();if(value.length()>MAX_COMMENT_LENGTH)throw new ReviewException("Review must be 2000 characters or fewer.");return value;}
    private boolean isDuplicate(Throwable error){Throwable t=error;while(t!=null){if(t instanceof java.sql.SQLException sql&&"23505".equals(sql.getSQLState()))return true;t=t.getCause();}return false;}
    public static class ReviewException extends RuntimeException { public ReviewException(String message){super(message);} }
}
