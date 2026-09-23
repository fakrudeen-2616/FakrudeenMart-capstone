package com.fakrudeenmart.servlet;
import com.fakrudeenmart.service.AdminService;
import javax.servlet.ServletException; import javax.servlet.http.*; import java.io.IOException;
public class AdminDashboardServlet extends HttpServlet { private AdminService service; public void init(){service=new AdminService();} protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{Long id=AdminServletSupport.adminId(req);req.setAttribute("admin",service.getAdminProfile(id));req.setAttribute("stats",service.getDashboardStatistics(id));req.getRequestDispatcher("/WEB-INF/views/admin/dashboard.jsp").forward(req,resp);} }
