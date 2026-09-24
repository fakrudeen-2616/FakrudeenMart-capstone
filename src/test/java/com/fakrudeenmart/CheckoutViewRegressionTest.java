package com.fakrudeenmart;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckoutViewRegressionTest {
    private static final Path CHECKOUT_VIEW = Path.of("src/main/webapp/WEB-INF/views/checkout.jsp");

    @Test
    void checkoutViewUsesTomcatCompatibleBooleanAttribute() throws IOException {
        String source = Files.readString(CHECKOUT_VIEW);
        assertTrue(source.contains("${checkoutEmpty}"));
        assertFalse(source.contains("${empty checkout or checkout.empty}"));
        assertFalse(source.contains("${empty checkout || checkout.empty}"));
        assertFalse(source.contains("${checkout.empty}"));
    }
}
