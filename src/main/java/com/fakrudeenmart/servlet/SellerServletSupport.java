package com.fakrudeenmart.servlet;

import com.fakrudeenmart.model.ProductStatus;
import com.fakrudeenmart.service.SellerService;
import com.fakrudeenmart.util.SessionUtil;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

final class SellerServletSupport {
    private SellerServletSupport() {}
    static Long sellerId(HttpServletRequest request) { return SessionUtil.getCurrentUserId(request); }
    static long id(String value, String message) { try { long id = Long.parseLong(value == null ? "" : value); if (id > 0) return id; } catch (NumberFormatException ignored) {} throw new SellerService.SellerException(message); }
    static int stock(String value) { try { int stock = Integer.parseInt(value == null ? "" : value); if (stock >= 0) return stock; } catch (NumberFormatException ignored) {} throw new SellerService.SellerException("Stock must be a non-negative whole number."); }
    static BigDecimal price(String value) { try { return new BigDecimal(value == null ? "" : value); } catch (NumberFormatException ignored) { throw new SellerService.SellerException("Price must be a valid number."); } }
    static ProductStatus status(String value) { try { return ProductStatus.valueOf(value == null ? "" : value); } catch (IllegalArgumentException ignored) { throw new SellerService.SellerException("Product status is invalid."); } }
    static String message(Exception e, String fallback) { return e instanceof SellerService.SellerException && e.getMessage() != null ? e.getMessage() : fallback; }
}
