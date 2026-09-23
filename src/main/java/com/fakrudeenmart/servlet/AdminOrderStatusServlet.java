package com.fakrudeenmart.servlet;
import com.fakrudeenmart.service.AdminService;
import javax.servlet.http.*; import java.io.IOException;
public class AdminOrderStatusServlet extends HttpServlet { private AdminService service; public void init(){service=new AdminService();} protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws IOException{try{long id=AdminServletSupport.id(req.getParameter("id"),"Order was not found.");service.updateOrderStatus(AdminServletSupport.adminId(req),id,AdminServletSupport.orderStatus(req.getParameter("status")));resp.sendRedirect(req.getContextPath()+"/admin/orders/view?id="+id);}catch(Exception e){resp.sendError(HttpServletResponse.SC_FORBIDDEN);}}}
