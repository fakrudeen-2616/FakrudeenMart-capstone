<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Create account | FakrudeenMart</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body class="auth-page">
<main class="auth-shell">
    <a class="brand" href="${pageContext.request.contextPath}/">FAKRUDEENMART</a>
    <section class="auth-card">
        <p class="eyebrow">Join the marketplace</p>
        <h1>Create your account</h1>
        <c:if test="${not empty error}"><p class="notice error">${error}</p></c:if>
        <form method="post" action="${pageContext.request.contextPath}/register">
            <label for="fullName">Full name</label>
            <input id="fullName" name="fullName" type="text" value="<c:out value='${fullName}'/>" required autocomplete="name">
            <label for="email">Email</label>
            <input id="email" name="email" type="email" value="<c:out value='${email}'/>" required autocomplete="email">
            <label for="phone">Phone <span class="muted">(optional)</span></label>
            <input id="phone" name="phone" type="tel" value="<c:out value='${phone}'/>" autocomplete="tel">
            <label for="password">Password</label>
            <input id="password" name="password" type="password" required minlength="6" autocomplete="new-password">
            <label for="confirmPassword">Confirm password</label>
            <input id="confirmPassword" name="confirmPassword" type="password" required minlength="6" autocomplete="new-password">
            <button type="submit">Create account</button>
        </form>
        <p class="auth-footer">Already have an account? <a href="${pageContext.request.contextPath}/login">Login</a></p>
    </section>
</main>
<script src="${pageContext.request.contextPath}/js/app.js" defer></script></body>
</html>
