package com.fakrudeenmart;

import com.fakrudeenmart.servlet.RoleLoginServlet;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RoutingMappingRegressionTest {
    @Test
    void canonicalAndRoleSpecificAuthenticationRoutesAreMapped() throws Exception {
        String webXml = Files.readString(Path.of("src/main/webapp/WEB-INF/web.xml"));
        RoleLoginServlet.class.getConstructor();
        assertTrue(webXml.contains("<url-pattern>/login</url-pattern>"));
        assertTrue(webXml.contains("<url-pattern>/buyer/login</url-pattern>"));
        assertTrue(webXml.contains("<url-pattern>/seller/login</url-pattern>"));
        assertTrue(webXml.contains("<url-pattern>/admin/login</url-pattern>"));
        assertTrue(webXml.contains("<url-pattern>/register</url-pattern>"));
        assertTrue(webXml.contains("<url-pattern>/buyer/register</url-pattern>"));
    }
}
