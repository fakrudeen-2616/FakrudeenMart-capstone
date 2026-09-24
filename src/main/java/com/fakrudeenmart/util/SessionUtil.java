package com.fakrudeenmart.util;

import com.fakrudeenmart.model.User;
import com.fakrudeenmart.model.UserRole;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public final class SessionUtil {
    public static final String USER_ID = "userId";
    public static final String USER_NAME = "userName";
    public static final String USER_EMAIL = "userEmail";
    public static final String USER_ROLE = "userRole";

    private SessionUtil() {
    }

    public static void establish(HttpServletRequest request, User user) {
        HttpSession existing = request.getSession(false);
        if (existing != null) {
            existing.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(USER_ID, user.getId());
        session.setAttribute(USER_NAME, user.getFullName());
        session.setAttribute(USER_EMAIL, user.getEmail());
        session.setAttribute(USER_ROLE, user.getRole().name());
    }

    public static void refreshIdentity(HttpServletRequest request, User user) {
        HttpSession session = request.getSession(false);
        if (session == null || user == null) return;
        session.setAttribute(USER_ID, user.getId());
        session.setAttribute(USER_NAME, user.getFullName());
        session.setAttribute(USER_EMAIL, user.getEmail());
        session.setAttribute(USER_ROLE, user.getRole().name());
    }

    public static boolean isLoggedIn(HttpServletRequest request) {
        return request.getSession(false) != null && getCurrentUserId(request) != null;
    }

    public static Long getCurrentUserId(HttpServletRequest request) {
        Object value = sessionValue(request, USER_ID);
        return value instanceof Long ? (Long) value : value instanceof Number ? ((Number) value).longValue() : null;
    }

    public static String getCurrentUserName(HttpServletRequest request) {
        Object value = sessionValue(request, USER_NAME);
        return value == null ? null : value.toString();
    }

    public static UserRole getCurrentUserRole(HttpServletRequest request) {
        Object value = sessionValue(request, USER_ROLE);
        if (value == null) return null;
        try {
            return UserRole.valueOf(value.toString());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public static boolean hasRole(HttpServletRequest request, UserRole role) {
        return role != null && role == getCurrentUserRole(request);
    }

    private static Object sessionValue(HttpServletRequest request, String key) {
        HttpSession session = request.getSession(false);
        return session == null ? null : session.getAttribute(key);
    }
}
