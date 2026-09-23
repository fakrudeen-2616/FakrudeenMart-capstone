package com.fakrudeenmart.model;

import java.time.LocalDateTime;

public class Review {
    private Long id; private Long userId; private Long productId; private int rating; private String comment; private LocalDateTime createdAt; private LocalDateTime updatedAt; private String reviewerName;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;}
    public Long getProductId(){return productId;} public void setProductId(Long v){productId=v;}
    public int getRating(){return rating;} public void setRating(int v){rating=v;}
    public String getComment(){return comment;} public void setComment(String v){comment=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
    public String getReviewerName(){return reviewerName;} public void setReviewerName(String v){reviewerName=v;}
}
