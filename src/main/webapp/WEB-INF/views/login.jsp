<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Sign in | FakrudeenMart</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body class="auth-page">
<main class="auth-shell">
    <a class="brand" href="${pageContext.request.contextPath}/">FAKRUDEENMART</a>
    <section class="auth-card">
        <p class="eyebrow">Welcome back</p>
        <h1>Sign in to your account</h1>
        <c:if test="${not empty message}"><p class="notice success">${message}</p></c:if>
        <c:if test="${param.loggedOut == '1'}"><p class="notice success">You have been signed out.</p></c:if>
        <c:if test="${not empty error}"><p class="notice error">${error}</p></c:if>
        <form method="post" action="${pageContext.request.contextPath}/login">
            <label for="email">Email</label>
            <input id="email" name="email" type="email" value="<c:out value='${email}'/>" required autocomplete="email">
            <label for="password">Password</label>
            <input id="password" name="password" type="password" required autocomplete="current-password">
            <button type="submit">Login</button>
        </form>
        <p class="auth-footer">Don't have an account? <a href="${pageContext.request.contextPath}/register">Create an account</a></p>
    </section>
</main>
<script src="${pageContext.request.contextPath}/js/app.js" defer></script></body>
</html>
