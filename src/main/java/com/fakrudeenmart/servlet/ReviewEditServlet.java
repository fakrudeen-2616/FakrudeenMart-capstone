package com.fakrudeenmart.servlet;
import com.fakrudeenmart.service.ReviewService;
import javax.servlet.ServletException; import javax.servlet.http.*; import java.io.IOException;
public class ReviewEditServlet extends HttpServlet { private ReviewService service; public void init(){service=new ReviewService();} protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{try{req.setAttribute("review",service.getOwnedReview(ReviewServletSupport.userId(req),ReviewServletSupport.reviewId(req.getParameter("id"))));req.getRequestDispatcher("/WEB-INF/views/review-edit.jsp").forward(req,resp);}catch(ReviewService.ReviewException e){resp.sendError(HttpServletResponse.SC_FORBIDDEN,e.getMessage());}}}
