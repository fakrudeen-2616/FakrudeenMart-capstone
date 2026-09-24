<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Seller Profile | FakrudeenMart</title><link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css"></head>
<body><main class="seller-shell">
<header class="site-header"><a class="brand" href="${pageContext.request.contextPath}/seller/dashboard">FAKRUDEENMART</a><nav><a href="${pageContext.request.contextPath}/seller/dashboard">Dashboard</a><a href="${pageContext.request.contextPath}/seller/products">Products</a><a href="${pageContext.request.contextPath}/seller/orders">Orders</a><span class="muted">Profile</span><a href="${pageContext.request.contextPath}/logout">Logout</a></nav></header>
<section class="form-heading"><p class="eyebrow">Account</p><h1>Seller profile</h1><p class="catalog-intro">Update your public contact details without exposing account credentials.</p></section>
<c:if test="${not empty error}"><p class="notice error"><c:out value="${error}"/></p></c:if><c:if test="${not empty message}"><p class="notice success"><c:out value="${message}"/></p></c:if>
<section class="seller-form-card"><form method="post" action="${pageContext.request.contextPath}/seller/profile"><label for="fullName">Name</label><input id="fullName" name="fullName" maxlength="120" required value="<c:out value='${seller.fullName}'/>"/><label for="email">Email</label><input id="email" value="<c:out value='${seller.email}'/>" readonly/><label for="phone">Phone</label><input id="phone" name="phone" maxlength="30" value="<c:out value='${seller.phone}'/>"/><button type="submit">Save profile</button></form></section>
<section class="profile-card"><dl><dt>Role</dt><dd><c:out value="${seller.role}"/></dd><dt>Account status</dt><dd><c:out value="${seller.status}"/></dd><dt>Registered</dt><dd><c:out value="${seller.createdAt}"/></dd></dl></section>
</main><script src="${pageContext.request.contextPath}/js/app.js" defer></script></body></html>
