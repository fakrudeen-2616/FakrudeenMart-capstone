package com.fakrudeenmart.servlet;
import com.fakrudeenmart.service.AdminService;
import javax.servlet.ServletException; import javax.servlet.http.*; import java.io.IOException;
public class AdminStatisticsServlet extends HttpServlet { private AdminService service; public void init(){service=new AdminService();} protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{req.setAttribute("stats",service.getDashboardStatistics(AdminServletSupport.adminId(req)));req.getRequestDispatcher("/WEB-INF/views/admin/statistics.jsp").forward(req,resp);} }
