package com.fakrudeenmart.dao;

import com.fakrudeenmart.model.Review;
import com.fakrudeenmart.util.DatabaseConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReviewDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReviewDAO.class);
    private final DataSource dataSource;
    public ReviewDAO() { this(DatabaseConnectionManager.getInstance().getDataSource()); }
    public ReviewDAO(DataSource dataSource) { this.dataSource = dataSource; }

    public Review createReview(Review review) {
        String sql = "INSERT INTO reviews (user_id, product_id, rating, comment, created_at, updated_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        try (Connection c=dataSource.getConnection(); PreparedStatement s=c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) { s.setLong(1,review.getUserId()); s.setLong(2,review.getProductId()); s.setInt(3,review.getRating()); s.setString(4,review.getComment()); s.executeUpdate(); try(ResultSet keys=s.getGeneratedKeys()){if(!keys.next())throw new DataAccessException("Review was created without an id",null);review.setId(keys.getLong(1));return review;} } catch(SQLException e){LOGGER.error("Unable to create review",e);throw new DataAccessException("Unable to create review",e);}
    }
    public Optional<Review> findById(Long id) { return findOne(" WHERE r.id = ?", id); }
    public Optional<Review> findByUserIdAndProductId(Long userId, Long productId) { String sql=select()+" WHERE r.user_id = ? AND r.product_id = ?"; try(Connection c=dataSource.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,userId);s.setLong(2,productId);try(ResultSet r=s.executeQuery()){return r.next()?Optional.of(map(r)):Optional.empty();}}catch(SQLException e){throw failure("Unable to find review",e);} }
    public List<Review> findByProductId(Long productId, String sort, int page, int pageSize) { List<Review> reviews=new ArrayList<>(); String order=sortSql(sort);String sql=select()+" WHERE r.product_id = ? ORDER BY "+order+" LIMIT ? OFFSET ?"; try(Connection c=dataSource.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,productId);s.setInt(2,Math.max(1,Math.min(pageSize,100)));s.setInt(3,Math.max(0,page-1)*Math.max(1,Math.min(pageSize,100)));try(ResultSet r=s.executeQuery()){while(r.next())reviews.add(map(r));}return reviews;}catch(SQLException e){throw failure("Unable to load reviews",e);} }
    public boolean hasUserReviewedProduct(Long userId, Long productId) { return findByUserIdAndProductId(userId,productId).isPresent(); }
    public void updateReview(Long reviewId, Long userId, int rating, String comment) { String sql="UPDATE reviews SET rating=?, comment=?, updated_at=CURRENT_TIMESTAMP WHERE id=? AND user_id=?"; try(Connection c=dataSource.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,rating);s.setString(2,comment);s.setLong(3,reviewId);s.setLong(4,userId);if(s.executeUpdate()!=1)throw new DataAccessException("Review was not found",null);}catch(SQLException e){throw failure("Unable to update review",e);} }
    public void deleteReview(Long reviewId, Long userId) { String sql="DELETE FROM reviews WHERE id=? AND user_id=?"; try(Connection c=dataSource.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,reviewId);s.setLong(2,userId);if(s.executeUpdate()!=1)throw new DataAccessException("Review was not found",null);}catch(SQLException e){throw failure("Unable to delete review",e);} }
    public BigDecimal getAverageRating(Long productId) { String sql="SELECT AVG(rating) FROM reviews WHERE product_id=?";try(Connection c=dataSource.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,productId);try(ResultSet r=s.executeQuery()){r.next();return r.getBigDecimal(1);}}catch(SQLException e){throw failure("Unable to calculate rating",e);} }
    public int getReviewCount(Long productId) { String sql="SELECT COUNT(*) FROM reviews WHERE product_id=?";try(Connection c=dataSource.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,productId);try(ResultSet r=s.executeQuery()){r.next();return r.getInt(1);}}catch(SQLException e){throw failure("Unable to count reviews",e);} }
    private Optional<Review> findOne(String suffix, Long id) { String sql=select()+suffix;try(Connection c=dataSource.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,id);try(ResultSet r=s.executeQuery()){return r.next()?Optional.of(map(r)):Optional.empty();}}catch(SQLException e){throw failure("Unable to find review",e);} }
    private String select(){return "SELECT r.id,r.user_id,r.product_id,r.rating,r.comment,r.created_at,r.updated_at,u.full_name AS reviewer_name FROM reviews r JOIN users u ON u.id=r.user_id";}
    private String sortSql(String sort){return "OLDEST".equals(sort)?"r.created_at ASC, r.id ASC":"HIGHEST".equals(sort)?"r.rating DESC, r.created_at DESC":"LOWEST".equals(sort)?"r.rating ASC, r.created_at DESC":"r.created_at DESC, r.id DESC";}
    private Review map(ResultSet r)throws SQLException{Review v=new Review();v.setId(r.getLong("id"));v.setUserId(r.getLong("user_id"));v.setProductId(r.getLong("product_id"));v.setRating(r.getInt("rating"));v.setComment(r.getString("comment"));v.setReviewerName(r.getString("reviewer_name"));Timestamp c=r.getTimestamp("created_at"),u=r.getTimestamp("updated_at");v.setCreatedAt(c==null?null:c.toLocalDateTime());v.setUpdatedAt(u==null?null:u.toLocalDateTime());return v;}
    private DataAccessException failure(String message, SQLException e){LOGGER.error(message,e);return new DataAccessException(message,e);}
}
