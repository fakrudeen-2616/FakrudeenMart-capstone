package com.fakrudeenmart;

import com.fakrudeenmart.dao.CategoryDAO;
import com.fakrudeenmart.dao.ProductDAO;
import com.fakrudeenmart.dao.UserDAO;
import com.fakrudeenmart.model.CatalogOptions;
import com.fakrudeenmart.model.Product;
import com.fakrudeenmart.model.User;
import com.fakrudeenmart.model.UserRole;
import com.fakrudeenmart.model.UserStatus;
import com.fakrudeenmart.service.ProductService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductServiceTest {
    @Test
    void invalidPaginationAndPriceRangesAreRejected() {
        ProductService service = new ProductService(mock(ProductDAO.class), mock(CategoryDAO.class), mock(UserDAO.class));
        ProductDAO.CatalogFilter invalidPage = new ProductDAO.CatalogFilter(null, null, null, null,
                CatalogOptions.StockFilter.ALL, CatalogOptions.SortOption.NEWEST, 0, 12);
        assertThrows(IllegalArgumentException.class, () -> service.filterProducts(invalidPage));
        ProductDAO.CatalogFilter invalidPrice = new ProductDAO.CatalogFilter(null, null, new BigDecimal("20"), new BigDecimal("10"),
                CatalogOptions.StockFilter.ALL, CatalogOptions.SortOption.NEWEST, 1, 12);
        assertThrows(IllegalArgumentException.class, () -> service.filterProducts(invalidPrice));
    }

    @Test
    void nonSellerCannotOwnAProductAndInvalidProductDataIsRejected() {
        ProductDAO productDAO = mock(ProductDAO.class);
        CategoryDAO categoryDAO = mock(CategoryDAO.class);
        UserDAO userDAO = mock(UserDAO.class);
        ProductService service = new ProductService(productDAO, categoryDAO, userDAO);
        Product product = new Product(null, 1L, "Test product", "Description", new BigDecimal("10"), 2, null, null);
        User buyer = new User("Buyer", "buyer@example.com", "hash", null, UserRole.BUYER, UserStatus.ACTIVE);
        buyer.setId(1L);
        assertThrows(IllegalArgumentException.class, () -> service.create(product, buyer));
        Product invalid = new Product(null, 1L, "", "Description", new BigDecimal("-1"), -1, null, null);
        assertThrows(IllegalArgumentException.class, () -> service.create(invalid, buyer));
        verify(productDAO, never()).create(any(Product.class));
    }

    @Test
    void validSellerOwnershipUsesAuthenticatedSellerId() {
        ProductDAO productDAO = mock(ProductDAO.class);
        CategoryDAO categoryDAO = mock(CategoryDAO.class);
        UserDAO userDAO = mock(UserDAO.class);
        when(categoryDAO.findById(1L)).thenReturn(Optional.of(new com.fakrudeenmart.model.Category("Electronics", "")));
        when(productDAO.create(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        User seller = new User("Seller", "seller@example.com", "hash", null, UserRole.SELLER, UserStatus.ACTIVE);
        seller.setId(42L);
        when(userDAO.findById(42L)).thenReturn(Optional.of(seller));
        Product product = new Product(null, 1L, "Test product", "Description", new BigDecimal("10"), 2, null, null);
        Product created = service(productDAO, categoryDAO, userDAO).create(product, seller);
        org.junit.jupiter.api.Assertions.assertEquals(42L, created.getSellerId());
    }

    private ProductService service(ProductDAO dao, CategoryDAO categories, UserDAO users) { return new ProductService(dao, categories, users); }
}
