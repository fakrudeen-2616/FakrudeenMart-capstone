package com.fakrudeenmart.model;

import java.math.BigDecimal;

public class ReviewSummary { private BigDecimal averageRating; private int reviewCount; private boolean purchased; private Review userReview;
    public BigDecimal getAverageRating(){return averageRating;} public void setAverageRating(BigDecimal v){averageRating=v;}
    public int getReviewCount(){return reviewCount;} public void setReviewCount(int v){reviewCount=v;}
    public boolean isPurchased(){return purchased;} public void setPurchased(boolean v){purchased=v;}
    public Review getUserReview(){return userReview;} public void setUserReview(Review v){userReview=v;}
    public boolean isReviewed(){return userReview!=null;}
}
