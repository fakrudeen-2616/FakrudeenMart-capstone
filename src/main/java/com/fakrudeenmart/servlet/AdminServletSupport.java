package com.fakrudeenmart.servlet;

import com.fakrudeenmart.model.OrderStatus;
import com.fakrudeenmart.model.ProductStatus;
import com.fakrudeenmart.model.UserRole;
import com.fakrudeenmart.model.UserStatus;
import com.fakrudeenmart.service.AdminService;
import com.fakrudeenmart.util.SessionUtil;
import javax.servlet.http.HttpServletRequest;

final class AdminServletSupport {
    private AdminServletSupport() {}
    static Long adminId(HttpServletRequest request) { return SessionUtil.getCurrentUserId(request); }
    static long id(String raw, String message) { try { long id=Long.parseLong(raw==null?"":raw); if(id>0)return id; } catch(NumberFormatException ignored){} throw new AdminService.AdminException(message); }
    static UserRole role(String raw) { if(raw==null||raw.isBlank())return null; try{return UserRole.valueOf(raw);}catch(IllegalArgumentException e){throw new AdminService.AdminException("Role filter is invalid.");} }
    static UserStatus userStatus(String raw) { if(raw==null||raw.isBlank())return null; try{return UserStatus.valueOf(raw);}catch(IllegalArgumentException e){throw new AdminService.AdminException("Account status is invalid.");} }
    static ProductStatus productStatus(String raw) { if(raw==null||raw.isBlank())return null; try{return ProductStatus.valueOf(raw);}catch(IllegalArgumentException e){throw new AdminService.AdminException("Product status is invalid.");} }
    static OrderStatus orderStatus(String raw) { if(raw==null||raw.isBlank())return null; try{return OrderStatus.valueOf(raw);}catch(IllegalArgumentException e){throw new AdminService.AdminException("Order status is invalid.");} }
    static String message(Exception e,String fallback){return e instanceof AdminService.AdminException&&e.getMessage()!=null?e.getMessage():fallback;}
}
