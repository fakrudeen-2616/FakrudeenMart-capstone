package com.fakrudeenmart.servlet;

import com.fakrudeenmart.model.ReviewSummary;
import com.fakrudeenmart.service.ProductService;
import com.fakrudeenmart.service.ReviewService;
import com.fakrudeenmart.util.SessionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ProductDetailsServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductDetailsServlet.class);
    private ProductService productService; private ReviewService reviewService;
    @Override public void init() { productService = new ProductService(); reviewService = new ReviewService(); }
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String value=request.getParameter("id");
        try {
            long id=Long.parseLong(value==null?"":value); if(id<=0)throw new NumberFormatException();
            productService.getProductById(id).ifPresentOrElse(product->{
                request.setAttribute("product",product); String sort=request.getParameter("reviewSort");
                request.setAttribute("reviews",reviewService.getProductReviews(id,sort,1,20));
                request.setAttribute("reviewSummary",reviewService.getProductRatingSummary(id,SessionUtil.getCurrentUserId(request)));
            },()->request.setAttribute("unavailableMessage","This product is currently unavailable."));
        } catch (IllegalArgumentException exception) { LOGGER.debug("Invalid product id supplied: {}",value); request.setAttribute("unavailableMessage","Product not found."); }
        request.setAttribute("reviewMessage",request.getParameter("reviewMessage")); request.setAttribute("reviewError",request.getParameter("reviewError"));
        request.getRequestDispatcher("/WEB-INF/views/product-details.jsp").forward(request,response);
    }
}
