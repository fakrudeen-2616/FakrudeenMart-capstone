package com.fakrudeenmart.servlet;
import com.fakrudeenmart.service.AdminService;
import javax.servlet.ServletException; import javax.servlet.http.*; import java.io.IOException;
public class AdminOrderDetailsServlet extends HttpServlet { private AdminService service; public void init(){service=new AdminService();} protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{try{req.setAttribute("order",service.getOrder(AdminServletSupport.adminId(req),AdminServletSupport.id(req.getParameter("id"),"Order was not found.")));req.getRequestDispatcher("/WEB-INF/views/admin/order-details.jsp").forward(req,resp);}catch(AdminService.AdminException e){resp.sendError(HttpServletResponse.SC_NOT_FOUND);}}}
