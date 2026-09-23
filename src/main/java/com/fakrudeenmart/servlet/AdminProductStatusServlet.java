package com.fakrudeenmart.servlet;
import com.fakrudeenmart.service.AdminService;
import javax.servlet.http.*; import java.io.IOException;
public class AdminProductStatusServlet extends HttpServlet { private AdminService service; public void init(){service=new AdminService();} protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws IOException{try{service.updateProductStatus(AdminServletSupport.adminId(req),AdminServletSupport.id(req.getParameter("id"),"Product was not found."),AdminServletSupport.productStatus(req.getParameter("status")));resp.sendRedirect(req.getContextPath()+"/admin/products");}catch(Exception e){resp.sendError(HttpServletResponse.SC_FORBIDDEN);}}}
