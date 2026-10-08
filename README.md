# FakrudeenMart — Java 17 Multi-Vendor Marketplace

FakrudeenMart is a Java 17 multi-vendor e-commerce application built with Servlets, JSP, JDBC, HikariCP, and H2. The application includes the complete buyer, seller, administrator, catalog, cart, checkout, orders, reviews, UI polish, QA, and Railway deployment-readiness work completed across Modules 1–10.

**Live deployment:** https://fakrudeenmart-capstone-production.up.railway.app/

The project preserves the original server-rendered architecture. It was not migrated to Spring Boot, Node.js, React, PostgreSQL, or another framework.

## Technology and architecture

Java 17, Maven, Apache Tomcat 9, Java Servlets, JSP, JSTL, JDBC, H2, HikariCP, Gson, jBCrypt, JUnit 5, Mockito, SLF4J, and Logback.

```text
JSP + JSTL → Servlet → Service → DAO → JDBC → HikariCP → H2
```

The application reuses the existing `users`, `categories`, `products`, `orders`, and `order_items` tables. Module 8 reuses the existing `reviews` table and adds only a migration-safe `updated_at` column; no duplicate review or product tables were introduced.

## Build

From the project root with Java 17:

```bash
mvn clean test
mvn package
```

The generated WAR is `target/fakrudeenmart.war`.

### Railway deployment

The repository includes a Java 17/Tomcat 9 Docker deployment for Railway:

```text
Dockerfile → Maven WAR build → Tomcat 9 ROOT.war → Railway $PORT
```

The deployment uses the existing file-based H2 database. A Railway Volume mounted at `/app/data` is required for persistence across redeployments.

## Admin authorization

Every `/admin/*` route is protected by the existing `AuthenticationFilter`. The server reads the authenticated user ID and role from the HTTP session; request parameters, hidden fields, cookies, and JavaScript values are never used as the authorization source.

Unauthenticated users are redirected to `/login`. Buyers and sellers receive HTTP 403. Only an active `ADMIN` session can use the admin service. The service layer repeats the active-admin check so direct servlet or service calls cannot bypass the route boundary.

The admin account cannot deactivate itself. Deactivation of an administrator is also rejected when it would leave the platform without an active administrator. Normal user-management forms do not expose role or password fields, and password hashes are never rendered in JSP pages.

The default seeded administrator login email is `faxz3142@gmail.com`. The password is stored as a BCrypt hash and is intentionally not included in this README.

## Admin routes and workflow

| URL | Purpose | Access |
| --- | --- | --- |
| `/admin/dashboard` | Real user, product, order, and sales overview | Active ADMIN |
| `/admin/statistics` | Detailed platform statistics | Active ADMIN |
| `/admin/users` | All-user search and role/status filtering | Active ADMIN |
| `/admin/buyers` | Buyer-only search, filtering, and account actions | Active ADMIN |
| `/admin/buyers/view?id={id}` | Buyer profile, order history, and buyer statistics | Active ADMIN |
| `/admin/buyers/edit?id={id}` | Edit buyer name, email, and phone | Active ADMIN |
| `/admin/sellers` | Seller-only search, filtering, and account actions | Active ADMIN |
| `/admin/sellers/view?id={id}` | Seller profile, products, orders, and statistics | Active ADMIN |
| `/admin/sellers/edit?id={id}` | Edit seller name, email, and phone | Active ADMIN |
| `/admin/users/status` | Activate or deactivate a user account | Active ADMIN, POST |
| `/admin/products` | Platform-wide product search and filters | Active ADMIN |
| `/admin/products/edit?id={id}` | Platform-wide product edit | Active ADMIN |
| `/admin/products/status` | Activate or deactivate any product | Active ADMIN, POST |
| `/admin/orders` | Platform-wide order search and status filters | Active ADMIN |
| `/admin/orders/view?id={id}` | Buyer, seller, item-snapshot, and total details | Active ADMIN |
| `/admin/orders/status` | Controlled order-status transition | Active ADMIN, POST |
| `/admin/profile` | Read-only authenticated admin profile | Active ADMIN |

The workflow is: sign in through the existing login system, open the admin dashboard, use buyer/seller/user lists to search and filter accounts, inspect profiles and related records, manage platform products, and review or advance orders through the existing status workflow.

## Buyer and seller management

Buyer and seller lists are filtered in SQL with prepared statements by name, email, phone, and account status. Admin profile editing is deliberately limited to name, email, and phone. Account activation/deactivation updates the existing `ACTIVE` or `INACTIVE` status and preserves users, products, orders, order items, and historical sales.

Buyer profiles show order count, non-cancelled spending, and order history. Seller profiles show product counts, active and out-of-stock products, seller-relevant order counts, seller-owned products, seller-relevant order items, and seller sales. Existing seller ownership rules remain unchanged for seller routes; admin access is separately platform-wide.

## Product and order management

Admin product queries support search by product, seller, or category, plus category, seller, status, and stock filters. Admin edits preserve the seller relationship and validate name, description, category, positive two-decimal price, non-negative stock, image URL, and status values. Product deactivation is a status change, not deletion.

Admin order queries support order ID, buyer name, buyer email, and status filters. Order details use the historical `order_items.product_name`, `unit_price`, `quantity`, and `subtotal` snapshots, so changing a current product price does not rewrite historical order information. Admin order transitions reuse the established workflow:

```text
PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
```

Arbitrary transitions and a second status system were not introduced.

## Platform statistics

Dashboard and statistics values are calculated from the database, including total/buyer/seller/active/inactive users, total/active/inactive/out-of-stock products, order counts by status, and platform sales. Platform sales use stored historical order totals and exclude cancelled orders consistently with seller statistics.

## Security

All admin search and filter values use prepared statements. IDs are parsed and validated server-side, records are checked for existence, and invalid or missing objects produce safe errors. JSP output uses JSTL/EL with escaped user-generated values. No passwords or BCrypt hashes are exposed to the UI or logs.

## Testing and verification

The full regression suite contains **46 passing tests**:

- Admin-only route authorization for unauthenticated, buyer, seller, and admin sessions
- Buyer and seller search, filtering, profile retrieval, editing, activation, and deactivation
- Admin self-deactivation and last-active-admin safeguards
- Platform-wide product search, filtering, editing, activation, and deactivation
- Seller ownership preservation during admin product edits
- Platform order search, details, historical snapshots, status transitions, and buyer order viewing
- Database-backed user, product, order, and sales statistics
- Existing authentication, catalog, cart, checkout, buyer-order, and seller-management regression coverage
- Review CRUD, purchase eligibility, duplicate prevention, ownership protection, sorting, rating averages, and catalog rating summaries

```text
mvn clean test → PASS (46 tests, 0 failures)
mvn package    → PASS
```

Tomcat 9 deployment was smoke-tested locally, including canonical buyer login, role-specific buyer/seller/admin login routes, seeded seller authentication, and seeded administrator authentication. The Railway deployment was also verified through its public health, buyer login, seller login, seller dashboard, and administrator dashboard routes.

## Module boundaries

Module 9 stops after UI/integration polish. Payments, wishlists, coupons, chat, notifications, delivery tracking, recommendation features, AI features, and Module 10 are intentionally not implemented.

**MODULE 9 — COMPLETE UI, INTEGRATION & POLISH COMPLETE**

## Reviews & Ratings

Buyers can view review lists and rating summaries on the existing product-details page. Product cards also display average rating and review count using correlated aggregate columns in the existing catalog query, avoiding a separate review query for each product.

The review workflow uses these routes:

| URL | Purpose | Access |
| --- | --- | --- |
| `/product?id={productId}` | Product details, rating summary, and review list | Public |
| `/reviews/create` | Create a purchased-product review | Active BUYER, POST |
| `/reviews/edit?id={reviewId}` | Edit the current buyer's review | Review owner, GET |
| `/reviews/update` | Save an owned review update | Review owner, POST |
| `/reviews/delete` | Delete an owned review | Review owner, POST |

A review requires an active authenticated buyer, an existing active product, and a non-cancelled order containing that product. Cart contents, URL values, hidden user IDs, and client-side eligibility flags are never used as purchase evidence. Review creation accepts only the product ID, rating, and comment; the buyer ID always comes from the authenticated session.

Ratings are integers from 1 to 5. Review comments are required, trimmed, and limited to 2,000 characters. The database retains the unique `(user_id, product_id)` constraint, and the service also checks for duplicates before insertion. Database duplicate errors are converted into a user-friendly message. Buyers can edit or delete only their own reviews, with server-side ownership checks on every operation.

Reviews are displayed using JSTL/EL escaping, so review comments and reviewer names are not rendered as arbitrary HTML. Guests see a login prompt, buyers who have not purchased see an eligibility message, eligible buyers see the review form, and buyers with an existing review see an edit action. Review sorting supports newest, oldest, highest-rated, and lowest-rated database ordering.

Average ratings and review counts are calculated from the `reviews` table. Empty products show a clear “No reviews yet” state rather than a misleading zero-star rating. No seller review impersonation, admin moderation system, or separate review frontend was introduced.

## Module 8 database change

The existing `reviews` table already provided foreign keys to `users` and `products`, a 1–5 rating check, and a unique buyer/product constraint. Module 8 adds:

```sql
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
```

The existing order tables are used for purchase validation, excluding cancelled orders while preserving all historical order and review data.

## Module 8 security and limitations

Review mutations are protected in the service layer because review display remains public. The service verifies active buyer role, product existence, purchase history, duplicate state, rating/comment validity, and review ownership. There is no CSRF mechanism in the pre-existing Servlet application; review POST routes remain compatible with that architecture and rely on server-side authorization and ownership validation.

## Module 9 — Complete UI, Integration & Polish

Module 9 preserves the existing Servlet/JSP/JSTL → Service → DAO → JDBC → HikariCP → H2 architecture and focuses on the presentation layer. The application now uses a consistent visual system with shared palette variables, typography fallbacks, spacing, focus states, buttons, alerts, status badges, cards, responsive tables, empty states, and footer treatment.

The homepage now presents FakrudeenMart as a multi-vendor marketplace with clear catalog, account, buyer, seller, and admin entry points. Existing product, review, cart, checkout, order, seller, and admin pages retain their server-rendered data and routes while receiving consistent spacing, hierarchy, responsive layouts, table overflow behavior, form controls, status styling, and semantic action links.

A lightweight `app.js` enhancement layer provides:

- Responsive collapsible navigation for existing site and admin headers
- Role-aware buyer Home and Orders links where applicable
- Dismissible alert controls
- Semantic status badge classes
- Catalog Clear filters link using the existing `/products` endpoint
- Native number-input convenience validation
- Shared footer insertion without duplicating large JSP blocks
- Reduced-motion awareness and keyboard-visible focus support

Friendly 403, 404, and 500 pages were added through `web.xml` error mappings. These pages avoid exposing stack traces, SQL statements, internal paths, or sensitive implementation details. Review comments, product names, profile values, and other user-controlled text continue to use JSTL/EL escaping in existing views.

The checkout remains a college MVP flow: totals are calculated server-side, no payment credentials are collected, and no real payment gateway was added. Existing buyer, seller, admin, ownership, stock, order-snapshot, review-purchase, unique-review, and authorization rules remain backend-enforced.

## Module 9 limitations

The project still has no real payment gateway or CSRF framework because those were outside the existing MVP architecture and Module 9 explicitly prohibits adding new business infrastructure. The deployed application was smoke-tested through the key authentication, storefront, seller, admin, checkout, and health routes.

## Module 10 — Final QA, Bug Fixes & Deployment Readiness

Module 10 performed a limited final QA pass without changing the architecture or adding new features. The existing Java 17, Maven, Tomcat 9, Servlet/JSP/JSTL, JDBC, HikariCP, H2, jBCrypt, JUnit 5, Mockito, SLF4J, and Logback stack remains unchanged.

The audit verified authentication, role authorization, seller ownership, buyer cart and order ownership, review purchase eligibility and ownership, checkout transaction rollback, server-side prices and stock checks, historical order snapshots, prepared JDBC queries, schema relationships, HikariCP initialization, WAR integrity, and secret-file exposure. The only confirmed application defect was unsafe rendering of user-controlled values in several JSP HTML attributes and product-description textareas. These values now use JSTL `c:out` escaping in the affected catalog, product, authentication, seller, and admin forms.

Final verification:

```text
mvn clean test → PASS (46 tests, 0 failures)
mvn package    → PASS
WAR integrity  → PASS
```

The final WAR is `target/fakrudeenmart.war`. Local Tomcat 9 and the public Railway deployment were verified after the final routing and administrator-seed fixes.

**MODULE 10 — FINAL QA COMPLETE**
