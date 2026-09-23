package com.fakrudeenmart;

import com.fakrudeenmart.dao.CategoryDAO;
import com.fakrudeenmart.dao.CartDAO;
import com.fakrudeenmart.dao.CartItemDAO;
import com.fakrudeenmart.dao.OrderDAO;
import com.fakrudeenmart.dao.OrderItemDAO;
import com.fakrudeenmart.dao.ProductDAO;
import com.fakrudeenmart.dao.ReviewDAO;
import com.fakrudeenmart.dao.UserDAO;
import com.fakrudeenmart.model.Product;
import com.fakrudeenmart.model.ProductStatus;
import com.fakrudeenmart.model.Review;
import com.fakrudeenmart.model.User;
import com.fakrudeenmart.model.UserRole;
import com.fakrudeenmart.model.UserStatus;
import com.fakrudeenmart.service.CartService;
import com.fakrudeenmart.service.OrderService;
import com.fakrudeenmart.service.ProductService;
import com.fakrudeenmart.service.ReviewService;
import com.fakrudeenmart.service.UserService;
import com.fakrudeenmart.util.DatabaseConnectionManager;
import com.fakrudeenmart.util.DatabaseInitializer;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

class ReviewIntegrationTest {
    @Test
    void onlyBuyerWhoPurchasedCanCreateAndDuplicateIsRejected() {
        try (HikariDataSource ds=dataSource("jdbc:h2:mem:reviews_rules;DB_CLOSE_DELAY=-1")) {
            DatabaseInitializer.initialize(ds); UserDAO users=new UserDAO(ds); User buyer=users.createUser(new User("Review Buyer","review-buyer@example.com","hash",null,UserRole.BUYER,UserStatus.ACTIVE)); User other=users.createUser(new User("Other Buyer","other-buyer@example.com","hash",null,UserRole.BUYER,UserStatus.ACTIVE)); User seller=users.findByEmail("catalog-seller-1@fakrudeenmart.local").orElseThrow(); Product product=product(ds); ReviewService reviews=reviewService(ds,users); buy(ds,users,buyer,product); Review created=reviews.createReview(buyer.getId(),product.getId(),5,"Excellent product <script>ignored as text</script>"); assertNotNull(created.getId()); assertThrows(ReviewService.ReviewException.class,()->reviews.createReview(buyer.getId(),product.getId(),4,"Duplicate")); assertThrows(ReviewService.ReviewException.class,()->reviews.createReview(other.getId(),product.getId(),5,"No purchase")); assertThrows(ReviewService.ReviewException.class,()->reviews.createReview(seller.getId(),product.getId(),5,"Seller cannot review")); assertThrows(ReviewService.ReviewException.class,()->reviews.createReview(buyer.getId(),product.getId(),6,"Bad rating")); assertThrows(ReviewService.ReviewException.class,()->reviews.createReview(buyer.getId(),product.getId(),5," "));
        }
    }

    @Test
    void reviewOwnershipAndCrudAreEnforced() {
        try (HikariDataSource ds=dataSource("jdbc:h2:mem:reviews_ownership;DB_CLOSE_DELAY=-1")) {
            DatabaseInitializer.initialize(ds); UserDAO users=new UserDAO(ds); User buyer=users.createUser(new User("Owner Buyer","owner-buyer@example.com","hash",null,UserRole.BUYER,UserStatus.ACTIVE)); User other=users.createUser(new User("Other Owner","other-owner@example.com","hash",null,UserRole.BUYER,UserStatus.ACTIVE)); Product product=product(ds); ReviewService reviews=reviewService(ds,users); buy(ds,users,buyer,product); Review created=reviews.createReview(buyer.getId(),product.getId(),4,"Good and useful"); assertEquals("Good and useful",reviews.getOwnedReview(buyer.getId(),created.getId()).getComment()); assertThrows(ReviewService.ReviewException.class,()->reviews.updateReview(other.getId(),created.getId(),5,"Hijack")); assertThrows(ReviewService.ReviewException.class,()->reviews.deleteReview(other.getId(),created.getId())); Review updated=reviews.updateReview(buyer.getId(),created.getId(),5,"Updated review"); assertEquals(5,updated.getRating()); reviews.deleteReview(buyer.getId(),created.getId()); assertTrue(reviews.getProductReviews(product.getId(),"NEWEST",1,20).isEmpty());
        }
    }

    @Test
    void ratingAverageAndCountUseDatabaseValues() {
        try (HikariDataSource ds=dataSource("jdbc:h2:mem:reviews_average;DB_CLOSE_DELAY=-1")) {
            DatabaseInitializer.initialize(ds); UserDAO users=new UserDAO(ds); Product product=product(ds); ReviewService reviews=reviewService(ds,users); for(int i=1;i<=3;i++){User buyer=users.createUser(new User("Average Buyer "+i,"average-"+i+"@example.com","hash",null,UserRole.BUYER,UserStatus.ACTIVE)); buy(ds,users,buyer,product); reviews.createReview(buyer.getId(),product.getId(),i==1?5:i==2?4:3,"Rating "+i);} assertEquals(3,reviews.getProductRatingSummary(product.getId(),null).getReviewCount()); assertEquals(new BigDecimal("4.0"),reviews.getProductRatingSummary(product.getId(),null).getAverageRating().setScale(1)); assertEquals(3,reviews.getProductReviews(product.getId(),"LOWEST",1,20).get(0).getRating());
        }
    }

    private Product product(HikariDataSource ds){return new ProductDAO(ds).findAll(1,24).getItems().stream().filter(p->p.getStatus()==ProductStatus.ACTIVE&&p.getStockQuantity()>3).findFirst().orElseThrow();}
    private ReviewService reviewService(HikariDataSource ds,UserDAO users){return new ReviewService(new ReviewDAO(ds),new OrderDAO(ds),new ProductService(new ProductDAO(ds),new CategoryDAO(ds),users),new UserService(users));}
    private void buy(HikariDataSource ds,UserDAO users,User buyer,Product product){CartDAO cart=new CartDAO(ds);CartItemDAO items=new CartItemDAO(ds);ProductDAO products=new ProductDAO(ds);UserService userService=new UserService(users);ProductService productService=new ProductService(products,new CategoryDAO(ds),users);new CartService(cart,items,productService,userService).addToCart(buyer.getId(),product.getId(),1);new OrderService(ds,cart,items,products,new OrderDAO(ds),new OrderItemDAO(ds),userService).placeOrder(buyer.getId());}
    private HikariDataSource dataSource(String url){Properties p=new Properties();p.setProperty("database.url",url);p.setProperty("database.driver","org.h2.Driver");p.setProperty("database.username","sa");p.setProperty("database.password","");p.setProperty("database.pool.maximum-size","8");p.setProperty("database.pool.minimum-idle","1");p.setProperty("database.pool.connection-timeout-ms","5000");return DatabaseConnectionManager.createDataSource(p);}
}
