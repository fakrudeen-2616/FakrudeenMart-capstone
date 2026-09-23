package com.fakrudeenmart.servlet;
import com.fakrudeenmart.service.AdminService;
import javax.servlet.ServletException; import javax.servlet.http.*; import java.io.IOException;
public class AdminOrderListServlet extends HttpServlet { private AdminService service; public void init(){service=new AdminService();} protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{req.setAttribute("orders",service.getOrders(AdminServletSupport.adminId(req),req.getParameter("search"),AdminServletSupport.orderStatus(req.getParameter("status"))));req.getRequestDispatcher("/WEB-INF/views/admin/orders.jsp").forward(req,resp);} }
