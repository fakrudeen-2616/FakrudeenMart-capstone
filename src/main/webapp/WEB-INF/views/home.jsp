<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>FakrudeenMart | Thoughtful shopping</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<main class="site-shell">
    <header class="site-header">
        <a class="brand" href="${pageContext.request.contextPath}/">FAKRUDEENMART</a>
        <nav aria-label="Primary navigation">
            <a href="${pageContext.request.contextPath}/products">Products</a>
            <c:choose>
                <c:when test="${not empty sessionScope.userId}">
                    <c:if test="${sessionScope.userRole == 'BUYER'}"><a href="${pageContext.request.contextPath}/cart">Cart</a><a href="${pageContext.request.contextPath}/orders">Orders</a></c:if>
                    <c:if test="${sessionScope.userRole == 'SELLER'}"><a href="${pageContext.request.contextPath}/seller/dashboard">Seller dashboard</a></c:if>
                    <c:if test="${sessionScope.userRole == 'ADMIN'}"><a href="${pageContext.request.contextPath}/admin/dashboard">Admin dashboard</a></c:if>
                    <span class="muted"><c:out value="${sessionScope.userName}"/></span>
                    <a href="${pageContext.request.contextPath}/logout">Logout</a>
                </c:when>
                <c:otherwise><a href="${pageContext.request.contextPath}/login">Login</a><a class="nav-cta" href="${pageContext.request.contextPath}/register">Create account</a></c:otherwise>
            </c:choose>
        </nav>
    </header>

    <section class="home-search" aria-label="Search the marketplace">
        <form method="get" action="${pageContext.request.contextPath}/products" role="search">
            <label class="sr-only" for="home-search-input">Search products</label>
            <input id="home-search-input" name="search" type="search" placeholder="Search products, categories, and more...">
            <button type="submit">Search</button>
        </form>
    </section>

    <c:if test="${not empty error}"><p class="notice error"><c:out value="${error}"/></p></c:if>

    <section class="hero hero-home">
        <div class="hero-copy">
            <p class="eyebrow">A considered marketplace</p>
            <h1>Discover useful products from independent sellers.</h1>
            <p>Browse a focused catalog, shop with confidence, and keep every order and review in one clear place.</p>
            <div class="hero-actions"><a class="primary-link" href="${pageContext.request.contextPath}/products">Shop the catalog</a><a class="secondary-link" href="#categories">Browse categories</a></div>
        </div>
        <div class="hero-panel" aria-label="Marketplace highlights"><span class="hero-panel-kicker">Built for clarity</span><strong>Find. Compare. Order.</strong><p>Product details, availability, ratings, cart, checkout, and order history work together in one lightweight experience.</p></div>
    </section>

    <section id="categories" class="home-section" aria-labelledby="category-heading">
        <div class="section-heading"><div><p class="eyebrow">Shop by interest</p><h2 id="category-heading">Explore categories</h2></div><a class="text-link" href="${pageContext.request.contextPath}/products">View all products →</a></div>
        <div class="category-grid">
            <c:forEach var="category" items="${categories}"><a class="category-card" href="${pageContext.request.contextPath}/products?category=${category.id}"><strong><c:out value="${category.name}"/></strong><span>Browse collection →</span></a></c:forEach>
        </div>
    </section>

    <section class="home-section" aria-labelledby="featured-heading">
        <div class="section-heading"><div><p class="eyebrow">From the catalog</p><h2 id="featured-heading">Featured products</h2></div><a class="text-link" href="${pageContext.request.contextPath}/products">See the full catalog →</a></div>
        <c:choose>
            <c:when test="${empty featuredProducts}"><section class="empty-state"><h2>No products found.</h2><p>Check back soon for new marketplace arrivals.</p></section></c:when>
            <c:otherwise><section class="product-grid home-product-grid"><c:forEach var="product" items="${featuredProducts}"><article class="product-card"><a class="product-image-link" href="${pageContext.request.contextPath}/product?id=${product.id}"><img class="product-image" src="<c:out value='${product.imageUrl}'/>" alt="<c:out value='${product.name}'/>" onerror="this.onerror=null;this.src='${pageContext.request.contextPath}/images/product-placeholder.svg';"></a><div class="product-card-body"><p class="product-category"><c:out value="${product.categoryName}"/></p><h2><c:out value="${product.name}"/></h2><p class="product-price">₹<fmt:formatNumber value="${product.price}" minFractionDigits="2" maxFractionDigits="2"/></p><p class="stock-label ${product.inStock ? 'in-stock' : 'out-stock'}"><c:choose><c:when test="${product.inStock}">In stock · ${product.stockQuantity}</c:when><c:otherwise>Out of stock</c:otherwise></c:choose></p><a class="card-link" href="${pageContext.request.contextPath}/product?id=${product.id}">View details →</a><c:if test="${sessionScope.userRole == 'BUYER' && product.inStock}"><form class="quick-add-form" method="post" action="${pageContext.request.contextPath}/cart/add"><input type="hidden" name="productId" value="${product.id}"><input type="hidden" name="quantity" value="1"><button type="submit">Add to cart</button></form></c:if></div></article></c:forEach></section></c:otherwise>
        </c:choose>
    </section>

    <section class="home-value-grid" aria-label="Marketplace benefits"><article><span class="value-number">01</span><h2>Explore with intent</h2><p>Search and filter the live catalog by category, price, stock, and sorting options.</p><a href="${pageContext.request.contextPath}/products">Browse products →</a></article><article><span class="value-number">02</span><h2>Buy with confidence</h2><p>Review server-calculated cart totals, stock availability, and order snapshots before placing an order.</p><a href="${pageContext.request.contextPath}/products">Start shopping →</a></article><article><span class="value-number">03</span><h2>Share experience</h2><p>Verified buyers can leave ratings and reviews after purchasing a product.</p><a href="${pageContext.request.contextPath}/products">See the catalog →</a></article></section>
</main>
<script src="${pageContext.request.contextPath}/js/app.js" defer></script>
</body>
</html>
